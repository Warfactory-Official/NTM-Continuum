package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.network.CableDiode;
import com.hbm.capability.CapRole;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModEnergyNetBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.EnergyPush;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** One-way relay: accepted network input becomes output stock on the following tick. */
public class TileEntityDiode extends TileEntityLoadedBase implements IEnergyReceiverMK2,
        IEnergyProviderMK2, IControlReceiver, DiodeControl {
    public static final long MAX_LIMIT = 10_000_000_000L;
    public long limit = 1_000L;
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    private long stock, arriving, power;
    private int pulses;
    private long counted = Long.MIN_VALUE;
    private boolean recursionBrake;

    public TileEntityDiode(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) PowerGraph.get(server).removeNode(getBlockPos().asLong());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDiode diode) {
        diode.rollover();
        diode.networkPackNT();
    }

    private Direction output() { return getBlockState().getValue(CableDiode.FACING).getOpposite(); }

    public boolean acceptsFace(Direction side, CapRole role) {
        return role == CapRole.POWER_OUT ? side == output() : role == CapRole.POWER_IN && side != output();
    }

    @Override public boolean canConnect(ForgeDirection side) { return side != ForgeDirection.UNKNOWN; }
    @Override public long getMaxPower() { return limit; }
    @Override public long getConfiguredLimit() { return limit; }
    @Override public ConnectionPriority getPriority() { return priority; }
    @Override public Component getControlTitle() { return getBlockState().getBlock().getName(); }
    @Override public boolean passThrough() { return true; }
    @Override public long getTrackedPower() { rollover(); return stock + arriving; }

    private void rollover() {
        var now = level.getGameTime();
        if (now == counted) return;
        counted = now;
        pulses = 0;
        power = 0;
        if (arriving != 0) {
            stock += arriving;
            arriving = 0;
            energyChanged();
        }
    }

    private boolean outputIsNet() {
        var side = output();
        var target = getBlockPos().relative(side).asLong();
        var graph = PowerGraph.get((ServerLevel) level);
        return graph.containsCell(target) && graph.isOpenAt(target, side.getOpposite());
    }

    private long netRoom() {
        rollover();
        return Math.min(limit - getPower(), IEnergyHandlerMK2.transitRoom(limit, stock, arriving));
    }

    @Override public long getPower() { rollover(); return Math.min(arriving + power, limit); }
    @Override public void setPower(long value) { power = IBatteryItem.clampCharge(value, limit); }
    @Override public long getProviderSpeed() { rollover(); return Math.min(stock, limit); }
    @Override public long getReceiverSpeed() {
        return outputIsNet() ? netRoom() : limit - getPower();
    }

    @Override
    public long transferPower(long amount, boolean simulate) {
        if (amount < 0) throw new IllegalArgumentException("Negative energy transfer");
        if (simulate) return amount - Math.min(amount, getReceiverSpeed());
        if (outputIsNet()) {
            var accepted = Math.min(amount, netRoom());
            if (accepted > 0) {
                arriving += accepted;
                energyChanged();
            }
            return amount - accepted;
        }
        if (recursionBrake) return amount;
        rollover();
        pulses++;
        if (getPower() >= limit || pulses > 10) return amount;
        recursionBrake = true;
        try {
            var side = output();
            var allowance = Math.min(amount, limit - getPower());
            var accepted = EnergyPush.push((ServerLevel) level, getBlockPos().relative(side),
                    ForgeDirection.getOrientation(side), allowance, this);
            if (accepted > 0) {
                power += accepted;
                energyChanged();
            }
            return amount - accepted;
        } finally {
            recursionBrake = false;
        }
    }

    @Override
    public void usePower(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative energy debit");
        var taken = Math.min(amount, stock);
        if (taken == 0) return;
        stock -= taken;
        energyChanged();
    }

    private void energyChanged() { setChanged(); dataChanged(); }
    private void settingsChanged() { energyChanged(); networkPackNT(); }

    public void setControlSettings(boolean limitPresent, long newLimit, boolean priorityPresent, ConnectionPriority newPriority) {
        if (limitPresent) limit = Math.max(0L, Math.min(newLimit, MAX_LIMIT));
        if (priorityPresent) priority = newPriority;
        if (limitPresent || priorityPresent) settingsChanged();
    }

    public void stepLimitUp() {
        var next = 10L;
        while (next <= limit && next < MAX_LIMIT) next *= 10L;
        setControlSettings(true, next, false, priority);
    }

    public void stepLimitDown() {
        if (limit <= 10) return;
        var previous = 10L;
        for (var next = 100L; next < limit && next <= MAX_LIMIT; next *= 10L) previous = next;
        setControlSettings(true, previous, false, priority);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        var hasLimit = Tags.contains(data, "limit");
        var hasPriority = Tags.contains(data, "priority");
        var ordinal = Tags.getByteOr(data, "priority", (byte) priority.ordinal());
        var chosen = ConnectionPriority.VALUES[Math.floorMod(ordinal, ConnectionPriority.VALUES.length)];
        setControlSettings(hasLimit, Tags.getLongOr(data, "limit", limit), hasPriority, chosen);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        limit = Math.max(0L, Math.min(input.getLongOr("limit", limit), MAX_LIMIT));
        priority = SavedIds.getEnum(input, "priority", ConnectionPriority.NORMAL);
        stock = Math.max(0L, input.getLongOr("stock", 0L));
        arriving = power = 0;
        pulses = 0;
        counted = Long.MIN_VALUE;
        recursionBrake = false;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("limit", limit);
        SavedIds.putEnum(output, "priority", priority);
        output.putLong("stock", stock + arriving);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeLong(limit);
        buffer.writeByte(priority.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        limit = Math.max(0L, Math.min(buffer.readLong(), MAX_LIMIT));
        var ordinal = Math.max(0, Math.min(buffer.readByte(), ConnectionPriority.VALUES.length - 1));
        priority = ConnectionPriority.VALUES[ordinal];
    }
}
