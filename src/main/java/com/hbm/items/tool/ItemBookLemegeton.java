package com.hbm.items.tool;

import com.hbm.inventory.ModUtilityToolMenus;
import com.hbm.inventory.container.ContainerLemegeton;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.CompatItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** 1.7 {@code ItemBookLemegeton}: right-click opens the material upgrade conversion ({@link ContainerLemegeton}). */
public class ItemBookLemegeton extends CompatItem {

    public ItemBookLemegeton(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Menus.openWithHand(serverPlayer, new SimpleMenuProvider((id, inventory, opener) ->
                    new ContainerLemegeton(ModUtilityToolMenus.LEMEGETON.get(), id, inventory),
                    Component.translatable("item.hbm.book_lemegeton")), hand);
        }
        return InteractionResult.SUCCESS;
    }
}
