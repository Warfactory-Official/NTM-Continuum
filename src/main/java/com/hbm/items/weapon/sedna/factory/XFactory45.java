package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;

/** The ordinary .45 ACP bullet family from 1.7's XFactory45. */
public final class XFactory45 {

    public static BulletConfig p45_sp;
    public static BulletConfig p45_fmj;
    public static BulletConfig p45_jhp;
    public static BulletConfig p45_ap;
    public static BulletConfig p45_du;

    private XFactory45() {}

    public static void init() {
        if (p45_sp != null) return;
        p45_sp = BulletConfigs.register("p45_sp", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_p45_sp")));
        p45_fmj = BulletConfigs.register("p45_fmj", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_p45_fmj"))
                .setDamage(0.8F).setThresholdNegation(2F).setArmorPiercing(0.1F));
        p45_jhp = BulletConfigs.register("p45_jhp", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_p45_jhp"))
                .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F));
        p45_ap = BulletConfigs.register("p45_ap", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_p45_ap"))
                .setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.25F).setThresholdNegation(5F).setArmorPiercing(0.15F));
        p45_du = BulletConfigs.register("p45_du", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_p45_du"))
                .setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(2.5F).setThresholdNegation(15F).setArmorPiercing(0.25F));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {p45_sp, p45_fmj, p45_jhp, p45_ap, p45_du};
    }
}
