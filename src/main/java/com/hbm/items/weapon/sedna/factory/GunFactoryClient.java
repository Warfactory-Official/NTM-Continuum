package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.hud.HUDComponentAmmoCounter;
import com.hbm.items.weapon.sedna.hud.HUDComponentDurabilityBar;

/** Client-only source catalogue for explicit Sedna HUD component assignment. */
public final class GunFactoryClient {
    public static final HUDComponentDurabilityBar HUD_COMPONENT_DURABILITY = new HUDComponentDurabilityBar();
    public static final HUDComponentDurabilityBar HUD_COMPONENT_DURABILITY_MIRROR = new HUDComponentDurabilityBar(true);
    public static final HUDComponentAmmoCounter HUD_COMPONENT_AMMO = new HUDComponentAmmoCounter(0);
    public static final HUDComponentAmmoCounter HUD_COMPONENT_AMMO_MIRROR = new HUDComponentAmmoCounter(0).mirror();
    public static final HUDComponentAmmoCounter HUD_COMPONENT_AMMO_NOCOUNTER = new HUDComponentAmmoCounter(0).noCounter();
    public static final HUDComponentAmmoCounter HUD_COMPONENT_AMMO_SECOND = new HUDComponentAmmoCounter(1);

    private static boolean initialized;

    private GunFactoryClient() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        hud(ModGunItems.gun_debug, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO, HUD_COMPONENT_AMMO_SECOND);
        hud(ModGunItems.gun_pepperbox, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_light_revolver, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_light_revolver_atlas, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_henry, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_henry_lincoln, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_greasegun, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_maresleg, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_maresleg_broken, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_flaregun, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_heavy_revolver, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_heavy_revolver_lilmac, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_heavy_revolver_protege, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_carbine, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_am180, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_liberator, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_congolake, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_flamer, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO_NOCOUNTER);
        hud(ModGunItems.gun_flamer_topaz, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO_NOCOUNTER);
        hud(ModGunItems.gun_flamer_daybreaker, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO_NOCOUNTER);
        hud(ModGunItems.gun_uzi, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_spas12, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_panzerschreck, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_g3, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_g3_zebra, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_stinger, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_mk108, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_chemthrower, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_amat, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_amat_subtlety, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_amat_penance, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_m2, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_autoshotgun, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_autoshotgun_shredder, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_autoshotgun_sexy, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_autoshotgun_heretic, 0, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_quadro, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_lag, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_minigun, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_minigun_lacunae, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_missile_launcher, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_tesla_cannon, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_laser_pistol, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_laser_pistol_pew_pew, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_laser_pistol_morning_glory, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_stg77, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_tau, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_fatman, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_lasrifle, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_coilgun, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_hangman, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_mas36, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_bolter, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_folly, 0, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_aberrator, 0, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_double_barrel, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_double_barrel_sacred_dragon, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_fireext, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_charge_thrower, 0, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);

        hud(ModGunItems.gun_light_revolver_dani, 0, HUD_COMPONENT_DURABILITY_MIRROR, HUD_COMPONENT_AMMO_MIRROR);
        hud(ModGunItems.gun_light_revolver_dani, 1, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_maresleg_akimbo, 0, HUD_COMPONENT_DURABILITY_MIRROR, HUD_COMPONENT_AMMO_MIRROR);
        hud(ModGunItems.gun_maresleg_akimbo, 1, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_uzi_akimbo, 0, HUD_COMPONENT_DURABILITY_MIRROR, HUD_COMPONENT_AMMO_MIRROR);
        hud(ModGunItems.gun_uzi_akimbo, 1, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_star_f_akimbo, 0, HUD_COMPONENT_DURABILITY_MIRROR, HUD_COMPONENT_AMMO_MIRROR);
        hud(ModGunItems.gun_star_f_akimbo, 1, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_minigun_dual, 0, HUD_COMPONENT_DURABILITY_MIRROR, HUD_COMPONENT_AMMO_MIRROR);
        hud(ModGunItems.gun_minigun_dual, 1, HUD_COMPONENT_DURABILITY, HUD_COMPONENT_AMMO);
        hud(ModGunItems.gun_aberrator_eott, 0, HUD_COMPONENT_AMMO_MIRROR);
        hud(ModGunItems.gun_aberrator_eott, 1, HUD_COMPONENT_AMMO);
    }

    private static void hud(java.util.function.Supplier<? extends ItemGunBaseNT> item, int config,
            com.hbm.items.weapon.sedna.hud.IHUDComponent... components) {
        item.get().getConfig(null, config).hud(components);
    }
}
