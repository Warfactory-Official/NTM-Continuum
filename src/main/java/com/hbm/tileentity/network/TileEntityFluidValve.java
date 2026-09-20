package com.hbm.tileentity.network;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.blocks.network.FluidValve;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The switchable conductor owned by {@link FluidValve}: a duct that only conducts while it is open.
 *
 * <p>On the graph that is the whole of it — a closed valve owns no cell, so the two runs it sits
 * between are two networks, and opening it mints the cell that makes them one. Nothing needs to tear a
 * network down or rebuild it; {@link #syncCell} is the only thing that ever touches the graph.
 */
public class TileEntityFluidValve extends CompatBlockEntity implements IFluidPipeMK2, ILoadedTile {

    protected FluidType type = Fluids.NONE;

    public TileEntityFluidValve(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FLUID_VALVE.get(), pos, state);
    }

    protected TileEntityFluidValve(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    public void updateState() {
        syncCell();
        setChanged();
    }

    public boolean isOpen() {
        return getBlockState().hasProperty(FluidValve.OPEN) && getBlockState().getValue(FluidValve.OPEN);
    }

    public FluidType getFluidType() {
        return type;
    }

    public void setFluidType(FluidType type) {
        if (type == null) type = Fluids.NONE;
        if (this.type == type) return;
        this.type = type;
        setChanged();
        syncCell();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        syncCell();
    }

    /** The cell exists exactly while the valve would conduct, and carries exactly what it conducts. */
    private void syncCell() {
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<DuctData> graph = FluidGraph.get(server);
        long key = getBlockPos().asLong();
        boolean conducts = isOpen() && type != Fluids.NONE;
        if (!conducts) {
            if (graph.containsCell(key)) graph.removeNode(key);
        } else if (graph.containsCell(key)) {
            graph.replaceNodeData(key, DuctData.of(type));
        } else {
            graph.addNode(key, DuctData.of(type), MachineCaps.FACE_MASK_ALL);
        }
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return isOpen() && this.type != Fluids.NONE && dir != ForgeDirection.UNKNOWN && this.type == type;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        FluidGraph.dropCells(this, getBlockPos());
    }

    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("type", type.getID());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        type = Fluids.fromID(input.getIntOr("type", Fluids.NONE.getID()));
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
