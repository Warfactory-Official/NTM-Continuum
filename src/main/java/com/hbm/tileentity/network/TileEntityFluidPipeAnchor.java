package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.blocks.network.BasePipeAnchor;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IRelocatable;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code TileEntityFluidPipeAnchor}: a one-faced duct mounted on a wall that conducts through the face it is
 * mounted against and through its pipelines. A chunk unload keeps every pipe; breaking the anchor removes them.
 */
public class TileEntityFluidPipeAnchor extends TileEntityFluidDuctVariant implements TileEntityPipelineBase, IRelocatable {

    protected final List<BlockPos> connected = new ArrayList<>();

    public TileEntityFluidPipeAnchor(BlockPos pos, BlockState state) {
        this(ModFluidNetBlockEntities.ANCHOR.get(), pos, state);
    }

    protected TileEntityFluidPipeAnchor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * A sub-level move carried this anchor. Pipes to anchors carried with it follow them; a pipe to an anchor left
     * behind is gone, because that anchor already struck this one from its list when the old position was cleared.
     */
    @Override
    public void relocate(SubLevels.Move move) {
        List<BlockPos> kept = new ArrayList<>();
        for (BlockPos peer : connected) if (move.carried(peer)) kept.add(move.apply(peer));
        connected.clear();
        connected.addAll(kept);
        setChanged();
    }

    @Override
    public NetworkType getNetworkType() {
        return NetworkType.FLUID;
    }

    public ForgeDirection openSide() {
        return BasePipeAnchor.openSide(getBlockState());
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved(); // mints/retypes the cell with every face open...
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<DuctData> graph = FluidGraph.get(server);
        graph.updateConnections(getBlockPos().asLong(), BasePipeAnchor.faceMask(getBlockState())); // ...and an anchor opens one face only
        for (BlockPos peer : connected) graph.addRemoteLink(getBlockPos().asLong(), peer.asLong());
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir == openSide() && type == this.type;
    }

    /** Connects with NONE type anchors by giving them this anchor's fluid. */
    @Override
    public int canConnect(TileEntityPipelineBase other) {
        if (!(other instanceof TileEntityFluidPipeAnchor pipe)) return 1;
        if (this.type == Fluids.NONE && pipe.type != this.type) this.setFluidType(pipe.type);
        if (pipe.type == Fluids.NONE && this.type != pipe.type) pipe.setFluidType(this.type);
        return this.type != pipe.type ? 4 : 0;
    }

    @Override
    public List<BlockPos> getConnected() {
        return connected;
    }

    @Override
    public void linkNode(BlockPos peer) {
        if (level instanceof ServerLevel server) FluidGraph.get(server).addRemoteLink(getBlockPos().asLong(), peer.asLong());
    }

    @Override
    public void unlinkNode(BlockPos peer) {
        if (level instanceof ServerLevel server) FluidGraph.get(server).removeRemoteLink(getBlockPos().asLong(), peer.asLong());
    }

    @Override
    public void setRemoved() {
        // whether the block went with the block entity, read before the cell goes
        boolean broken = level instanceof ServerLevel && LevelNodeGraph.blockWentWithIt(this);
        if (broken) disconnectAll();
        super.setRemoved();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        TileEntityPipelineBase.save(connected, output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        TileEntityPipelineBase.load(connected, input);
    }
}
