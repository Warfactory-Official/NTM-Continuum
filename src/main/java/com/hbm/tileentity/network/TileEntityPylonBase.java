package com.hbm.tileentity.network;

import dev.golem.core.data.Tags;

import com.hbm.network.power.CableData;
import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.network.power.PowerGraph;
import com.hbm.network.power.PowerGraphProvider;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.graph.LevelNodeGraph;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.network.PylonDyes;

/**
 * 1.7's {@code TileEntityPylonBase}: a conductor that also links to other pylons over a distance.
 *
 * <p>The connection list is the pylon's own (persisted, synced, and what the wires are drawn from);
 * the graph mirrors it as remote links between the two pylons' cells, which is what 1.7's
 * {@code PowerNode.addConnection(DirPos UNKNOWN)} became. A pylon with several cells (the substation's
 * corners) links them to its core cell the way the battery socket ties its ports together.
 */
public abstract class TileEntityPylonBase extends SyncedBlockEntity implements IEnergyConductorMK2 {

    protected final List<BlockPos> connected = new ArrayList<>();
    public int color;
    /** The cells this pylon minted, kept because the footprint is unreadable once the block is gone. */
    private long[] cells = new long[0];

    protected TileEntityPylonBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 0 = ok, 1 = type mismatch, 2 = same pylon, 3 = too far. */
    public static int canConnect(TileEntityPylonBase first, TileEntityPylonBase second) {
        if (first.getConnectionType() != second.getConnectionType()) return 1;
        if (first == second) return 2;
        double len = Math.min(first.getMaxWireLength(), second.getMaxWireLength());
        Vec3 delta = second.getConnectionPoint().subtract(first.getConnectionPoint());
        return len >= delta.length() ? 0 : 3;
    }

    public boolean setColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        int color = PylonDyes.colorFromDye(stack);
        if (color == 0 || color == this.color) return false;
        stack.shrink(1);
        this.color = color;
        setChanged();
        sync();
        return true;
    }

    // --- graph ---

    /** The cell positions this pylon conducts through; the first is the anchor every other is linked to. */
    protected BlockPos[] getCellPos() {
        return new BlockPos[]{getBlockPos()};
    }

    /** The open faces of the cell at {@code index} of {@link #getCellPos}, as a {@code 1 << Direction.ordinal()} mask. */
    protected abstract int getCellMask(int index);

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        syncCells();
    }

    /** Mints the cells and re-states every link; idempotent, so a chunk load repeats it for free. */
    protected void syncCells() {
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<CableData> graph = PowerGraph.get(server);
        BlockPos[] ports = getCellPos();
        long anchor = ports[0].asLong();
        for (int i = 0; i < ports.length; i++) {
            long key = ports[i].asLong();
            if (graph.containsCell(key)) {
                graph.updateConnections(key, getCellMask(i));
            } else {
                graph.addNode(key, PowerGraphProvider.INSTANCE.createData(getBlockState()), getCellMask(i));
            }
        }
        for (int i = 1; i < ports.length; i++) graph.addRemoteLink(anchor, ports[i].asLong());
        cells = new long[ports.length];
        for (int i = 0; i < ports.length; i++) cells[i] = ports[i].asLong();
        for (BlockPos peer : connected) graph.addRemoteLink(anchor, peer.asLong());
    }

    public void addConnection(BlockPos pos) {
        connected.add(pos.immutable());
        if (level instanceof ServerLevel server) {
            PowerGraph.get(server).addRemoteLink(getBlockPos().asLong(), pos.asLong());
        }
        setChanged();
        sync();
    }

    /** Tears this pylon out of every peer's list; the peers' graph links go with this pylon's cells. */
    public void disconnectAll() {
        if (level == null) return;
        for (BlockPos pos : connected) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te == this || !(te instanceof TileEntityPylonBase pylon)) continue;
            pylon.connected.removeIf(p -> p.equals(getBlockPos()));
            pylon.setChanged();
            pylon.sync();
        }
        connected.clear();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (cells.length == 0) return;
        // dropCells says whether the block went with the block entity; a chunk unload keeps every wire.
        if (PowerGraph.dropCells(this, cells)) disconnectAll();
    }

    /** Pushes the full tag to tracking clients (the old description packet). */
    protected void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public abstract ConnectionType getConnectionType();

    /** Wire mounting points, relative to this block. */
    public abstract Vec3[] getMountPos();

    public abstract double getMaxWireLength();

    /** The point the range check measures from: the first mount, in world space. */
    public Vec3 getConnectionPoint() {
        Vec3[] mounts = getMountPos();
        if (mounts == null || mounts.length == 0) {
            return new Vec3(getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5);
        }
        return mounts[0].add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
    }

    public List<BlockPos> getConnected() {
        return connected;
    }

    /** The 1.7 metadata the renderers and mount tables keyed on: the facing's ForgeDirection ordinal (2..5). */
    public int facingMeta() {
        return com.hbm.tileentity.MachineCore.facingOrdinal(getBlockState(), 2);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("conCount", connected.size());
        output.putInt("color", color);
        for (int i = 0; i < connected.size(); i++) {
            BlockPos p = connected.get(i);
            output.putIntArray("con" + i, new int[]{p.getX(), p.getY(), p.getZ()});
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        int count = input.getIntOr("conCount", 0);
        color = input.getIntOr("color", 0);
        connected.clear();
        for (int i = 0; i < count; i++) {
            int[] c = input.getIntArrayOr("con" + i, null);
            if (c != null && c.length == 3) connected.add(new BlockPos(c[0], c[1], c[2]));
        }
    }

    public enum ConnectionType {
        SINGLE,
        TRIPLE,
        QUAD
    }
}
