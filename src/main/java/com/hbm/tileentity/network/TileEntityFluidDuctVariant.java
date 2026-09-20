package com.hbm.tileentity.network;

import com.hbm.tileentity.ModFluidNetBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code TileEntityPipeBaseNT} for every duct in {@code com.hbm.fluidnet} that is a plain typed
 * conductor: the three {@code fluid_duct_neo} skins, the paintable duct, the box ducts and the gauge's
 * parent. Same behaviour as {@link TileEntityFluidDuctNT}; only the block-entity type differs so the
 * type's valid-block set can name the variant blocks.
 */
public class TileEntityFluidDuctVariant extends TileEntityFluidDuctNT {

    public TileEntityFluidDuctVariant(BlockPos pos, BlockState state) {
        super(typeFor(state), pos, state);
    }

    /** One tile class serves three block-entity types, split by renderer: neo pipe, box duct, paintable cube. */
    private static net.minecraft.world.level.block.entity.BlockEntityType<?> typeFor(BlockState state) {
        if (state.getBlock() instanceof com.hbm.blocks.network.FluidDuctBox) return ModFluidNetBlockEntities.BOX.get();
        if (state.getBlock() instanceof com.hbm.blocks.network.FluidDuctPaintable) return ModFluidNetBlockEntities.PAINTABLE.get();
        return ModFluidNetBlockEntities.DUCT.get();
    }

    protected TileEntityFluidDuctVariant(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
