package com.hbm.items.weapon.sedna.factory;

import com.hbm.capability.HbmLivingProps;
import com.hbm.entity.effect.EntityFireLingering;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.impl.ItemGunChemthrower;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.mixin.FireBlockAccessor;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Flamethrower projectiles. Only the two gravity-free loads are built: they are what the Fritz turret
 * sprays, and it is the calibre's only consumer here — the handheld flamethrowers, their napalm and gas
 * loads and the Topaz/Daybreaker variants arrive with the guns.
 *
 * <p>A tongue of flame is a short-lived projectile, not a hitscan: it sets what it touches alight, and
 * where it lands it leaves a patch of {@link EntityFireLingering} that keeps burning after the burst.
 */
public final class XFactoryFlamer {

    public static BulletConfig flame_nograv;
    public static BulletConfig flame_nograv_bf;
    public static BulletConfig flame_diesel;
    public static BulletConfig flame_gas;
    public static BulletConfig flame_napalm;
    public static BulletConfig flame_balefire;
    public static BulletConfig flame_topaz_diesel;
    public static BulletConfig flame_topaz_gas;
    public static BulletConfig flame_topaz_napalm;
    public static BulletConfig flame_topaz_balefire;
    public static BulletConfig flame_daybreaker_diesel;
    public static BulletConfig flame_daybreaker_gas;
    public static BulletConfig flame_daybreaker_napalm;
    public static BulletConfig flame_daybreaker_balefire;

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_IGNITE_FIRE = (bullet, mop) -> {
        if (mop instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            HbmLivingProps.setFireAtLeast(living, 100);
        }
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_IGNITE_BALEFIRE = (bullet, mop) -> {
        if (mop instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            HbmLivingProps.setBalefireAtLeast(living, 200);
        }
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_DIESEL = (bullet, mop) -> {
        if (!igniteIfPossible(bullet, mop)) {
            spawnFire(bullet, mop, 2F, 1F, 100, EntityFireLingering.TYPE_DIESEL);
        }
    };

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_BALEFIRE = (bullet, mop) ->
            spawnFire(bullet, mop, 3F, 1F, 300, EntityFireLingering.TYPE_BALEFIRE);

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_GAS =
            (bullet, mop) -> igniteIfPossible(bullet, mop);
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_LINGER_NAPALM =
            (bullet, mop) -> spawnFire(bullet, mop, 2.5F, 1F, 200, EntityFireLingering.TYPE_DIESEL);

    private XFactoryFlamer() {}

    /**
     * Sets the struck face alight if it will take a flame. Flammability comes off vanilla's burn-odds
     * map rather than upstream's per-face Forge query, which has no Fabric counterpart.
     */
    public static boolean igniteIfPossible(EntityBulletBaseMK4 bullet, HitResult mop) {
        if (!(mop instanceof BlockHitResult blockHit)) return false;

        Level world = bullet.level();
        BlockPos pos = blockHit.getBlockPos();
        Direction dir = blockHit.getDirection();
        BlockState state = world.getBlockState(pos);

        if (((FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(state) > 0) {
            BlockPos target = pos.relative(dir);
            if (world.getBlockState(target).isAir()) {
                world.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                return true;
            }
        }

        bullet.discard();
        return false;
    }

    /** One patch per spot: a burst of twenty flames must not stack twenty overlapping fires. */
    public static void spawnFire(EntityBulletBaseMK4 bullet, HitResult mop, float width, float height,
                                 int duration, int type) {
        if (!(mop instanceof BlockHitResult)) return;

        Vec3 hit = mop.getLocation();
        List<EntityFireLingering> existing = bullet.level().getEntitiesOfClass(EntityFireLingering.class,
                new AABB(hit.x, hit.y, hit.z, hit.x, hit.y, hit.z)
                        .inflate(width / 2 + 0.5, height / 2 + 0.5, width / 2 + 0.5));

        if (existing.isEmpty()) {
            EntityFireLingering fire = new EntityFireLingering(bullet.level())
                    .setArea(width, height).setDuration(duration).setFireType(type);
            fire.setPos(hit);
            bullet.level().addFreshEntity(fire);
        }

        bullet.discard();
    }

    public static void init() {
        flame_nograv = BulletConfigs.register("flame_nograv", new BulletConfig()
                .setItem(com.hbm.items.ModAmmoItems.flame_diesel).setupDamageClass(DamageClass.FIRE).setLife(100).setVel(1F).setGrav(0F)
                .setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
                .setOnImpact(LAMBDA_IGNITE_FIRE).setOnRicochet(LAMBDA_LINGER_DIESEL));

        flame_nograv_bf = BulletConfigs.register("flame_nograv_bf", new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo("flame_balefire")).setupDamageClass(DamageClass.FIRE).setLife(100).setVel(1F).setGrav(0F)
                .setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
                .setOnImpact(LAMBDA_IGNITE_BALEFIRE).setOnRicochet(LAMBDA_LINGER_BALEFIRE));

        flame_diesel = BulletConfigs.register("flame_diesel", new BulletConfig().setItem(com.hbm.items.ModAmmoItems.flame_diesel)
                .setupDamageClass(DamageClass.FIRE).setLife(100).setVel(1F).setGrav(0.02F)
                .setReloadCount(500).setSelfDamageDelay(20).setOnImpact(LAMBDA_IGNITE_FIRE)
                .setOnRicochet(LAMBDA_LINGER_DIESEL));
        flame_gas = BulletConfigs.register("flame_gas", new BulletConfig().setItem(SpecialGunCatalog.standardAmmo("flame_gas"))
                .setupDamageClass(DamageClass.FIRE).setLife(10).setVel(1F).setSpread(0.05F)
                .setReloadCount(500).setSelfDamageDelay(20).setOnImpact(LAMBDA_IGNITE_FIRE)
                .setOnRicochet(LAMBDA_LINGER_GAS));
        flame_napalm = BulletConfigs.register("flame_napalm", new BulletConfig().setItem(SpecialGunCatalog.standardAmmo("flame_napalm"))
                .setupDamageClass(DamageClass.FIRE).setLife(200).setVel(1F).setGrav(0.02F)
                .setReloadCount(500).setSelfDamageDelay(20).setOnImpact(LAMBDA_IGNITE_FIRE)
                .setOnRicochet(LAMBDA_LINGER_NAPALM));
        flame_balefire = BulletConfigs.register("flame_balefire", new BulletConfig().setItem(SpecialGunCatalog.standardAmmo("flame_balefire"))
                .setupDamageClass(DamageClass.FIRE).setLife(200).setVel(1F).setGrav(0.02F)
                .setReloadCount(500).setSelfDamageDelay(20).setOnImpact(LAMBDA_IGNITE_BALEFIRE)
                .setOnRicochet(LAMBDA_LINGER_BALEFIRE));
        flame_topaz_diesel = BulletConfigs.register("flame_topaz_diesel", new BulletConfig().setItem(com.hbm.items.ModAmmoItems.flame_diesel)
                .setupDamageClass(DamageClass.FIRE).setProjectiles(2).setLife(60).setVel(1F).setGrav(0F).setSpread(0.05F)
                .setOnImpact(LAMBDA_IGNITE_FIRE).setOnRicochet(LAMBDA_LINGER_DIESEL));
        flame_topaz_gas = BulletConfigs.register("flame_topaz_gas", flame_gasLike("flame_topaz_gas", LAMBDA_LINGER_GAS)
                .setLife(10).setVel(1F).setGrav(0F));
        flame_topaz_napalm = BulletConfigs.register("flame_topaz_napalm", flame_gasLike("flame_topaz_napalm", LAMBDA_LINGER_NAPALM)
                .setLife(60).setVel(1F).setGrav(0F));
        flame_topaz_balefire = BulletConfigs.register("flame_topaz_balefire", flame_gasLike("flame_topaz_balefire", LAMBDA_LINGER_BALEFIRE)
                .setLife(60).setVel(1F).setGrav(0F)
                .setOnImpact(LAMBDA_IGNITE_BALEFIRE));
        flame_daybreaker_diesel = BulletConfigs.register("flame_daybreaker_diesel", flame_dieselLike("flame_daybreaker_diesel", LAMBDA_LINGER_DIESEL));
        flame_daybreaker_gas = BulletConfigs.register("flame_daybreaker_gas", flame_dieselLike("flame_daybreaker_gas", LAMBDA_LINGER_GAS));
        flame_daybreaker_napalm = BulletConfigs.register("flame_daybreaker_napalm", flame_dieselLike("flame_daybreaker_napalm", LAMBDA_LINGER_NAPALM));
        flame_daybreaker_balefire = BulletConfigs.register("flame_daybreaker_balefire", flame_dieselLike("flame_daybreaker_balefire", LAMBDA_LINGER_BALEFIRE)
                .setOnImpact(LAMBDA_IGNITE_BALEFIRE));

    }

    private static BulletConfig flame_gasLike(String id, BiConsumer<EntityBulletBaseMK4, HitResult> ricochet) {
        return new BulletConfig().setItem(SpecialGunCatalog.standardAmmo(id)).setupDamageClass(DamageClass.FIRE)
                .setProjectiles(2).setSpread(0.05F).setOnImpact(LAMBDA_IGNITE_FIRE).setOnRicochet(ricochet);
    }

    private static BulletConfig flame_dieselLike(String id, BiConsumer<EntityBulletBaseMK4, HitResult> ricochet) {
        return new BulletConfig().setItem(SpecialGunCatalog.standardAmmo(id)).setupDamageClass(DamageClass.FIRE)
                .setLife(200).setVel(2F).setGrav(0.035F).setOnImpact(LAMBDA_IGNITE_FIRE).setOnRicochet(ricochet);
    }

    public static GunConfig gunConfig(int variant) {
        BulletConfig[] rounds = variant == 0 ? new BulletConfig[]{flame_diesel, flame_gas, flame_napalm, flame_balefire}
                : variant == 1 ? new BulletConfig[]{flame_topaz_diesel, flame_topaz_gas, flame_topaz_napalm, flame_topaz_balefire}
                : new BulletConfig[]{flame_daybreaker_diesel, flame_daybreaker_gas, flame_daybreaker_napalm, flame_daybreaker_balefire};
        return new GunConfig().dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                .rec(new Receiver(0).dmg(variant == 2 ? 25F : variant == 1 ? 1.5F : 1F).delay(1).auto(true)
                        .reload(90).jam(17).mag(new MagazineFullReload(0, variant == 1 ? 500 : variant == 2 ? 50 : 300)
                                .addConfigs(rounds)).offset(0.75, -0.0625, -0.25D).setupStandardFire())
                .setupStandardConfiguration();
    }

    public static GunConfig chemthrowerConfig() {
        return new GunConfig().dura(90_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                .rec(new Receiver(0).delay(1).auto(true).canFire(ItemGunChemthrower.LAMBDA_CAN_FIRE)
                        .fire(ItemGunChemthrower.LAMBDA_FIRE).offset(0.75, -0.0625, -0.25D))
                .setupStandardConfiguration();
    }
}
