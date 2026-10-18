package com.hbm.tileentity.network;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.7 {@code TileEntityPipelineBase}: an anchor linked by the pipe wrench to other anchors of its network type. An
 * interface here because the fluid and exhaust anchors are duct cells and the pneumatic one is not. Each anchor keeps
 * the list of its peers (saved, synced, drawn) and mirrors it as remote links in its graph.
 */
public interface TileEntityPipelineBase {

    enum ConnectionType { SMALL }

    enum NetworkType { FLUID, EXHAUST, PNEUMATIC }

    NetworkType getNetworkType();

    List<BlockPos> getConnected();

    /** Joins this anchor's graph cell with the one at {@code peer}. */
    void linkNode(BlockPos peer);

    void unlinkNode(BlockPos peer);

    default ConnectionType getConnectionType() {
        return ConnectionType.SMALL;
    }

    default Vec3 getMountPos() {
        return new Vec3(0.5D, 0.5D, 0.5D);
    }

    default double getMaxPipeLength() {
        return 10;
    }

    /** The type check of {@link #canConnect(TileEntityPipelineBase, TileEntityPipelineBase)}: 0 or a status code. */
    default int canConnect(TileEntityPipelineBase other) {
        return 0;
    }

    default Vec3 getConnectionPoint() {
        BlockPos pos = self().getBlockPos();
        return getMountPos().add(pos.getX(), pos.getY(), pos.getZ());
    }

    default BlockEntity self() {
        return (BlockEntity) this;
    }

    default void addConnection(BlockPos pos) {
        getConnected().add(pos.immutable());
        linkNode(pos);
        self().setChanged();
        sync(self());
    }

    /** Takes this anchor out of every peer's list and drops the graph links. */
    default void disconnectAll() {
        Level level = self().getLevel();
        BlockPos own = self().getBlockPos();
        for (BlockPos pos : getConnected()) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te == this || !(te instanceof TileEntityPipelineBase pipeline)) continue;
            pipeline.getConnected().removeIf(p -> p.equals(own));
            unlinkNode(pos);
            te.setChanged();
            sync(te);
        }
        getConnected().clear();
    }

    /**
     * Returns a status code based on the operation.<br>
     * 0: Connected<br>
     * 1: Connections are incompatible<br>
     * 2: Both parties are the same block<br>
     * 3: Connection length exceeds maximum<br>
     * 4: Pipeline fluid types do not match
     */
    static int canConnect(TileEntityPipelineBase first, TileEntityPipelineBase second) {
        if (first.getNetworkType() != second.getNetworkType()) return 1;
        if (first.getConnectionType() != second.getConnectionType()) return 1;
        if (first == second) return 2;

        int ret = first.canConnect(second);
        if (ret != 0) return ret;

        double len = Math.min(first.getMaxPipeLength(), second.getMaxPipeLength());
        Vec3 delta = second.getConnectionPoint().subtract(first.getConnectionPoint());
        return len >= delta.length() ? 0 : 3;
    }

    static void sync(BlockEntity te) {
        Level level = te.getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(te.getBlockPos(), te.getBlockState(), te.getBlockState(), 3);
        }
    }

    static void save(List<BlockPos> connected, DataOut output) {
        output.putInt("conCount", connected.size());
        for (int i = 0; i < connected.size(); i++) {
            BlockPos p = connected.get(i);
            output.putIntArray("con" + i, new int[]{p.getX(), p.getY(), p.getZ()});
        }
    }

    static void load(List<BlockPos> connected, DataIn input) {
        connected.clear();
        int count = input.getIntOr("conCount", 0);
        for (int i = 0; i < count; i++) {
            int[] p = input.getIntArrayOr("con" + i, new int[0]);
            if (p.length == 3) connected.add(new BlockPos(p[0], p[1], p[2]));
        }
    }
}
