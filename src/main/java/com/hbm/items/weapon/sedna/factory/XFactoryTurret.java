package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Rounds that belong to no gun: the turret systems feed from these directly: the Goalkeeper's belt
 * and the 240 mm shells the Jeremy and the artillery fire.
 */
public final class XFactoryTurret {

    public static final ConfigWrapper<Float> TURRET_CHEKHOV_DAMAGE = new ConfigWrapper<>(10F);
    public static final ConfigWrapper<Float> TURRET_FRIENDLY_DAMAGE = new ConfigWrapper<>(5F);
    public static final ConfigWrapper<Float> TURRET_FRITZ_DAMAGE = new ConfigWrapper<>(1F);
    public static final ConfigWrapper<Float> TURRET_HOWARD_DAMAGE = new ConfigWrapper<>(2F);
    public static final ConfigWrapper<Float> TURRET_METHUSALEM_DAMAGE = new ConfigWrapper<>(2F);
    public static final ConfigWrapper<Float> TURRET_JEREMY_DAMAGE = new ConfigWrapper<>(50F);
    public static final ConfigWrapper<Float> TURRET_MAXWELL_DAMAGE = new ConfigWrapper<>(0.25F);
    public static final ConfigWrapper<Float> TURRET_RICHARD_DAMAGE = new ConfigWrapper<>(30F);
    public static final ConfigWrapper<Float> TURRET_BROWN_DAMAGE = new ConfigWrapper<>(5F);
    public static final ConfigWrapper<Float> TURRET_EDWIN_DAMAGE = new ConfigWrapper<>(5F);
    public static final ConfigWrapper<Float> TURRET_TAUON_DAMAGE = new ConfigWrapper<>(30F);

    public static void initConfig() {
        ServerConfig.configMap.put("TURRET_CHEKHOV_DAMAGE", TURRET_CHEKHOV_DAMAGE);
        ServerConfig.configMap.put("TURRET_FRIENDLY_DAMAGE", TURRET_FRIENDLY_DAMAGE);
        ServerConfig.configMap.put("TURRET_FRITZ_DAMAGE", TURRET_FRITZ_DAMAGE);
        ServerConfig.configMap.put("TURRET_HOWARD_DAMAGE", TURRET_HOWARD_DAMAGE);
        ServerConfig.configMap.put("TURRET_METHUSALEM_DAMAGE", TURRET_METHUSALEM_DAMAGE);
        ServerConfig.configMap.put("TURRET_JEREMY_DAMAGE", TURRET_JEREMY_DAMAGE);
        ServerConfig.configMap.put("TURRET_MAXWELL_DAMAGE", TURRET_MAXWELL_DAMAGE);
        ServerConfig.configMap.put("TURRET_RICHARD_DAMAGE", TURRET_RICHARD_DAMAGE);
        ServerConfig.configMap.put("TURRET_BROWN_DAMAGE", TURRET_BROWN_DAMAGE);
        ServerConfig.configMap.put("TURRET_EDWIN_DAMAGE", TURRET_EDWIN_DAMAGE);
        ServerConfig.configMap.put("TURRET_TAUON_DAMAGE", TURRET_TAUON_DAMAGE);
    }

    private static final int CASING_BRASS = 0xEBC35E;

    public static BulletConfig dgk_normal;

    public static BulletConfig shell_normal;
    public static BulletConfig shell_explosive;
    public static BulletConfig shell_ap;
    public static BulletConfig shell_du;
    public static BulletConfig shell_w9;

    /**
     * 1.12.2 keeps this on the catapult's factory, which is unported; the shells are its only caller
     * here, so it lives with them until the launcher arrives.
     */
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_NUKE_STANDARD = (bullet, mop) -> {
        if (mop instanceof EntityHitResult entityHit && bullet.tickCount < 3
                && entityHit.getEntity() == bullet.getThrower()) {
            return;
        }
        if (bullet.isRemoved()) return;
        bullet.discard();

        Vec3 hit = mop.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 10);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();

        incrementRad(bullet.level(), hit.x, hit.y, hit.z, 1F);
        spawnMush(bullet.level(), hit);
    };

    private XFactoryTurret() {}

    private static BulletConfig.CasingProfile casing(String id) {
        return new BulletConfig.CasingProfile(id, "BOTTLENECK", 7.5F, 0.02F, 0.05F,
                CASING_BRASS, 1F, 0.5D, 60, 20);
    }

    /** Fallout across a five-by-five block of chunks, thinning with the taxicab distance from ground zero. */
    private static void incrementRad(Level world, double x, double y, double z, float mult) {
        if (!(world instanceof ServerLevel server)) return;

        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) >= 4) continue;
                double amount = 50F / (Math.abs(i) + Math.abs(j) + 1) * mult;
                RadiationSystemNT.incrementRad(server, BlockPos.containing(x + i * 16, y, z + j * 16),
                        amount, amount * 1024D + 1D);
            }
        }
    }

    private static void spawnMush(Level world, Vec3 hit) {
        world.playSound(null, hit.x, hit.y + 0.5, hit.z, ModSounds.MUKE_EXPLOSION.get(), SoundSource.BLOCKS,
                15.0F, 1.0F);
        HbmEffectNT.Muke.send(world, hit.x, hit.y + 0.5, hit.z, new CompoundTag());
    }

    public static void init() {
        dgk_normal = BulletConfigs.register("dgk_normal", new BulletConfig().setItem(() -> ModItems.ammo_dgk.get()));

        shell_normal = BulletConfigs.register("shell_normal", new BulletConfig()
                .setItem(ModAmmoItems.shell_normal).setDamage(1F).setCasing(casing("240standard"))
                .setOnImpact((bullet, mop) -> {
                    Lego.standardExplode(bullet, mop, 10F);
                    bullet.discard();
                }));
        shell_explosive = BulletConfigs.register("shell_explosive", new BulletConfig()
                .setItem(ModAmmoItems.shell_explosive).setDamage(1.5F).setCasing(casing("240ext"))
                .setOnImpact((bullet, mop) -> {
                    Vec3 hit = mop.getLocation();
                    ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 10F);
                    vnt.setBlockAllocator(new BlockAllocatorStandard());
                    vnt.setBlockProcessor(new BlockProcessorStandard());
                    vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
                    vnt.setPlayerProcessor(new PlayerProcessorStandard());
                    vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
                    vnt.explode();
                    bullet.discard();
                }));
        shell_ap = BulletConfigs.register("shell_ap", new BulletConfig()
                .setItem(ModAmmoItems.shell_ap).setDamage(2F).setDoesPenetrate(true)
                .setCasing(casing("240w")));
        shell_du = BulletConfigs.register("shell_du", new BulletConfig()
                .setItem(ModAmmoItems.shell_du).setDamage(2.5F).setDoesPenetrate(true)
                .setDamageFalloffByPen(false).setCasing(casing("240u")));
        shell_w9 = BulletConfigs.register("shell_w9", new BulletConfig()
                .setItem(ModAmmoItems.shell_w9).setDamage(2.5F).setCasing(casing("240n"))
                .setOnImpact(LAMBDA_NUKE_STANDARD));
    }
}
