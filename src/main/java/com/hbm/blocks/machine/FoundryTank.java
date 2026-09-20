package com.hbm.blocks.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.tileentity.machine.TileEntityFoundryTank;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * The 1.7 {@code FoundryTank} (foundry storage basin): a firebrick basin that holds molten metal poured
 * in from above and spreads it between neighbouring basins (see {@link TileEntityFoundryTank}). Its
 * walls disappear where another basin touches, and show an outlet notch where a {@code foundry_outlet}
 * points away from it — which the 1.12 port spelled as ten boolean state properties and a multipart
 * blockstate (copied here, {@code blockstates/foundry_tank.json}). The molten pool is drawn by the
 * renderer. A shovel scoops the contents out as scraps; breaking it drops them.
 */
public class FoundryTank extends dev.golem.core.world.CompatBlock implements TickingBlock, ICrucibleAcceptor {

    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty OUT_NORTH = BooleanProperty.create("out_north");
    public static final BooleanProperty OUT_SOUTH = BooleanProperty.create("out_south");
    public static final BooleanProperty OUT_EAST = BooleanProperty.create("out_east");
    public static final BooleanProperty OUT_WEST = BooleanProperty.create("out_west");

    public static final Map<Direction, BooleanProperty> TANK_BY_DIRECTION = Map.of(
            Direction.UP, UP, Direction.DOWN, DOWN, Direction.NORTH, NORTH,
            Direction.SOUTH, SOUTH, Direction.EAST, EAST, Direction.WEST, WEST);
    public static final Map<Direction, BooleanProperty> OUTLET_BY_DIRECTION = Map.of(
            Direction.NORTH, OUT_NORTH, Direction.SOUTH, OUT_SOUTH, Direction.EAST, OUT_EAST, Direction.WEST, OUT_WEST);

    private static final VoxelShape SHAPE = Shapes.or(
            Shapes.box(0, 0, 0, 1, 0.125, 1),
            Shapes.box(0, 0, 0, 1, 1, 0.125), Shapes.box(0, 0, 0.875, 1, 1, 1),
            Shapes.box(0, 0, 0, 0.125, 1, 1), Shapes.box(0.875, 0, 0, 1, 1, 1));

    public FoundryTank(Properties properties) {
        super(properties);
        BlockState def = stateDefinition.any();
        for (BooleanProperty p : TANK_BY_DIRECTION.values()) def = def.setValue(p, false);
        for (BooleanProperty p : OUTLET_BY_DIRECTION.values()) def = def.setValue(p, false);
        registerDefaultState(def);
    }

    /** The four side booleans travel with the rotation; up and down stay put. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            rotated = rotated.setValue(TANK_BY_DIRECTION.get(rotation.rotate(dir)), state.getValue(TANK_BY_DIRECTION.get(dir)));
            rotated = rotated.setValue(OUTLET_BY_DIRECTION.get(rotation.rotate(dir)), state.getValue(OUTLET_BY_DIRECTION.get(dir)));
        }
        return rotated;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            mirrored = mirrored.setValue(TANK_BY_DIRECTION.get(mirror.mirror(dir)), state.getValue(TANK_BY_DIRECTION.get(dir)));
            mirrored = mirrored.setValue(OUTLET_BY_DIRECTION.get(mirror.mirror(dir)), state.getValue(OUTLET_BY_DIRECTION.get(dir)));
        }
        return mirrored;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, SOUTH, EAST, WEST, OUT_NORTH, OUT_SOUTH, OUT_EAST, OUT_WEST);
    }

    public static boolean isTank(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FoundryTank;
    }

    /** An outlet next to the basin counts when it points away from it (1.7 {@code meta == dir.ordinal()}). */
    private static boolean isOutlet(BlockGetter level, BlockPos pos, Direction dir) {
        BlockState neighbour = level.getBlockState(pos.relative(dir));
        return neighbour.getBlock() instanceof FoundryOutlet
                && neighbour.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && neighbour.getValue(BlockStateProperties.HORIZONTAL_FACING) == dir;
    }

    private static BlockState withNeighbour(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        state = state.setValue(TANK_BY_DIRECTION.get(dir), isTank(level, pos.relative(dir)));
        if (OUTLET_BY_DIRECTION.containsKey(dir)) state = state.setValue(OUTLET_BY_DIRECTION.get(dir), isOutlet(level, pos, dir));
        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) state = withNeighbour(state, context.getLevel(), context.getClickedPos(), dir);
        return state;
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        return withNeighbour(state, level, pos, direction);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFoundryTank(pos, state);
    }

    // --- ICrucibleAcceptor: pour only, delegated to the tile ---

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        return world.getBlockEntity(pos) instanceof TileEntityFoundryTank te && te.canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityFoundryTank te)) return stack;
        return te.pour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override public boolean canAcceptPartialFlow(Level world, BlockPos pos, ForgeDirection side, MaterialStack stack) { return false; }
    @Override public MaterialStack flow(Level world, BlockPos pos, ForgeDirection side, MaterialStack stack) { return stack; }

    /** A shovel scoops the pool out as scraps (into the inventory, else dropped on the rim). */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!(stack.getItem() instanceof ShovelItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityFoundryTank cast && cast.amount > 0 && cast.type != null) {
            ItemStack scrap = ItemScraps.create(new MaterialStack(cast.type, cast.amount));
            if (!player.getInventory().add(scrap)) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, scrap));
            } else {
                player.inventoryMenu.broadcastChanges();
            }
            cast.amount = 0;
            cast.type = null;
            cast.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
        return InteractionResult.SUCCESS;
    }
}
