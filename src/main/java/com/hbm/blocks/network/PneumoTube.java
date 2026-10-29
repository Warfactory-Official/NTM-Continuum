package com.hbm.blocks.network;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.api.block.IToolable;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

import com.hbm.api.block.IPneumaticConnector;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;

/**
 * 1.7 {@code com.hbm.blocks.network.pneumatic.PneumoTube}. The block state holds one bit per face for the tube
 * arms ({@code canConnectTo}: a neighbouring tube or pneumatic block); the nozzles (insertion/ejection faces and
 * {@code canConnectToAir}) ride the block entity. 1.7 drew it with an ISBRH; the same boxes are its chunk model
 * here ({@code PneumoTubeModel}).
 */
public class PneumoTube extends CompatBlock implements TickingBlock, IToolable, IFluidConnectorBlockMK2, ICapabilityBlock {

    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;

    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.DOWN, DOWN, Direction.UP, UP, Direction.NORTH, NORTH,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST, Direction.EAST, EAST);

    // 1.7: 5px..11px (0.3125..0.6875)
    public static final double LOWER = 5 / 16D;
    public static final double UPPER = 11 / 16D;

    private static final VoxelShape CORE = Shapes.box(LOWER, LOWER, LOWER, UPPER, UPPER, UPPER);
    private static final VoxelShape[] ARMS = buildArms();
    private static final VoxelShape[] COLLISION = buildCollisionShapes();

    public PneumoTube(Properties properties) {
        super(properties);
        BlockState def = this.stateDefinition.any();
        for (BooleanProperty p : PROPERTY_BY_DIRECTION.values()) def = def.setValue(p, false);
        this.registerDefaultState(def);
    }

    private static VoxelShape[] buildArms() {
        VoxelShape[] arm = new VoxelShape[6];
        arm[Direction.DOWN.ordinal()] = Shapes.box(LOWER, 0, LOWER, UPPER, LOWER, UPPER);
        arm[Direction.UP.ordinal()] = Shapes.box(LOWER, UPPER, LOWER, UPPER, 1, UPPER);
        arm[Direction.NORTH.ordinal()] = Shapes.box(LOWER, LOWER, 0, UPPER, UPPER, LOWER);
        arm[Direction.SOUTH.ordinal()] = Shapes.box(LOWER, LOWER, UPPER, UPPER, UPPER, 1);
        arm[Direction.WEST.ordinal()] = Shapes.box(0, LOWER, LOWER, LOWER, UPPER, UPPER);
        arm[Direction.EAST.ordinal()] = Shapes.box(UPPER, LOWER, LOWER, 1, UPPER, UPPER);
        return arm;
    }

    /**
     * 1.7 addCollisionBoxesToList: the core cube plus one box per connected face.
     */
    private static VoxelShape[] buildCollisionShapes() {
        VoxelShape[] shapes = new VoxelShape[64];
        shapes[0] = CORE;
        for (int mask = 1; mask < 64; mask++) {
            int last = Integer.highestOneBit(mask);
            shapes[mask] = Shapes.or(shapes[mask ^ last], ARMS[Integer.numberOfTrailingZeros(last)]);
        }
        return shapes;
    }

    public static BooleanProperty faceProperty(Direction dir) {
        return PROPERTY_BY_DIRECTION.get(dir);
    }

    public static int faceMask(BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.values()) if (state.getValue(faceProperty(dir))) mask |= 1 << dir.ordinal();
        return mask;
    }

    private static boolean bit(int mask, Direction dir) {
        return (mask & (1 << dir.ordinal())) != 0;
    }

    /**
     * 1.7 canConnectTo: a neighbouring tube, or a block that accepts a pneumatic connection on the facing side.
     */
    public static boolean canConnectTo(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos npos = pos.relative(dir);
        BlockState neighbour = level.getBlockState(npos);
        Block block = neighbour.getBlock();
        if (block instanceof PneumoTube || block instanceof PneumoTubePaintableBlock) return true;
        return block instanceof IPneumaticConnector connector && connector.canConnectPneumatic(neighbour,
                dir.getOpposite());
    }

    /**
     * The mask of every face this tube touches: arms from the state, nozzles from the block entity.
     */
    public static int fullMask(BlockState state, BlockGetter level, BlockPos pos) {
        int mask = faceMask(state);
        if (level.getBlockEntity(pos) instanceof TileEntityPneumoTube tube) {
            if (tube.insertionDir != null) mask |= 1 << tube.insertionDir.ordinal();
            if (tube.ejectionDir != null) mask |= 1 << tube.ejectionDir.ordinal();
            mask |= tube.airMask & 0x3F;
        }
        return mask;
    }

    /**
     * 1.7 {@code ForgeDirection.getOrientation((rot.ordinal() + 1) % 7)}: DOWN..EAST then UNKNOWN (null).
     */
    public static @Nullable Direction nextDir(@Nullable Direction dir) {
        int ordinal = dir == null ? 6 : dir.ordinal();
        int next = (ordinal + 1) % 7;
        return next == 6 ? null : Direction.from3DDataValue(next);
    }

    /**
     * The four side booleans travel with the rotation; up and down stay put.
     */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            rotated = rotated.setValue(PROPERTY_BY_DIRECTION.get(rotation.rotate(dir)),
                    state.getValue(PROPERTY_BY_DIRECTION.get(dir)));
        }
        return rotated;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mirrored = mirrored.setValue(PROPERTY_BY_DIRECTION.get(mirror.mirror(dir)),
                    state.getValue(PROPERTY_BY_DIRECTION.get(dir)));
        }
        return mirrored;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOWN, UP, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // the tube's boxes are added to its chunk model (PneumoTubeModel)
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(faceProperty(dir), canConnectTo(level, pos, dir));
        }
        return state;
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        return state.setValue(faceProperty(direction), canConnectTo(level, pos, direction));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return COLLISION[fullMask(state, level, pos) & 0x3F];
    }

    /**
     * 1.7 setBlockBoundsBasedOnState: ONE box spanning every connected face, nozzles included.
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        int mask = fullMask(state, level, pos);
        return Shapes.box(
                bit(mask, Direction.WEST) ? 0 : LOWER,
                bit(mask, Direction.DOWN) ? 0 : LOWER,
                bit(mask, Direction.NORTH) ? 0 : LOWER,
                bit(mask, Direction.EAST) ? 1 : UPPER,
                bit(mask, Direction.UP) ? 1 : UPPER,
                bit(mask, Direction.SOUTH) ? 1 : UPPER);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        // 1.7: the screwdriver is handled by onScrew, not by the GUI arm
        if (ToolType.getType(stack) == ToolType.SCREWDRIVER) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (useGasIdentifier(level, pos, player, stack)) return InteractionResult.SUCCESS;
        return openTubeMenu(level, pos, player);
    }

    /**
     * Both tube forms use the same identifier action and compressor gate.
     */
    public static boolean useGasIdentifier(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (TileEntityPneumoTube.anyGas && !player.isShiftKeyDown()
                && stack.getItem() instanceof IItemFluidId ident
                && level.getBlockEntity(pos) instanceof TileEntityPneumoTube tube && tube.isCompressor()) {
            if (!level.isClientSide()) setGas(level, pos, player, tube,
                    ident.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack));
            return true;
        }
        return false;
    }

    /**
     * hbm-space {@code PneumoTube.onBlockActivated}: a fluid identifier on a compressor picks its gas, any gas that is
     * neither antimatter nor corrosive ({@link TileEntityPneumoTube#anyGas}, V-177).
     */
    private static void setGas(Level level, BlockPos pos, Player player, TileEntityPneumoTube tube, FluidType type) {
        boolean canUse = !type.hasTrait(FluidTraitSimple.FT_Amat.class)
                && !type.hasTrait(FT_Corrosive.class)
                && FluidTraitSimple.isGaseousAny(type);
        if (canUse) {
            tube.compair.setTankTypeByIdentifier(type);
            tube.setChanged();
            tube.refreshAirMask();
            player.sendSystemMessage(Component.literal("Changed type to ")
                                              .withStyle(ChatFormatting.YELLOW).append(type.getDisplayName())
                                              .append(Component.literal("!")));
        } else {
            player.sendSystemMessage(Component.literal("Invalid gas!")
                                              .withStyle(ChatFormatting.RED));
        }
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return openTubeMenu(level, pos, player);
    }

    /**
     * 1.7: the full GUI for compressors, the endpoint-only GUI for endpoints, nothing for a plain tube.
     */
    public static InteractionResult openTubeMenu(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof TileEntityPneumoTube tube))
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!tube.isCompressor() && !tube.isEndpoint()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Menus.openWithPos(serverPlayer, tube, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        return screwDirections(level, player, pos);
    }

    /**
     * 1.7 onScrew: the screwdriver walks the insertion (or, sneaking, the ejection) direction one step per click
     * over the 7-value {@code ForgeDirection} ring, skipping the other end's face and stopping at the first inventory
     * it finds. {@code UNKNOWN} - null here - always terminates the walk and simply disables that part.
     */
    public static boolean screwDirections(Level level, Player player, BlockPos pos) {
        if (level.isClientSide()) return true;
        if (!(level.getBlockEntity(pos) instanceof TileEntityPneumoTube tube)) return false;

        boolean sneaking = player.isShiftKeyDown();
        Direction rot = sneaking ? tube.ejectionDir : tube.insertionDir;
        Direction oth = sneaking ? tube.insertionDir : tube.ejectionDir;

        for (int i = 0; i < 7; i++) {
            rot = nextDir(rot);
            if (rot == null) break; // unknown is always valid, simply disables this part
            if (rot == oth) continue; // skip if both positions collide
            BlockEntity next = level.getBlockEntity(pos.relative(rot));
            if (next instanceof TileEntityPneumoTube) continue;
            if (next instanceof Container) break; // valid if connected to an inventory
        }

        if (sneaking) tube.ejectionDir = rot;
        else tube.insertionDir = rot;

        tube.refreshAirMask();
        tube.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        // the port walk moves the destination and the air intake without touching a neighbour
        if (level instanceof ServerLevel sl) LevelNodeGraph.invalidateEndpointsAt(sl, pos);
        return true;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level instanceof ServerLevel sl) LevelNodeGraph.invalidateEndpointsAt(sl, pos);
        if (level.getBlockEntity(pos) instanceof TileEntityPneumoTube tube) tube.refreshAirMask();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPneumoTube(pos, state);
    }

    /**
     * 1.7 TileEntityPneumoTube.canConnect: compressed air on every face that is neither insertion nor ejection.
     */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return world.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityPneumoTube tube
                && tube.canConnect(type, dir);
    }

    /**
     * Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts.
     */
    static final MachineCaps CAPS = MachineCaps.blockKeyed().fluidIn()
                                               .fluidContacts(TileEntityPneumoTube.class);

    @Override
    public MachineCaps caps() {
        return CAPS;
    }
}
