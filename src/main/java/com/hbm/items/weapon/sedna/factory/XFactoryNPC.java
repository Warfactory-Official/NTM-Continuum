package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.sound.ModSounds;
import com.hbm.util.BobMathUtil;
import com.hbm.lib.ModDamageSources.DamageClass;
import dev.golem.core.world.Effects;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What the bosses shoot. 1.7 kept these in the {@code @Deprecated} {@code BulletConfigSyncingUtil} registry next
 * to gun ammo, reached by integer id from {@code GunNPCFactory}; sedna's {@link BulletConfig} replaces that
 * registry, and none of these are loadable into a magazine, so they get their own factory the way
 * {@link XFactoryWarhead} does.
 *
 * <p>Two families of 1.7 field have no sedna counterpart and are dropped rather than approximated:
 * {@code style} (ORB / ROCKET / METEOR) and {@code setToBolt(trail)} were renderer choices with no mechanical
 * effect, and {@code vPFX} named a 1.7 particle string. The {@code plasmablast} broadcast on the UFO rocket's
 * impact has no ported particle type either - the same gap the artillery warheads recorded.
 *
 * <p>1.7 rolled an absolute {@code dmgMin..dmgMax} per shot where sedna's config carries a multiplier against a
 * base the shooter passes in, so each config's 1.7 range collapses to the constant beside it.
 *
 * <p>Muzzle velocity lives in the config. 1.7 passed it per shot and read {@code config.velocity} only on the gun
 * path; sedna scales the unit heading by {@code config.velocity + accel} every tick. {@code worm_laser} is the one config two shooters fire at different speeds - the head at 1,
 * the saucer at 2 - and the saucer makes up the difference with {@code accel}.
 */
public final class XFactoryNPC {

    /** 1.7 dmgMin/dmgMax 100. */
    public static final float MASKMAN_ORB_DAMAGE = 100F;
    /** 1.7 15..20. */
    public static final float MASKMAN_BOLT_DAMAGE = 17.5F;
    /** 1.7 15..20. */
    public static final float MASKMAN_TRACER_DAMAGE = 17.5F;
    /** 1.7 15..20. */
    public static final float MASKMAN_ROCKET_DAMAGE = 17.5F;
    /** 1.7 20..30. */
    public static final float MASKMAN_METEOR_DAMAGE = 25F;
    /** 1.7 15..25. */
    public static final float WORM_BOLT_DAMAGE = 20F;
    /** 1.7 35..60. */
    public static final float WORM_LASER_DAMAGE = 47.5F;
    /** 1.7 GunRocketFactory.getRocketConfig - 10..15. */
    public static final float UFO_ROCKET_DAMAGE = 12.5F;

    public static BulletConfig maskman_orb;
    public static BulletConfig maskman_bolt;
    public static BulletConfig maskman_tracer;
    public static BulletConfig maskman_meteor;
    public static BulletConfig maskman_rocket;
    public static BulletConfig worm_bolt;
    public static BulletConfig worm_laser;
    public static BulletConfig ufo_rocket;

    private XFactoryNPC() {
    }

    public static void init() {

        // 1.7 getMaskmanBolt: standardBulletConfig, no spread, explosive 0.5, BOLT_LACUNAE
        maskman_bolt = new BulletConfig().setVel(0.5F).setSpread(0F).setGrav(0F).setLife(100)
                                         .setDoesPenetrate(true).setupDamageClass(DamageClass.LASER)
                                         .setOnImpact((bullet, mop) -> {
                                             Lego.standardExplode(bullet, mop, 0.5F);
                                             bullet.discard();
                                         });

        // 1.7 getMaskmanMeteor: standardGrenadeConfig with gravity 0.1, no ricochet, no block damage,
        // incendiary 3 (sedna has no such field - the burn rides on the impact instead)
        maskman_meteor = new BulletConfig().setVel(1F).setSpread(0F).setGrav(0.1F).setLife(300)
                                           .setRicochetCount(0).setOnRicochet(null)
                                           .setupDamageClass(DamageClass.FIRE)
                                           .setOnImpact((bullet, mop) -> {
                                               Lego.standardExplode(bullet, mop, 2.5F);
                                               Vec3 hit = mop.getLocation();
                                               for (Entity entity : bullet.level().getEntities(bullet,
                                                       bullet.getBoundingBox().inflate(2.5D))) {
                                                   if (entity.distanceToSqr(hit) < 2.5D * 2.5D) {
                                                       Effects.ignite(entity, 3);
                                                   }
                                               }
                                               bullet.discard();
                                           });

        // 1.7 getMaskmanTracer: BOLT_NIGHTMARE, and every impact calls a meteor down on the spot
        maskman_tracer = new BulletConfig().setVel(1F).setSpread(0F).setGrav(0F).setLife(100)
                                           .setDoesPenetrate(true).setupDamageClass(DamageClass.LASER)
                                           .setOnImpact((bullet, mop) -> {
                                               summonMeteor(bullet);
                                               bullet.discard();
                                           });

        // 1.7 getMaskmanRocket: standardGrenadeConfig, gravity 0.1, explosive 5, no block damage
        maskman_rocket = new BulletConfig().setVel(1F).setSpread(0F).setGrav(0.1F).setLife(300)
                                           .setRicochetCount(0).setOnRicochet(null)
                                           .setupDamageClass(DamageClass.EXPLOSIVE)
                                           .setOnImpact((bullet, mop) -> {
                                               Lego.standardExplode(bullet, mop, 5F);
                                               bullet.discard();
                                           });

        // 1.7 getMaskmanOrb: drifts at a quarter block a tick and rains bolts on everyone within fifty blocks
        maskman_orb = new BulletConfig().setVel(2F).setSpread(0F).setGrav(0F).setLife(60)
                                        .setRicochetCount(0).setOnRicochet(null).setDoesPenetrate(false)
                                        .setupDamageClass(DamageClass.LASER)
                                        .setOnUpdate(entity -> {
                                            if (entity.level().isClientSide()) return;
                                            if (entity.tickCount % 10 != 5) return;
                                            rainBolts((EntityBulletBaseMK4) entity);
                                        })
                                        .setOnImpact((bullet, mop) -> {
                                            Lego.standardExplode(bullet, mop, 1.5F);
                                            bullet.discard();
                                        });

        // 1.7 getWormBolt: BOLT_WORM, no ricochet, sixty ticks
        worm_bolt = new BulletConfig().setVel(0.5F).setSpread(0F).setGrav(0F).setLife(60)
                                      .setRicochetCount(0).setOnRicochet(null).setDoesPenetrate(true)
                                      .setupDamageClass(DamageClass.LASER);

        // 1.7 getWormHeadBolt: BOLT_LASER, harder and longer-lived than the body's
        worm_laser = new BulletConfig().setVel(1F).setSpread(0F).setGrav(0F).setLife(100)
                                       .setRicochetCount(0).setOnRicochet(null).setDoesPenetrate(true)
                                       .setupDamageClass(DamageClass.LASER);

        // 1.7 getRocketUFOConfig: the standard rocket with explosive zeroed and block destruction off - the
        // blast is the flat dealDamage below, not an explosion
        ufo_rocket = new BulletConfig().setVel(2F).setSpread(0.005F).setGrav(0.005F).setLife(300)
                                       .setRicochetAngle(10F).setRicochetCount(2)
                                       .setupDamageClass(DamageClass.EXPLOSIVE)
                                       .setOnUpdate(entity -> {
                                           if (entity.level().isClientSide()) return;
                                           EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) entity;
                                           if (bullet.lockonTarget == null || !bullet.lockonTarget.isAlive()) {
                                               bullet.lockonTarget = chooseTarget(bullet);
                                           }
                                       })
                                       .setOnImpact((bullet, mop) -> {
                                           bullet.level().playSound(null, bullet.getX(), bullet.getY(), bullet.getZ(),
                                                   ModSounds.UFO_BLAST.get(), SoundSource.HOSTILE, 5F,
                                                   0.9F + bullet.level().getRandom().nextFloat() * 0.2F);
                                           ExplosionNukeGeneric.dealDamage(bullet.level(), bullet.getX(),
                                                   bullet.getY(), bullet.getZ(), 10, 50F);
                                           bullet.discard();
                                       });
    }


    public static @Nullable EntityBulletBaseMK4 aimed(LivingEntity shooter, BulletConfig config, float damage,
                                                      Entity target, float spread) {

        double posY = shooter.getEyeY() - 0.10000000149011612D;
        double dx = target.getX() - shooter.getX();
        double dy = target.getBoundingBox().minY + target.getBbHeight() / 3F - posY;
        double dz = target.getZ() - shooter.getZ();
        double hyp = Math.sqrt(dx * dx + dz * dz);

        if (hyp < 1.0E-7D) return null;

        return new EntityBulletBaseMK4(shooter.level(), shooter, config, damage, spread,
                shooter.getX() + dx / hyp, posY, shooter.getZ() + dz / hyp, dx, dy, dz);
    }

    private static void rainBolts(EntityBulletBaseMK4 orb) {

        for (Player player : orb.level().getEntitiesOfClass(Player.class, orb.getBoundingBox().inflate(50D))) {

            Vec3 motion = new Vec3(player.getX() - orb.getX(),
                    player.getY() + player.getEyeHeight() - orb.getY(),
                    player.getZ() - orb.getZ()).normalize();

            EntityBulletBaseMK4 bolt = new EntityBulletBaseMK4(orb.level(), orb.getThrower(), maskman_bolt,
                    MASKMAN_BOLT_DAMAGE, 0.05F, orb.getX(), orb.getY(), orb.getZ(),
                    motion.x * 0.5D, motion.y * 0.5D, motion.z * 0.5D);
            orb.level().addFreshEntity(bolt);
        }
    }

    private static void summonMeteor(EntityBulletBaseMK4 tracer) {

        double y = tracer.getY() + 30 + tracer.level().getRandom().nextInt(10);
        EntityBulletBaseMK4 meteor = new EntityBulletBaseMK4(tracer.level(), tracer.getThrower(), maskman_meteor,
                MASKMAN_METEOR_DAMAGE, 0F, tracer.getX(), y, tracer.getZ(), 0D, -1D, 0D);
        tracer.level().addFreshEntity(meteor);
    }


    private static Entity chooseTarget(EntityBulletBaseMK4 bullet) {

        double range = 100D;
        Vec3 motion = bullet.getDeltaMovement();
        List<LivingEntity> candidates = bullet.level().getEntitiesOfClass(LivingEntity.class,
                bullet.getBoundingBox().inflate(range));

        Entity best = null;
        double bestAngle = 90D;

        for (LivingEntity candidate : candidates) {

            if (!candidate.isAlive() || candidate == bullet.getThrower()) continue;
            if (candidate.distanceToSqr(bullet) >= range * range) continue;

            Vec3 eye = new Vec3(candidate.getX(), candidate.getY() + candidate.getBbHeight() / 2D, candidate.getZ());
            if (bullet.level().clip(new ClipContext(bullet.position(), eye, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, bullet)).getType() != HitResult.Type.MISS) continue;

            double angle = BobMathUtil.getCrossAngle(motion, eye.subtract(bullet.position()));

            if (angle < bestAngle) {
                best = candidate;
                bestAngle = angle;
            }
        }

        return best;
    }
}
