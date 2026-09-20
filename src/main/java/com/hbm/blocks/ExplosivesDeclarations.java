package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Tags;
import dev.golem.core.dejson.Variants;
import com.hbm.blocks.bomb.DetMiner;

/**
 * Dejson declarations for the conventional explosives. Touches no Minecraft class: it runs in a plain
 * JVM from {@code dejson.gradle.kts}.
 *
 * <p>The renderer-drawn blocks (mines, timed charges, det cord, duds, multi purpose bomb) get the
 * shared empty blockstate here and an authored {@code hbm:machine_obj} item definition under
 * {@code assets/hbm/items/}.
 */
public final class ExplosivesDeclarations {

    private static final String NS = "hbm";
    private static final String EMPTY = "hbm:block/machine_dummy_empty";
    private static boolean declared;

    private ExplosivesDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // Landmines: renderer-drawn, never drop (breaking one sets it off).
        for (String mine : new String[] { "mine_ap", "mine_he", "mine_shrap", "mine_fat", "mine_naval" }) {
            Dejson.block(NS, mine).rendered(EMPTY).states(Variants.simple()).register();
            Drops.nothing(NS, mine);
        }

        // Timed charges: renderer-drawn, six-way facing, only the defuser yields the item back.
        Dispatch sixWay = Variants.uniform("facing", "down", "up", "north", "south", "west", "east");
        for (String charge : new String[] { "charge_dynamite", "charge_miner", "charge_c4", "charge_semtex" }) {
            Dejson.block(NS, charge).rendered(EMPTY).states(sixWay).register();
            Drops.nothing(NS, charge);
        }

        // Det cord: renderer-drawn cable with six connection flags.
        Dejson.block(NS, "det_cord").rendered(EMPTY).states(Variants.product(
                Variants.uniform("down", "false", "true"), Variants.uniform("up", "false", "true"),
                Variants.uniform("north", "false", "true"), Variants.uniform("south", "false", "true"),
                Variants.uniform("west", "false", "true"), Variants.uniform("east", "false", "true"))).register();
        Drops.self(NS, "det_cord");

        // The plain det blocks: 1.7 icons (det_nuke and det_miner carry a top icon on up/down).
        Dejson.block(NS, "det_charge").models(Models.cubeAll("hbm:blocks/det_charge")).states(Variants.simple())
                .blockItemDirect().register();
        Drops.self(NS, "det_charge");
        Dejson.block(NS, "det_nuke").models(Models.column("hbm:blocks/det_nuke_top", "hbm:blocks/det_nuke"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "det_nuke");
        Dejson.block(NS, "det_miner").models(Models.column("hbm:blocks/det_miner_top", "hbm:blocks/det_miner_side"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.nothing(NS, "det_miner"); // 1.7 DetMiner.getItemDropped returns null

        // Solid plastic explosive: the _front icon on the face opposite the piston-style orientation
        // (old-hbm block_semtex.json: front on south, facing=north unrotated).
        plastic("block_semtex");
        plastic("block_c4");

        // Fissure bomb: TNT-family block with the armed flag.
        Dejson.block(NS, "fissure_bomb")
                .models(Models.cubeBottomTop("hbm:blocks/fissure_bomb_bottom", "hbm:blocks/fissure_bomb_side",
                        "hbm:blocks/fissure_bomb_top"))
                .states(Variants.uniform("armed", "false", "true"))
                .blockItemDirect()
                .register();
        Drops.self(NS, "fissure_bomb");

        // Crashed duds: renderer-drawn, unbreakable (the defuser drops the payload instead).
        for (String dud : new String[] { "crashed_bomb_balefire", "crashed_bomb_conventional", "crashed_bomb_nuke",
                "crashed_bomb_salted" }) {
            Dejson.block(NS, dud).rendered(EMPTY).states(Variants.simple()).register();
            Drops.nothing(NS, dud);
        }

        // Multi purpose bomb: renderer-drawn, 1.7 facing metadata 2..5.
        Dejson.block(NS, "bomb_multi").rendered(EMPTY).states(Variants.uniformRange("facing", 2, 5)).register();
        Drops.self(NS, "bomb_multi");

        // 1.7 "record" ore-dict entry for the golden wire cutter: the discs every target version ships.
        Tags.item(NS, "music_discs", "minecraft:music_disc_13", "minecraft:music_disc_cat", "minecraft:music_disc_blocks",
                "minecraft:music_disc_chirp", "minecraft:music_disc_far", "minecraft:music_disc_mall",
                "minecraft:music_disc_mellohi", "minecraft:music_disc_stal", "minecraft:music_disc_strad",
                "minecraft:music_disc_ward", "minecraft:music_disc_11", "minecraft:music_disc_wait",
                "minecraft:music_disc_otherside", "minecraft:music_disc_5", "minecraft:music_disc_pigstep",
                "minecraft:music_disc_relic");

        // Items.
        for (String det : new String[] { "detonator_multi", "detonator_laser", "detonator_deadman", "detonator_de" }) {
            Dejson.item(NS, det).models(Models.handheld("hbm:items/" + det)).register();
        }
        Dejson.item(NS, "defuser_gold").models(Models.generated("hbm:items/defuser_gold")).register();
        for (String type : new String[] { "carpet", "napalm", "chlorine", "orange", "abomb", "stinger", "boxcar", "pc" }) {
            Dejson.item(NS, "bomb_caller_" + type).models(Models.handheld("hbm:items/bomb_caller")).register();
        }
    }

    private static void plastic(String id) {
        String side = "hbm:blocks/" + id;
        Dejson.block(NS, id)
                .models(Models.parented("minecraft:block/cube", Slots.of("particle", side)
                        .and("down", side).and("up", side).and("north", side).and("south", side + "_front")
                        .and("west", side).and("east", side)))
                .states(Variants.facingAll())
                .blockItemDirect()
                .register();
        Drops.self(NS, id);
    }
}
