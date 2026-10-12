package com.hbm.blocks.generic;

import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import dev.golem.core.world.BlockTickScheduler;

/**
 * The chain-link fence, port of 1.7.10's {@code BlockMetalFence} (a {@code BlockFence} with two
 * metadata sub-blocks). Metadata 0 is {@code fence_metal}: the post shows only where the run is not a
 * straight line; metadata 1 is {@code fence_metal_post}: the post always shows. Both connect to
 * each other, to fence gates and to opaque full blocks, exactly like the old {@code canConnectFenceTo}.
 *
 * <p>Collision is the old {@code addCollisionBoxesToList}: a 4-px post column plus 4-px arms, one
 * block tall (not the 1.5 of a vanilla fence). The visuals — flat chain-link panes through the
 * block centre and the post — are the hand-authored multipart blockstate; the post's visibility rule
 * is spelled out there as the six "not a straight run" cases. Waterloggable (port addition, see
 * {@link Waterlogging}).
 */
public class BlockMetalFence extends CompatBlock implements SimpleWaterloggedBlock {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final VoxelShape POST = Block.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape ARM_NORTH = Block.box(6, 0, 0, 10, 16, 6);
    private static final VoxelShape ARM_SOUTH = Block.box(6, 0, 10, 10, 16, 16);
    private static final VoxelShape ARM_WEST = Block.box(0, 0, 6, 6, 16, 10);
    private static final VoxelShape ARM_EAST = Block.box(10, 0, 6, 16, 16, 10);

    /** Whether this is the {@code fence_metal_post} variant (old metadata 1). */
    private final boolean forcePost;

    public BlockMetalFence(Properties properties, boolean forcePost) {
        super(properties);
        this.forcePost = forcePost;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(Waterlogging.WATERLOGGED, false));
    }

    public boolean forcePost() {
        return forcePost;
    }

    /**
     * Blocks another mod adds to the fence's connections (block tag {@code hbm:metal_fence_connects}; HBM puts nothing
     * in it): hbm-space's chain-link fence gate, which its {@code canConnectFenceTo} named. A holder, so the tag key is
     * made only in a running game.
     */
    public static final class Connects {
        private Connects() {}

        public static final net.minecraft.tags.TagKey<Block> TAG = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.BLOCK, dev.golem.core.Ids.of("hbm", "metal_fence_connects"));
    }

    /**
     * 1.7.10 {@code BlockFence.canConnectFenceTo}: another metal fence, a fence gate, or an opaque normal cube; and the
     * {@link Connects#TAG} blocks.
     */
    public boolean canConnectFenceTo(BlockGetter level, BlockPos neighbour, Direction towards) {
        BlockState state = level.getBlockState(neighbour);
        if (state.getBlock() instanceof BlockMetalFence || state.getBlock() instanceof FenceGateBlock) return true;
        if (state.is(Connects.TAG)) return true;
        return !Block.isExceptionForConnection(state) && state.isFaceSturdy(level, neighbour, towards.getOpposite())
                && dev.golem.core.world.Compat.isSolidRender(state);
    }

    private BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(NORTH, canConnectFenceTo(level, pos.north(), Direction.NORTH))
                .setValue(EAST, canConnectFenceTo(level, pos.east(), Direction.EAST))
                .setValue(SOUTH, canConnectFenceTo(level, pos.south(), Direction.SOUTH))
                .setValue(WEST, canConnectFenceTo(level, pos.west(), Direction.WEST));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return Waterlogging.place(connections(this.defaultBlockState(), context.getLevel(), context.getClickedPos()), context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Waterlogging.fluid(state);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        Waterlogging.tick(state, level, ticks, pos);
        if (direction.getAxis().isHorizontal()) {
            return connections(state, level, pos);
        }
        return state;
    }

    private VoxelShape shapeFor(BlockState state) {
        VoxelShape shape = POST;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_WEST);
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_EAST);
        return shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_180 -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH)).setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state.setValue(NORTH, state.getValue(EAST)).setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST)).setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state.setValue(NORTH, state.getValue(WEST)).setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST)).setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default -> state;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, Waterlogging.WATERLOGGED);
    }
}
