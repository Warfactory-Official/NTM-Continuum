package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.effect.EntityFireLingering;
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
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.mixin.FireBlockAccessor;
import com.hbm.lib.ModDamageSources;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.util.DamageResistanceHandler;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The 84 mm rockets. A rocket leaves the tube at a standstill and builds up speed under its own
 * motor, which is why its config velocity is zero and the acceleration lives on the entity.
 *
 * <p>The launchers that fire the other variants ({@code rocket_rpzb}, {@code rocket_qd},
 * {@code rocket_ncrpa}) are absent — only the missile launcher's belt is built here, since the
 * Richard turret is its only consumer.
 */
public final class XFactoryRocket {

    public static BulletConfig[] rocket_template;
    public static BulletConfig[] rocket_rpzb;
    public static BulletConfig[] rocket_qd;
    public static BulletConfig[] rocket_ml;
    public static BulletConfig[] rocket_ncrpa;
    public static BulletConfig[] rocket_ncrpa_steer;

    /** The motor: a rocket adds to its step every tick until it tops out. */
    public static final Consumer<Entity> LAMBDA_STANDARD_ACCELERATE = entity -> {
        if (entity instanceof EntityBulletBaseMK4 bullet && bullet.accel < 7) bullet.accel += 0.4D;
    };

    /** Guided rounds accelerate more slowly and follow the shooter's aimed block ray when held. */
    public static final Consumer<Entity> LAMBDA_STEERING_ACCELERATE = entity -> {
        if (!(entity instanceof EntityBulletBaseMK4 bullet)) return;
        if (!(bullet.getThrower() instanceof Player player)) {
            if (bullet.accel < 7) bullet.accel += 0.4D;
            return;
        }
        var held = player.getMainHandItem();
        steeringAccelerate(bullet, held.isEmpty() || !(held.getItem() instanceof ItemGunBaseNT)
                || !ItemGunBaseNT.getIsAiming(held));
    };

    public static final BiConsumer<net.minecraft.world.item.ItemStack, ItemGunBaseNT.LambdaContext>
            LAMBDA_STINGER_SECONDARY_PRESS = (stack, ctx) ->
                    com.hbm.items.weapon.sedna.impl.ItemGunStinger.setIsLockingOn(stack, true);
    public static final BiConsumer<net.minecraft.world.item.ItemStack, ItemGunBaseNT.LambdaContext>
            LAMBDA_STINGER_SECONDARY_RELEASE = (stack, ctx) ->
                    com.hbm.items.weapon.sedna.impl.ItemGunStinger.setIsLockingOn(stack, false);
    public static final BiConsumer<net.minecraft.world.item.ItemStack, ItemGunBaseNT.LambdaContext>
            LAMBDA_MISSILE_LAUNCHER_PRIMARY_PRESS = (stack, ctx) ->
                    missileLauncherPrimary(stack, ctx);

    private static void missileLauncherPrimary(net.minecraft.world.item.ItemStack stack,
            ItemGunBaseNT.LambdaContext ctx) {
        if (ItemGunBaseNT.getIsAiming(stack)) {
            int target = com.hbm.items.weapon.sedna.impl.ItemGunStinger.getLockonTarget(
                    ctx.getPlayer(), 150D, 20D);
            if (target != -1) {
                ItemGunBaseNT.setLockonTarget(stack, target);
                ItemGunBaseNT.setIsLockedOn(stack, true);
            }
        }
        Lego.LAMBDA_STANDARD_CLICK_PRIMARY.accept(stack, ctx);
        ItemGunBaseNT.setIsLockedOn(stack, false);
    }

    private static void steeringAccelerate(EntityBulletBaseMK4 bullet, boolean noSteer) {
        if (bullet.accel < 4) bullet.accel += 0.4D;
        Player player = bullet.getThrower() instanceof Player value ? value : null;
        if (player == null || noSteer) return;
        if (bullet.position().distanceTo(player.position()) > 100D) return;

        Vec3 start = player.getEyePosition(1F);
        Vec3 end = start.add(player.getViewVector(1F).scale(200D));
        HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 direction = hit.getLocation().subtract(bullet.position());
        if (direction.length() < 3D) return;

        Vec3 motion = bullet.getDeltaMovement();
        bullet.setDeltaMovement(direction.normalize().scale(motion.length()));
    }

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

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_INC =
            (bullet, mop) -> spawnFire(bullet, mop, false, 300);

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE_PHOSPHORUS =
            (bullet, mop) -> spawnFire(bullet, mop, true, 600);

    private XFactoryRocket() {}

    /**
     * A blast, a patch of lingering fire, and ordinary vanilla flame on anything nearby that will take
     * it — the last is what makes an incendiary round set the scenery alight rather than just the mob
     * standing in it. Flammability is read off vanilla's own burn-odds map rather than upstream's
     * per-face Forge query, which has no Fabric counterpart; the answer differs only for a block that
     * burns on some faces and not others, and vanilla has none.
     */
    private static void spawnFire(EntityBulletBaseMK4 bullet, HitResult mop, boolean phosphorus, int duration) {
        if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;

        Level world = bullet.level();
        Lego.standardExplode(bullet, mop, 3F);

        EntityFireLingering fire = new EntityFireLingering(world).setArea(6, 2).setDuration(duration)
                .setFireType(phosphorus ? EntityFireLingering.TYPE_PHOSPHORUS : EntityFireLingering.TYPE_DIESEL);
        fire.setPos(mop.getLocation());
        world.addFreshEntity(fire);
        bullet.discard();

        Vec3 hit = mop.getLocation();
        BlockPos centre = BlockPos.containing(hit.x, hit.y, hit.z);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (!world.getBlockState(pos).isAir()) continue;
                    for (Direction dir : Direction.values()) {
                        BlockState neighbour = world.getBlockState(pos.relative(dir));
                        if (((FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(neighbour) > 0) {
                            world.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                            break;
                        }
                    }
                }
            }
        }
    }

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

        BulletConfig inc = BulletConfigs.register("rocket_inc", baseRocket()
                .setItem(ModAmmoItems.rocket_inc).setDamage(0.75F).setOnImpact(LAMBDA_STANDARD_EXPLODE_INC));
        BulletConfig phosphorus = BulletConfigs.register("rocket_phosphorus", baseRocket()
                .setItem(ModAmmoItems.rocket_phosphorus).setDamage(0.75F)
                .setOnImpact(LAMBDA_STANDARD_EXPLODE_PHOSPHORUS));

        rocket_template = new BulletConfig[]{he, heat, demo, inc, phosphorus};
        rocket_rpzb = new BulletConfig[rocket_template.length];
        rocket_qd = new BulletConfig[rocket_template.length];
        rocket_ml = new BulletConfig[rocket_template.length];
        rocket_ncrpa = new BulletConfig[rocket_template.length];
        rocket_ncrpa_steer = new BulletConfig[rocket_template.length];
        for (int i = 0; i < rocket_template.length; i++) {
            rocket_rpzb[i] = copyRocket("rocket_rpzb", rocket_template[i], false);
            rocket_qd[i] = copyRocket("rocket_qd", rocket_template[i], true);
            rocket_ml[i] = copyRocket("rocket_ml", rocket_template[i], false);
            rocket_ncrpa[i] = copyRocket("rocket_ncrpa", rocket_template[i], false);
            rocket_ncrpa_steer[i] = copyRocket("rocket_ncrpa_steer", rocket_template[i], true);
        }
    }

    private static BulletConfig copyRocket(String family, BulletConfig original, boolean guided) {
        String id = family + "_" + original.id.getPath().substring("rocket_".length());
        BulletConfig copy = BulletConfigs.register(id, baseRocket()
                .setItem(original.ammo).setDamage(original.damageMult)
                .setOnImpact(original.onImpact));
        if (guided) copy.setLife(400).setOnUpdate(LAMBDA_STEERING_ACCELERATE);
        return copy;
    }

    /** Shared configuration constructor used by the catalog and by turret consumers. */
    public static GunConfig rocketConfig(int family, int capacity, int rounds, float damage,
            int delay, int reload, int jam) {
        BulletConfig[] roundsForGun = family == 0 ? rocket_rpzb
                : family == 1 ? rocket_qd : family == 2 ? rocket_ml : rocket_ncrpa;
        return new GunConfig().dura(300).draw(7).inspect(40).crosshair(Crosshair.L_CIRCUMFLEX)
                .rec(new Receiver(0).dmg(damage).delay(delay).reload(reload).jam(jam)
                        .sound(com.hbm.sound.ModGunSounds.FIRE_ROCKET::get, 1F, 1F)
                        .mag(new MagazineSingleReload(0, capacity).addConfigs(roundsForGun))
                        .offset(1, -0.0625 * 1.5, -0.1875D).setupStandardFire())
                .setupStandardConfiguration();
    }

    public static GunConfig lockonRocketConfig(int family, int capacity, int rounds, float damage,
            int delay, int reload, int jam) {
        GunConfig config = rocketConfig(family, capacity, rounds, damage, delay, reload, jam);
        config.getReceivers(null)[0].setupLockonFire();
        return config;
    }
}
