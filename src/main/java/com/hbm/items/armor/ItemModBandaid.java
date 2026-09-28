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

/** 1.7 {@code ItemModBandaid}: a 3% chance for any hit to be nullified and fully heal the wearer. */
public class ItemModBandaid extends ItemArmorMod {

    public ItemModBandaid(Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.RED, "3% chance for full heal when damaged"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.RED, stack, " (3% chance for full heal)"));
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        if (event.entity().level().getRandom().nextInt(100) < 3) {
            event.setAmount(0);
            event.entity().heal(event.entity().getMaxHealth());
        }
    }
}
