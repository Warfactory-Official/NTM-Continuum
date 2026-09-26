package com.hbm.hazard.type;

import com.hbm.api.hazard.IHazardType;
import java.util.List;

import com.hbm.config.RadiationConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.api.hazard.IHazardModifier;
import dev.golem.core.world.Effects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

        Effects.add(target, MobEffects.MINING_FATIGUE, 110, baseLevel);
        Effects.add(target, MobEffects.SLOWNESS, 110, Math.min(4, baseLevel));
        Effects.add(target, MobEffects.WEAKNESS, 110, baseLevel);

        if (level > 4) {
            Effects.add(target, MobEffects.WITHER, 110, witherLevel);
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        list.add(Component.literal("[").append(Component.translatable("trait.cryogenic")).append(Component.literal("]")).withStyle(ChatFormatting.AQUA));
    }
}
