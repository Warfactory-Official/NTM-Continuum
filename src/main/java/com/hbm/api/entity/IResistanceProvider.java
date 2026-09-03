package com.hbm.api.entity;

import net.minecraft.world.damagesource.DamageSource;

/**
 * Allows an HBM entity to contribute dynamic damage threshold and resistance values.
 *
 * <p>The array returned by {@link #getCurrentDTDR} is {@code [threshold, resistance]}, matching the
 * legacy contract. Both values are added after the registered armor set and piece profiles.
 */
public interface IResistanceProvider {
    float[] getCurrentDTDR(DamageSource damage, float amount, float pierceDT, float pierce);

    void onDamageDealt(DamageSource damage, float amount);
}
