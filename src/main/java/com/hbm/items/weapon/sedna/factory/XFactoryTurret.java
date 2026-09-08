package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;

/**
 * Rounds that belong to no gun: the turret systems feed from these directly.
 *
 * <p>1.12.2 also declares the five 240 mm shells here. They need {@code Lego.standardExplode} and the
 * NT piercing-damage path, neither of which is ported, so they arrive with the Jeremy.
 */
public final class XFactoryTurret {

    public static BulletConfig dgk_normal;

    private XFactoryTurret() {}

    public static void init() {
        dgk_normal = BulletConfigs.register("dgk_normal", new BulletConfig().setItem(() -> ModItems.ammo_dgk.get()));
    }
}
