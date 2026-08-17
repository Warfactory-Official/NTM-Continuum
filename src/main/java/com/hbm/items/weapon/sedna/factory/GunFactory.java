package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;

/**
 * Declares the bullet configs, then hands off to the per-calibre factories.
 *
 * <p>Ported from 1.12.2 {@code GunFactory}. Upstream also registers the ammo <em>items</em> here; in
 * this port those are ordinary registry entries in {@link ModAmmoItems}, so this class is left with the
 * configs and the init order.
 *
 * <p>Order matters in one direction only: a bullet config must exist before a gun's magazine names it,
 * which is why every calibre's {@code init()} runs before the guns are built.
 */
public class GunFactory {

    /** The debug gun's round: barely any spread, and it bounces off almost anything. */
    public static BulletConfig ammo_debug;

    private GunFactory() {}

    public static void init() {
        ammo_debug = BulletConfigs.register("ammo_debug", new BulletConfig()
                .setItem(ModAmmoItems.ammo_debug)
                .setSpread(0.01F).setRicochetAngle(45));

        XFactory357.init();
    }
}
