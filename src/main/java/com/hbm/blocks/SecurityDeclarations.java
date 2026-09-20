package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import com.hbm.blocks.generic.BarbedWire;

/**
 * Dejson declarations for the security family. Touches no Minecraft class (runs in a plain JVM).
 *
 * <p>Hand-authored instead (Dejson has no vocabulary for them): the multipart blockstates and
 * {@code elements} models of {@code fence_metal} / {@code fence_metal_post} / {@code wood_barrier},
 * the {@code wood_barrier_inventory} item model, and the {@code hbm:machine_obj} item definitions of
 * the barbed wires and spikes (OBJ-drawn blocks whose blockstate is the shared empty placeholder).
 */
public final class SecurityDeclarations {

    private static final String NS = "hbm";

    /** The six barbed-wire ids (kept as strings: {@code BarbedWire} is a Minecraft class). */
    private static final String[] BARBED_WIRES = {
            "barbed_wire", "barbed_wire_fire", "barbed_wire_poison", "barbed_wire_acid", "barbed_wire_wither",
            "barbed_wire_ultradeath"
    };

    private static boolean declared;

    private SecurityDeclarations() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        blocks();
        items();
        drops();
    }

    private static void blocks() {
        // OBJ-drawn: the blockstate is the empty placeholder, the BER draws the mesh.
        for (String wire : BARBED_WIRES) {
            Dejson.block(NS, wire).rendered("hbm:block/machine_dummy_empty")
                    .states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        }
        Dejson.block(NS, "spikes").rendered("hbm:block/machine_dummy_empty").states(Variants.simple()).register();

        Dejson.block(NS, "barricade").models(Models.cubeAll("hbm:blocks/barricade"))
                .states(Variants.simple()).blockItemDirect().register();
        Dejson.block(NS, "machine_keyforge")
                .models(Models.cubeBottomTop("hbm:blocks/machine_keyforge_bottom", "hbm:blocks/machine_keyforge_side",
                        "hbm:blocks/machine_keyforge_top"))
                .states(Variants.simple()).blockItemDirect().register();
    }

    private static void items() {
        // 1.7.10 ItemBlockBase for the fence: a flat sprite of the block icon (fence_metal / fence_metal_post).
        Dejson.item(NS, "fence_metal").models(Models.generated("hbm:blocks/fence_metal")).register();
        Dejson.item(NS, "fence_metal_post").models(Models.generated("hbm:blocks/fence_metal_post")).register();
        // The old renderInventoryBlock drew a post and two rails; that is the authored wood_barrier_inventory model.
        Dejson.item(NS, "wood_barrier").models(Models.blockItem("hbm:block/wood_barrier_inventory")).register();

        Dejson.item(NS, "padlock").models(Models.generated("hbm:items/padlock")).register();
        Dejson.item(NS, "padlock_rusty").models(Models.generated("hbm:items/padlock_rusty")).register();
        Dejson.item(NS, "padlock_reinforced").models(Models.generated("hbm:items/padlock_reinforced")).register();
        Dejson.item(NS, "padlock_unbreakable").models(Models.generated("hbm:items/padlock_unbreakable")).register();
        // setTextureName("key_pair")
        Dejson.item(NS, "key_kit").models(Models.generated("hbm:items/key_pair")).register();
    }

    private static void drops() {
        for (String wire : BARBED_WIRES) {
            Drops.self(NS, wire);
        }
        Drops.self(NS, "spikes");
        // BlockNoDrop: getItemDropped returns null.
        Drops.nothing(NS, "barricade");
        Drops.self(NS, "fence_metal");
        Drops.self(NS, "fence_metal_post");
        Drops.self(NS, "wood_barrier");
        Drops.self(NS, "machine_keyforge");
    }
}
