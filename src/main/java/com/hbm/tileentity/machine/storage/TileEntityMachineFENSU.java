package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerMachineFENSU;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The legacy FEnSU storage BE, built on the current reusable battery-network base. Items as 1.7.10 (where it
 * extends the energy storage block and uses its menu): slot 0 discharges a battery into the FEnSU, slot 1
 * charges one from it; top face slot 0, bottom both, sides slot 1.
 */
public class TileEntityMachineFENSU extends TileEntityBatteryBase implements MenuProvider, WorldlyContainer {

    public static final long MAX_POWER = Long.MAX_VALUE;
    public static final long MAX_TRANSFER = 10_000_000_000_000_000L;

    public long power;
    public long delta;
    public long prevPowerState;
    public DyeColor color = DyeColor.LIGHT_BLUE;
    public float prevRotation;
    public float rotation;

    private final long[] log = new long[20];

    public TileEntityMachineFENSU(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FENSU.get(), pos, state, 2);
    }

    @Override
    public String getDefaultName() {
        return "container.battery";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineFENSU fensu) {
        long previous = fensu.power;

        // 1.7 order around the network update: charge the item in slot 1 first, then drain slot 0.
        fensu.power = Library.chargeItemsFromTE(fensu.inventory, 1, fensu.power, MAX_POWER);
        fensu.serverTickBattery(level);
        fensu.power = Library.chargeTEFromItems(fensu.inventory, 0, fensu.power, MAX_POWER);

        long average = average(previous, fensu.power);
        fensu.delta = average - fensu.log[0];
        System.arraycopy(fensu.log, 1, fensu.log, 0, fensu.log.length - 1);
        fensu.log[fensu.log.length - 1] = average;
        fensu.prevPowerState = fensu.power;
        fensu.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineFENSU fensu) {
        fensu.prevRotation = fensu.rotation;
        fensu.rotation += fensu.getSpeed();
        if (fensu.rotation >= 360F) {
            fensu.rotation -= 360F;
            fensu.prevRotation -= 360F;
        }
    }

    private static long average(long first, long second) {
        // Exact floor((first + second) / 2) without overflowing at Long.MAX_VALUE.
        return first / 2L + second / 2L + (first % 2L + second % 2L) / 2L;
    }

    public float getSpeed() {
        return (float) Math.pow(Math.log(power * 0.75D + 1D) * 0.05F, 5);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long value) {
        power = Math.max(0L, Math.min(value, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    /** Avoid the default {@code power * scale} overflow at the FEnSU's intentionally absurd capacity. */
    public long getPowerRemainingScaled(long scale) {
        if (scale <= 0L || power <= 0L) return 0L;
        if (power >= MAX_POWER) return scale;
        return (long) ((double) power / (double) MAX_POWER * (double) scale);
    }

    @Override
    public byte getComparatorPower() {
        return (byte) Math.max(0L, Math.min(15L, getPowerRemainingScaled(15L)));
    }

    @Override
    public long getProviderSpeed() {
        int mode = getRelevantMode(true);
        return mode == mode_output || mode == mode_buffer ? MAX_TRANSFER : 0L;
    }

    @Override
    public long getReceiverSpeed() {
        int mode = getRelevantMode(true);
        return mode == mode_input || mode == mode_buffer ? MAX_TRANSFER : 0L;
    }

    @Override
    public long transferPower(long incoming, boolean simulate) {
        long current = Math.max(0L, Math.min(power, MAX_POWER));
        long free = MAX_POWER - current;
        long accepted = Math.min(Math.min(incoming, MAX_TRANSFER), free);
        if (accepted < 0L) accepted = 0L;
        if (!simulate) power = current + accepted;
        return incoming - accepted;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir == ForgeDirection.DOWN;
    }

    @Override
    public BlockPos[] getPortPos() {
        return new BlockPos[]{getBlockPos()};
    }

    @Override
    public DirPos[] getConPos() {
        BlockPos pos = getBlockPos();
        return new DirPos[]{new DirPos(pos.below(), Library.NEG_Y)};
    }

    /** 1.7 (inherited from the storage block): every item is accepted. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 || slot == 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return getAccessibleSlotsFromSide(side.ordinal());
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == 0 ? new int[]{0, 1} : side == 1 ? new int[]{0} : new int[]{1};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (stack.getItem() instanceof com.hbm.api.energymk2.IBatteryItem battery) {
            if (slot == 0 && battery.getCharge(stack) == 0) return true;
            if (slot == 1 && battery.getCharge(stack) == battery.getMaxCharge(stack)) return true;
        }
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_fensu");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineFENSU(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(delta);
        buf.writeByte(color.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = Math.max(0L, buf.readLong());
        delta = buf.readLong();
        color = DyeColor.values()[Math.max(0, Math.min(buf.readByte(), DyeColor.values().length - 1))];
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putLong("prevPowerState", prevPowerState);
        com.hbm.util.SavedIds.putEnum(output, "color", color); // by name, never an ordinal
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = Math.max(0L, Math.min(input.getLongOr("power", 0L), MAX_POWER));
        prevPowerState = input.getLongOr("prevPowerState", 0L);
        color = com.hbm.util.SavedIds.getEnum(input, "color", DyeColor.LIGHT_BLUE);
    }
}
