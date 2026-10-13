package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageSources;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.world.Compat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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

    /**
     * 1.7 :39-49: half damage to the exploder; a non-living target takes a plain hurt, a living one
     * {@code attackEntityFromNT(..., ignoreIFrame true, allowSpecialCancel false, knockback 0, pierceDT,
     * pierceDR)} and is gibbed by {@code ConfettiUtil} if that killed it.
     */
    @Override
    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (!entity.isAlive()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        float damage = source.exploder == entity ? amount * 0.5F : amount;
        DamageSource dmg = ModDamageSources.bullet(level, clazz.type(), null,
                source.exploder instanceof LivingEntity living ? living : null);
        if (!(entity instanceof LivingEntity living)) {
            Compat.hurt(entity, level, dmg, damage);
        } else {
            EntityDamageUtil.attackEntityFromNT(living, dmg, damage, true, false, 0F, pierceDT, pierceDR);
            if (!living.isAlive()) ConfettiUtil.decideConfetti(living, dmg);
        }
    }

    /**
     * 1.7 :51-54. The density gate (an entity less than 12.5% exposed takes nothing) is 1.7's "shitty
     * hack"; 1.12.2 dropped it, 1.7 is the specification.
     */
    @Override
    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        if (density < 0.125) return 0;
        return (float) (fixedDamage * (1 - distanceScaled));
    }
}
