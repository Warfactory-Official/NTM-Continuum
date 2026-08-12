package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Base for HBM "energy storage" machines that act as a conductor on the power grid — they own a network
 * node spanning their footprint and, per a configurable I/O mode, register themselves as a provider
 * and/or receiver on that net. Ported from 1.12.2's {@code TileEntityBatteryBase}.
 *
 * <p>Two mode slots — {@link #redLow} (while <b>not</b> redstone-powered) and {@link #redHigh} (while
 * powered) — each one of input / buffer / output / none, plus a connection {@link #priority}. Subclasses
 * supply the storage ({@link #getPower}/{@link #setPower}/{@link #getMaxPower}) and the multiblock's port
 * geometry ({@link #getPortPos}/{@link #getConPos}); the energy-storage <em>block battery</em>
 * ({@code machine_battery}) keeps its own standalone implementation, so this base is currently the
 * battery <em>socket</em>'s. The SC-battery discharge/lightning behaviour and the OpenComputers/RoR hooks
 * are dropped (see {@link TileEntityBatterySocket}).
 */
public abstract class TileEntityBatteryBase extends TileEntityMachineBase
        implements IEnergyConductorMK2, IEnergyProviderMK2, IEnergyReceiverMK2 {

    public static final int mode_input = 0;
    public static final int mode_buffer = 1;
    public static final int mode_output = 2;
    public static final int mode_none = 3;

    /** Mode while NOT receiving a redstone signal (default: input). */
    public short redLow = mode_input;
    /** Mode while receiving a redstone signal (default: output). */
    public short redHigh = mode_output;
    public ConnectionPriority priority = ConnectionPriority.LOW;

    public byte lastRedstone = 0;
    private short modeCache = 0;

    protected Nodespace.PowerNode node;

    public TileEntityBatteryBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state, slotCount);
    }

    public abstract BlockPos[] getPortPos();

    public abstract DirPos[] getConPos();

    /** Walks the I/O state machine — owns/keeps the network node and toggles provider/receiver per mode. */
    protected void serverTickBattery(Level level) {
        if (priority == null || priority.ordinal() == 0 || priority.ordinal() == 4) {
            priority = ConnectionPriority.LOW; // LOWEST/HIGHEST aren't selectable; clamp to LOW
        }

        if (node == null || node.expired) {
            node = UniNodespace.getNode(level, getBlockPos(), Nodespace.THE_POWER_PROVIDER);
            if (node == null || node.expired) {
                node = createNode();
                UniNodespace.createNode(level, node);
            }
        }

        if (node != null && node.hasValidNet()) {
            switch (getRelevantMode(false)) {
                case mode_input -> { node.net.removeProvider(this); node.net.addReceiver(this); }
                case mode_output -> { node.net.addProvider(this); node.net.removeReceiver(this); }
                case mode_buffer -> { node.net.addProvider(this); node.net.addReceiver(this); }
                case mode_none -> { node.net.removeProvider(this); node.net.removeReceiver(this); }
            }
        }

        lastRedstone = getComparatorPower();
        setChanged();
        networkPackNT();
    }

    @Override
    public Nodespace.PowerNode createNode() {
        return new Nodespace.PowerNode(getPortPos()).setConnections(getConPos());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide() && node != null) {
            UniNodespace.destroyNode(level, getBlockPos(), Nodespace.THE_POWER_PROVIDER);
            node = null;
        }
    }

    /** 0..15 comparator output from the fill fraction. */
    public byte getComparatorPower() {
        long max = getMaxPower();
        if (max <= 0) return 0;
        double frac = (double) getPower() / (double) max * 15D;
        return (byte) Mth.clamp((int) frac + 1, 0, 15);
    }

    /** The mode in effect right now (cached), picked by whether any port cell is redstone-powered. */
    public short getRelevantMode(boolean useCache) {
        if (useCache) return modeCache;
        boolean powered = false;
        for (BlockPos p : getPortPos()) {
            if (level.hasNeighborSignal(p)) { powered = true; break; }
        }
        modeCache = powered ? redHigh : redLow;
        return modeCache;
    }

    public void cycleRedLow() { redLow = (short) ((redLow + 1) % 4); setChanged(); }
    public void cycleRedHigh() { redHigh = (short) ((redHigh + 1) % 4); setChanged(); }

    public void cyclePriority() {
        priority = switch (priority) {
            case LOW -> ConnectionPriority.NORMAL;
            case NORMAL -> ConnectionPriority.HIGH;
            default -> ConnectionPriority.LOW;
        };
        setChanged();
    }

    @Override
    public boolean allowDirectProvision() {
        return false;
    }

    @Override
    public ConnectionPriority getPriority() {
        return priority;
    }

    // --- sync + persistence (subclasses extend with their own fields) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(redLow);
        buf.writeShort(redHigh);
        buf.writeByte(priority.ordinal());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        redLow = buf.readShort();
        redHigh = buf.readShort();
        priority = ConnectionPriority.VALUES[buf.readByte()];
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putShort("redLow", redLow);
        output.putShort("redHigh", redHigh);
        output.putByte("lastRedstone", lastRedstone);
        output.putByte("priority", (byte) priority.ordinal());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        redLow = (short) input.getShortOr("redLow", (short) mode_input);
        redHigh = (short) input.getShortOr("redHigh", (short) mode_output);
        lastRedstone = input.getByteOr("lastRedstone", (byte) 0);
        priority = ConnectionPriority.VALUES[input.getByteOr("priority", (byte) ConnectionPriority.LOW.ordinal())];
    }
}
