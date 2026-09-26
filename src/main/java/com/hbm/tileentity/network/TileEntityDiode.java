package com.hbm.tileentity.network;

import net.minecraft.core.Direction;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.network.CableDiode;
import com.hbm.tileentity.ModEnergyNetBlockEntities;
import com.hbm.util.EnergyPush;

/**
 * 1.7's {@code CableDiode.TileEntityDiode}: a receiver on every face but one, whose "storage" is a
 * per-tick transfer counter -- what it accepts it hands straight through its output face, up to
 * {@link #limit} a tick, and never back. The two nets on either side never meet.
 */
public class TileEntityDiode extends SyncedBlockEntity implements IEnergyReceiverMK2, IControlReceiver {

    public static final long MAX_LIMIT = 10_000_000_000L;

    /** Intra-tick tracker of how much has been passed on; reset every tick, capped by the limit. */
    private long power;
    private boolean recursionBrake = false;
    private int pulses = 0;
    public ConnectionPriority priority = ConnectionPriority.NORMAL;
    public long limit = 1_000;

    public TileEntityDiode(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    /** The output side: 1.7's {@code getOrientation(meta).getOpposite()}. */
    public ForgeDirection getDir() {
        return ForgeDirection.getOrientation(getBlockState().getValue(CableDiode.FACING)).getOpposite();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDiode diode) {
        diode.pulses = 0;
        diode.setPower(0); // tick is over, reset our allowed transfer
        diode.syncState();
    }

    @Override
    public boolean canConnect(Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return dir != getDir();
    }

    private BlockPos outPos() {
        ForgeDirection dir = getDir();
        return getBlockPos().offset(dir.offsetX, dir.offsetY, dir.offsetZ);
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (recursionBrake) return power;
        pulses++;
        if (this.getPower() >= this.getMaxPower() || pulses > 10) return power; // maxed out transfer or pulses: abort
        if (!(level instanceof ServerLevel server)) return power;

        recursionBrake = true;
        try {
            long toTransfer = Math.min(power, this.getMaxPower() - this.getPower());
            long transferred = simulate
                    ? Math.min(toTransfer, EnergyPush.capacity(server, outPos(), getDir(), this))
                    : EnergyPush.push(server, outPos(), getDir(), toTransfer, this);
            if (!simulate) this.power += transferred;
            return power - transferred;
        } finally {
            recursionBrake = false;
        }
    }

    /** Advertise only what the far side could take, so a shortage is not allocated to a diode that would refund it. */
    @Override
    public long getReceiverSpeed() {
        long headroom = getMaxPower() - getPower();
        if (headroom <= 0 || !(level instanceof ServerLevel server)) return 0L;
        return Math.min(headroom, EnergyPush.capacity(server, outPos(), getDir(), this));
    }

    @Override public long getProviderSpeed() { return 0L; }
    @Override public long getMaxPower() { return limit; }
    @Override public long getPower() { return Math.min(power, getMaxPower()); }
    @Override public void setPower(long power) { this.power = power; }
    @Override public ConnectionPriority getPriority() { return priority; }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(getBlockPos().getX() + 0.5D, getBlockPos().getY() + 0.5D, getBlockPos().getZ() + 0.5D) <= 128;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "limit")) limit = Tags.getLongOr(data, "limit", limit);
        if (Tags.contains(data, "priority")) {
            int p = Tags.getByteOr(data, "priority", (byte) priority.ordinal());
            priority = ConnectionPriority.VALUES[Math.max(0, Math.min(p, ConnectionPriority.VALUES.length - 1))];
        }
        if (limit < 0) limit = 0;
        if (limit > MAX_LIMIT) limit = MAX_LIMIT;
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte((byte) priority.ordinal());
        buf.writeLong(limit);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        int p = buf.readByte();
        priority = ConnectionPriority.VALUES[Math.max(0, Math.min(p, ConnectionPriority.VALUES.length - 1))];
        limit = buf.readLong();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("limit", limit);
        output.putByte("p", (byte) priority.ordinal());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        int levelKey = input.getIntOr("level", -1);
        limit = levelKey >= 0 ? (long) Math.pow(10, levelKey) : input.getLongOr("limit", 1_000L);
        int p = input.getByteOr("p", (byte) ConnectionPriority.NORMAL.ordinal());
        priority = ConnectionPriority.VALUES[Math.max(0, Math.min(p, ConnectionPriority.VALUES.length - 1))];
    }
}
