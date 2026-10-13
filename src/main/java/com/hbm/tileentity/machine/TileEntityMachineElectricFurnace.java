package com.hbm.tileentity.machine;

import com.hbm.lib.ForgeDirection;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.util.TickPhase;
import com.hbm.blocks.machine.MachineElectricFurnace;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineElectricFurnace;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.world.ItemStacks;
import java.util.HashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
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
import dev.golem.core.world.Recipes;

/**
 * Electric Furnace: a functional powered smelter. It draws HE from adjacent power networks (and a
 * battery item in slot 0) and smelts vanilla furnace recipes (slot 1 -> slot 2), consuming power per
 * tick. Faithful port of the 1.12.2 {@code TileEntityMachineElectricFurnace}.
 *
 * <p>Slots: 0 battery, 1 input, 2 output, 3 upgrade (SPEED/POWER, 1.12.2 formulas on the base
 * 50 HE / 100 ticks). Soot: 1.7's {@code SOOT_PER_SECOND} once a second while smelting. Deferred (see
 * IMPLEMENTATION.md §18): the lit flame particles and the per-loader energy capability bridge.
 */
public class TileEntityMachineElectricFurnace extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, WorldlyContainer, IUpgradeInfoProvider {

    private static final int[] IO_SLOTS = {0, 1, 2}; // battery / input (insert) + output (extract)

    public static final long maxPower = 100000;

    public long power;
    public int progress;
    public int maxProgress = 100;
    public int consumption = 50;
    /** Synced to the client so the GUI can light its fire indicator without a client recipe lookup. */
    public boolean canProgress;
    private int cooldown = 0;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineElectricFurnace(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ELECTRIC_FURNACE.get(), pos, state, 4);
    }

    @Override
    public String getDefaultName() {
        return "container.electricFurnace";
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return switch (i) {
            case 0 -> Library.isBattery(stack);
            case 1 -> true; // any item; the recipe check decides whether it actually smelts
            case 3 -> Library.isMachineUpgrade(stack); // upgrade slot (any type, like SlotUpgrade)
            default -> false; // output (2)
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineElectricFurnace te) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (te.cooldown > 0) te.cooldown--;

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);

        if (level.getGameTime() % 40 == 0) { // 1.7 updateConnections every 40 ticks (the net keeps a subscriber 3 s)
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                te.trySubscribe(level, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
            }
        }

        te.consumption = 50;
        te.maxProgress = 100;

        // Upgrades (1.12.2 formulas): SPEED -25 ticks / +50 HE/t per level; POWER +10 ticks / -15 HE/t.
        te.upgradeManager.checkSlots(te.inventory, 3, 3);
        int speedLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 3);
        te.maxProgress -= speedLevel * 25;
        te.consumption += speedLevel * 50;
        te.maxProgress += powerLevel * 10;
        te.consumption -= powerLevel * 15;

        boolean hasPower = te.power >= te.consumption;
        if (!hasPower) te.cooldown = 20;

        boolean canProcess = te.canProcess(serverLevel);
        te.canProgress = canProcess && hasPower;

        if (canProcess && hasPower) {
            te.progress++;
            te.power -= te.consumption;

            if (TickPhase.every(level, pos, 20L)) PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);

            if (te.progress >= te.maxProgress) {
                te.progress = 0;
                te.processItem(serverLevel);
                te.setChanged();
            }
        } else {
            te.progress = 0;
        }

        // Drive the LIT block state (lit while smelting), matching the old off/on block swap. The LIT state
        // is only RE-EVALUATED when `trigger` holds, crucially NOT on the completion tick where progress
        // has just reset to 0 but smelting will continue (canProcess && hasPower, progress==0): there
        // `trigger` is false, so the state is left ON and doesn't flicker off for one tick. (Faithful to the
        // 1.12.2 `trigger = !hasPower || !canProcess || progress != 0` gate.)
        boolean trigger = !hasPower || !canProcess || te.progress != 0;
        if (trigger && state.hasProperty(MachineElectricFurnace.LIT)) {
            boolean lit = te.progress > 0;
            if (state.getValue(MachineElectricFurnace.LIT) != lit) {
                level.setBlock(pos, state.setValue(MachineElectricFurnace.LIT, lit), 2);
            }
        }

        te.networkPackNT();
    }

    private boolean canProcess(ServerLevel level) {
        if (cooldown > 0) return false;
        ItemStack result = Recipes.smeltingResult(level, inventory.getStackInSlot(1)).orElse(ItemStack.EMPTY);
        if (result.isEmpty()) return false;

        ItemStack outSlot = inventory.getStackInSlot(2);
        if (outSlot.isEmpty()) return true;
        if (!ItemStacks.sameItemAndData(outSlot, result)) return false;
        return outSlot.getCount() + result.getCount() <= outSlot.getMaxStackSize();
    }

    private void processItem(ServerLevel level) {
        ItemStack result = Recipes.smeltingResult(level, inventory.getStackInSlot(1)).orElse(ItemStack.EMPTY);
        if (result.isEmpty()) return;

        ItemStack outSlot = inventory.getStackInSlot(2);
        if (outSlot.isEmpty()) inventory.setStackInSlot(2, result.copy());
        else outSlot.grow(result.getCount());
        inventory.getStackInSlot(1).shrink(1);
    }

    // WorldlyContainer: hoppers extract the output (slot 2) or a drained battery (slot 0); insert a
    // battery (0) / smeltable (1). Restricts the "hopper underneath takes from every slot" behaviour. ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot == 0 || slot == 1) && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 2 || (slot == 0 && Library.isEmptyBattery(stack));
    }

    /** Power connection points: all six faces of the furnace block. */
    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z),
                new DirPos(x, y + 1, z, Library.POS_Y),
                new DirPos(x, y - 1, z, Library.NEG_Y),
        };
    }

    // IUpgradeInfoProvider (text ported from the 1.12.2 provideInfo)

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{3, 3};
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_electric_furnace_off.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 30) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (level * 10) + "%").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        return upgrades;
    }

    // IEnergyReceiverMK2

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower / 20; }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return (progress * i) / maxProgress; }
    public boolean hasPower() { return power >= consumption; }
    public boolean isProcessing() { return progress > 0; }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_electric_furnace_off");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineElectricFurnace(containerId, playerInventory, this);
    }

    // Sync (BufPacket) + persistence

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(maxProgress);
        buf.writeInt(progress);
        buf.writeInt(consumption);
        buf.writeBoolean(canProgress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxProgress = buf.readInt();
        progress = buf.readInt();
        consumption = buf.readInt();
        canProgress = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
    }
}
