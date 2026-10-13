package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** Vanilla ladder behavior with the 1.12.2 {@code ladder_red_top} cap exception. */
public class BlockNTMLadder extends LadderBlock {

    private static final VoxelShape NORTH_TOP = Block.box(0, 0, 14, 16, 4, 16);
    private static final VoxelShape SOUTH_TOP = Block.box(0, 0, 0, 16, 4, 2);
    private static final VoxelShape WEST_TOP = Block.box(14, 0, 0, 16, 4, 16);
    private static final VoxelShape EAST_TOP = Block.box(0, 0, 0, 2, 4, 16);

    private final boolean top;

    public BlockNTMLadder(Properties properties, boolean top) {
        super(properties);
        this.top = top;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return top || super.canSurvive(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        if (!top) return super.getShape(state, level, pos, context);
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_TOP;
            case WEST -> WEST_TOP;
            case EAST -> EAST_TOP;
            default -> NORTH_TOP;
        };
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return top
                ? defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                : super.getStateForPlacement(context);
    }
}
