package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.projectile.EntityBoxcar;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityTorpedo;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The Gen-3 .44 Magnum family, ported from upstream's {@code XFactory44}. */
public final class XFactory44 {

    public static final ConfigWrapper<Float> GUN_HENRY_DAMAGE = new ConfigWrapper<>(10F);
    public static final ConfigWrapper<Float> GUN_LINCOLN_DAMAGE = new ConfigWrapper<>(20F);
    public static final ConfigWrapper<Float> GUN_HEAVY_REVOLVER_DAMAGE = new ConfigWrapper<>(15F);
    public static final ConfigWrapper<Float> GUN_LILMAC_DAMAGE = new ConfigWrapper<>(30F);
    public static final ConfigWrapper<Float> GUN_PROTEGE_DAMAGE = new ConfigWrapper<>(30F);
    public static final ConfigWrapper<Float> GUN_HANGMAN_DAMAGE = new ConfigWrapper<>(25F);
    public static final ConfigWrapper<Float> GUN_HANGMAN_SMACK_DAMAGE = new ConfigWrapper<>(10F);

    public static void initConfig() {
        ServerConfig.configMap.put("GUN_HENRY_DAMAGE", GUN_HENRY_DAMAGE);
        ServerConfig.configMap.put("GUN_LINCOLN_DAMAGE", GUN_LINCOLN_DAMAGE);
        ServerConfig.configMap.put("GUN_HEAVY_REVOLVER_DAMAGE", GUN_HEAVY_REVOLVER_DAMAGE);
        ServerConfig.configMap.put("GUN_LILMAC_DAMAGE", GUN_LILMAC_DAMAGE);
        ServerConfig.configMap.put("GUN_PROTEGE_DAMAGE", GUN_PROTEGE_DAMAGE);
        ServerConfig.configMap.put("GUN_HANGMAN_DAMAGE", GUN_HANGMAN_DAMAGE);
        ServerConfig.configMap.put("GUN_HANGMAN_SMACK_DAMAGE", GUN_HANGMAN_SMACK_DAMAGE);
    }

    private XFactory44() {}

    public static BulletConfig m44_bp;
    public static BulletConfig m44_sp;
    public static BulletConfig m44_fmj;
    public static BulletConfig m44_jhp;
    public static BulletConfig m44_ap;
    public static BulletConfig m44_express;
    public static BulletConfig m44_equestrian_pip;
    public static BulletConfig m44_equestrian_mn7;

    /** 1.7 {@code XFactory44.LAMBDA_BOXCAR}: a boxcar spawned 50 blocks above the hit, with the train horn. */
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_BOXCAR = (bullet, mop) -> {
        Entity pippo = dropFromSky(bullet, mop, new EntityBoxcar(ModNtmEntities.BOXCAR.get(), bullet.level()));
        // 1.7 plays the horn at the boxcar's y + 50 (100 above the hit); kept as written. 1.7 category "record".
        bullet.level().playSound(null, pippo.getX(), pippo.getY() + 50, pippo.getZ(), ModSounds.ALARM_TRAIN_HORN.get(),
                SoundSource.RECORDS, 100F, 1F);
        bullet.discard();
    };

    /** 1.7 {@code XFactory44.LAMBDA_TORPEDO}: a torpedo spawned 50 blocks above the hit. */
    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_TORPEDO = (bullet, mop) -> {
        dropFromSky(bullet, mop, new EntityTorpedo(ModNtmEntities.TORPEDO.get(), bullet.level()));
        bullet.discard();
    };

    /** Places {@code prop} 50 blocks above the impact point and adds it, as the 1.7 equestrian lambdas do. */
    public static Entity dropFromSky(EntityBulletBaseMK4 bullet, HitResult mop, Entity prop) {
        Vec3 hit = mop.getLocation();
        prop.setPos(hit.x, hit.y + 50, hit.z);
        bullet.level().addFreshEntity(prop);
        return prop;
    }

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_HENRY =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1F));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_NOPIP =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_HANGMAN =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1F));

    /** Upstream's secondary-trigger inspection action for Hangman. */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> SMACK_A_FUCKER = (stack, ctx) -> {
        if (ItemGunBaseNT.getState(stack, ctx.configIndex) == ItemGunBaseNT.GunState.IDLE
                || ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimation.CYCLE) {
            ItemGunBaseNT.setState(stack, ctx.configIndex, ItemGunBaseNT.GunState.DRAWING);
            ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimation.INSPECT, ctx.configIndex);
        }
    };

    public static void init() {
        // Spent casing particles are not part of the shared projectile surface yet.
        m44_bp = BulletConfigs.register("m44_bp", new BulletConfig()
                .setItem(ModAmmoItems.m44_bp)
                .setDamage(0.75F).setBlackPowder(true));
        m44_sp = BulletConfigs.register("m44_sp", new BulletConfig()
                .setItem(ModAmmoItems.m44_sp));
        m44_fmj = BulletConfigs.register("m44_fmj", new BulletConfig()
                .setItem(ModAmmoItems.m44_fmj)
                .setDamage(0.8F).setThresholdNegation(3F).setArmorPiercing(0.1F));
        m44_jhp = BulletConfigs.register("m44_jhp", new BulletConfig()
                .setItem(ModAmmoItems.m44_jhp)
                .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F));
        m44_ap = BulletConfigs.register("m44_ap", new BulletConfig()
                .setItem(ModAmmoItems.m44_ap)
                .setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.25F).setThresholdNegation(7.5F).setArmorPiercing(0.15F));
        m44_express = BulletConfigs.register("m44_express", new BulletConfig()
                .setItem(ModAmmoItems.m44_express)
                .setDoesPenetrate(true).setDamage(1.5F)
                .setThresholdNegation(3F).setArmorPiercing(0.1F).setWear(1.5F));

        // Both 1.7 equestrian configs consume the same secret item; the impact drops a boxcar or a torpedo.
        m44_equestrian_pip = BulletConfigs.register("m44_equestrian_pip", new BulletConfig()
                .setItem(ModAmmoItems.m44_equestrian).setDamage(0F).setOnImpact(LAMBDA_BOXCAR));
        m44_equestrian_mn7 = BulletConfigs.register("m44_equestrian_mn7", new BulletConfig()
                .setItem(ModAmmoItems.m44_equestrian).setDamage(0F).setOnImpact(LAMBDA_TORPEDO));
    }

    /** Standard rounds in upstream declaration order, which is also magazine priority order. */
    public static BulletConfig[] all() {
        return new BulletConfig[] {m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express};
    }

    /** 1.7 {@code LAMBDA_NAME_NOPIP} (gun_heavy_revolver). */
    public static final Function<ItemStack, String> LAMBDA_NAME_NOPIP = stack ->
            XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SCOPE) ? XFactory12ga.mutated(stack, "_scoped") : null;
}
