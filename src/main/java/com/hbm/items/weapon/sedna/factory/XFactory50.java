package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;

/**
 * The .50 BMG family. Only the rounds exist here: the calibre's guns (the M2 and the AMAT) are not
 * ported, but the Chekhov turret feeds from the same belt, so the configs are needed on their own.
 *
 * <p>Two of upstream's nine loads are absent. {@code bmg50_he} needs {@code Lego.tinyExplode}, which
 * rests on the NT piercing-damage path this port does not have; {@code bmg50_black} and
 * {@code bmg50_equestrian} are {@code ammo_secret} loads whose impact hangs off entities that are
 * likewise absent. Their items are still registered, so nothing goes missing from the ammo list.
 */
public final class XFactory50 {

    public static BulletConfig bmg50_sp;
    public static BulletConfig bmg50_fmj;
    public static BulletConfig bmg50_jhp;
    public static BulletConfig bmg50_ap;
    public static BulletConfig bmg50_du;
    public static BulletConfig bmg50_sm;

    private XFactory50() {}

    public static void init() {
        bmg50_sp = BulletConfigs.register("bmg50_sp", new BulletConfig().setItem(ModAmmoItems.bmg50_sp));
        bmg50_fmj = BulletConfigs.register("bmg50_fmj", new BulletConfig()
                .setItem(ModAmmoItems.bmg50_fmj).setDamage(0.8F)
                .setThresholdNegation(7F).setArmorPiercing(0.1F));
        bmg50_jhp = BulletConfigs.register("bmg50_jhp", new BulletConfig()
                .setItem(ModAmmoItems.bmg50_jhp).setDamage(1.5F)
                .setHeadshot(1.5F).setArmorPiercing(-0.25F));
        bmg50_ap = BulletConfigs.register("bmg50_ap", new BulletConfig()
                .setItem(ModAmmoItems.bmg50_ap).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.25F).setThresholdNegation(17.5F).setArmorPiercing(0.15F));
        bmg50_du = BulletConfigs.register("bmg50_du", new BulletConfig()
                .setItem(ModAmmoItems.bmg50_du).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.5F).setThresholdNegation(21F).setArmorPiercing(0.25F));
        bmg50_sm = BulletConfigs.register("bmg50_sm", new BulletConfig()
                .setItem(ModAmmoItems.bmg50_sm).setWear(10F).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(2.5F).setThresholdNegation(30F).setArmorPiercing(0.35F));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_sm};
    }
}
