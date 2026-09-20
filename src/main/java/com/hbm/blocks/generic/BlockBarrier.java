package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The wooden barrier, port of 1.7.10's {@code BlockBarrier}: a fence-like panel that grows a post
 * and two rails on every side that touches an opaque block, plus the side it was placed against
 * ({@link #FACING}), and a rail frame under an opaque block above. 1.7.10 read the neighbours at
 * render time; here they are the {@link #NORTH}/{@link #EAST}/{@link #SOUTH}/{@link #WEST}/{@link #UP}
 * properties the hand-authored multipart blockstate selects on, refreshed on every neighbour change.
 *
 * <p>Collision: a 2-px full-height slab per present side (old {@code addCollisionBoxesToList});
 * the outline is only the placed side's slab (old {@code setBlockBoundsBasedOnState}).
 */
public class BlockBarrier extends CompatBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;

    private static final Map<Direction, VoxelShape> SLAB = new EnumMap<>(Direction.class);
    private static final Map<Direction, BooleanProperty> SIDE = new EnumMap<>(Direction.class);
    static {
        SLAB.put(Direction.WEST, Block.box(0, 0, 0, 2, 16, 16));
        SLAB.put(Direction.EAST, Block.box(14, 0, 0, 16, 16, 16));
        SLAB.put(Direction.NORTH, Block.box(0, 0, 0, 16, 16, 2));
        SLAB.put(Direction.SOUTH, Block.box(0, 0, 14, 16, 16, 16));
        SIDE.put(Direction.NORTH, NORTH);
        SIDE.put(Direction.EAST, EAST);
        SIDE.put(Direction.SOUTH, SOUTH);
        SIDE.put(Direction.WEST, WEST);
    }

    public BlockBarrier(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false));
    }

    /** 1.7.10 {@code isOpaqueCube() || isNormalCube()} on the neighbour. */
    private static boolean solid(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return Compat.isSolidRender(state) || state.isRedstoneConductor(level, pos);
    }

    private static BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            state = state.setValue(SIDE.get(dir), facing == dir || solid(level, pos.relative(dir)));
        }
        return state.setValue(UP, solid(level, pos.above()));
    }

    /**
     * 1.7.10 {@code onBlockPlaced} + {@code onBlockPlacedBy}: placed against a side face the panel
     * sits on that face; placed on a floor or ceiling it sits on the side the player is facing.
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face.getOpposite() : context.getHorizontalDirection();
        return connections(this.defaultBlockState().setValue(FACING, facing), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        return connections(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SLAB.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (state.getValue(SIDE.get(dir))) shape = Shapes.or(shape, SLAB.get(dir));
        }
        return shape;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, EAST, SOUTH, WEST, UP);
    }
}
