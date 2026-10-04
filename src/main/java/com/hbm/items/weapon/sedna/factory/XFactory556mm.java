package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import java.util.function.BiConsumer;
import net.minecraft.world.item.ItemStack;

/** The ordinary 5.56 mm family. */
public final class XFactory556mm {

    public static BulletConfig r556_sp;
    public static BulletConfig r556_fmj;
    public static BulletConfig r556_jhp;
    public static BulletConfig r556_ap;
    public static BulletConfig r556_inc_sp;
    public static BulletConfig r556_inc_fmj;
    public static BulletConfig r556_inc_jhp;
    public static BulletConfig r556_inc_ap;

    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_G3 =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25),
                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));

    private XFactory556mm() {}

    public static void init() {
        if (r556_sp != null) return;
        r556_sp = BulletConfigs.register("r556_sp", new BulletConfig().setItem(ModAmmoItems.r556_sp));
        r556_fmj = BulletConfigs.register("r556_fmj", new BulletConfig()
                .setItem(ModAmmoItems.r556_fmj).setDamage(0.8F)
                .setThresholdNegation(4F).setArmorPiercing(0.1F));
        r556_jhp = BulletConfigs.register("r556_jhp", new BulletConfig()
                .setItem(ModAmmoItems.r556_jhp).setDamage(1.5F)
                .setHeadshot(1.5F).setArmorPiercing(-0.25F));
        r556_ap = BulletConfigs.register("r556_ap", new BulletConfig()
                .setItem(ModAmmoItems.r556_ap).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.25F).setThresholdNegation(10F).setArmorPiercing(0.15F));

        java.util.function.BiConsumer<com.hbm.entity.projectile.EntityBulletBaseMK4,
                net.minecraft.world.phys.HitResult> incendiary = (bullet, hit) -> {
            if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
                    && entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
                com.hbm.capability.HbmLivingProps.setPhosphorusAtLeast(living, 300);
            }
        };
        r556_inc_sp = BulletConfigs.register("r556_inc_sp", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_r556_sp")).setOnImpact(incendiary));
        r556_inc_fmj = BulletConfigs.register("r556_inc_fmj", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_r556_fmj")).setDamage(0.8F)
                .setThresholdNegation(4F).setArmorPiercing(0.1F).setOnImpact(incendiary));
        r556_inc_jhp = BulletConfigs.register("r556_inc_jhp", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_r556_jhp")).setDamage(1.5F)
                .setHeadshot(1.5F).setArmorPiercing(-0.25F).setOnImpact(incendiary));
        r556_inc_ap = BulletConfigs.register("r556_inc_ap", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_r556_ap")).setDoesPenetrate(true)
                .setDamageFalloffByPen(false).setDamage(1.25F).setThresholdNegation(10F)
                .setArmorPiercing(0.15F).setOnImpact(incendiary));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {r556_sp, r556_fmj, r556_jhp, r556_ap};
    }

    public static BulletConfig[] incendiary() {
        return new BulletConfig[] {r556_inc_sp, r556_inc_fmj, r556_inc_jhp, r556_inc_ap};
    }

    /** 1.7 {@code LAMBDA_NAME_G3} (gun_g3, gun_g3_zebra). */
    public static final java.util.function.Function<ItemStack, String> LAMBDA_NAME_G3 = stack -> {
        if (com.hbm.items.weapon.sedna.mods.XWeaponModManager.hasUpgrade(stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SILENCER)
                && com.hbm.items.weapon.sedna.mods.XWeaponModManager.hasUpgrade(stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_NO_STOCK)
                && com.hbm.items.weapon.sedna.mods.XWeaponModManager.hasUpgrade(stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_FURNITURE_BLACK)
                && com.hbm.items.weapon.sedna.mods.XWeaponModManager.hasUpgrade(stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_SCOPE)) return XFactory12ga.mutated(stack, "_infiltrator");
        if (!com.hbm.items.weapon.sedna.mods.XWeaponModManager.hasUpgrade(stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_NO_STOCK)
                && com.hbm.items.weapon.sedna.mods.XWeaponModManager.hasUpgrade(stack, 0, com.hbm.items.weapon.sedna.mods.XWeaponModManager.ID_FURNITURE_GREEN)) return XFactory12ga.mutated(stack, "_a3");
        return null;
    };
}
