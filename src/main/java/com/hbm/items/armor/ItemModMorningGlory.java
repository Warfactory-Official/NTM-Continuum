package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.DamageEvents;
import dev.golem.core.world.Effects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.7 {@code ItemModMorningGlory}: 5% chance of Resistance V when hit, and wither immunity. */
public class ItemModMorningGlory extends ItemArmorMod {

    public ItemModMorningGlory(Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.LIGHT_PURPLE, "5% chance to apply resistance when hit, wither immunity"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.LIGHT_PURPLE, stack, " (5% for resistance, wither immunity)"));
    }

    @Override
    public void modDamage(DamageEvents.DamageEvent event, ItemStack armor) {
        LivingEntity entity = event.entity();
        if (!entity.level().isClientSide() && entity.level().getRandom().nextInt(20) == 0) {
            Effects.add(entity, Effects.resistance(), 100, 4);
        }
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!entity.level().isClientSide() && Effects.has(entity, MobEffects.WITHER)) {
            Effects.remove(entity, MobEffects.WITHER);
        }
    }
}
