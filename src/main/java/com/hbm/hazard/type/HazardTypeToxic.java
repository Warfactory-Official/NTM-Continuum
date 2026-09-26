package com.hbm.hazard.type;

import com.hbm.api.hazard.IHazardType;
import com.hbm.api.hazard.HazardClass;
import java.util.List;

import com.hbm.config.RadiationConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.helper.HazardHelper;
import com.hbm.api.hazard.IHazardModifier;
import com.hbm.util.ArmorRegistry;
import dev.golem.core.world.Effects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HazardTypeToxic implements IHazardType {

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {

        if (RadiationConfig.disableToxic) return;

        final boolean reacher = HazardHelper.isHoldingReacher(target);
        boolean hasToxFilter = false;
        boolean hasHazmat = false;

        if (target instanceof Player player) {
            hasToxFilter = ArmorRegistry.hasProtection(player, EquipmentSlot.HEAD, HazardClass.NERVE_AGENT);

            if (hasToxFilter) {
                ArmorUtil.damageGasMaskFilter(player, hazardRate);
            }

            hasHazmat = ArmorUtil.checkForHazmat(player);
        }

        final boolean isUnprotected = !(hasToxFilter || hasHazmat || reacher);

        if (isUnprotected) {
            Effects.add(target, MobEffects.WEAKNESS, 110, (int) (level - 1));

            if (level > 2) {
                Effects.add(target, MobEffects.SLOWNESS, 110, (int) Math.min(4, level - 4));
            }

            if (level > 4) {
                Effects.add(target, MobEffects.HUNGER, 110, (int) level);
            }

            if (level > 6 && target.getRandom().nextInt((int) (2000 / level)) == 0) {
                Effects.add(target, MobEffects.POISON, 110, (int) (level - 4));
            }
        }

        if (!hasHazmat || !hasToxFilter || !reacher) {
            if (level > 8) {
                Effects.add(target, MobEffects.MINING_FATIGUE, 110, (int) (level - 8));
            }

            if (level > 16) {
                Effects.add(target, MobEffects.INSTANT_DAMAGE, 110, (int) (level - 16));
            }
        }
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
    }

    @Override
    public void addHazardInformation(final Player player, final List<Component> list, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        final String adjectiveKey;

        if (level > 16) {
            adjectiveKey = "adjective.extreme";
        } else if (level > 8) {
            adjectiveKey = "adjective.veryhigh";
        } else if (level > 4) {
            adjectiveKey = "adjective.high";
        } else if (level > 2) {
            adjectiveKey = "adjective.medium";
        } else {
            adjectiveKey = "adjective.little";
        }

        list.add(Component.literal("[").append(Component.translatable(adjectiveKey))
                .append(Component.literal(" ")).append(Component.translatable("trait.toxic")).append(Component.literal("]")).withStyle(ChatFormatting.GREEN));
    }
}
