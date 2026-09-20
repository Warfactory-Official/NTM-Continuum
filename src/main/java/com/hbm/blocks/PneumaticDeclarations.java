package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import com.hbm.blocks.network.PneumoStorageClutter;
import com.hbm.blocks.network.PneumoStorageMono;

/**
 * Dejson declarations for the pneumatic tube storage system. Touches no Minecraft class: it runs in a plain JVM
 * from {@code dejson.gradle.kts}.
 *
 * <p>The two tubes are renderer-drawn (their blockstates are the authored multipart placeholders and the plain
 * tube's item is an authored element model, both under {@code src/main/resources}); the storage blocks are plain
 * cubes over the 1.7 sprites, the access terminal an orientable cube with its front sprite on the facing side.
 */
public final class PneumaticDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private PneumaticDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        Dejson.block(NS, "pneumatic_storage_access")
                .models(Models.orientable("hbm:blocks/pneumatic_storage_access_front",
                        "hbm:blocks/pneumatic_storage_access", "hbm:blocks/pneumatic_storage_access"))
                .states(Variants.facingAll())
                .blockItemDirect()
                .register();
        Drops.self(NS, "pneumatic_storage_access");

        for (String block : new String[]{"pneumatic_storage_clutter", "pneumatic_storage_mono",
                "pneumatic_storage_importer", "pneumatic_storage_exporter"}) {
            Dejson.block(NS, block)
                    .models(Models.cubeAll("hbm:blocks/" + block))
                    .states(Variants.simple())
                    .blockItemDirect()
                    .register();
        }
        Drops.self(NS, "pneumatic_storage_importer");
        Drops.self(NS, "pneumatic_storage_exporter");
        // 1.7 PneumoStorageClutter.removedByPlayer / PneumoStorageMono.removedByPlayer: contents ride the drop
        Drops.container(NS, "pneumatic_storage_clutter", "minecraft:custom_name", "minecraft:container");
        Drops.container(NS, "pneumatic_storage_mono", "minecraft:container", "minecraft:custom_data",
                "minecraft:custom_name");

        // Renderer-drawn tubes: the blockstates are authored multipart placeholders.
        Drops.self(NS, "pneumatic_tube");
        Drops.self(NS, "pneumatic_tube_paintable");
        Dejson.item(NS, "pneumatic_tube_paintable")
                .models(Models.parented("minecraft:block/cube_all", Slots.of("all", "hbm:blocks/pneumatic_tube_paintable")))
                .register();
    }
}
