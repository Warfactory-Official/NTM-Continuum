package com.hbm.tileentity.network;

import com.hbm.lib.ModDamageSources;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import dev.golem.core.world.Compat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** The old controller's fixed 50-damage, piercing entity processor. */
final class RadioControllerEntityProcessor extends EntityProcessorCross {
    private static final float FIXED_DAMAGE = 50F;
    private static final float PIERCING_THRESHOLD = 5F;
    private static final float PIERCING_RESISTANCE = 0.5F;

    RadioControllerEntityProcessor() {
        super(1D);
        setAllowSelfDamage();
    }

    @Override
    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        return (float) (FIXED_DAMAGE * (1D - distanceScaled));
    }

    @Override
    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (entity instanceof LivingEntity living && !living.isAlive()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (source.exploder == entity) amount *= 0.5F;

        entity.invulnerableTime = 0;
        var damage = ModDamageSources.source(level, ModDamageSources.DamageClass.EXPLOSIVE.type());
        if (entity instanceof LivingEntity) {
            DamageResistanceHandler.hurt(level, entity, damage, amount,
                    PIERCING_THRESHOLD, PIERCING_RESISTANCE);
        } else {
            Compat.hurt(entity, level, damage, amount);
        }
    }
}
