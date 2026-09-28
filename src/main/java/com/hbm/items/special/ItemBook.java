package com.hbm.items.special;

import com.hbm.inventory.ModMeleeKitMenus;
import com.hbm.inventory.container.ContainerBook;
import dev.golem.core.gui.Menus;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 1.7 {@code ItemBook}, the Book of Boxcars: right-click opens its {@code MagicRecipes} crafting page. */
public class ItemBook extends CompatItem {

    public ItemBook(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Edition 4, gold lined pages"));
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            Menus.openWithHand(serverPlayer, new SimpleMenuProvider((id, inventory, opener) ->
                    new ContainerBook(ModMeleeKitMenus.BOOK.get(), id, inventory),
                    player.getItemInHand(hand).getHoverName()), hand);
        }
        return InteractionResult.SUCCESS;
    }
}
