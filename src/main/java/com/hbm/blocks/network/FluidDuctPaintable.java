package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityFluidDuctVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code FluidDuctPaintable}: a full-cube coated duct — a steel cube with a fluid-coloured port
 * overlay on every face. Networking is the plain duct's (the connection flags only feed the renderer's
 * overlay).
 *
 * <p><b>Deferred:</b> 1.7 let a solid block be right-clicked onto it as "paint" (the cube then wore
 * that block's texture), cleared with a screwdriver, with the port overlay toggled by a defuser and
 * copied by the copy tool. Painting is not ported: drawing an arbitrary block's model over a duct has no
 * era-neutral rendering path yet. The duct renders its own coat only. The tooltip lines
 * ({@code block.hbm.fluid_duct_paintable.desc.N}) still advertise painting, faithfully to 1.7's text;
 * the behaviour behind them is what is deferred.
 */
public class FluidDuctPaintable extends BlockFluidDuct {

    public FluidDuctPaintable(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidDuctVariant(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** The coated ducts are ordinary cube models (1.7 pass 0 = the block icon); only the overlay is a BER. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** A full cube never holds water (the parent's waterlogging is for the thin pipe). */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(WATERLOGGED, false);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }
}
