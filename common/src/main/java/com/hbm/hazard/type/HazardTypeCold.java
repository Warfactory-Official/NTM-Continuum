package com.hbm.hazard.type;

import java.util.List;

import com.hbm.config.RadiationConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.hazard.modifier.IHazardModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeCold implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {
        final boolean reacher = HazardHelper.isHoldingReacher(target);
        if (RadiationConfig.disableCold || reacher) return;

        if (target instanceof Player && ArmorUtil.checkForHazmat(target)) return;

        final int baseLevel = (int) level - 1;
        final int witherLevel = (int) level - 3;

        applyPotionEffect(target, MobEffects.MINING_FATIGUE, 110, baseLevel);
        applyPotionEffect(target, MobEffects.SLOWNESS, 110, Math.min(4, baseLevel));
        applyPotionEffect(target, MobEffects.WEAKNESS, 110, baseLevel);

        if (level > 4) {
            applyPotionEffect(target, MobEffects.WITHER, 110, witherLevel);
        }
    }

    private static void applyPotionEffect(LivingEntity target, Holder<MobEffect> effect, int duration, int amplifier) {
        if (amplifier >= 0) target.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[").append(Component.translatable("trait.cryogenic")).append(Component.literal("]")).withStyle(ChatFormatting.AQUA));
    }
}
