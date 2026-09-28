package com.hbm.items.tool;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

/**
 * 1.7 {@code ItemCatalog}: Bobmazon and the hidden catalogue. Right-click opens the client-only offer screen
 * ({@code GUIScreenBobmazon}); the client registrations hand this item the screen opener so the common item
 * never names a client class.
 */
public class ItemCatalog extends CompatItem {

    private static Consumer<ItemStack> clientScreenOpener = stack -> {};
    private final boolean hidden;

    public ItemCatalog(Properties properties, boolean hidden) {
        super(properties);
        this.hidden = hidden;
    }

    public static void setClientScreenOpener(Consumer<ItemStack> opener) {
        clientScreenOpener = opener;
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide()) clientScreenOpener.accept(player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        if (hidden) {
            list.add(Component.literal("For a guide on how to obtain this, visit https://bit.ly/2TPgcqT"));
            list.add(Component.literal("No tricks this time, i promise."));
        }
    }
}
