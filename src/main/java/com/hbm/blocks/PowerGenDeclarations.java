package com.hbm.blocks;

import com.hbm.items.machine.ItemCassette.TrackType;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tints;
import dev.golem.core.dejson.Variants;
import com.hbm.items.machine.ItemCassette;

/**
 * Dejson declarations for the large power-generation family. Touches no Minecraft class: it runs in a
 * plain JVM from {@code dejson.gradle.kts}. {@link TrackType} is referenced for its table only.
 *
 * <p>The four OBJ-rendered machines get the shared empty blockstate placeholder here and an authored
 * {@code hbm:machine_obj} item definition under {@code assets/hbm/items/}; the siren is a plain cube and the
 * cassettes are 1.7's two-pass sprite ({@code cassette} under a colour-tinted {@code cassette_overlay}).
 */
public final class PowerGenDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private PowerGenDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // 1.7 icons: block_steel on top and bottom, machine_siren on the sides.
        Dejson.block(NS, "machine_siren")
                .models(Models.cubeBottomTop("hbm:blocks/block_steel", "hbm:blocks/machine_siren",
                        "hbm:blocks/block_steel"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        // Renderer-drawn folded machines: the blockstate exists only because vanilla demands one.
        for (String folded : new String[]{"machine_chungus", "machine_lpw2", "machine_hephaestus",
                "machine_industrial_generator"}) {
            Dejson.block(NS, folded).rendered("hbm:block/machine_dummy_empty")
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).register();
        }

        for (String block : new String[]{"machine_chungus", "machine_lpw2", "machine_hephaestus",
                "machine_industrial_generator", "machine_siren"}) {
            Drops.self(NS, block);
        }

        // 1.7 ModBlocks: the LPW-2 has no item renderer; its item is the flat block_steel sprite.
        Dejson.item(NS, "machine_lpw2").models(Models.generated("hbm:blocks/block_steel")).register();

        // 1.7 ItemCassette: pass 0 is the plain cassette, pass 1 the overlay tinted with the track colour.
        for (TrackType track : TrackType.VALUES) {
            if (track == TrackType.NULL) continue;
            Dejson.item(NS, track.registryName())
                    .models(Models.generated("hbm:items/cassette", "hbm:items/cassette_overlay"))
                    .tints(Tints.constant(-1), Tints.customModelData(0, track.getColor()))
                    .register();
        }
    }
}
