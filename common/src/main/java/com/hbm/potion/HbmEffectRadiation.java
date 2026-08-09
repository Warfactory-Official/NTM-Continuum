package com.hbm.potion;

import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * The radiation status effect, ported from 1.12.2's {@code HbmPotion.radiation}: every tick it feeds
 * {@code (amplifier + 1) * 0.05} rads into the contamination pipeline (CREATIVE type — hazmat armor
 * bonuses still apply, creative players are exempt). Applied by contact with fallout layers and
 * glowing mycelium; amp 14 ≙ 0.75 RAD/t = 15 RAD/s.
 */
public class HbmEffectRadiation extends MobEffect {

    public HbmEffectRadiation(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, (amplifier + 1) * 0.05F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // old isReady: every tick for radiation
    }
}
