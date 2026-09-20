package com.hbm.blocks.bomb;

import com.hbm.interfaces.IBomb;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Block-side plumbing shared by the special bombs ({@code nuke_custom}, {@code nuke_fleija},
 * {@code nuke_solinium}, {@code nuke_prototype}, {@code nuke_fstbmb}). Original-hbm gave each of them a
 * copy of the same {@code BlockContainer} boilerplate: invisible (TESR-drawn), a 2..5 facing metadata
 * written by {@code onBlockPlacedBy}, right-click opens the GUI unless sneaking, redstone calls
 * {@code explode}. The facing numbering follows the port's {@link NukeBombBlock#FACING} so the renderers
 * share the MK5 bombs' yaw convention.
 */
public abstract class NukeBomb2Block extends CompatBlock implements TickingBlock, IBomb {

    protected NukeBomb2Block(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NukeBombBlock.FACING, 2));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NukeBombBlock.FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(NukeBombBlock.FACING,
                legacyFacing(context.getHorizontalDirection().getOpposite()));
    }

    /** The same numbering the MK5 bombs use, so the shared renderer yaw tables apply unchanged. */
    static int legacyFacing(Direction direction) {
        return switch (direction) {
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
            default -> 2;
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TileEntityNukeBomb bomb) {
            bomb.placerID = player.getUUID();
        }
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block block, boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) explode(level, pos, null);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(NukeBombBlock.FACING,
                rotation.rotate(Direction.from3DDataValue(state.getValue(NukeBombBlock.FACING))).get3DDataValue());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(NukeBombBlock.FACING,
                mirror.mirror(Direction.from3DDataValue(state.getValue(NukeBombBlock.FACING))).get3DDataValue());
    }
}
