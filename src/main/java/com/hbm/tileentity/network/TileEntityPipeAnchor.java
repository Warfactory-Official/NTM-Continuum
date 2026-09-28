package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.blocks.network.FluidPipeAnchor;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The 1.7 {@code TileEntityPipeAnchor} with its {@code TileEntityPipelineBase}: a one-faced duct mounted on a wall that
 * conducts through the face it is mounted against, and - linked with the pipe wrench - through a physical pipeline to
 * other anchors up to 10 m away ({@link ConnectionType#SMALL}, mount point the block centre).
 *
 * <p>The connection list is the anchor's own (persisted, synced to the client, drawn by {@code RenderPipeAnchor}); the
 * fluid graph mirrors it as remote links between the two anchors' cells - what 1.7's
 * {@code FluidNode.addConnection(DirPos UNKNOWN)} became - re-stated on every load. Breaking an anchor tears it out of
 * every peer's list (1.7 {@code invalidate -> disconnectAll}); a chunk unload keeps them.
 */
public class TileEntityPipeAnchor extends TileEntityFluidDuctVariant {

    /** 1.7 {@code TileEntityPipelineBase.ConnectionType}. */
    public enum ConnectionType { SMALL }

    protected final List<BlockPos> connected = new ArrayList<>();

    public TileEntityPipeAnchor(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.ANCHOR.get(), pos, state);
    }

    /** The open face: opposite the face the anchor was placed against (1.7 {@code orientation(meta).getOpposite()}). */
    public ForgeDirection openSide() {
        BlockState state = getBlockState();
        if (!state.hasProperty(FluidPipeAnchor.FACING)) return ForgeDirection.UNKNOWN;
        Direction facing = state.getValue(FluidPipeAnchor.FACING);
        return ForgeDirection.getOrientation(facing).getOpposite();
    }

    private int faceMask() {
        ForgeDirection side = openSide();
        return side == ForgeDirection.UNKNOWN ? 0 : 1 << side.ordinal();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved(); // mints/retypes the cell with every face open...
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<DuctData> graph = FluidGraph.get(server);
        graph.updateConnections(getBlockPos().asLong(), faceMask()); // ...and an anchor opens one face only
        for (BlockPos peer : connected) graph.addRemoteLink(getBlockPos().asLong(), peer.asLong());
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir == openSide() && type == this.type;
    }

    // --- 1.7 TileEntityPipelineBase ---

    public ConnectionType getConnectionType() {
        return ConnectionType.SMALL;
    }

    public Vec3 getMountPos() {
        return new Vec3(0.5, 0.5, 0.5);
    }

    public double getMaxPipeLength() {
        return 10;
    }

    public Vec3 getConnectionPoint() {
        return getMountPos().add(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ());
    }

    public List<BlockPos> getConnected() {
        return connected;
    }

    /**
     * 1.7 {@code canConnect}: 0 connected, 1 incompatible types, 2 the same anchor, 3 too far, 4 fluid types differ.
     * An anchor without a fluid takes the other's first, as in 1.7.
     */
    public static int canConnect(TileEntityPipeAnchor first, TileEntityPipeAnchor second) {
        if (first.getConnectionType() != second.getConnectionType()) return 1;
        if (first == second) return 2;

        // connect with NONE type anchors
        if (first.type == Fluids.NONE && second.type != first.type) first.setFluidType(second.type);
        if (second.type == Fluids.NONE && first.type != second.type) second.setFluidType(first.type);

        if (first.type != second.type) return 4;

        double len = Math.min(first.getMaxPipeLength(), second.getMaxPipeLength());
        Vec3 delta = second.getConnectionPoint().subtract(first.getConnectionPoint());
        return len >= delta.length() ? 0 : 3;
    }

    public void addConnection(BlockPos pos) {
        connected.add(pos.immutable());
        if (level instanceof ServerLevel server) {
            FluidGraph.get(server).addRemoteLink(getBlockPos().asLong(), pos.asLong());
        }
        setChanged();
        sync();
    }

    /** 1.7 {@code disconnectAll}: this anchor leaves every peer's list and the graph drops the links. */
    public void disconnectAll() {
        if (level == null) return;
        for (BlockPos pos : connected) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te == this || !(te instanceof TileEntityPipeAnchor pipeline)) continue;
            pipeline.connected.removeIf(p -> p.equals(getBlockPos()));
            if (level instanceof ServerLevel server) {
                FluidGraph.get(server).removeRemoteLink(getBlockPos().asLong(), pos.asLong());
            }
            pipeline.setChanged();
            pipeline.sync();
        }
        connected.clear();
    }

    @Override
    public void setRemoved() {
        // whether the block went with the block entity (a chunk unload keeps every pipe) - read before the cell goes
        boolean broken = level instanceof ServerLevel && LevelNodeGraph.blockWentWithIt(this);
        if (broken) disconnectAll();
        super.setRemoved();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("conCount", connected.size());
        for (int i = 0; i < connected.size(); i++) {
            BlockPos p = connected.get(i);
            output.putIntArray("con" + i, new int[]{p.getX(), p.getY(), p.getZ()});
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        connected.clear();
        int count = input.getIntOr("conCount", 0);
        for (int i = 0; i < count; i++) {
            int[] p = input.getIntArrayOr("con" + i, new int[0]);
            if (p.length == 3) connected.add(new BlockPos(p[0], p[1], p[2]));
        }
    }
}
