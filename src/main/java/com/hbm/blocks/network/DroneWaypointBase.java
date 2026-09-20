package com.hbm.blocks.network;

import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code DroneWaypoint}/{@code DroneWaypointRequest}: a torch-shaped marker (drawn through the radio torch's
 * renderer) mounted on any side of a solid block, with no collision box and the 1.7 six-pixel selection box.
 */
public abstract class DroneWaypointBase extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private static final VoxelShape[] SHAPES = buildShapes();

    protected DroneWaypointBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    /** 1.7 collisionRayTrace: 0.375..0.625 across, extended to the mounting face along the facing axis. */
    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[6];
        for (Direction dir : Direction.values()) {
            shapes[dir.ordinal()] = Shapes.box(
                    dir.getStepX() == 1 ? 0 : 0.375, dir.getStepY() == 1 ? 0 : 0.375, dir.getStepZ() == 1 ? 0 : 0.375,
                    dir.getStepX() == -1 ? 1 : 0.625, dir.getStepY() == -1 ? 1 : 0.625, dir.getStepZ() == -1 ? 1 : 0.625);
        }
        return shapes;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 1.7 onBlockPlaced: the meta is the clicked side. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** 1.7 canPlaceBlockOnSide: the support face must be solid, or the block a plain full cube. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        BlockState support = level.getBlockState(supportPos);
        return support.isFaceSturdy(level, supportPos, facing)
                || (Compat.isSolidRender(support, level, supportPos) && !support.isAir());
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState() : state;
    }

    /** 1.7 onNeighborBlockChange: drop as an item when the support goes. */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (!level.isClientSide() && !state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    /** 1.7 getCollisionBoundingBoxFromPool: none. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
