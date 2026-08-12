package com.hbm.entity;

/**
 * Duck interface implemented onto every {@link net.minecraft.world.entity.LivingEntity} by
 * {@code com.hbm.mixin.MixinLivingEntity} — the cross-loader replacement for the old Forge entity capability
 * carrying {@link EntityHbmProps}. Cast any LivingEntity to this to reach its HBM state (or use the
 * {@link com.hbm.capability.HbmLivingProps} facade, which does the cast for you).
 */
public interface IHbmLivingDuck {

    EntityHbmProps hbm$getProps();
}
