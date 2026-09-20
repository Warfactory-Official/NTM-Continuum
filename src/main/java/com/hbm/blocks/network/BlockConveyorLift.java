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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.util.ConveyorLiftPart;

/**
 * 1.7 {@code blocks/network/BlockConveyorLift}: a chain lift carrying items up a column. 1.7 reads its two vertical
 * neighbours live at every bounds, snapping, travel and render call; here that pair is the registered {@link #PART}.
 */
public class BlockConveyorLift extends BlockConveyorBase implements IToolable {

    public static final EnumProperty<ConveyorLiftPart> PART = EnumProperty.create("part", ConveyorLiftPart.class);

    private static final VoxelShape HEAD = Shapes.box(0D, 0D, 0D, 1D, 0.5D, 1D);

    public BlockConveyorLift(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, ConveyorLiftPart.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    /** 1.7: {@code bottom = !(below instanceof belt)}, {@code top = !(above is belt) && !bottom && !(above is enterable)}. */
    public static ConveyorLiftPart partAt(BlockGetter level, BlockPos pos) {
        if (!(level.getBlockState(pos.below()).getBlock() instanceof IConveyorBelt)) return ConveyorLiftPart.BOTTOM;

        Block above = level.getBlockState(pos.above()).getBlock();
        return above instanceof IConveyorBelt || above instanceof IEnterableBlock
                ? ConveyorLiftPart.MIDDLE : ConveyorLiftPart.TOP;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(PART, partAt(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        if (direction != Direction.UP && direction != Direction.DOWN) return state;
        return state.setValue(PART, partAt(level, pos));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == ConveyorLiftPart.TOP ? HEAD : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public Direction getInputDirection(Level level, BlockPos pos) {
        return Direction.DOWN;
    }

    @Override
    public Direction getOutputDirection(Level level, BlockPos pos) {
        return Direction.UP;
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        BlockState state = level.getBlockState(pos);
        if (state.getValue(PART) != ConveyorLiftPart.TOP) return Direction.DOWN;
        return state.getValue(FACING).getOpposite();
    }

    @Override
    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        if (level.getBlockState(pos).getValue(PART) != ConveyorLiftPart.TOP) {
            return new Snap(new Vec3(pos.getX() + 0.5, itemPos.y, pos.getZ() + 0.5), itemPos);
        }
        return super.snap(level, pos, itemPos);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        return screw(level, player, pos);
    }

    /** 1.7: sneaking turns the lift into a chute with the same metadata. */
    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, ModConveyorBlocks.CONVEYOR_CHUTE.get().defaultBlockState()
                .setValue(FACING, state.getValue(FACING)), UPDATE_ALL);
    }
}
