package com.hbm.blocks.machine;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import com.hbm.blocks.generic.SpotlightBeams;

/**
 * The invisible light-emitting block a spotlight or tritium lamp lays down along its beam (1.7
 * {@code SpotlightBeam} over {@code BlockBeamBase}).
 *
 * <p>The old block kept its six incoming-light bits in a {@code TileEntityData}; here they are six
 * boolean properties, and the old block metadata (the beam colour, {@code META_YELLOW/GREEN/BLUE}) is
 * {@link #COLOR}. The old {@code Material.air} + {@code isAir()} override is <b>not</b> reproduced with
 * {@code Properties.air()}: a modern chunk section holding only air-flagged blocks reports itself
 * empty and hands back vanilla air from {@code getBlockState}, which would lose a beam that crosses
 * open sky. {@code replaceable()} plus an empty shape gives the same placement and light behaviour.
 */
public class SpotlightBeam extends CompatBlock {

    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 2);
    public static final Map<Direction, BooleanProperty> DIRECTIONS = new EnumMap<>(Direction.class);

    static {
        DIRECTIONS.put(Direction.DOWN, BlockStateProperties.DOWN);
        DIRECTIONS.put(Direction.UP, BlockStateProperties.UP);
        DIRECTIONS.put(Direction.NORTH, BlockStateProperties.NORTH);
        DIRECTIONS.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        DIRECTIONS.put(Direction.WEST, BlockStateProperties.WEST);
        DIRECTIONS.put(Direction.EAST, BlockStateProperties.EAST);
    }

    public SpotlightBeam(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(COLOR, 0);
        for (BooleanProperty property : DIRECTIONS.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
        for (BooleanProperty property : DIRECTIONS.values()) builder.add(property);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
    // Skylight passes: vanilla derives propagatesSkylightDown from the empty shape (its signature differs per era).
    /** Old {@code getDirections}: every direction whose incoming-light bit is set. */
    public static List<Direction> getDirections(BlockState state) {
        List<Direction> directions = new ArrayList<>(6);
        if (!(state.getBlock() instanceof SpotlightBeam)) return directions;
        for (Direction dir : Direction.values()) {
            if (state.getValue(DIRECTIONS.get(dir))) directions.add(dir);
        }
        return directions;
    }

    /** Old {@code setDirection}: flips one bit and returns how many bits remain, so the caller may remove the beam. */
    public static int setDirection(Level level, BlockPos pos, Direction dir, boolean lit) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SpotlightBeam)) return 0; // shouldn't happen; cancel propagation
        BlockState next = state.setValue(DIRECTIONS.get(dir), lit);
        if (next != state) level.setBlock(pos, next, 3);
        int count = 0;
        for (BooleanProperty property : DIRECTIONS.values()) if (next.getValue(property)) count++;
        return count;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction dir : Direction.values()) {
            rotated = rotated.setValue(DIRECTIONS.get(rotation.rotate(dir)), state.getValue(DIRECTIONS.get(dir)));
        }
        return rotated;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction dir : Direction.values()) {
            mirrored = mirrored.setValue(DIRECTIONS.get(mirror.mirror(dir)), state.getValue(DIRECTIONS.get(dir)));
        }
        return mirrored;
    }

    // If a block is placed onto the beam, handle the new cutoff
    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level.isClientSide()) return;
        for (Direction dir : getDirections(state)) {
            SpotlightBeams.unpropagateBeam(level, pos, dir);
        }
    }

    // If a block in the beam path is removed, repropagate beam
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (neighbourBlock instanceof SpotlightBeam) return;
        for (Direction dir : getDirections(state)) {
            SpotlightBeams.backPropagate(level, pos, dir, state.getValue(COLOR));
        }
    }
}
