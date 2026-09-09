package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Armor polish: the legacy 5% per-hit nullification chance. */
public final class ItemModPolish extends ItemArmorMod {
    public ItemModPolish(Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("5% chance to nullify damage").withStyle(ChatFormatting.BLUE));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        if (event.entity().getRandom().nextInt(20) == 0) event.setAmount(0F);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(Component.literal("  ").append(stack.getHoverName().copy().withStyle(ChatFormatting.BLUE))
                .append(Component.literal(" (5% chance to nullify damage)").withStyle(ChatFormatting.BLUE)));
    }
}
