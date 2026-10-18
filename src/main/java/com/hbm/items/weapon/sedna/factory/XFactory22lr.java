package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;

/** The Gen-3 .22 LR rounds, ported from upstream's {@code XFactory22lr}. */
public final class XFactory22lr {

    public static final ConfigWrapper<Float> GUN_AM180_DAMAGE = new ConfigWrapper<>(2F);
    public static final ConfigWrapper<Float> GUN_STARF_DAMAGE = new ConfigWrapper<>(12.5F);
    public static final ConfigWrapper<Float> GUN_STARF_AKIMBO_LEFT_DAMAGE = new ConfigWrapper<>(12.5F);
    public static final ConfigWrapper<Float> GUN_STARF_AKIMBO_RIGHT_DAMAGE = new ConfigWrapper<>(12.5F);

    public static void initConfig() {
        ServerConfig.configMap.put("GUN_AM180_DAMAGE", GUN_AM180_DAMAGE);
        ServerConfig.configMap.put("GUN_STARF_DAMAGE", GUN_STARF_DAMAGE);
        ServerConfig.configMap.put("GUN_STARF_AKIMBO_LEFT_DAMAGE", GUN_STARF_AKIMBO_LEFT_DAMAGE);
        ServerConfig.configMap.put("GUN_STARF_AKIMBO_RIGHT_DAMAGE", GUN_STARF_AKIMBO_RIGHT_DAMAGE);
    }

    private XFactory22lr() {}

    public static BulletConfig p22_sp;
    public static BulletConfig p22_fmj;
    public static BulletConfig p22_jhp;
    public static BulletConfig p22_ap;

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_AM180 =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25),
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_STAR_F =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(2.5F,
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));

    public static void init() {
        // Casing particles are not part of the new shared projectile surface yet.
        p22_sp = BulletConfigs.register("p22_sp", new BulletConfig()
                .setItem(ModAmmoItems.p22_sp)
                .setKnockback(0F));
        p22_fmj = BulletConfigs.register("p22_fmj", new BulletConfig()
                .setItem(ModAmmoItems.p22_fmj)
                .setKnockback(0F)
                .setDamage(0.8F).setThresholdNegation(1F).setArmorPiercing(0.1F));
        p22_jhp = BulletConfigs.register("p22_jhp", new BulletConfig()
                .setItem(ModAmmoItems.p22_jhp)
                .setKnockback(0F)
                .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F));
        p22_ap = BulletConfigs.register("p22_ap", new BulletConfig()
                .setItem(ModAmmoItems.p22_ap)
                .setKnockback(0F)
                .setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.25F).setThresholdNegation(2.5F).setArmorPiercing(0.15F));
    }

    /** Upstream declaration order, which is also magazine reload priority. */
    public static BulletConfig[] all() {
        return new BulletConfig[] {p22_sp, p22_fmj, p22_jhp, p22_ap};
    }

    /** 1.7 {@code LAMBDA_NAME_SILENCED} (gun_am180, gun_star_f). */
    public static final Function<ItemStack, String> LAMBDA_NAME_SILENCED = stack ->
            XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER) ? XFactory12ga.mutated(stack, "_silenced") : null;
}
