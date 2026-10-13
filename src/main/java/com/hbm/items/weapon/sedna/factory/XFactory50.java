package com.hbm.items.weapon.sedna.factory;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.projectile.EntityBuilding;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import net.minecraft.world.phys.EntityHitResult;

/** The .50 BMG family from 1.7's XFactory50. */
public final class XFactory50 {

    public static BulletConfig bmg50_sp;
    public static BulletConfig bmg50_fmj;
    public static BulletConfig bmg50_jhp;
    public static BulletConfig bmg50_ap;
    public static BulletConfig bmg50_du;
    public static BulletConfig bmg50_he;
    public static BulletConfig bmg50_sm;
    public static BulletConfig bmg50_black;
    public static BulletConfig bmg50_equestrian;

    private XFactory50() {}

    public static void init() {
        if (bmg50_sp != null) return;
        bmg50_sp = BulletConfigs.register("bmg50_sp", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_bmg50_sp")));
        bmg50_fmj = BulletConfigs.register("bmg50_fmj", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_bmg50_fmj")).setDamage(0.8F)
                .setThresholdNegation(7F).setArmorPiercing(0.1F));
        bmg50_jhp = BulletConfigs.register("bmg50_jhp", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_bmg50_jhp")).setDamage(1.5F)
                .setHeadshot(1.5F).setArmorPiercing(-0.25F));
        bmg50_ap = BulletConfigs.register("bmg50_ap", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_bmg50_ap")).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.25F).setThresholdNegation(17.5F).setArmorPiercing(0.15F));
        bmg50_du = BulletConfigs.register("bmg50_du", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_bmg50_du")).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(1.5F).setThresholdNegation(21F).setArmorPiercing(0.25F));
        bmg50_he = BulletConfigs.register("bmg50_he", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_bmg50_he")).setWear(3F).setDamage(1.75F)
                .setOnImpact((bullet, mop) -> {
                    if (mop instanceof EntityHitResult hit && bullet.tickCount < 3
                            && hit.getEntity() == bullet.getThrower()) {
                        return;
                    }
                    Lego.tinyExplode(bullet, mop, 2F);
                    bullet.discard();
                }));
        bmg50_sm = BulletConfigs.register("bmg50_sm", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_bmg50_sm")).setWear(10F).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setDamage(2.5F).setThresholdNegation(30F).setArmorPiercing(0.35F));

        bmg50_black = BulletConfigs.register("bmg50_black", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_secret_bmg50_black"))
                .setWear(5F).setDoesPenetrate(true).setDamageFalloffByPen(false)
                .setSpectral(true).setDamage(1.5F).setHeadshot(3F)
                .setThresholdNegation(30F).setArmorPiercing(0.35F));
        // 1.7 LAMBDA_BUILDING: a falling building spawned 50 blocks above the hit.
        bmg50_equestrian = BulletConfigs.register("bmg50_equestrian", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_secret_bmg50_equestrian"))
                .setDamage(0F).setOnImpact((bullet, mop) -> {
                    XFactory44.dropFromSky(bullet, mop, new EntityBuilding(
                            ModNtmEntities.FALLING_BUILDING.get(), bullet.level()));
                    bullet.discard();
                }));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_he, bmg50_sm};
    }
}
