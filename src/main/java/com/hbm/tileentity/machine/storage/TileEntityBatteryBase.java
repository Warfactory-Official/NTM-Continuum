package com.hbm.tileentity.machine.storage;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerGraphProvider;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.DirPos;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Base for HBM "energy storage" machines that act as a conductor on the power grid — they own a graph
 * cell at every port position, and a configurable I/O mode decides which of the two roles they answer
 * in. Ported from 1.12.2's {@code TileEntityBatteryBase}.
 *
 * <p>Two mode slots — {@link #redLow} (while <b>not</b> redstone-powered) and {@link #redHigh} (while
 * powered) — each one of input / buffer / output / none, plus a connection {@link #priority}. Subclasses
 * supply the storage ({@link #getPower}/{@link #setPower}/{@link #getMaxPower}) and the multiblock's port
 * geometry ({@link #getPortPos}/{@link #getConPos}); the energy-storage <em>block battery</em>
 * ({@code machine_battery}) keeps its own standalone implementation, so this base is currently the
 * battery <em>socket</em>'s. The SC-battery discharge and the RoR hooks live in {@link TileEntityBatterySocket}.
 * 1.7's OpenComputers component {@code ntm_energy_storage} is served to CC: Tweaked by
 * {@code com.hbm.compat.computercraft.storage.EnergyStoragePeripheral} (socket and REDD).
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

    /** The cells this machine minted, kept because its port geometry is unreadable once its block is gone. */
    private long[] cells = new long[0];

    public TileEntityBatteryBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state, slotCount);
    }

    public abstract BlockPos[] getPortPos();

    public abstract DirPos[] getConPos();

    /** Walks the I/O state machine — keeps the footprint's graph cells and the mode the roles read. */
    protected void serverTickBattery(Level level) {
        if (priority == null || priority.ordinal() == 0 || priority.ordinal() == 4) {
            priority = ConnectionPriority.LOW; // LOWEST/HIGHEST aren't selectable; clamp to LOW
        }

        refreshMode();
        if (level instanceof ServerLevel server) updateCells(server);

        byte comparator = getComparatorPower();
        if (comparator != lastRedstone) {
            for (BlockPos port : getPortPos()) {
                level.updateNeighborsAt(port, getBlockState().getBlock());
            }
            lastRedstone = comparator;
        }
        setChanged();
        networkPackNT();
    }

    /**
     * The footprint is a conductor, which is what makes a battery bank one network rather than one per
     * side it is cabled on. 1.7 says that with a single node holding every port position; the graph
     * holds a cell per position and ties the scattered ones together with remote links, since the REDD's
     * six ports are corners of a frame and touch nothing. The machine itself answers on the anchor cell
     * through the endpoint registry, because on the REDD that cell carries no block entity of its own.
     */
    private void updateCells(ServerLevel level) {
        LevelNodeGraph<CableData> graph = PowerGraph.get(level);
        BlockPos[] ports = getPortPos();
        if (ports.length == 0) return;
        long anchor = ports[0].asLong();
        boolean minted = false;
        for (BlockPos port : ports) {
            if (graph.containsCell(port.asLong())) continue;
            graph.addNode(port.asLong(), PowerGraphProvider.INSTANCE.createData(getBlockState()),
                    MachineCaps.FACE_MASK_ALL);
            minted = true;
        }
        if (minted) {
            for (int i = 1; i < ports.length; i++) graph.addRemoteLink(anchor, ports[i].asLong());
            cells = new long[ports.length];
            for (int i = 0; i < ports.length; i++) cells[i] = ports[i].asLong();
        }
        EndpointRegistry.of(level).declare(ports[0], getBlockPos(), MachineCaps.FACE_MASK_ALL,
                MachineCaps.POWER_IN | MachineCaps.POWER_OUT);
        graph.setSelfEndpoint(anchor, true);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (cells.length == 0) return;
        if (PowerGraph.dropCells(this, cells) && level instanceof ServerLevel server) {
            EndpointRegistry.of(server).withdraw(BlockPos.of(cells[0]));
        }
    }

    @Override
    public long getProviderSpeed() {
        short mode = getRelevantMode(true);
        return mode == mode_output || mode == mode_buffer ? getMaxPower() : 0L;
    }

    @Override
    public long getReceiverSpeed() {
        short mode = getRelevantMode(true);
        return mode == mode_input || mode == mode_buffer ? getMaxPower() : 0L;
    }

    @Override
    public boolean bufferedEndpoint() {
        return getRelevantMode(true) == mode_buffer;
    }

    /** 0..15 comparator output from the fill fraction. */
    public byte getComparatorPower() {
        long max = getMaxPower();
        if (max <= 0) return 0;
        double frac = (double) getPower() / (double) max * 15D;
        return (byte) Mth.clamp((int) frac + 1, 0, 15);
    }

    /**
     * Re-reads the redstone the mode is picked from. The gather asks three times a round — for each
     * speed and for the buffer flag — and every answer is six neighbour lookups, so it is read once a
     * tick and cached.
     */
    public void refreshMode() {
        getRelevantMode(false);
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
        com.hbm.util.SavedIds.putEnum(output, "priority", priority); // by name, never an ordinal
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        redLow = (short) input.getShortOr("redLow", (short) mode_input);
        redHigh = (short) input.getShortOr("redHigh", (short) mode_output);
        lastRedstone = input.getByteOr("lastRedstone", (byte) 0);
        priority = com.hbm.util.SavedIds.getEnum(input, "priority", ConnectionPriority.LOW);
    }
}
