package com.hbm.blocks.machine;

import com.hbm.inventory.container.ContainerArmorTable;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The nine-slot armor module table. It has no block entity; all state is on the armor stack. */
public final class BlockArmorTable extends CompatBlock {

    public BlockArmorTable(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Menus.openWithPos(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.armorTable");
                }

                @Override
                public AbstractContainerMenu createMenu(
                        int containerId, Inventory inventory, Player menuPlayer) {
                    return new ContainerArmorTable(containerId, inventory, pos);
                }
            }, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
