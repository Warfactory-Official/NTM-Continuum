package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorBalefire;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
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
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.main.MainRegistry;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Nuclear and cluster warheads from the original catapult factory. */
public final class XFactoryCatapult {
    public static BulletConfig nuke_standard;
    public static BulletConfig nuke_demo;
    public static BulletConfig nuke_high;
    public static BulletConfig nuke_tots;
    public static BulletConfig nuke_hive;
    public static BulletConfig nuke_balefire;
    public static BulletConfig cluster_submunition;

    private XFactoryCatapult() {}

    private static boolean selfHit(EntityBulletBaseMK4 bullet, HitResult hit) {
        return hit instanceof EntityHitResult entityHit && bullet.tickCount < 3
                && entityHit.getEntity() == bullet.getThrower();
    }

    /** 1.7 shell terrain behaviour: none, standard blocks + fire (demo), or standard blocks + balefire. */
    private enum Terrain { NONE, FIRE, BALEFIRE }

    /**
     * 1.7 LAMBDA_NUKE_STANDARD / _DEMO / _BALEFIRE / _TINYTOT: a VNT with a cross-smooth entity pass
     * (and, for demo and balefire, a 64-ray standard block allocator whose cleared cells catch fire or
     * balefire), the radiation ring, then the LOW satellite burst, the mini-nuke report and the cloud.
     */
    private static BiConsumer<EntityBulletBaseMK4, HitResult> blast(float radius, float rad,
            HbmEffectNT effect, int balefireMode, Terrain terrain) {
        return (bullet, hit) -> {
            if (selfHit(bullet, hit) || bullet.isRemoved()) return;
            bullet.discard();
            var pos = hit.getLocation();
            ExplosionVNT vnt = new ExplosionVNT(bullet.level(), pos.x, pos.y, pos.z, radius); // 1.7: no exploder
            if (terrain != Terrain.NONE) {
                vnt.setBlockAllocator(new BlockAllocatorStandard(64));
                vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(
                        terrain == Terrain.FIRE ? new BlockMutatorFire() : new BlockMutatorBalefire()));
            }
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.explode();
            irradiate(bullet.level(), pos.x, pos.y, pos.z, rad);
            spawnMush(bullet, pos, effect,
                    balefireMode == 2 || (balefireMode == 1 && (MainRegistry.polaroidID == 11
                            || bullet.level().getRandom().nextInt(100) == 0)));
        };
    }

    /** 1.7 spawnMush: the LOW satellite burst, the mini-nuke report at y + 0.5, and the cloud effect. */
    private static void spawnMush(EntityBulletBaseMK4 bullet, Vec3 pos, HbmEffectNT effect,
            boolean balefire) {
        SatelliteDetector.reportEvent(bullet.level(), SatelliteDetector.DURATION_LOW,
                SatelliteDetector.BurstIntensity.LOW, bullet.getX(), bullet.getZ());
        bullet.level().playSound(null, pos.x, pos.y + 0.5, pos.z, ModSounds.MUKE_EXPLOSION.get(),
                SoundSource.BLOCKS, 15.0F, 1.0F);
        mushroom(bullet.level(), pos, effect, balefire);
    }

    /**
     * 1.7 LAMBDA_NUKE_HIVE (cross-smooth 1, weapon SFX, no blocks) and LAMBDA_SUBMUNITION (standard
     * blocks, cross-smooth 1, weapon SFX).
     */
    private static BiConsumer<EntityBulletBaseMK4, HitResult> smallBlast(float radius, boolean blocks, boolean guardSelf) {
        return (bullet, hit) -> {
            if (guardSelf && (selfHit(bullet, hit) || bullet.isRemoved())) return;
            if (guardSelf) bullet.discard();
            var pos = hit.getLocation();
            ExplosionVNT vnt = new ExplosionVNT(bullet.level(), pos.x, pos.y, pos.z, radius,
                    blocks ? bullet.getThrower() : null);
            if (blocks) {
                vnt.setBlockAllocator(new BlockAllocatorStandard());
                vnt.setBlockProcessor(new BlockProcessorStandard());
            }
            EntityProcessorCrossSmooth entities = new EntityProcessorCrossSmooth(1, bullet.damage);
            vnt.setEntityProcessor(blocks ? entities : entities.withRangeMod(1.5F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            vnt.explode();
            if (!guardSelf) bullet.discard();
        };
    }

    /** The 1.7 mini-nuke fallout ring; shared with the grenade fillings (ntm-next {@code incrementRad}). */
    public static void irradiate(Level level, double x, double y, double z, float multiplier) {
        if (!(level instanceof ServerLevel server)) return;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) + Math.abs(dz) >= 4) continue;
            float amount = 50F / (Math.abs(dx) + Math.abs(dz) + 1) * multiplier;
            RadiationSystemNT.incrementRad(server, BlockPos.containing(x + dx * 16, y, z + dz * 16),
                    amount, amount * 1024D + 1D);
        }
    }

    private static void mushroom(Level level, Vec3 pos, HbmEffectNT effect,
            boolean balefire) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("balefire", balefire);
        effect.send(level, pos.x, pos.y + 0.5, pos.z, data);
    }

    public static void init() {
        nuke_standard = BulletConfigs.register("nuke_standard", new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo("nuke_standard")).setLife(300).setVel(3F)
                .setGrav(0.025F).setOnImpact(blast(10F, 1F, HbmEffectNT.Muke, 1, Terrain.NONE)));
        nuke_demo = BulletConfigs.register("nuke_demo", new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo("nuke_demo")).setLife(300).setVel(3F)
                .setGrav(0.025F).setOnImpact(blast(15F, 1.5F, HbmEffectNT.Muke, 1, Terrain.FIRE)));
        nuke_high = BulletConfigs.register("nuke_high", new BulletConfig()
                .setItem(ModAmmoItems.ammo_standard_nuke_high).setLife(300).setVel(3F)
                .setGrav(0.025F).setOnImpact((bullet, hit) -> {
                    if (selfHit(bullet, hit) || bullet.isRemoved()) return;
                    bullet.discard();
                    var pos = hit.getLocation();
                    bullet.level().addFreshEntity(EntityNukeExplosionMK5.statFac(bullet.level(), 35,
                            pos.x, pos.y, pos.z));
                    spawnMush(bullet, pos, HbmEffectNT.Muke,
                            MainRegistry.polaroidID == 11 || bullet.level().getRandom().nextInt(100) == 0);
                }));
        nuke_tots = BulletConfigs.register("nuke_tots", new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo("nuke_tots")).setProjectiles(8).setLife(300)
                .setVel(3F).setGrav(0.025F).setSpread(0.1F).setDamage(0.35F)
                .setOnImpact(blast(5F, 0.25F, HbmEffectNT.TinyTot, 1, Terrain.NONE)));
        nuke_hive = BulletConfigs.register("nuke_hive", new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo("nuke_hive")).setProjectiles(12).setLife(300)
                .setVel(1F).setGrav(0.025F).setSpread(0.15F).setDamage(0.25F)
                .setOnImpact(smallBlast(5F, false, true)));
        nuke_balefire = BulletConfigs.register("nuke_balefire", new BulletConfig()
                .setItem(SpecialGunCatalog.standardAmmo("nuke_balefire")).setLife(300).setVel(3F)
                .setGrav(0.025F).setDamage(2.5F)
                .setOnImpact(blast(10F, 1.5F, HbmEffectNT.Muke, 2, Terrain.BALEFIRE)));
        cluster_submunition = BulletConfigs.register("cluster_submunition", new BulletConfig()
                .setLife(1200).setGrav(0.025F).setOnImpact(smallBlast(7.5F, true, false)));
    }

    /** 1.7 {@code LAMBDA_RECOIL_FATMAN}: empty in the source. */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext>
            LAMBDA_RECOIL_FATMAN = (stack, ctx) -> { };

    /** 1.7 gun_fatman (reload changes type, crosshair shown, no hipfire spread, own scoped offset). */
    public static GunConfig gunConfig() {
        return new GunConfig().dura(300).draw(20).inspect(30).reloadChangeType(true).crosshair(Crosshair.L_CIRCUMFLEX)
                .hideCrosshair(false)
                .rec(new Receiver(0).dmg(100F).spreadHipfire(0F).delay(10).reload(57).jam(40)
                        .sound(ModGunSounds.FIRE_FATMAN::get, 1F, 1F)
                        .mag(new MagazineSingleReload(0, 1).addConfigs(nuke_standard, nuke_demo, nuke_high,
                                nuke_tots, nuke_hive, nuke_balefire))
                        .offset(1, -0.0625 * 1.5, -0.1875D).offsetScoped(1, -0.0625 * 1.5, -0.125D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_FATMAN))
                .setupStandardConfiguration();
    }
}
