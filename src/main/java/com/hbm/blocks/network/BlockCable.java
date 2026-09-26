package com.hbm.blocks.network;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.network.power.PowerGraph;
import com.hbm.network.power.PowerGraphProvider;
import dev.golem.core.graph.Domains;
import dev.golem.core.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import dev.golem.core.registry.TickingBlock;
import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.network.TileEntityCableBaseNT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * HBM's red power cable: a connecting conductor. Six {@link BooleanProperty} connection flags drive a
 * multipart model (a central core + an arm per connected side), like vanilla {@code PipeBlock}. It
 * connects to any neighbour whose BlockEntity is an {@link IEnergyConnectorMK2} (cables, machines,
 * batteries, generators). These flags are purely the visual/collision shape; conduction is the graph
 * cell this block owns, which lives and dies with the block rather than with its BlockEntity.
 *
 * <p>Ported from 1.12.2's {@code BlockCable} (which used an OBJ-baked model + {@code getActualState});
 * the 26.1 equivalent is connection properties + a multipart blockstate + {@code updateShape}.
 */
public class BlockCable extends dev.golem.core.world.CompatBlock implements TickingBlock, SimpleWaterloggedBlock {

    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.DOWN, DOWN, Direction.UP, UP, Direction.NORTH, NORTH,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST, Direction.EAST, EAST);

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARM = Map.of(
            Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11),
            Direction.UP, Block.box(5, 11, 5, 11, 16, 11),
            Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5),
            Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16),
            Direction.WEST, Block.box(0, 5, 5, 5, 11, 11),
            Direction.EAST, Block.box(11, 5, 5, 16, 11, 11));

    public BlockCable(Properties properties) {
        super(properties);
        BlockState def = this.stateDefinition.any();
        for (BooleanProperty p : PROPERTY_BY_DIRECTION.values()) def = def.setValue(p, false);
        this.registerDefaultState(def.setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST, WATERLOGGED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Drawn by CableObjRenderer (the real cable_neo.obj), so the block model itself is invisible.
        return RenderShape.INVISIBLE;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /**
     * Whether the cable should visually/physically connect to the block on side {@code dir}. Mirrors the
     * 1.12.2 {@code Library.canConnect}: a block-level {@link IEnergyConnectorBlock} (so multiblock
     * machine footprints connect without a BlockEntity at every cell) OR a tile-level
     * {@link IEnergyConnectorMK2}. {@code dir.getOpposite()} is the neighbour's side facing the cable.
     */
    private static boolean canConnectTo(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos neighbour = pos.relative(dir);
        ForgeDirection neighbourSide = ForgeDirection.getOrientation(dir.getOpposite());

        if (level.getBlockState(neighbour).getBlock() instanceof IEnergyConnectorBlock connectorBlock
                && connectorBlock.canConnect(level, neighbour, neighbourSide.toDirection())) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(neighbour);
        return be instanceof IEnergyConnectorMK2 connector && connector.canConnect(neighbourSide.toDirection());
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState()
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet()) {
            state = state.setValue(e.getValue(), canConnectTo(level, pos, e.getKey()));
        }
        return state;
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour),
                canConnectTo(level, pos, directionToNeighbour));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            PowerGraph.get(server).addNode(pos.asLong(), PowerGraphProvider.INSTANCE.createData(state),
                    Domains.FACES_ALL);
        }
    }

    /**
     * A block appearing beside a conductor is the one endpoint transition nothing else announces. Our
     * own machines say so from {@code clearRemoved}, but a foreign one has no reason to, and a face
     * that has already probed absent stays absent until something asks it again.
     */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet()) {
            if (state.getValue(e.getValue())) shape = Shapes.or(shape, ARM.get(e.getKey()));
        }
        return shape;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCableBaseNT(pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return ConnectionRing.rotate(state, rotation, PROPERTY_BY_DIRECTION);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return ConnectionRing.mirror(state, mirror, PROPERTY_BY_DIRECTION);
    }
}
