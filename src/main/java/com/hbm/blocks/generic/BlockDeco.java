package com.hbm.blocks.generic;

import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Decorative building blocks ported from 1.12.2 {@code DecoBlock}: a horizontal-facing, non-occluding block
 * whose <b>visual</b> comes entirely from its (mostly-transparent) JSON model — 26.1 auto-classifies such a
 * model as the cutout render layer from the sprite's transparency, so no render-layer registration is needed.
 *
 * <p>The {@link Shape} fixes the <b>collision/outline</b> box, which for several variants is a thin slice that
 * does not match the full-cube visual (faithful to the original {@code getBoundingBox}/{@code addCollisionBox}
 * switch): {@code WALL} = a 2px slab on one side, {@code CORNER} = two such slabs, {@code ROOF} = a 1px floor,
 * {@code BEAM} = a central 2×2 post, {@code FULL} = a normal cube. The old easter-egg statue variants
 * ({@code DecoBlockAlt}) are intentionally not ported here. Waterloggable (port addition, see {@link Waterlogging}).
 */
public class BlockDeco extends dev.golem.core.world.CompatBlock implements SimpleWaterloggedBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public enum Shape { FULL, WALL, CORNER, ROOF, BEAM }

    // Per-facing (NORTH/SOUTH/WEST/EAST) collision boxes, in 1/16 units, mirroring the 1.12.2 AABBs.
    private static final VoxelShape WALL_WEST  = Block.box(14, 0, 0, 16, 16, 16);
    private static final VoxelShape WALL_EAST  = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape WALL_NORTH = Block.box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(0, 0, 0, 16, 16, 2);
    private static final VoxelShape ROOF  = Block.box(0, 0, 0, 16, 1, 16);
    private static final VoxelShape BEAM  = Block.box(7, 0, 7, 9, 16, 9);

    private static final Map<Direction, VoxelShape> WALL   = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> CORNER = new EnumMap<>(Direction.class);
    static {
        WALL.put(Direction.WEST, WALL_WEST);
        WALL.put(Direction.EAST, WALL_EAST);
        WALL.put(Direction.NORTH, WALL_NORTH);
        WALL.put(Direction.SOUTH, WALL_SOUTH);
        // The 1.12.2 corner joins two adjacent wall slabs per facing.
        CORNER.put(Direction.EAST,  Shapes.or(WALL_EAST, WALL_SOUTH));
        CORNER.put(Direction.NORTH, Shapes.or(WALL_NORTH, WALL_EAST));
        CORNER.put(Direction.SOUTH, Shapes.or(WALL_SOUTH, WALL_WEST));
        CORNER.put(Direction.WEST,  Shapes.or(WALL_WEST, WALL_NORTH));
    }

    private final Shape shape;

    public BlockDeco(Properties properties, Shape shape) {
        super(properties);
        this.shape = shape;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Waterlogging.WATERLOGGED, false));
    }

    private VoxelShape shapeFor(BlockState state) {
        Direction facing = state.getValue(FACING);
        return switch (shape) {
            case WALL   -> WALL.getOrDefault(facing, Shapes.block());
            case CORNER -> CORNER.getOrDefault(facing, Shapes.block());
            case ROOF   -> ROOF;
            case BEAM   -> BEAM;
            case FULL   -> Shapes.block();
        };
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
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Faithful to DecoBlock: the block faces the same way the player looks (not the opposite).
        return Waterlogging.place(this.defaultBlockState().setValue(FACING, context.getHorizontalDirection()), context);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Waterlogging.fluid(state);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        Waterlogging.tick(state, level, ticks, pos);
        return state;
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
        builder.add(FACING, Waterlogging.WATERLOGGED);
    }
}
