package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's waterlogging pattern for HBM's non-full deco blocks (a port addition: 1.7 had no waterlogging).
 * A block that uses it implements {@link net.minecraft.world.level.block.SimpleWaterloggedBlock} (bucket fill and
 * pick-up), adds {@link #WATERLOGGED} to its state definition, and routes placement, {@code getFluidState} and
 * {@code onUpdateShape} through here.
 */
public final class Waterlogging {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private Waterlogging() {}

    /** The placement state, waterlogged when it replaces a water source. Null passes through (placement refused). */
    public static @Nullable BlockState place(@Nullable BlockState state, BlockPlaceContext context) {
        return state == null ? null : place(state, context.getLevel(), context.getClickedPos());
    }

    public static BlockState place(BlockState state, LevelReader level, BlockPos pos) {
        return state.setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    /**
     * A structure block written over the world: any waterloggable state (HBM's or vanilla's chains, bars, …) that
     * replaces a water source holds it, as vanilla {@code StructureTemplate} placement does. A state already saved
     * waterlogged stays so; everything else passes through unchanged.
     */
    public static BlockState inWorld(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.hasProperty(WATERLOGGED) || state.getValue(WATERLOGGED)) return state;
        return level.getFluidState(pos).getType() == Fluids.WATER ? state.setValue(WATERLOGGED, true) : state;
    }

    public static FluidState fluid(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    /** Keeps the held water flowing into neighbours, as every vanilla waterloggable does in {@code updateShape}. */
    public static void tick(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }
}
