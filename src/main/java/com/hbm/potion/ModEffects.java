package com.hbm.potion;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * HBM status effects (the 1.12.2 {@code HbmPotion} set). Only {@code radiation} is ported so far — the
 * others (radaway, taint, bang, …) land with their systems. First non-BLOCK/ITEM {@code DeferredRegister}
 * besides entities/menus/sounds; the generic flush path handles the MOB_EFFECT registry on both loaders.
 */
public final class ModEffects {

    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, Constants.MOD_ID);

    /** Colour = the old {@code HbmPotion.radiation} liquid colour (8700200). */
    public static final RegistryObject<MobEffect> RADIATION =
            EFFECTS.register("radiation", () -> new HbmEffectRadiation(MobEffectCategory.HARMFUL, 8700200));

    public static void init() {}
}
