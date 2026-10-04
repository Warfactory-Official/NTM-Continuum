package com.hbm.items.weapon.sedna.factory;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.mixin.FireBlockAccessor;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** The 26 mm flare and 40 mm grenade families from original-hbm's XFactory40mm. */
public final class XFactory40mm {

    public static BulletConfig g26_flare;
    public static BulletConfig g26_flare_supply;
    public static BulletConfig g26_flare_weapon;
    public static BulletConfig g40_he;
    public static BulletConfig g40_heat;
    public static BulletConfig g40_demo;
    public static BulletConfig g40_inc;
    public static BulletConfig g40_phosphorus;

    private XFactory40mm() {}

    public static final java.util.function.BiConsumer<com.hbm.entity.projectile.EntityBulletBaseMK4, HitResult>
            LAMBDA_STANDARD_IGNITE = (bullet, hit) -> {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            HbmLivingProps.getData(living).setFire(HbmLivingProps.getFire(living) + 200);
        }
    };

    public static final java.util.function.BiConsumer<com.hbm.entity.projectile.EntityBulletBaseMK4, HitResult>
            LAMBDA_STANDARD_EXPLODE = (bullet, hit) -> {
        Lego.standardExplode(bullet, hit, 5F);
        bullet.discard();
    };

    public static final java.util.function.BiConsumer<com.hbm.entity.projectile.EntityBulletBaseMK4, HitResult>
            LAMBDA_STANDARD_EXPLODE_DEMO = (bullet, hit) -> {
        if (hit instanceof EntityHitResult entityHit && bullet.tickCount < 3
                && entityHit.getEntity() == bullet.getThrower()) return;
        var location = hit.getLocation();
        ExplosionVNT explosion = new ExplosionVNT(bullet.level(), location.x, location.y, location.z,
                5F, bullet.getThrower());
        explosion.setBlockAllocator(new BlockAllocatorStandard());
        explosion.setBlockProcessor(new BlockProcessorStandard());
        explosion.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
        explosion.setPlayerProcessor(new PlayerProcessorStandard());
        explosion.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        explosion.explode();
        bullet.discard();
    };

    public static final java.util.function.BiConsumer<com.hbm.entity.projectile.EntityBulletBaseMK4, HitResult>
            LAMBDA_STANDARD_EXPLODE_HEAT = (bullet, hit) -> {
        if (hit instanceof EntityHitResult entityHit && bullet.tickCount < 3
                && entityHit.getEntity() == bullet.getThrower()) return;
        Lego.standardExplode(bullet, hit, 3.5F);
        bullet.discard();
        if (hit instanceof EntityHitResult entityHit && bullet.level() instanceof ServerLevel level) {
            var source = com.hbm.lib.ModDamageSources.bullet(level,
                    com.hbm.lib.ModDamageSources.DamageClass.EXPLOSIVE.type(), bullet, bullet.getThrower());
            if (entityHit.getEntity() instanceof LivingEntity living) {
                // 1.7 XFactory40mm: attackEntityFromNT(living, source, dmg * 3, true, true, 0.5, 3, 0.15) - the 40 mm
                // shaped charge pierces less than the rocket's (5, 0.2).
                com.hbm.util.EntityDamageUtil.attackEntityFromNT(living, source, bullet.damage * 3F, true, true, 0.5D, 3F, 0.15F);
            } else {
                com.hbm.util.EntityDamageUtil.attackEntityFromIgnoreIFrame(entityHit.getEntity(), source, bullet.damage * 3F);
            }
        }
    };

    public static final java.util.function.BiConsumer<com.hbm.entity.projectile.EntityBulletBaseMK4, HitResult>
            LAMBDA_STANDARD_EXPLODE_INC = (bullet, hit) -> spawnFire(bullet, hit, false, 200);
    public static final java.util.function.BiConsumer<com.hbm.entity.projectile.EntityBulletBaseMK4, HitResult>
            LAMBDA_STANDARD_EXPLODE_PHOSPHORUS = (bullet, hit) -> spawnFire(bullet, hit, true, 400);

    private static void spawnFire(com.hbm.entity.projectile.EntityBulletBaseMK4 bullet, HitResult hit,
                                  boolean phosphorus, int duration) {
        if (hit instanceof EntityHitResult entityHit && bullet.tickCount < 3
                && entityHit.getEntity() == bullet.getThrower()) return;
        Level level = bullet.level();
        Lego.standardExplode(bullet, hit, 3F);
        EntityFireLingering fire = new EntityFireLingering(level)
                .setArea(5F, 2F).setDuration(duration)
                .setFireType(phosphorus ? EntityFireLingering.TYPE_PHOSPHORUS : EntityFireLingering.TYPE_DIESEL);
        fire.setPos(hit.getLocation());
        level.addFreshEntity(fire);
        bullet.discard();

        BlockPos centre = BlockPos.containing(hit.getLocation());
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos pos = centre.offset(dx, dy, dz);
            if (!level.getBlockState(pos).isAir()) continue;
            for (Direction direction : Direction.values()) {
                if (((FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(
                        level.getBlockState(pos.relative(direction))) > 0) {
                    level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    break;
                }
            }
        }
    }

    /** 1.7 {@code LAMBDA_SPAWN_C130_SUPPLIESS} (sic). */
    public static final java.util.function.Consumer<net.minecraft.world.entity.Entity> LAMBDA_SPAWN_C130_SUPPLIES =
            entity -> spawnPlane(entity, com.hbm.entity.logic.EntityC130.C130PayloadType.SUPPLIES);
    /** 1.7 {@code LAMBDA_SPAWN_C130_WEAPONS}. */
    public static final java.util.function.Consumer<net.minecraft.world.entity.Entity> LAMBDA_SPAWN_C130_WEAPONS =
            entity -> spawnPlane(entity, com.hbm.entity.logic.EntityC130.C130PayloadType.WEAPONS);

    /**
     * 1.7 {@code XFactory40mm.spawnPlane}: 40 ticks after firing, the airdrop flare bleeps at its shooter and calls a
     * C-130 that overflies the flare's column at the terrain height and drops a crate of the requested payload.
     */
    public static void spawnPlane(net.minecraft.world.entity.Entity entity,
                                  com.hbm.entity.logic.EntityC130.C130PayloadType payload) {
        if (!(entity.level() instanceof ServerLevel level) || entity.tickCount != 40) return;
        com.hbm.entity.projectile.EntityBulletBaseMK4 bullet = (com.hbm.entity.projectile.EntityBulletBaseMK4) entity;
        net.minecraft.world.entity.Entity thrower = bullet.getThrower();
        if (thrower != null) {
            level.playSound(null, thrower.getX(), thrower.getY(), thrower.getZ(), com.hbm.sound.ModSounds.TECH_BLEEP.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        com.hbm.entity.logic.EntityC130 c130 = new com.hbm.entity.logic.EntityC130(
                com.hbm.entity.ModNtmEntities.C130.get(), level);
        int x = (int) Math.floor(bullet.getX());
        int z = (int) Math.floor(bullet.getZ());
        // 1.7 World.getHeightValue: the column's surface height.
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        c130.fac(level, x, y, z, payload);
        // 1.7 WorldUtil.loadAndSpawnEntityInWorld + TrackerUtil.setTrackingRange(250): fac() force-loads the spawn
        // chunk and the C130 entity type already tracks far beyond 250 blocks.
        level.addFreshEntity(c130);
    }

    public static void init() {
        if (g26_flare != null) return;
        g26_flare = BulletConfigs.register("g26_flare", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g26_flare"))
                .setLife(100).setVel(2F).setGrav(0.015F).setRenderRotations(false)
                .setOnImpact(LAMBDA_STANDARD_IGNITE));
        g26_flare_supply = BulletConfigs.register("g26_flare_supply", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g26_flare_supply"))
                .setLife(100).setVel(2F).setGrav(0.015F).setRenderRotations(false)
                .setOnImpact(LAMBDA_STANDARD_IGNITE)
                .setOnUpdate(LAMBDA_SPAWN_C130_SUPPLIES));
        g26_flare_weapon = BulletConfigs.register("g26_flare_weapon", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_g26_flare_weapon"))
                .setLife(100).setVel(2F).setGrav(0.015F).setRenderRotations(false)
                .setOnImpact(LAMBDA_STANDARD_IGNITE)
                .setOnUpdate(LAMBDA_SPAWN_C130_WEAPONS));

        g40_he = BulletConfigs.register("g40_he", base().setItem(
                BallisticGunCatalog.ammo("ammo_standard_g40_he")).setOnImpact(LAMBDA_STANDARD_EXPLODE));
        g40_heat = BulletConfigs.register("g40_heat", base().setItem(
                BallisticGunCatalog.ammo("ammo_standard_g40_heat")).setDamage(0.5F)
                .setOnImpact(LAMBDA_STANDARD_EXPLODE_HEAT));
        g40_demo = BulletConfigs.register("g40_demo", base().setItem(
                BallisticGunCatalog.ammo("ammo_standard_g40_demo")).setDamage(0.75F)
                .setOnImpact(LAMBDA_STANDARD_EXPLODE_DEMO));
        g40_inc = BulletConfigs.register("g40_inc", base().setItem(
                BallisticGunCatalog.ammo("ammo_standard_g40_inc")).setDamage(0.75F)
                .setOnImpact(LAMBDA_STANDARD_EXPLODE_INC));
        g40_phosphorus = BulletConfigs.register("g40_phosphorus", base().setItem(
                BallisticGunCatalog.ammo("ammo_standard_g40_phosphorus")).setDamage(0.75F)
                .setOnImpact(LAMBDA_STANDARD_EXPLODE_PHOSPHORUS));
    }

    private static BulletConfig base() {
        return new BulletConfig().setLife(200).setVel(2F).setGrav(0.035F);
    }

    public static BulletConfig[] all40() {
        return new BulletConfig[] {g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus};
    }
}
