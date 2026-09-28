package com.hbm.blocks.generic;

import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockWoodStructure}, flattened: {@code wood_structure_roof}, {@code _scaffold} and {@code _ceiling},
 * all in the {@code wood_barrier} texture. Its ISBRH joined neighbours of the same kind: a roof's planks run
 * through to a roof beside it on the X axis; a scaffold drops its side rail towards a scaffold next to it and its
 * top plank under one above it, stretching its posts up to meet it. Those joins are the block's own state here,
 * picked by the multipart model. The scaffold is climbable (1.7 set {@code isOnLadder}) and its top is solid.
 */
public class BlockWoodStructure extends CompatBlock {

    public enum EnumWoodStructure { ROOF, SCAFFOLD, CEILING }

    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;

    private static final VoxelShape ROOF = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape SCAFFOLD = Block.box(1, 0, 1, 15, 16, 15);
    private static final VoxelShape CEILING = Block.box(0, 14, 0, 16, 16, 16);

    public final EnumWoodStructure type;

    public BlockWoodStructure(Properties properties, EnumWoodStructure type) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(UP, false).setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(EAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, NORTH, SOUTH, WEST, EAST);
    }

    /** The joins this kind draws; the roof only looks along X, the ceiling at nothing. */
    private BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (type) {
            case ROOF -> state.setValue(WEST, same(level, pos.west())).setValue(EAST, same(level, pos.east()));
            case SCAFFOLD -> state.setValue(UP, same(level, pos.above())).setValue(NORTH, same(level, pos.north()))
                    .setValue(SOUTH, same(level, pos.south())).setValue(WEST, same(level, pos.west()))
                    .setValue(EAST, same(level, pos.east()));
            case CEILING -> state;
        };
    }

    private boolean same(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == this;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighborPos, BlockState neighborState) {
        return connect(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (type) {
            case ROOF -> ROOF;
            case CEILING -> CEILING;
            // 1.7 getSelectedBoundingBoxFromPool: the whole cell
            case SCAFFOLD -> Shapes.block();
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (type) {
            case ROOF -> ROOF;
            case CEILING -> CEILING;
            case SCAFFOLD -> SCAFFOLD;
        };
    }

    /** Structure rotation and mirroring move the side connections, as vanilla's fences and panes do. */
    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
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
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> state.setValue(NORTH, state.getValue(SOUTH)).setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state.setValue(EAST, state.getValue(WEST)).setValue(WEST, state.getValue(EAST));
            default -> state;
        };
    }
}
