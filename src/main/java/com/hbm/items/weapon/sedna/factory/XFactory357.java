package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.BiConsumer;
import net.minecraft.world.item.ItemStack;

/**
 * The .357 Magnum family, ported from 1.12.2 {@code XFactory357}.
 *
 * <p>Every stat is upstream's. The six rounds are what makes one revolver feel like six different
 * weapons: black powder is weak but cheap, soft point is the baseline, FMJ trades a little damage for
 * armour penetration, JHP hits far harder and is <em>worse</em> against armour (its negative piercing
 * value is upstream's), AP punches through targets entirely, and express does the same while wearing
 * the gun out half again as fast.
 *
 * <p>Casings ({@code setCasing}) are omitted throughout: the spent-casing particle and the casing bag
 * are not ported.
 */
public class XFactory357 {

    public static final ConfigWrapper<Float> GUN_LIGHT_REVOLVER_DAMAGE = new ConfigWrapper<>(7.5F);
    public static final ConfigWrapper<Float> GUN_ATLAS_DAMAGE = new ConfigWrapper<>(12.5F);
    public static final ConfigWrapper<Float> GUN_DANI_CELESTIAL_DAMAGE = new ConfigWrapper<>(15F);
    public static final ConfigWrapper<Float> GUN_DANI_LUNAR_DAMAGE = new ConfigWrapper<>(15F);

    public static void initConfig() {
        ServerConfig.configMap.put("GUN_LIGHT_REVOLVER_DAMAGE", GUN_LIGHT_REVOLVER_DAMAGE);
        ServerConfig.configMap.put("GUN_ATLAS_DAMAGE", GUN_ATLAS_DAMAGE);
        ServerConfig.configMap.put("GUN_DANI_CELESTIAL_DAMAGE", GUN_DANI_CELESTIAL_DAMAGE);
        ServerConfig.configMap.put("GUN_DANI_LUNAR_DAMAGE", GUN_DANI_LUNAR_DAMAGE);
    }

    public static BulletConfig m357_bp;
    public static BulletConfig m357_sp;
    public static BulletConfig m357_fmj;
    public static BulletConfig m357_jhp;
    public static BulletConfig m357_ap;
    public static BulletConfig m357_express;

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_ATLAS =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_DANI =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.75));

    public static void init() {
        m357_bp = BulletConfigs.register("m357_bp", new BulletConfig()
                .setItem(ModAmmoItems.m357_bp)
                .setDamage(0.75F).setBlackPowder(true));
        m357_sp = BulletConfigs.register("m357_sp", new BulletConfig()
                .setItem(ModAmmoItems.m357_sp));
        m357_fmj = BulletConfigs.register("m357_fmj", new BulletConfig()
                .setItem(ModAmmoItems.m357_fmj)
                .setDamage(0.8F).setThresholdNegation(2F).setArmorPiercing(0.1F));
        m357_jhp = BulletConfigs.register("m357_jhp", new BulletConfig()
                .setItem(ModAmmoItems.m357_jhp)
                .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F));
        m357_ap = BulletConfigs.register("m357_ap", new BulletConfig()
                .setItem(ModAmmoItems.m357_ap)
                .setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.25F).setThresholdNegation(5F).setArmorPiercing(0.15F));
        m357_express = BulletConfigs.register("m357_express", new BulletConfig()
                .setItem(ModAmmoItems.m357_express)
                .setDoesPenetrate(true).setDamage(1.5F)
                .setThresholdNegation(2F).setArmorPiercing(0.1F).setWear(1.5F));
    }

    /** Every .357 round, in upstream's declaration order, which is also magazine priority order. */
    public static BulletConfig[] all() {
        return new BulletConfig[] {m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express};
    }
}
