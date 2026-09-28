package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import dev.golem.core.registry.TickingBlock;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.tileentity.network.TileEntityFluidValve;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HBM's solid fluid duct: a connecting fluid conductor backed by {@link TileEntityFluidDuctNT}. Six
 * {@link BooleanProperty} connection flags drive the {@code pipe_neo.obj} model (via
 * {@link com.hbm.client.render.FluidDuctObjRenderer}), exactly like the power cable's
 * {@code cable_neo.obj}. Conduction is a cell of the level's fluid graph, which the block entity keeps
 * in step with the type it carries; these flags are purely the visual/collision shape.
 *
 * <p>The duct's fluid type defaults to {@code NONE} (no connections) and is set by right-clicking it with
 * a {@code fluid_identifier}: the connection flags are then recomputed against that one fluid, so a typed
 * duct only links to neighbours carrying the same fluid.
 */
public class BlockFluidDuct extends dev.golem.core.world.CompatBlock implements TickingBlock, SimpleWaterloggedBlock, ILookOverlay,
        com.hbm.blocks.IAnalyzable {

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

    public BlockFluidDuct(Properties properties) {
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
        // Drawn by FluidDuctObjRenderer (the real pipe_neo.obj), so the block model itself is invisible.
        return RenderShape.INVISIBLE;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? net.minecraft.world.level.material.Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    /**
     * Whether the duct at {@code pos} should connect on side {@code dir}: whether the neighbour can connect
     * with the duct's current fluid type. Reads the duct's type from its BlockEntity — so two untyped
     * (NONE) ducts still connect to each other (you can lay a line, then type it), a typed duct links only
     * to same-type ducts, and either connects to a machine port (which accepts any type).
     */
    private static boolean canConnectTo(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos neighbour = pos.relative(dir);
        ForgeDirection ductSide = ForgeDirection.getOrientation(dir);
        return Library.canConnectFluid(level, neighbour, ductSide, ductType(level, pos));
    }

    private static FluidType ductType(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityFluidDuctNT duct ? duct.getFluidType() : Fluids.NONE;
    }

    /** Recomputes all six connection flags from the duct's current type (after the type changes). */
    private static BlockState recomputeConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet()) {
            state = state.setValue(e.getValue(), canConnectTo(level, pos, e.getKey()));
        }
        return state;
    }

    /**
     * Retypes the connected old-style pipe network, matching fluid_identifier's sneaking behavior. The whole run is
     * retyped first and its arms recomputed afterwards, as ntm-next's {@code retypeComponent} does: recomputing
     * pipe by pipe during the walk left stale arms. An untyped pipe already reaches toward a freshly typed
     * neighbour, so when its own turn came its state did not change, {@code setBlock} did nothing, and the
     * neighbour was never told to grow its arm back.
     */
    public static void spreadType(Level level, BlockPos pos, FluidType hand, FluidType pipe, int remaining) {
        java.util.List<BlockPos> retyped = new java.util.ArrayList<>();
        retype(level, pos, hand, pipe, remaining, retyped);
        for (BlockPos p : retyped) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof BlockFluidDuct) {
                level.setBlock(p, recomputeConnections(state, level, p), 3);
            } else {
                level.updateNeighborsAt(p, state.getBlock());
            }
        }
    }

    private static void retype(Level level, BlockPos pos, FluidType hand, FluidType pipe, int remaining, java.util.List<BlockPos> retyped) {
        if (remaining <= 0) return;

        BlockEntity entity = level.getBlockEntity(pos);
        FluidType current;
        if (entity instanceof TileEntityFluidDuctNT duct) {
            current = duct.getFluidType();
            if (current != pipe || current == hand) return;
            duct.setFluidType(hand);
            if (duct.getFluidType() != hand) return; // refused (an exhaust takes only the smokes): nothing to spread through
        } else if (entity instanceof TileEntityFluidValve valve) {
            current = valve.getFluidType();
            if (current != pipe || current == hand) return;
            valve.setFluidType(hand);
        } else {
            return;
        }
        retyped.add(pos.immutable());

        if (remaining == 1) return;
        for (Direction direction : Direction.values()) {
            retype(level, pos.relative(direction), hand, pipe, remaining - 1, retyped);
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState()
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == net.minecraft.world.level.material.Fluids.WATER);
        for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet()) {
            state = state.setValue(e.getValue(), canConnectTo(level, pos, e.getKey()));
        }
        return state;
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER,
                    net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        }
        return state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour),
                canConnectTo(level, pos, directionToNeighbour));
    }

    /**
     * A block appearing beside a duct is the one endpoint transition nothing else announces. Our own
     * machines say so from {@code clearRemoved}, and a face that has already probed absent stays absent
     * until something asks it again.
     */
    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAt(server, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The hitbox must match what FluidDuctObjRenderer.selectParts DRAWS, not just the raw connection
        // flags: a duct with a single connection on one axis (or none) renders a full straight pipe (both
        // arms of that axis), so the hitbox has to cover the opposite arm too — otherwise the visible far
        // arm has no collision ("straight but not connected on one side" click‑through). §20 fix.
        boolean pX = state.getValue(EAST), nX = state.getValue(WEST);
        boolean pY = state.getValue(UP), nY = state.getValue(DOWN);
        boolean pZ = state.getValue(SOUTH), nZ = state.getValue(NORTH);
        boolean anyX = pX || nX, anyY = pY || nY, anyZ = pZ || nZ;

        VoxelShape shape = CORE;
        if (!anyX && !anyY && !anyZ) {
            for (VoxelShape arm : ARM.values()) shape = Shapes.or(shape, arm); // renderer draws all 6 arms
        } else if (anyX && !anyY && !anyZ) {
            shape = Shapes.or(shape, ARM.get(Direction.EAST), ARM.get(Direction.WEST)); // straight X pipe
        } else if (anyY && !anyX && !anyZ) {
            shape = Shapes.or(shape, ARM.get(Direction.UP), ARM.get(Direction.DOWN));   // straight Y pipe
        } else if (anyZ && !anyX && !anyY) {
            shape = Shapes.or(shape, ARM.get(Direction.SOUTH), ARM.get(Direction.NORTH)); // straight Z pipe
        } else {
            for (Map.Entry<Direction, BooleanProperty> e : PROPERTY_BY_DIRECTION.entrySet()) {
                if (state.getValue(e.getValue())) shape = Shapes.or(shape, ARM.get(e.getKey()));
            }
        }
        return shape;
    }

    /** Right-click with a fluid identifier → set the duct's fluid type and recompute its connections. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof IItemFluidId ident)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityFluidDuctNT duct) {
            FluidType type = ident.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            FluidType before = duct.getFluidType();
            if (type != before) {
                if (player.isShiftKeyDown()) {
                    spreadType(level, pos, type, before, 256);
                } else {
                    duct.setFluidType(type);
                }
                // A duct may refuse a type (the exhaust takes only the smokes): re-read rather than assume.
                if (duct.getFluidType() != before) {
                    if (!player.isShiftKeyDown()) {
                        level.setBlock(pos, recomputeConnections(state, level, pos), 3); // refresh this duct + notify neighbours
                    }
                    player.sendSystemMessage(Component.literal("Duct fluid: ").append(duct.getFluidType().getDisplayName())
                            .withStyle(ChatFormatting.AQUA));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFluidDuctNT(pos, state);
    }

    /** Look-overlay: the duct's carried fluid type, tinted by its colour. */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFluidDuctNT duct)) return null;
        FluidType type = duct.getFluidType();
        List<String> text = new ArrayList<>();
        text.add("&[" + type.getColor() + "&]" + type.getDisplayNameString());
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return ConnectionRing.rotate(state, rotation, PROPERTY_BY_DIRECTION);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return ConnectionRing.mirror(state, mirror, PROPERTY_BY_DIRECTION);
    }

    /** 1.7 {@code FluidDuctBase.getDebugInfo}: the analysis tool's readout of the duct's fluid net. */
    @Override
    public java.util.@Nullable List<String> getDebugInfo(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFluidDuctNT)) return null;
        com.hbm.uninos.graph.NodeNetwork<com.hbm.api.fluidmk2.DuctData> net =
                com.hbm.api.fluidmk2.FluidGraph.get(level).networkAt(pos.asLong());
        if (net == null) return null;
        java.util.List<String> debug = new java.util.ArrayList<>();
        debug.add("Links: " + net.size());
        debug.add("Subscribers: " + com.hbm.util.NetworkDiagnostics.countEndpoints(level, net,
                be -> be instanceof com.hbm.api.fluidmk2.IFluidReceiverMK2));
        debug.add("Providers: " + com.hbm.util.NetworkDiagnostics.countEndpoints(level, net,
                be -> be instanceof com.hbm.api.fluidmk2.IFluidProviderMK2));
        debug.add("Transfer: " + net.tracker);
        return debug;
    }
}
