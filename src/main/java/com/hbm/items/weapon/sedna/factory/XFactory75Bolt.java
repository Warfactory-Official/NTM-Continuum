package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.capability.HbmLivingProps;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** The .75 bolt family from 1.7's XFactory75Bolt. */
public final class XFactory75Bolt {

    public static final ConfigWrapper<Float> GUN_BOLTER_DAMAGE = new ConfigWrapper<>(15F);

    public static void initConfig() {
        ServerConfig.configMap.put("GUN_BOLTER_DAMAGE", GUN_BOLTER_DAMAGE);
    }

    public static BulletConfig b75;
    public static BulletConfig b75_inc;
    public static BulletConfig b75_exp;

    private XFactory75Bolt() {}

    public static void init() {
        if (b75 != null) return;
        b75 = BulletConfigs.register("b75", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_b75"))
                .setOnImpact((bullet, hit) -> {
                    if (hit instanceof EntityHitResult entityHit && bullet.tickCount < 3
                            && entityHit.getEntity() == bullet.getThrower()) return;
                    Lego.tinyExplode(bullet, hit, 2F);
                    bullet.discard();
                }));
        b75_inc = BulletConfigs.register("b75_inc", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_b75_inc"))
                .setDamage(0.8F).setArmorPiercing(0.1F)
                .setOnImpact((bullet, hit) -> {
                    if (hit instanceof EntityHitResult entityHit
                            && entityHit.getEntity() instanceof LivingEntity living) {
                        HbmLivingProps.setPhosphorusAtLeast(living, 300);
                    }
                }));
        b75_exp = BulletConfigs.register("b75_exp", new BulletConfig()
                .setItem(BallisticGunCatalog.ammo("ammo_standard_b75_exp"))
                .setDamage(1.5F).setArmorPiercing(-0.25F)
                .setOnImpact((bullet, hit) -> {
                    Lego.standardExplode(bullet, hit, 5F);
                    bullet.discard();
                }));
    }

    public static BulletConfig[] all() {
        return new BulletConfig[] {b75, b75_inc, b75_exp};
    }
}
