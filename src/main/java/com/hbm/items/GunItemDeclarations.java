package com.hbm.items;

import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.ItemModels;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;

/**
 * Item definitions of the Sedna guns: {@code hbm:gun} nodes naming the shared base model and the
 * weapon renderer, some behind a {@code select} on display context; the two 1.12.2 guns are {@code hbm:custom_item}s.
 *
 * <p>Touches no Minecraft class: Dejson runs {@link #declare()} in a plain JVM at build time.
 */
public final class GunItemDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private GunItemDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        part0();
        part1();
        part2();
    }

    private static void part0() {
        Dejson.item(NS, "gun_aberrator")
                .definition(gun("hbm:item/gun_obj_base", "aberrator")).oversizedInGui().register();
        Dejson.item(NS, "gun_aberrator_eott")
                .definition(gun("hbm:item/gun_obj_base", "aberrator_eott")).oversizedInGui().register();
        Dejson.item(NS, "gun_am180")
                .definition(gun("hbm:item/gun_obj_base", "am180")).oversizedInGui().register();
        Dejson.item(NS, "gun_amat")
                .definition(gun("hbm:item/gun_obj_base", "amat")).oversizedInGui().register();
        Dejson.item(NS, "gun_amat_penance")
                .definition(gun("hbm:item/gun_obj_base", "amat_penance")).oversizedInGui().register();
        Dejson.item(NS, "gun_amat_subtlety")
                .definition(gun("hbm:item/gun_obj_base", "amat_subtlety")).oversizedInGui().register();
        Dejson.item(NS, "gun_autoshotgun")
                .definition(gun("hbm:item/gun_obj_base", "autoshotgun")).oversizedInGui().register();
        Dejson.item(NS, "gun_autoshotgun_heretic")
                .definition(gun("hbm:item/gun_obj_base", "autoshotgun_heretic")).oversizedInGui().register();
        Dejson.item(NS, "gun_autoshotgun_sexy")
                .definition(gun("hbm:item/gun_obj_base", "autoshotgun_sexy")).oversizedInGui().register();
        Dejson.item(NS, "gun_autoshotgun_shredder")
                .definition(gun("hbm:item/gun_obj_base", "autoshotgun_shredder")).oversizedInGui().register();
        Dejson.item(NS, "gun_b92")
                .definition(ItemModels.select("minecraft:display_context")
                        .whenAny(ItemModels.model("hbm:item/gun_b92_flat"), "gui")
                        .fallback(gun("hbm:item/gun_b92_base", "b92"))).register();
        Dejson.item(NS, "gun_bolter")
                .definition(gun("hbm:item/gun_obj_base", "bolter")).oversizedInGui().register();
        Dejson.item(NS, "gun_carbine")
                .definition(gun("hbm:item/gun_obj_base", "carbine")).oversizedInGui().register();
        Dejson.item(NS, "gun_charge_thrower")
                .definition(gun("hbm:item/gun_obj_base", "charge_thrower")).oversizedInGui().register();
        Dejson.item(NS, "gun_chemthrower")
                .definition(gun("hbm:item/gun_obj_base", "chemthrower")).oversizedInGui().register();
        Dejson.item(NS, "gun_coilgun")
                .definition(gun("hbm:item/gun_obj_base", "coilgun")).oversizedInGui().register();
        Dejson.item(NS, "gun_congolake")
                .definition(gun("hbm:item/gun_obj_base", "congolake")).oversizedInGui().register();
        Dejson.item(NS, "gun_debug")
                .definition(gun("hbm:item/gun_obj_base", "debug")).oversizedInGui().register();
        Dejson.item(NS, "gun_double_barrel")
                .definition(gun("hbm:item/gun_obj_base", "double_barrel")).oversizedInGui().register();
        Dejson.item(NS, "gun_double_barrel_sacred_dragon")
                .definition(gun("hbm:item/gun_obj_base", "double_barrel_sacred_dragon")).oversizedInGui().register();
        Dejson.item(NS, "gun_drill")
                .definition(gun("hbm:item/gun_obj_base", "drill")).oversizedInGui().register();
        Dejson.item(NS, "gun_fatman")
                .definition(gun("hbm:item/gun_obj_base", "fatman")).oversizedInGui().register();
        Dejson.item(NS, "gun_fireext")
                .definition(gun("hbm:item/gun_obj_base", "fireext")).oversizedInGui().register();
        Dejson.item(NS, "gun_flamer")
                .definition(gun("hbm:item/gun_obj_base", "flamer")).oversizedInGui().register();
        Dejson.item(NS, "gun_flamer_daybreaker")
                .definition(gun("hbm:item/gun_obj_base", "flamer_daybreaker")).oversizedInGui().register();
        Dejson.item(NS, "gun_flamer_topaz")
                .definition(gun("hbm:item/gun_obj_base", "flamer_topaz")).oversizedInGui().register();
        Dejson.item(NS, "gun_flaregun")
                .definition(gun("hbm:item/gun_obj_base", "flaregun")).oversizedInGui().register();
        Dejson.item(NS, "gun_folly")
                .definition(gun("hbm:item/gun_obj_base", "folly")).oversizedInGui().register();
        Dejson.item(NS, "gun_g3")
                .definition(gun("hbm:item/gun_obj_g3_base", "g3")).oversizedInGui().register();
        Dejson.item(NS, "gun_g3_zebra")
                .definition(gun("hbm:item/gun_obj_base", "g3_zebra")).oversizedInGui().register();
    }

    private static void part1() {
        Dejson.item(NS, "gun_greasegun")
                .definition(gun("hbm:item/gun_obj_base", "greasegun")).oversizedInGui().register();
        Dejson.item(NS, "gun_hangman")
                .definition(gun("hbm:item/gun_obj_base", "hangman")).oversizedInGui().register();
        Dejson.item(NS, "gun_heavy_revolver")
                .definition(gun("hbm:item/gun_obj_base", "heavy_revolver")).oversizedInGui().register();
        Dejson.item(NS, "gun_heavy_revolver_lilmac")
                .definition(gun("hbm:item/gun_obj_base", "heavy_revolver_lilmac")).oversizedInGui().register();
        Dejson.item(NS, "gun_heavy_revolver_protege")
                .definition(gun("hbm:item/gun_obj_base", "heavy_revolver_protege")).oversizedInGui().register();
        Dejson.item(NS, "gun_henry")
                .definition(gun("hbm:item/gun_obj_base", "henry")).oversizedInGui().register();
        Dejson.item(NS, "gun_henry_lincoln")
                .definition(gun("hbm:item/gun_obj_base", "henry_lincoln")).oversizedInGui().register();
        Dejson.item(NS, "gun_lag")
                .definition(gun("hbm:item/gun_obj_base", "lag")).oversizedInGui().register();
        Dejson.item(NS, "gun_laser_pistol")
                .definition(gun("hbm:item/gun_obj_base", "laser_pistol")).oversizedInGui().register();
        Dejson.item(NS, "gun_laser_pistol_morning_glory")
                .definition(gun("hbm:item/gun_obj_base", "laser_pistol_morning_glory")).oversizedInGui().register();
        Dejson.item(NS, "gun_laser_pistol_pew_pew")
                .definition(gun("hbm:item/gun_obj_base", "laser_pistol_pew_pew")).oversizedInGui().register();
        Dejson.item(NS, "gun_lasrifle")
                .definition(gun("hbm:item/gun_obj_base", "lasrifle")).oversizedInGui().register();
        Dejson.item(NS, "gun_liberator")
                .definition(gun("hbm:item/gun_obj_base", "liberator")).oversizedInGui().register();
        Dejson.item(NS, "gun_light_revolver")
                .definition(gun("hbm:item/gun_obj_base", "atlas")).oversizedInGui().register();
        Dejson.item(NS, "gun_light_revolver_atlas")
                .definition(gun("hbm:item/gun_obj_base", "atlas_variant")).oversizedInGui().register();
        Dejson.item(NS, "gun_light_revolver_dani")
                .definition(gun("hbm:item/gun_obj_dani_base", "dani")).oversizedInGui().register();
        Dejson.item(NS, "gun_m2")
                .definition(gun("hbm:item/gun_obj_base", "m2")).oversizedInGui().register();
        Dejson.item(NS, "gun_maresleg")
                .definition(gun("hbm:item/gun_obj_base", "maresleg")).oversizedInGui().register();
        Dejson.item(NS, "gun_maresleg_akimbo")
                .definition(gun("hbm:item/gun_obj_base", "maresleg_akimbo")).oversizedInGui().register();
        Dejson.item(NS, "gun_maresleg_broken")
                .definition(gun("hbm:item/gun_obj_base", "maresleg_broken")).oversizedInGui().register();
        Dejson.item(NS, "gun_mas36")
                .definition(gun("hbm:item/gun_obj_base", "mas36")).oversizedInGui().register();
        Dejson.item(NS, "gun_minigun")
                .definition(gun("hbm:item/gun_obj_base", "minigun")).oversizedInGui().register();
        Dejson.item(NS, "gun_minigun_dual")
                .definition(gun("hbm:item/gun_obj_base", "minigun_dual")).oversizedInGui().register();
        Dejson.item(NS, "gun_minigun_lacunae")
                .definition(gun("hbm:item/gun_obj_base", "minigun_lacunae")).oversizedInGui().register();
        Dejson.item(NS, "gun_missile_launcher")
                .definition(gun("hbm:item/gun_obj_base", "missile_launcher")).oversizedInGui().register();
        Dejson.item(NS, "gun_mk108")
                .definition(gun("hbm:item/gun_obj_base", "mk108")).oversizedInGui().register();
        Dejson.item(NS, "gun_n_i_4_n_i")
                .definition(gun("hbm:item/gun_obj_base", "n_i_4_n_i")).oversizedInGui().register();
        Dejson.item(NS, "gun_pa_melee")
                .definition(gun("hbm:item/gun_obj_base", "pa_melee")).oversizedInGui().register();
        Dejson.item(NS, "gun_panzerschreck")
                .definition(gun("hbm:item/gun_obj_base", "panzerschreck")).oversizedInGui().register();
        Dejson.item(NS, "gun_pepperbox")
                .definition(gun("hbm:item/gun_obj_pepperbox_base", "pepperbox")).oversizedInGui().register();
    }

    private static void part2() {
        Dejson.item(NS, "gun_quadro")
                .definition(gun("hbm:item/gun_obj_base", "quadro")).oversizedInGui().register();
        Dejson.item(NS, "gun_spas12")
                .definition(gun("hbm:item/gun_obj_base", "spas12")).oversizedInGui().register();
        Dejson.item(NS, "gun_star_f")
                .definition(gun("hbm:item/gun_obj_base", "star_f")).oversizedInGui().register();
        Dejson.item(NS, "gun_star_f_akimbo")
                .definition(gun("hbm:item/gun_obj_base", "star_f_akimbo")).oversizedInGui().register();
        Dejson.item(NS, "gun_stg77")
                .definition(gun("hbm:item/gun_obj_base", "stg77")).oversizedInGui().register();
        Dejson.item(NS, "gun_stinger")
                .definition(gun("hbm:item/gun_obj_base", "stinger")).oversizedInGui().register();
        // 1.12.2 TEISR guns: their own renderers in the bare frame, not the Sedna weapon renderer
        ItemRendererDeclarations.model("gun_egon", "egon", "teisr_bare");
        ItemRendererDeclarations.model("gun_jshotgun", "jshotgun", "teisr_bare");
        // port: 1.12.2 CraftingManager's jade shotgun, a gun 1.7 does not have. Its two Uboiniks are 1.7's double-barrel
        // shotgun, its advanced rifle mechanism is 1.7's weapon steel mechanism and its spinning light is the halogen floodlight.
        shaped(NS, "gun_jshotgun", "hbm:gun_jshotgun", rows("LPP", "SSW", "PPD"),
                "L", ItemIds.spotlight_halogen, "P", TagIds.plates_steel, "S", ItemIds.gun_double_barrel,
                "W", TagIds.mechanisms_weaponsteel, "D", TagIds.dyes_green);
        Dejson.item(NS, "gun_tau")
                .definition(gun("hbm:item/gun_obj_base", "tau")).oversizedInGui().register();
        Dejson.item(NS, "gun_tesla_cannon")
                .definition(gun("hbm:item/gun_obj_base", "tesla_cannon")).oversizedInGui().register();
        Dejson.item(NS, "gun_uzi")
                .definition(gun("hbm:item/gun_obj_base", "uzi")).oversizedInGui().register();
        Dejson.item(NS, "gun_uzi_akimbo")
                .definition(gun("hbm:item/gun_obj_base", "uzi_akimbo")).oversizedInGui().register();
    }

    /** An {@code hbm:gun} node: the shared base model and the weapon whose renderer draws it. */
    private static ItemModels.Custom gun(String base, String weapon) {
        return ItemModels.custom("hbm:gun").with("base", base).with("weapon", weapon);
    }
}
