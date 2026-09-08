package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.lib.ModDamageSources.DamageClass;

/**
 * The particle-accelerator calibres. Only the Tauon turret's slug is here, and only as far as the
 * turret uses it: the turret hits its target directly rather than spawning a projectile, so the
 * {@code BEAM} projectile type and its impact lambda — neither of which this port has — never run.
 * The rest of the family arrives with the accelerator weapons.
 */
public final class XFactoryAccelerator {

    public static BulletConfig tau_uranium;

    private XFactoryAccelerator() {}

    public static void init() {
        tau_uranium = BulletConfigs.register("tau_uranium", new BulletConfig()
                .setItem(ModAmmoItems.tau_uranium)
                .setupDamageClass(DamageClass.SUBATOMIC).setLife(5)
                .setRenderRotations(false).setDoesPenetrate(true).setDamageFalloffByPen(false));
    }
}
