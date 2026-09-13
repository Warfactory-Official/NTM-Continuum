package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityDiFurnace;
import com.hbm.tileentity.machine.TileEntityDiFurnaceExtension;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Separate chimney/extension block; its inventory and menu are delegated to the furnace below.
 *
 * <p>The block entity carries no data — the flue is a truncated pyramid, which is not a shape a vanilla
 * block model can state, so it is drawn by a block-entity renderer and this is what anchors it.
 */
public class MachineDiFurnaceExtension extends dev.golem.core.world.CompatBlock
        implements TickingBlock, WorldlyContainerHolder {

    public MachineDiFurnaceExtension(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDiFurnaceExtension(pos, state);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos.below()) instanceof TileEntityDiFurnace furnace ? furnace : null;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(pos.below()) instanceof MenuProvider provider) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos.below());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
