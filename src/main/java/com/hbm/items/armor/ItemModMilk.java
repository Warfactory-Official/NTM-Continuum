package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Effects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.7 {@code ItemModMilk} (Bottle of Spider Milk): strips every harmful effect, every tick, on both sides. */
public class ItemModMilk extends ItemArmorMod {

    public ItemModMilk(Item.Properties properties) {
        super(ArmorModHandler.extra, true, true, true, true, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(coloured(ChatFormatting.WHITE, "Removes bad potion effects"));
        tooltip.add(Component.empty());
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public void addArmorModDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(modLine(ChatFormatting.WHITE, stack, " (Removes bad potion effects)"));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        List<Object> bad = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (Effects.isHarmful(effect)) bad.add(Effects.kind(effect));
        }
        for (Object kind : bad) Effects.remove(entity, kind);
    }
}
