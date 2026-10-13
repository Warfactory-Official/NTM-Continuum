package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineSolderingStation;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import dev.golem.core.world.ItemStacks;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.SolderingRecipes.SolderingRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Soldering station: a multiblock that solders the higher-tier circuits from lower
 * circuits + lead fine-wire (+ a fluid at higher tiers). Auto-matches its recipe from the six grouped input
 * slots (3 toppings / 2 pcb / 1 solder), so no selector/blueprint is needed. Power + one receiving fluid
 * tank (typed by a {@code fluid_identifier} in slot 8).
 *
 * <p>Slot roles: 0–2 toppings, 3–4 pcb, 5 solder, 6 output (take-only), 7 battery, 8 fluid identifier,
 * 9–10 upgrades (SPEED/POWER/OVERDRIVE, 1.12.2 red/blue/black formulas). Slim port (deferred): the
 * looping sound, the Tau particle effect, the floating output-item render, and the {@code collisionPrevention}
 * redstone toggle.
 */
public class TileEntityMachineSolderingStation extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2, WorldlyContainer, IUpgradeInfoProvider {

    private static final int SLOTS = 11;
    private static final int[] IO_SLOTS = {0, 1, 2, 3, 4, 5, 6};

    public long power;
    public long maxPower = 2_000;
    public long consumption;
    public int progress;
    public int processTime = 1;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 8_000);
    private SolderingRecipe recipe;
    /** The matched recipe's output, synced to the client for the floating-item render (like the 1.12.2 tile). */
    public ItemStack display = ItemStack.EMPTY;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineSolderingStation(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SOLDERING_STATION.get(), pos, state, SLOTS);
        SolderingRecipes.register(); // idempotent; registries are flushed by now
    }

    @Override
    public String getDefaultName() {
        return "container.machine_soldering_station";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSolderingStation te) {
        te.power = Library.chargeTEFromItems(te.inventory, 7, te.getPower(), te.getMaxPower());
        te.updateTankType();

        if (level.getGameTime() % 20 == 0) {
            for (DirPos con : te.getConPos()) {
                te.trySubscribe(level, con);
                if (te.tank.getTankType() != Fluids.NONE) te.trySubscribe(te.tank.getTankType(), level, con);
            }
        }

        te.recipe = SolderingRecipes.getRecipe(new ItemStack[]{
                te.inventory.getStackInSlot(0), te.inventory.getStackInSlot(1), te.inventory.getStackInSlot(2),
                te.inventory.getStackInSlot(3), te.inventory.getStackInSlot(4), te.inventory.getStackInSlot(5)});
        te.display = te.recipe != null ? te.recipe.output : ItemStack.EMPTY;

        // Adopt the matched recipe's fluid type when the tank has none, so the machine can subscribe to and
        // pull that fluid from the network (it picks its fluid from the recipe, like the chemical plant, so no
        // fluid identifier is needed). A tank already typed (slot-8 identifier, or an earlier recipe) is left as-is.
        if (te.recipe != null && te.recipe.fluid != null && te.tank.getTankType() == Fluids.NONE) {
            te.tank.setTankType(te.recipe.fluid.type);
        }


        // Upgrades (1.12.2 red/blue/black formulas): SPEED (red) -duration/6 & +100% power per level;
        // POWER (blue) +duration/3 & -power/6 per level; OVERDRIVE (black) ×2 power & +1 progress/tick.
        te.upgradeManager.checkSlots(te.inventory, 9, 10);
        int redLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int blueLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 3);
        int blackLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);

        long intendedMaxPower;
        if (te.recipe != null) {
            te.processTime = te.recipe.duration - (te.recipe.duration * redLevel / 6) + (te.recipe.duration * blueLevel / 3);
            te.consumption = te.recipe.consumption + (te.recipe.consumption * redLevel) - (te.recipe.consumption * blueLevel / 6);
            te.consumption *= (long) Math.pow(2, blackLevel);
            intendedMaxPower = te.consumption * 20;

            if (te.canProcess(te.recipe)) {
                te.progress += (1 + blackLevel);
                te.power -= te.consumption;
                if (te.progress >= te.processTime) {
                    te.progress = 0;
                    te.consumeItems(te.recipe);
                    if (te.inventory.getStackInSlot(6).isEmpty()) {
                        te.inventory.setStackInSlot(6, te.recipe.output.copy());
                    } else {
                        te.inventory.getStackInSlot(6).grow(te.recipe.output.getCount());
                    }
                    te.setChanged();
                }
            } else {
                te.progress = 0;
            }
        } else {
            te.progress = 0;
            te.consumption = 100;
            intendedMaxPower = 2_000;
        }
        te.maxPower = Math.max(intendedMaxPower, te.power);
        te.networkPackNT();
    }

    public boolean canProcess(SolderingRecipe recipe) {
        if (this.power < this.consumption) return false;
        if (recipe.fluid != null) {
            if (this.tank.getTankType() != recipe.fluid.type) return false;
            if (this.tank.getFill() < recipe.fluid.fill) return false;
        }
        ItemStack out = inventory.getStackInSlot(6);
        if (!out.isEmpty()) {
            if (!ItemStacks.sameItemAndData(out, recipe.output)) return false;
            if (out.getCount() + recipe.output.getCount() > out.getMaxStackSize()) return false;
        }
        return true;
    }

    private void consumeItems(SolderingRecipe recipe) {
        consumeGroup(recipe.toppings, 0, 3);
        consumeGroup(recipe.pcb, 3, 5);
        consumeGroup(recipe.solder, 5, 6);
        if (recipe.fluid != null) this.tank.setFill(tank.getFill() - recipe.fluid.fill);
    }

    private void consumeGroup(AStack[] group, int from, int to) {
        for (AStack aStack : group) {
            for (int i = from; i < to; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (aStack.matchesRecipe(stack, true) && stack.getCount() >= aStack.stacksize) {
                    stack.shrink(aStack.stacksize);
                    break;
                }
            }
        }
    }

    /** Slot 8 holds a fluid identifier; adopt its type (kept in-slot, matching the 1.12.2 {@code setType(8, ...)}). */
    private void updateTankType() {
        ItemStack id = inventory.getStackInSlot(8);
        if (id.getItem() instanceof IItemFluidId ident) {
            var t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
            if (t != tank.getTankType()) tank.setTankType(t);
        }
    }

    /** The 8 network-connection positions around the footprint, facing-dependent (ported as-is). */
    public DirPos[] getConPos() {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + dir.offsetX, y, z + dir.offsetZ, dir),
                new DirPos(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 2, y, z - dir.offsetZ * 2, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
                new DirPos(x - rot.offsetX, y, z - rot.offsetZ, rot.getOpposite()),
                new DirPos(x - dir.offsetX - rot.offsetX, y, z - dir.offsetZ - rot.offsetZ, rot.getOpposite()),
                new DirPos(x + rot.offsetX * 2, y, z + rot.offsetZ * 2, rot),
                new DirPos(x - dir.offsetX + rot.offsetX * 2, y, z - dir.offsetZ + rot.offsetZ * 2, rot),
        };
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 3) return validForGroup(slot, 0, 3, SolderingRecipes.toppings, stack);
        if (slot < 5) return validForGroup(slot, 3, 5, SolderingRecipes.pcb, stack);
        if (slot < 6) return validForGroup(slot, 5, 6, SolderingRecipes.solder, stack);
        if (slot == 7) return Library.isBattery(stack);
        if (slot == 8) return stack.getItem() instanceof IItemFluidId;
        if (slot == 9 || slot == 10) return Library.isMachineUpgrade(stack); // upgrades (any type, like SlotUpgrade)
        return false; // 6 output
    }

    /** Valid if some group ingredient matches AND no other slot in the group already holds the same item. */
    private boolean validForGroup(int slot, int from, int to, Set<AStack> group, ItemStack stack) {
        for (int i = from; i < to; i++) {
            if (i != slot && !inventory.getStackInSlot(i).isEmpty()
                    && ItemStacks.sameItemAndData(inventory.getStackInSlot(i), stack)) return false;
        }
        for (AStack t : group) if (t.matchesRecipe(stack, true)) return true;
        return false;
    }

    // WorldlyContainer: hoppers/pipes feed the inputs, pull the output

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot <= 5 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 6; }

    // IUpgradeInfoProvider (text ported from the 1.12.2 provideInfo)

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{9, 10};
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_soldering_station.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 100 / 6) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 100 / 6) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    // IEnergyReceiverMK2

    @Override public long getPower() { return Math.max(Math.min(power, maxPower), 0); }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }

    // IFluidStandardReceiverMK2

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    // MenuProvider

    @Override public Component getDisplayName() { return Component.translatable("block.hbm.machine_soldering_station"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineSolderingStation(containerId, playerInventory, this);
    }

    // Sync (BufPacket) + persistence

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        tank.serialize(buf);
        ItemStacks.writeOptional(buf, display);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        progress = buf.readInt();
        processTime = buf.readInt();
        tank.deserialize(buf);
        display = ItemStacks.readOptional(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        output.putInt("progress", progress);
        output.putInt("processTime", processTime);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 2_000L);
        progress = input.getIntOr("progress", 0);
        processTime = input.getIntOr("processTime", 1);
        tank.readFromNBT(input, "tank");
    }
}
