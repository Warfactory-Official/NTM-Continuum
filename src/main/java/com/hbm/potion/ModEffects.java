package com.hbm.potion;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** HBM status effects, matching the 1.12.2 {@code HbmPotion} registry. */
public final class ModEffects {

    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Constants.MOD_ID);

    /** Colour = the old {@code HbmPotion.radiation} liquid colour (8700200). */
    public static final RegistryObject<MobEffect> RADIATION =
            EFFECTS.register("radiation", () -> new HbmEffectRadiation(MobEffectCategory.HARMFUL, 8700200));

    public static final RegistryObject<MobEffect> TAINT = effect("taint", MobEffectCategory.HARMFUL, 8388736,
            HbmEffect.Kind.TAINT);
    public static final RegistryObject<MobEffect> BANG = effect("bang", MobEffectCategory.HARMFUL, 1118481,
            HbmEffect.Kind.BANG);
    public static final RegistryObject<MobEffect> MUTATION = effect("mutation", MobEffectCategory.BENEFICIAL,
            0xFF8132, null);
    public static final RegistryObject<MobEffect> RADX = effect("radx", MobEffectCategory.BENEFICIAL, 0x225900,
            null);
    public static final RegistryObject<MobEffect> LEAD = effect("lead", MobEffectCategory.HARMFUL, 0x767682,
            HbmEffect.Kind.LEAD);
    public static final RegistryObject<MobEffect> RADAWAY = effect("radaway", MobEffectCategory.BENEFICIAL,
            0xFFE400, HbmEffect.Kind.RADAWAY);
    public static final RegistryObject<MobEffect> TELEKINESIS = effect("telekinesis", MobEffectCategory.HARMFUL,
            0x00F3FF, HbmEffect.Kind.TELEKINESIS);
    public static final RegistryObject<MobEffect> PHOSPHORUS = effect("phosphorus", MobEffectCategory.HARMFUL,
            0xFF3A00, HbmEffect.Kind.PHOSPHORUS);
    public static final RegistryObject<MobEffect> STABILITY = effect("stability", MobEffectCategory.BENEFICIAL,
            0xD0D0D0, null);
    public static final RegistryObject<MobEffect> POTION_SICKNESS = effect("potionsickness",
            MobEffectCategory.BENEFICIAL, 0xFF8080, HbmEffect.Kind.POTION_SICKNESS);
    public static final RegistryObject<MobEffect> DEATH = effect("death", MobEffectCategory.BENEFICIAL, 0x111111,
            null);

    private static RegistryObject<MobEffect> effect(String id, MobEffectCategory category, int color,
                                                    HbmEffect.Kind kind) {
        return EFFECTS.register(id, () -> kind == null
                ? new PassiveHbmEffect(category, color)
                : new HbmEffect(category, color, kind));
    }

    /** Effects with no per-tick action still need a concrete HBM type for their registry identity. */
    private static final class PassiveHbmEffect extends dev.golem.core.world.CompatMobEffect {
        private PassiveHbmEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        protected boolean applyCompatEffectTick(net.minecraft.world.entity.LivingEntity entity, int amplifier) {
            return true;
        }
    }

    public static void init() {}
}
