package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.blocks.network.FluidValve;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.uninos.UniNodespace;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The typed network node owned by {@link FluidValve}. */
public class TileEntityFluidValve extends CompatBlockEntity implements IFluidPipeMK2, ILoadedTile {

    protected FluidNode node;
    protected FluidType type = Fluids.NONE;

    public TileEntityFluidValve(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FLUID_VALVE.get(), pos, state);
    }

    protected TileEntityFluidValve(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
            BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFluidValve valve) {
        if (!valve.isOpen() || valve.type == Fluids.NONE) {
            valve.destroyNode(level);
            return;
        }

        if (valve.node == null || valve.node.expired) {
            valve.node = (FluidNode) UniNodespace.getNode(level, pos, valve.type.getNetworkProvider());
            if (valve.node == null || valve.node.expired) {
                valve.node = valve.createNode(valve.type);
                UniNodespace.createNode(level, valve.node);
            }
        }
    }

    public void updateState() {
        if (level != null && !level.isClientSide() && !isOpen()) destroyNode(level);
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
        if (level != null && !level.isClientSide()) destroyNode(level);
        this.type = type;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return isOpen() && this.type != Fluids.NONE && dir != ForgeDirection.UNKNOWN && this.type == type;
    }

    private void destroyNode(Level level) {
        if (node != null) {
            UniNodespace.destroyNode(level, node);
            node = null;
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) destroyNode(level);
        super.setRemoved();
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
