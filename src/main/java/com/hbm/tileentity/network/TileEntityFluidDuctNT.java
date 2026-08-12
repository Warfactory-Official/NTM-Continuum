package com.hbm.tileentity.network;

import dev.golem.core.data.CompatBlockEntity;
import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * A solid fluid duct: a pure fluid conductor, the fluid analogue of {@link TileEntityCableBaseNT}. It
 * owns a {@link FluidNode} in UNINOS keyed to a single {@link FluidType}, which the engine links into a
 * network with adjacent ducts of the same type. Carries no fluid itself — providers/receivers subscribe
 * to the network the ducts form.
 *
 * <p>Like the 1.12.2 {@code fluid_duct_mk2}, the duct defaults to {@code Fluids.NONE} (transfers nothing)
 * and the player sets its type by right-clicking it with a {@code fluid_identifier} — it then forms a
 * network with adjacent ducts/machines of that one fluid only. The carried type is synced to the client
 * (chunk-load tag + update packet) so the renderer can tint the duct's overlay with the fluid's colour.
 */
public class TileEntityFluidDuctNT extends CompatBlockEntity implements IFluidPipeMK2, ILoadedTile {

    protected FluidNode node;
    protected FluidType type = Fluids.NONE;

    public TileEntityFluidDuctNT(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_DUCT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFluidDuctNT duct) {
        if (duct.type == Fluids.NONE) return; // an untyped duct carries nothing
        if (duct.node == null || duct.node.expired) {
            duct.node = (FluidNode) UniNodespace.getNode(level, pos, duct.type.getNetworkProvider());
            if (duct.node == null || duct.node.expired) {
                duct.node = duct.createNode(duct.type);
                UniNodespace.createNode(level, duct.node);
            }
        }
    }

    public FluidType getFluidType() {
        return this.type;
    }

    public void setFluidType(FluidType type) {
        if (this.type == type) return;
        // Tear down the node in the old type's nodespace; serverTick rebuilds it under the new type.
        if (this.node != null && level != null && !level.isClientSide()) {
            UniNodespace.destroyNode(level, this.node);
        }
        this.type = type;
        this.node = null;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3); // push the new type to clients
        }
    }

    // --- client sync of the carried type (for the colour-tinted overlay) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
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
        if (level != null && !level.isClientSide() && this.node != null) {
            UniNodespace.destroyNode(level, this.node);
        }
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
