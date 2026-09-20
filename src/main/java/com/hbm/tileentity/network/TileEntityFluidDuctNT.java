package com.hbm.tileentity.network;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.api.tile.ILoadedTile;
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
 * A solid fluid duct: a pure fluid conductor, the fluid analogue of {@link TileEntityCableBaseNT}.
 * Conduction is a cell of the level's fluid graph, minted by the block on placement; this carries no
 * fluid and does not tick.
 *
 * <p>Like the 1.12.2 {@code fluid_duct_mk2}, a duct defaults to {@code Fluids.NONE} and the player sets
 * its type by right-clicking with a {@code fluid_identifier} — it then forms a network with adjacent
 * ducts and machines of that one fluid only, which on the graph is
 * {@link com.hbm.api.fluidmk2.FluidGraphProvider#dataCompatible}. The carried type is synced to the
 * client so the renderer can tint the duct's overlay.
 *
 * <p>The block entity is authoritative for the type and the graph follows it: {@link #clearRemoved}
 * reconciles the cell on every chunk load, which is also what gives a duct placed before the fluid
 * graph existed a cell at all.
 */
public class TileEntityFluidDuctNT extends CompatBlockEntity implements IFluidPipeMK2, ILoadedTile {

    protected FluidType type = Fluids.NONE;

    public TileEntityFluidDuctNT(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DUCT.get(), pos, state);
    }

    /** For duct variants registered under their own block-entity type (see {@code com.hbm.fluidnet}). */
    protected TileEntityFluidDuctNT(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidType getFluidType() {
        return this.type;
    }

    public void setFluidType(FluidType type) {
        if (this.type == type) return;
        this.type = type;
        setChanged();
        if (level instanceof ServerLevel server) {
            FluidGraph.get(server).replaceNodeData(getBlockPos().asLong(), ductData());
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * The datum this duct's graph cell carries: its one fluid. The exhaust duct answers
     * {@link DuctData#SMOKE_ANY} instead, whatever smoke it is skinned as.
     */
    protected DuctData ductData() {
        return DuctData.of(type);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<DuctData> graph = FluidGraph.get(server);
        long key = getBlockPos().asLong();
        if (graph.containsCell(key)) graph.replaceNodeData(key, ductData());
        else graph.addNode(key, ductData(), MachineCaps.FACE_MASK_ALL);
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
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && type == this.type;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        FluidGraph.dropCells(this, getBlockPos());
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("type", this.type.getID());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.type = Fluids.fromID(input.getIntOr("type", Fluids.NONE.getID()));
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
