package com.hbm.blocks.network;

import com.hbm.api.block.IToolable;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IEnterableBlock;
import dev.golem.core.world.BlockTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModConveyorBlocks;

/**
 * 1.7 {@code blocks/network/BlockConveyorChute}: items fall down a stack of these; the lowest one, with nothing to
 * hand down to, acts as an ordinary belt. 1.7's ISBRH read the five neighbours at every chunk build; here they
 * are blockstate ({@link #FEEDS} = a belt or enterable block below, the four horizontals = a belt beside), which
 * the multipart blockstate draws from.
 */
public class BlockConveyorChute extends BlockConveyorBase implements IToolable {

    /** True where a belt or enterable block sits below: the chute is a drop, not the bottom belt. */
    public static final BooleanProperty FEEDS = BooleanProperty.create("feeds");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    public BlockConveyorChute(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FEEDS, false)
                .setValue(NORTH, false).setValue(SOUTH, false).setValue(WEST, false).setValue(EAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FEEDS, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    /** 1.7 renderer/travel test: something to hand items down to. */
    public static boolean feedsOn(BlockGetter level, BlockPos pos) {
        Block below = level.getBlockState(pos.below()).getBlock();
        return below instanceof IConveyorBelt || below instanceof IEnterableBlock;
    }

    private static boolean beltAt(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof IConveyorBelt;
    }

    /** The state as placement/neighbour updates settle it; also what the wand's ghost preview folds in. */
    public static BlockState withNeighbours(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(FEEDS, feedsOn(level, pos))
                .setValue(NORTH, beltAt(level, pos.north()))
                .setValue(SOUTH, beltAt(level, pos.south()))
                .setValue(WEST, beltAt(level, pos.west()))
                .setValue(EAST, beltAt(level, pos.east()));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : withNeighbours(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        return withNeighbours(state, level, pos);
    }

    private static boolean falling(Level level, BlockPos pos, Vec3 itemPos) {
        return feedsOn(level, pos) || itemPos.y > pos.getY() + 0.25;
    }

    @Override
    public Direction getInputDirection(Level level, BlockPos pos) {
        return Direction.UP;
    }

    @Override
    public Direction getOutputDirection(Level level, BlockPos pos) {
        return Direction.DOWN;
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        if (falling(level, pos, itemPos)) return Direction.UP;
        return level.getBlockState(pos).getValue(FACING).getOpposite();
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        if (feedsOn(level, pos)) speed *= 5;
        else if (itemPos.y > pos.getY() + 0.25) speed *= 3;

        return super.getTravelLocation(level, pos, itemPos, speed);
    }

    @Override
    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        if (falling(level, pos, itemPos)) {
            return new Snap(new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5), itemPos);
        }
        return super.snap(level, pos, itemPos);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        return screw(level, player, pos);
    }

    /** 1.7: sneaking turns the chute back into a plain belt with the same metadata. */
    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, ModConveyorBlocks.CONVEYOR.get().defaultBlockState()
                .setValue(FACING, state.getValue(FACING)), UPDATE_ALL);
    }
}
