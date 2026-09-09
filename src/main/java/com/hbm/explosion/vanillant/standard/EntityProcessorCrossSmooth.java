package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.lib.ModDamageSources;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.util.DamageResistanceHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * A blast whose damage falls off linearly with distance instead of following vanilla's curve, and
 * whose peak is stated outright rather than derived from the size. That is what lets a weapon set its
 * warhead's damage directly, so an explosive round hits for what the round says it does.
 *
 * <p>The pierce pair is the round's own {@code armorThresholdNegation} / {@code armorPiercingPercent},
 * carried through so an AP shell keeps its armour behaviour when it detonates.
 */
public class EntityProcessorCrossSmooth extends EntityProcessorCross {

    protected float fixedDamage;
    protected float pierceDT = 0;
    protected float pierceDR = 0;
    protected DamageClass clazz = DamageClass.EXPLOSIVE;

    public EntityProcessorCrossSmooth(double nodeDist, float fixedDamage) {
        super(nodeDist);
        this.fixedDamage = fixedDamage;
        this.setAllowSelfDamage();
    }

    public EntityProcessorCrossSmooth setupPiercing(float pierceDT, float pierceDR) {
        this.pierceDT = pierceDT;
        this.pierceDR = pierceDR;
        return this;
    }

    public EntityProcessorCrossSmooth setDamageClass(DamageClass clazz) {
        this.clazz = clazz;
        return this;
    }

    @Override
    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (!entity.isAlive()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        float damage = source.exploder == entity ? amount * 0.5F : amount;
        DamageSource dmg = ModDamageSources.bullet(level, clazz.type(), null,
                source.exploder instanceof net.minecraft.world.entity.LivingEntity living ? living : null);
        DamageResistanceHandler.hurt(level, entity, dmg, damage, pierceDT, pierceDR);
    }

    /**
     * 1.7's density gate, which 1.12.2 dropped. Kept: without it a blast behind cover still lands its
     * full linear falloff, which is the whole point of sampling exposure.
     */
    @Override
    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        return (float) (fixedDamage * (1 - distanceScaled));
    }
}
