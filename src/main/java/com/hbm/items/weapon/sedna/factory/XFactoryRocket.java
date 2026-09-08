package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.lib.ModDamageSources;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.util.DamageResistanceHandler;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The 84 mm rockets. A rocket leaves the tube at a standstill and builds up speed under its own
 * motor, which is why its config velocity is zero and the acceleration lives on the entity.
 *
 * <p>Two of the five loads have their items but no config: the incendiary and white-phosphorus
 * warheads spawn lingering fire, which needs an entity and a pair of living-props counters that are
 * not ported. The launchers that fire the other variants ({@code rocket_rpzb}, {@code rocket_qd},
 * {@code rocket_ncrpa}) are likewise absent — only the missile launcher's belt is built here, since
 * the Richard turret is its only consumer.
 */
public final class XFactoryRocket {

    public static BulletConfig[] rocket_ml;

    /** The motor: a rocket adds to its step every tick until it tops out. */
    public static final Consumer<Entity> LAMBDA_STANDARD_ACCELERATE = entity -> {
        if (entity instanceof EntityBulletBaseMK4 bullet && bullet.accel < 7) bullet.accel += 0.4D;
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE = (bullet, mop) -> {
        if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;
        Lego.standardExplode(bullet, mop, 5F);
        bullet.discard();
    };

    /** A shaped charge: a smaller blast, and a heavy armour-piercing hit on whatever it struck. */
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_HEAT = (bullet, mop) -> {
        if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;
        Lego.standardExplode(bullet, mop, 3.5F);
        bullet.discard();

        if (!(mop instanceof EntityHitResult entityHit)) return;
        if (!(bullet.level() instanceof ServerLevel level)) return;

        Entity victim = entityHit.getEntity();
        DamageSource source = ModDamageSources.bullet(level, DamageClass.EXPLOSIVE.type(), bullet,
                bullet.getThrower());
        victim.invulnerableTime = 0;
        DamageResistanceHandler.hurt(level, victim, source, bullet.damage * 3F, 5F, 0.2F);
    };

    /** The demolition load is the only one that takes the terrain with it. */
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_DEMO = (bullet, mop) -> {
        if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;

        Vec3 hit = mop.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 5F, bullet.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
        bullet.discard();
    };

    private XFactoryRocket() {}

    private static BulletConfig baseRocket() {
        return new BulletConfig().setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0F)
                                 .setOnEntityHit(null).setOnRicochet(null)
                                 .setOnUpdate(LAMBDA_STANDARD_ACCELERATE);
    }

    public static void init() {
        BulletConfig he = BulletConfigs.register("rocket_he", baseRocket()
                .setItem(ModAmmoItems.rocket_he).setOnImpact(LAMBDA_STANDARD_EXPLODE));
        BulletConfig heat = BulletConfigs.register("rocket_heat", baseRocket()
                .setItem(ModAmmoItems.rocket_heat).setDamage(0.5F).setOnImpact(LAMBDA_STANDARD_EXPLODE_HEAT));
        BulletConfig demo = BulletConfigs.register("rocket_demo", baseRocket()
                .setItem(ModAmmoItems.rocket_demo).setDamage(0.75F).setOnImpact(LAMBDA_STANDARD_EXPLODE_DEMO));

        rocket_ml = new BulletConfig[]{he, heat, demo};
    }
}
