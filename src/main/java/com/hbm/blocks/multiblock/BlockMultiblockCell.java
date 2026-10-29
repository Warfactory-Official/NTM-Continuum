package com.hbm.blocks.multiblock;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiConsumer;

import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared invisible folded cell for future cores that do not supply their own visible cell state.
 */
public class BlockMultiblockCell extends CompatBlock {

    /**
     * A piston may not take one cell out of a machine. 1.7 refused the stroke because
     * {@code BlockDummyable} extends {@code BlockContainer}, so every cell answered {@code hasTileEntity}
     * and vanilla declined; a folded cell carries no block entity, so the refusal has to be stated here.
     * The machines whose cells are their own block get it back from {@code TickingBlock} and state nothing.
     */
    public static Properties rigid(Properties properties) {
        return properties.pushReaction(PushReaction.BLOCK);
    }

    public BlockMultiblockCell(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * 1.7 dummies had light opacity 0, as {@link BlockMultiblockCore#isLightTransparent}.
     */
    @Override
    protected boolean isLightTransparent(BlockState state) {
        return true;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        BlockPos core = MultiblockSurface.coreOfCell(level, pos);
        if (core == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockState coreState = level.getBlockState(core);
        BlockMultiblockCore block = MultiblockSurface.foldedCore(coreState);
        return block == null ? InteractionResult.TRY_WITH_EMPTY_HAND : block.useAtCore(coreState, level, core, player,
                hit);
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (level instanceof ServerLevel server) BlockMultiblockCore.foldedCellRemoved(server, pos, movedByPiston);
    }

    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !playerPreventsBlockDrops(player) && player.hasCorrectToolForDrops(
                state)) {
            BlockMultiblockCore.FoldedOwner owner = MultiblockSurface.ownerOf(server, pos);
            if (owner != null) {
                Block.dropResources(owner.state(), server, pos, server.getBlockEntity(owner.pos()), player,
                        player.getMainHandItem());
            }
        }
    }

    @Override
    protected boolean onExplosion(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        BlockMultiblockCore.FoldedOwner owner = MultiblockSurface.ownerOf(level, pos);
        if (owner == null) return false;
        owner.block().onExplosionHitAtCore(owner.state(), level, owner.pos(), explosion, onHit);
        return true;
    }
}
