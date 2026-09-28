package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModObsidian} (Obsidian Skin): dropped armor carrying it becomes an invulnerable item
 * entity. 1.7 flagged it from {@code ItemTossEvent}; no loader-neutral toss hook exists, so
 * {@link #protectDropped} runs from the server's item-entity tick instead, which also covers armor that
 * leaves the inventory some other way (death, a broken container) - a slight widening, noted.
 */
public class ItemModObsidian extends ItemArmorMod {

    public ItemModObsidian(Item.Properties properties) {
        super(ArmorModHandler.cladding, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.DARK_PURPLE, "Makes dropped armor indestructible"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.DARK_PURPLE, stack, " (Item indestructible)"));
    }

    /** Marks a dropped armor piece with obsidian skin invulnerable. Server item-entity tick. */
    public static void protectDropped(ItemEntity entity) {
        if (entity.isInvulnerable()) return;
        ItemStack stack = entity.getItem();
        if (!(stack.getItem() instanceof dev.golem.core.world.CompatArmorItem) || !ArmorModHandler.hasMods(stack)) return;
        if (ArmorModHandler.pryMod(stack, ArmorModHandler.cladding).getItem() instanceof ItemModObsidian) {
            entity.setInvulnerable(true);
        }
    }
}
