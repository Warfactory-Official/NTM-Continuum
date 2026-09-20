package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 1.7 {@code BlockChain} (dungeon_chain): a climbable chain that hangs from the block above (old meta 0)
 * or flat against a wall (meta 2-5). {@link #FACING} is the side the chain faces, away from its
 * support, as the 1.12 blockstate spelt it; {@link #END} is the old "is the block below not holding
 * the chain" test that swapped in the {@code chain_end} icon and lifted the box by a quarter block —
 * a property here because a model cannot look at its neighbour.
 */
public class BlockChain extends CompatBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    public static final BooleanProperty END = BooleanProperty.create("end");

    public BlockChain(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WALL, false).setValue(END, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL, END);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double minY = state.getValue(END) ? 4 : 0;
        if (!state.getValue(WALL)) return Shapes.box(6 / 16D, minY / 16D, 6 / 16D, 10 / 16D, 1, 10 / 16D);
        return switch (state.getValue(FACING)) {
            case SOUTH -> Shapes.box(6 / 16D, minY / 16D, 0, 10 / 16D, 1, 2 / 16D);
            case WEST -> Shapes.box(14 / 16D, minY / 16D, 6 / 16D, 1, 1, 10 / 16D);
            case EAST -> Shapes.box(0, minY / 16D, 6 / 16D, 2 / 16D, 1, 10 / 16D);
            default -> Shapes.box(6 / 16D, minY / 16D, 14 / 16D, 10 / 16D, 1, 1);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    private static boolean sturdy(LevelReader level, BlockPos pos, Direction face) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, face);
    }

    /** The old {@code isSideSolid(below, UP) || (below is this chain with the same meta)} test. */
    private static boolean heldBelow(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos below = pos.below();
        if (sturdy(level, below, Direction.UP)) return true;
        BlockState other = level.getBlockState(below);
        return other.is(state.getBlock()) && sameAttachment(other, state);
    }

    private static boolean sameAttachment(BlockState a, BlockState b) {
        return a.getValue(WALL) == b.getValue(WALL) && (!a.getValue(WALL) || a.getValue(FACING) == b.getValue(FACING));
    }

    private static BlockState withEnd(LevelReader level, BlockPos pos, BlockState state) {
        return state.setValue(END, !heldBelow(level, pos, state));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        BlockState state = null;
        if (side.getAxis().isHorizontal() && sturdy(level, pos.relative(side.getOpposite()), side)) {
            state = defaultBlockState().setValue(WALL, true).setValue(FACING, side);
        }
        if (state == null) {
            BlockState above = level.getBlockState(pos.above());
            if (above.is(this)) {
                state = above;
            } else if (sturdy(level, pos.above(), Direction.DOWN)) {
                state = defaultBlockState();
            }
        }
        if (state == null) {
            for (Direction facing : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                if (sturdy(level, pos.relative(facing.getOpposite()), facing)) {
                    state = defaultBlockState().setValue(WALL, true).setValue(FACING, facing);
                    break;
                }
            }
        }
        if (state == null) return null;
        return withEnd(level, pos, state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (sturdy(level, pos.above(), Direction.DOWN) || level.getBlockState(pos.above()).is(this)) return true;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (sturdy(level, pos.relative(facing.getOpposite()), facing)) return true;
        }
        return false;
    }

    private boolean isHeld(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(this) && sameAttachment(above, state)) return true;
        if (sturdy(level, pos.above(), Direction.DOWN) && !state.getValue(WALL)) return true;
        if (state.getValue(WALL)) {
            Direction facing = state.getValue(FACING);
            return sturdy(level, pos.relative(facing.getOpposite()), facing);
        }
        return false;
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        return withEnd(level, pos, state);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (!isHeld(state, level, pos)) {
            level.destroyBlock(pos, true);
        }
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
