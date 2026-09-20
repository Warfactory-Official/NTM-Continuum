package com.hbm.blocks.machine;

import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityPistonInserter;

/**
 * 1.7 {@code blocks/machine/PistonInserter}: holds one item and, on a redstone rising edge, extends to push it
 * into an {@link com.hbm.api.block.IInsertable} two blocks ahead. {@link #FACING} is the 1.7 metadata: the
 * side the piston extends towards, which is the face the placer looked at it from.
 */
public class PistonInserter extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public PistonInserter(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** 1.7 {@code BlockPistonBase.determineOrientation}. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPistonInserter(pos, state);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        updateState(state, level, pos);
    }

    protected void updateState(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        Direction dir = state.getValue(FACING);

        // no obstructions allowed!
        if (level.getBlockState(pos.relative(dir)).isRedstoneConductor(level, pos.relative(dir))) return;

        boolean flag = checkRedstone(level, pos);
        if (!(level.getBlockEntity(pos) instanceof TileEntityPistonInserter piston)) return;

        if (flag && !piston.lastState && piston.extend <= 0) piston.isRetracting = false;

        piston.lastState = flag;
        piston.setChanged();
    }

    /** 1.7 {@code checkRedstone}: any neighbour emitting indirect power towards this block. */
    protected boolean checkRedstone(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getSignal(pos.relative(dir), dir) > 0) return true;
        }
        return false;
    }

    /** 1.7 {@code onBlockActivated}: only the extending face reacts; sneak ejects, a held item loads. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return use(state, level, pos, player, hand, stack, hit);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return use(state, level, pos, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hit);
    }

    private InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                  ItemStack held, BlockHitResult hit) {
        if (hit.getDirection() != state.getValue(FACING)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityPistonInserter piston) {
                if (!piston.slot.isEmpty() && piston.isRetracting) {
                    Direction dir = state.getValue(FACING);

                    ItemEntity dust = new ItemEntity(level, pos.getX() + 0.5D + dir.getStepX() * 0.75D,
                            pos.getY() + 0.5D + dir.getStepY() * 0.75D, pos.getZ() + 0.5D + dir.getStepZ() * 0.75D, piston.slot);
                    piston.slot = ItemStack.EMPTY;
                    piston.setChanged();

                    dust.setDeltaMovement(dir.getStepX() * 0.25, dir.getStepY() * 0.25, dir.getStepZ() * 0.25);
                    level.addFreshEntity(dust);
                }
            }
            return InteractionResult.SUCCESS;
        } else if (!held.isEmpty()) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityPistonInserter piston) {
                if (piston.slot.isEmpty()) {
                    piston.slot = held.split(1);
                    piston.setChanged();
                    player.inventoryMenu.broadcastChanges();
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
