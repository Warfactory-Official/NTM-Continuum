package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import com.hbm.blocks.bomb.BombFloat;
import com.hbm.blocks.bomb.BombThermo;

/**
 * Dejson declarations for the special bombs. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}.
 *
 * <p>The five OBJ bombs share the MK5 bombs' empty placeholder blockstate (facing 2..5) and carry an
 * authored {@code hbm:machine_obj} item definition under {@code assets/hbm/items/}; the icon-drawn bombs
 * reproduce the 1.7 {@code getIcon} side/top split.
 */
public final class Bombs2Declarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private Bombs2Declarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        for (String bomb : new String[]{"nuke_custom", "nuke_fleija", "nuke_solinium", "nuke_prototype", "nuke_fstbmb"}) {
            Dejson.block(NS, bomb).rendered("hbm:block/machine_dummy_empty")
                    .states(Variants.uniformRange("facing", 2, 5)).register();
            Drops.self(NS, bomb);
        }

        // 1.7 BombThermo.getIcon: therm_top on up/down, the endo/exo icon on the four sides.
        column("therm_endo", "hbm:blocks/therm_top", "hbm:blocks/therm_endo");
        column("therm_exo", "hbm:blocks/therm_top", "hbm:blocks/therm_exo");
        // 1.7 BombFloat.getIcon: its own top on up/down, side elsewhere.
        column("float_bomb", "hbm:blocks/bomb_float_top", "hbm:blocks/bomb_float");
        column("emp_bomb", "hbm:blocks/bomb_emp_top", "hbm:blocks/bomb_emp_side");

        Dejson.block(NS, "flame_war").models(Models.cubeAll("hbm:blocks/flame_war"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "flame_war");

        Dejson.block(NS, "frozen_dirt").models(Models.cubeAll("hbm:blocks/frozen_dirt"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "frozen_dirt");
        Dejson.block(NS, "frozen_planks").models(Models.cubeAll("hbm:blocks/frozen_planks"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "frozen_planks");
        // 1.7 WasteEarth icons: the block's own top, frozen dirt underneath, the grass side around.
        Dejson.block(NS, "frozen_grass")
                .models(Models.cubeBottomTop("hbm:blocks/frozen_dirt", "hbm:blocks/frozen_grass_side", "hbm:blocks/frozen_grass_top"))
                .states(Variants.simple()).blockItemDirect().register();
        // 1.7 WasteEarth.getItemDropped: frozen_grass drops one snowball (silk touch keeps the block).
        Drops.oreNoFortune(NS, "frozen_grass", "minecraft:snowball", 1, 1);
        Dejson.block(NS, "frozen_log").models(Models.column("hbm:blocks/frozen_log_top", "hbm:blocks/frozen_log"))
                .states(Variants.axis()).blockItemDirect().register();
        // 1.7 WasteLog: 2 + rand(3) snowballs, silk touch keeps the block.
        Drops.oreNoFortune(NS, "frozen_log", "minecraft:snowball", 2, 4);

        for (String part : new String[]{"fleija_igniter", "fleija_propellant", "fleija_core",
                "solinium_igniter", "solinium_propellant", "solinium_core"}) {
            Dejson.item(NS, part).models(Models.generated("hbm:items/" + part)).register();
        }
    }

    private static void column(String id, String end, String side) {
        Dejson.block(NS, id).models(Models.column(end, side)).states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, id);
    }
}
