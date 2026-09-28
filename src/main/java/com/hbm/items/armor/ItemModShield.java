package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemModShield} (Mark III Life Extender): raises the player's effective maximum shield, read
 * by {@code ArmorModEvents.effectiveMaxShield}. Its installed line says "health" where the tooltip says
 * "shield"; that mismatch is upstream's.
 */
public class ItemModShield extends ItemArmorMod {

    public final float shield;

    public ItemModShield(float shield, Item.Properties properties) {
        super(ArmorModHandler.kevlar, false, true, false, false, properties);
        this.shield = shield;
    }

    private static ChatFormatting colour() {
        return System.currentTimeMillis() % 1000 < 500 ? ChatFormatting.YELLOW : ChatFormatting.GOLD;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(colour(), "+" + (Math.round(shield * 10) * 0.1) + " shield"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(colour(), stack, " (+" + (Math.round(shield * 10) * 0.1) + " health)"));
    }
}
