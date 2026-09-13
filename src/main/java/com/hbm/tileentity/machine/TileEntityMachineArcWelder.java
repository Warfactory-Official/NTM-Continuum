package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineArcWelder;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes.ArcWelderRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
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

import java.util.HashMap;
import java.util.List;

/** The eight-slot Arc Welder core, ported from the 1.12.2 machine without particle substitutions. */
public class TileEntityMachineArcWelder extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2, WorldlyContainer,
        IUpgradeInfoProvider {

    public static final int SLOTS = 8;
    private static final int[] IO_SLOTS = {0, 1, 2, 3};

    public long power;
    public long maxPower = 2_000;
    public long consumption;
    public int progress;
    public int processTime = 1;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 24_000);
    public ItemStack display = ItemStack.EMPTY;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    private ArcWelderRecipe recipe;

    public TileEntityMachineArcWelder(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ARC_WELDER.get(), pos, state, SLOTS);
        ArcWelderRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.machineArcWelder";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineArcWelder te) {
        te.power = Library.chargeTEFromItems(te.inventory, 4, te.getPower(), te.getMaxPower());
        te.updateTankType();


        te.recipe = ArcWelderRecipes.getRecipe(
                te.inventory.getStackInSlot(0), te.inventory.getStackInSlot(1), te.inventory.getStackInSlot(2));

        te.upgradeManager.checkSlots(te.inventory, 6, 7);
        int speed = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerUpgrade = te.upgradeManager.getLevel(UpgradeType.POWER);
        int overdrive = te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        long intendedMaxPower;
        if (te.recipe != null) {
            te.processTime = te.recipe.duration - (te.recipe.duration * speed / 6)
                    + (te.recipe.duration * powerUpgrade / 3);
            te.consumption = te.recipe.consumption + (te.recipe.consumption * speed)
                    - (te.recipe.consumption * powerUpgrade / 6);
            te.consumption *= (long) Math.pow(2, overdrive);
            intendedMaxPower = te.consumption * 20;

            if (te.canProcess(te.recipe)) {
                te.progress += 1 + overdrive;
                te.power -= te.consumption;
                if (te.progress >= te.processTime) {
                    te.progress = 0;
                    te.consumeItems(te.recipe);
                    if (te.inventory.getStackInSlot(3).isEmpty()) {
                        te.inventory.setStackInSlot(3, te.recipe.output.copy());
                    } else {
                        te.inventory.getStackInSlot(3).grow(te.recipe.output.getCount());
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

        te.display = te.recipe == null ? ItemStack.EMPTY : te.recipe.output;
        te.maxPower = Math.max(intendedMaxPower, te.power);
        te.networkPackNT();
    }

    private void updateTankType() {
        ItemStack identifier = inventory.getStackInSlot(5);
        if (identifier.getItem() instanceof IItemFluidId fluidId) {
            FluidType type = fluidId.getType(null, 0, 0, 0, identifier);
            if (tank.getTankType() != type) tank.setTankType(type);
        }
    }

    public boolean canProcess(ArcWelderRecipe recipe) {
        if (power < consumption) return false;

        if (recipe.fluid != null) {
            if (tank.getTankType() != recipe.fluid.type) return false;
            if (tank.getFill() < recipe.fluid.fill) return false;
        }

        ItemStack output = inventory.getStackInSlot(3);
        if (!output.isEmpty()) {
            if (!ItemStacks.sameItemAndData(output, recipe.output)) return false;
            if (output.getCount() + recipe.output.getCount() > output.getMaxStackSize()) return false;
        }
        return true;
    }

    public void consumeItems(ArcWelderRecipe recipe) {
        for (AStack ingredient : recipe.ingredients) {
            for (int slot = 0; slot < 3; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (ingredient.matchesRecipe(stack, true) && stack.getCount() >= ingredient.stacksize) {
                    stack.shrink(ingredient.stacksize);
                    break;
                }
            }
        }
        if (recipe.fluid != null) tank.setFill(tank.getFill() - recipe.fluid.fill);
    }

    /** The ten old proxy positions, in their original order and facing. */
    public DirPos[] getConPos() {
        ForgeDirection direction = ForgeDirection.getOrientation(
                getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
        ForgeDirection rotation = direction.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + direction.offsetX, y, z + direction.offsetZ, direction),
                new DirPos(x + direction.offsetX + rotation.offsetX, y,
                        z + direction.offsetZ + rotation.offsetZ, direction),
                new DirPos(x + direction.offsetX - rotation.offsetX, y,
                        z + direction.offsetZ - rotation.offsetZ, direction),
                new DirPos(x - direction.offsetX * 2, y, z - direction.offsetZ * 2, direction.getOpposite()),
                new DirPos(x - direction.offsetX * 2 + rotation.offsetX, y,
                        z - direction.offsetZ * 2 + rotation.offsetZ, direction.getOpposite()),
                new DirPos(x - direction.offsetX * 2 - rotation.offsetX, y,
                        z - direction.offsetZ * 2 - rotation.offsetZ, direction.getOpposite()),
                new DirPos(x + rotation.offsetX * 2, y, z + rotation.offsetZ * 2, rotation),
                new DirPos(x - direction.offsetX + rotation.offsetX * 2, y,
                        z - direction.offsetZ + rotation.offsetZ * 2, rotation),
                new DirPos(x - rotation.offsetX * 2, y, z - rotation.offsetZ * 2, rotation.getOpposite()),
                new DirPos(x - direction.offsetX - rotation.offsetX * 2, y,
                        z - direction.offsetZ - rotation.offsetZ * 2, rotation.getOpposite())
        };
    }

    /** Recreates the old red/yellow/green proxy-relative inventory access. */
    public int[] getAccessibleSlotsFromSide(BlockPos accessor) {
        ForgeDirection direction = ForgeDirection.getOrientation(
                getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
        ForgeDirection rotation = direction.getRotation(ForgeDirection.UP);
        BlockPos core = worldPosition;

        BlockPos red1 = core.relative(rotation.toDirection());
        BlockPos red2 = core.relative(rotation.getOpposite().toDirection()).relative(direction.getOpposite().toDirection());
        if (accessor.equals(red1) || accessor.equals(red2)) return new int[]{0, 3};

        BlockPos yellow = core.relative(direction.getOpposite().toDirection());
        if (accessor.equals(yellow)) return new int[]{1, 3};

        BlockPos green1 = core.relative(rotation.getOpposite().toDirection());
        BlockPos green2 = core.relative(rotation.toDirection()).relative(direction.getOpposite().toDirection());
        if (accessor.equals(green1) || accessor.equals(green2)) return new int[]{2, 3};

        return new int[]{};
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 3) return true;
        if (slot == 4) return Library.isBattery(stack);
        if (slot == 5) return stack.getItem() instanceof IItemFluidId;
        return slot == 6 || slot == 7 ? Library.isMachineUpgrade(stack) : false;
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{6, 7};
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < 3 && isItemValidForSlot(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 3; }
    public boolean canInsertItem(int slot, ItemStack stack) { return slot < 3; }
    public boolean canExtractItem(int slot, ItemStack stack, int amount) { return slot == 3; }

    @Override public long getPower() { return Math.max(Math.min(power, maxPower), 0); }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_arc_welder.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 100 / 6) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 100 / 6) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink()
                    ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
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

    @Override public Component getDisplayName() { return Component.translatable("block.hbm.machine_arc_welder"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineArcWelder(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeLong(consumption);
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
        consumption = buf.readLong();
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
        tank.writeToNBT(output, "t");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 2_000L);
        progress = input.getIntOr("progress", 0);
        processTime = input.getIntOr("processTime", 1);
        tank.readFromNBT(input, "t");
    }
}
