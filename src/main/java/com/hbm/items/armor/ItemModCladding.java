package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Radiation cladding module; its value is read by {@link com.hbm.handler.HazmatRegistry}. */
public final class ItemModCladding extends ItemArmorMod {
    public final double rad;

    public ItemModCladding(double rad, Item.Properties properties) {
        super(ArmorModHandler.cladding, true, true, true, true, properties);
        this.rad = rad;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("+" + rad + " rad-resistance").withStyle(ChatFormatting.YELLOW));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }
}
