package com.hbm.blocks.network;

import com.hbm.api.block.IToolable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import com.hbm.util.ConveyorBend;

/** 1.7 {@code blocks/network/BlockConveyorBendable}: a belt that can turn left or right within its own cell. */
public abstract class BlockConveyorBendable extends BlockConveyorBase implements IToolable {

    public static final EnumProperty<ConveyorBend> BEND = EnumProperty.create("bend", ConveyorBend.class);

    protected BlockConveyorBendable(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BEND, ConveyorBend.STRAIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BEND);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return super.mirror(state, mirror).setValue(BEND, state.getValue(BEND).mirrored());
    }

    /** 1.7: {@code primary.getRotation(DOWN)} for a left bend, {@code getRotation(UP)} for a right one. */
    @Override
    public Direction getOutputDirection(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Direction primary = state.getValue(FACING);

        return switch (state.getValue(BEND)) {
            case STRAIGHT -> primary;
            case LEFT -> primary.getCounterClockWise();
            case RIGHT -> primary.getClockWise();
        };
    }

    /**
     * 1.7: a bent belt hands the item to the turn only once it is a whole taxicab block away from the pivot
     * corner; the two bends measure from opposite corners of the cell.
     */
    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        BlockState state = level.getBlockState(pos);
        Direction primary = state.getValue(FACING).getOpposite();
        ConveyorBend bend = state.getValue(BEND);

        if (bend != ConveyorBend.STRAIGHT) {
            int dir = bend.ordinal() - 1;
            Direction secondary = primary.getClockWise(); // ForgeDirection.getRotation(UP)

            double ix = pos.getX() + 0.5 - (-primary.getStepX() * 0.5 + secondary.getStepX() * (0.5 - dir));
            double iz = pos.getZ() + 0.5 - (-primary.getStepZ() * 0.5 + secondary.getStepZ() * (0.5 - dir));

            double dX = Math.abs(itemPos.x - ix);
            double dZ = Math.abs(itemPos.z - iz);

            if (dX + dZ >= 1) {
                return dir == 0 ? secondary.getOpposite() : secondary;
            }
        }

        return primary;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        return screw(level, player, pos);
    }

    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(BEND, state.getValue(BEND).next()), UPDATE_ALL);
    }
}
