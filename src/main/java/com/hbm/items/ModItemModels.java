package com.hbm.items;

import com.google.gson.JsonObject;
import dev.golem.core.dejson.Atlases;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.render.display.DisplaySlot;
import dev.golem.core.render.display.Pose17;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The item models that have no registration chain to live on, declared through Golem Core's
 * {@code dejson} vocabulary instead of written as JSON: the shared sprite atlases, the OBJ machine/gun
 * frames and base models, layered sprites of items whose owners cannot initialise in Dejson's plain JVM,
 * and a few ids registered elsewhere (a GameTest fixture, a gun). Since the 2026-10-01 Reg migration
 * almost every item states its model on its own registration ({@link com.hbm.registration.Reg}).
 *
 * <p>Items whose block owns them are not here either; a block's own item is declared beside the block in
 * its registration chain ({@link com.hbm.registration.Reg}), so the two cannot drift apart. Neither are the items whose
 * 26.1 definition is a {@code select}, a {@code condition} or a {@code special} — a gun or an RBMK
 * column drawn from an OBJ by its own renderer says something this vocabulary deliberately cannot.
 */
public final class ModItemModels {

    private static final String NS = "hbm";

    private static boolean declared;

    private ModItemModels() {}

    /**
     * The GameTest-only block {@code test_fan_blowable}. Its owner, {@code IntakeFanGameTests}, registers it
     * through a GameTest suite whose class cannot initialise in Dejson's plain JVM, so its plain stone
     * cube is stated here.
     */
    private static void gameTestFixtures() {
        dev.golem.core.dejson.Dejson.block(NS, "test_fan_blowable")
                .models(Models.cubeAll("minecraft:block/stone"))
                .states(dev.golem.core.dejson.Variants.simple())
                .blockItem()
                .register();
    }

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        sprites();
        renamedSprites();
        layeredSprites();
        blockItems();
        machineFrames();
        funnelModel();
        gameTestFixtures();
        // 1.7 names block sprites textures/blocks (any file there), outside the block/ directory vanilla stitches. The 1.7 GUI tank
        // textures (every file under textures/gui/fluids) are stitched too: they are what other mods draw an HBM fluid with
        // (Fluids.tokenSpec, History section 19ek-fluids), and a fluid sprite must be in the block atlas.
        Atlases.directories("minecraft", "blocks", "blocks", "blocks/", "gui/fluids", "gui/fluids/");
        // The fourteen rail/tower/compressor OBJ item definitions are authored JSON (assets/hbm/items/,
        // listed in gradle/dejson-authored.txt): hand-tuned transforms stay files, not JsonObject calls.
        // 1.7 names item sprites textures/items (any file there), outside the item/ directory vanilla stitches. Below 1.21.4
        // there is no items atlas; legacy-assets folds this source into blocks.json.
        Atlases.directories("minecraft", "items", "items", "items/");
    }


    private static final String[] SPRITE_ITEMS_0 = {
            "bedrock_ore_fragment_bismuth",
            "blueprint_folder_discover", "blueprints_528", "blueprints_discover", "blueprints_secret",
            "bolt"
    };

    private static final String[] SPRITE_ITEMS_2 = {
            "part_barrel_heavy", "part_barrel_light",
            "part_grip", "part_mechanism",
            "part_receiver_heavy", "part_receiver_light", "part_stock",
            "pipe",
            "plate_cast",
            "plate_welded"
    };

    private static final String[] SPRITE_ITEMS_3 = {
            "scraps_liquid",
            "shell",
            "wire_dense", "wire_fine"
    };

    /**
     * Items drawn as a single sprite named after the item, which is the overwhelming majority.
     * Split across several arrays only to keep any one static initialiser small.
     */
    private static void sprites() {
        for (String name : SPRITE_ITEMS_0) {
            Dejson.item(NS, name).models(Models.generated("hbm:items/" + name)).register();
        }
        for (String name : SPRITE_ITEMS_2) {
            Dejson.item(NS, name).models(Models.generated("hbm:items/" + name)).register();
        }
        for (String name : SPRITE_ITEMS_3) {
            Dejson.item(NS, name).models(Models.generated("hbm:items/" + name)).register();
        }
        // ingot_steel_dusted / ingot_chainsteel (sprite + ItemRendererHot glow) are on their registrations
        // (ModJetpackDependencyItems).
    }

    /** Items whose sprite is not named after them; the sprite is given rather than derived. */
    private static void renamedSprites() {
        renamed0();
        renamed1();
        // RPA/NCRPA/Taurun draw 1.7 renderStandard OBJ icons (ArmorSetDeclarations), not sprites.
    }

    private static void renamed0() {
        Dejson.item(NS, "gun_b92_flat").models(Models.generated("hbm:items/gun_b92")).register();
        // gun_pa_ranged is registered by NtmToolGunCatalog, which cannot run in Dejson's plain JVM.
        // 1.7 setFull3D() and no IItemRenderer: the sprite in 1.7's full-3D hand poses (Presets.handheld's parent)
        Dejson.item(NS, "gun_pa_ranged").models(Models.parented("hbm:item/handheld_full3d",
                Slots.of("layer0", "hbm:items/gun_pa_ranged"))).register();
    }

    private static void renamed1() {
        Dejson.item(NS, "zirnox_depleted_rod").models(Models.generated("hbm:items/rod_zirnox_empty")).register();
        Dejson.item(NS, "zirnox_rod").models(Models.generated("hbm:items/rod_zirnox_empty")).register();
        Dejson.item(NS, "zirnox_waste").models(Models.generated("hbm:items/rod_zirnox_empty")).register();
    }

    /** Items stacked from two or three sprites, drawn bottom layer first. */
    private static void layeredSprites() {
        layered0();
        layered1();
        layered2();
        layered3();
    }

    private static void layered0() {
        Dejson.item(NS, "rbmk_pellet_balefire_e0").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e1").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e2").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e3").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e4").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e0").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e1").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e2").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e3").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e4").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e0").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e1").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e2").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e3").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e4").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e0").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e1").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e2").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e3").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e4").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e0").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e1").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e2").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e3").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e4").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e0").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e1").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e2").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e3").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e4").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e0").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e1").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e2").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e3").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e4").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e0").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e1").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e2").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e3").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e4").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
    }

    private static void layered1() {
        Dejson.item(NS, "rbmk_pellet_hep241_e0").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e1").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e2").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e3").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e4").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e0").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e1").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e2").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e3").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e4").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e0").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e1").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e2").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e3").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e4").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e0").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e1").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e2").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e3").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e4").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e0").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e1").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e2").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e3").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e4").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e0").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e1").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e2").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e3").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e4").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e0").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e1").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e2").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e3").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e4").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e0").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e1").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e2").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e3").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e4").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
    }

    private static void layered2() {
        Dejson.item(NS, "rbmk_pellet_les_e0").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_les_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_les_e1").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_les_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_les_e2").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_les_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_les_e3").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_les_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_les_e4").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_les_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e0").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e1").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e2").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e3").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e4").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e0").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_men_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e1").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_men_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e2").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_men_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e3").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_men_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e4").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_men_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e0").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e1").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e2").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e3").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e4").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e0").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e1").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e2").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e3").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e4").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e0").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e1").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e2").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e3").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e4").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e0").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e1").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e2").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e3").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e4").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e0").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e1").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e2").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e3").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e4").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
    }

    private static void layered3() {
        Dejson.item(NS, "rbmk_pellet_pu238be_e0").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e1").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e2").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e3").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e4").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e0").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e1").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e2").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e3").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e4").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e0").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e1").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e2").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e3").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e4").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e0").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e1").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e2").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e3").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e4").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e0").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e1").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e2").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e3").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e4").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e0").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e1").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e2").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e3").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e4").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e0").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e1").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e2").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e3").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e4").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e0").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e1").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e2").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e3").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e4").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
    }

    /**
     * The two base models an OBJ machine item wears its frame on.
     *
     * <p>A machine icon is a frame times a body. The body is the item's own verbatim 1.7
     * {@code renderInventory}/{@code renderCommon} numbers, which its item definition carries and
     * {@code MachineItemModel} applies at draw time. The frame is what 1.7's {@code ItemRenderBase}
     * wrapped around every such body, and it is shared — so it lives here, once, and every machine
     * inherits it by pointing at one of these two models.
     *
     * <p>Two spellings of one frame because the renderer differs, not the pose. 26.1 reads display
     * properties off {@code machine_obj_context_base} through the special-model system; below 1.21.4
     * the same items are routed to a per-loader BEWLR, which vanilla only reaches for a model whose
     * parent chain ends at {@code minecraft:builtin/entity}.
     *
     * <p>Composed rather than written out. A {@code display{}} entry is a translate/rotate/scale triple
     * applied as {@code T . R . S}; the 1.7 chains are not in that shape until
     * {@link dev.golem.core.render.display.DisplayFrame} decomposes them, and copying one field by
     * field is how these icons ended up most of a slot too high.
     */
    private static void machineFrames() {
        machineFrame("machine", Pose17.Klass.ITEM_RENDER_BASE);
        machineFrame("missile", Pose17.Klass.ITEM_RENDER_MISSILE);
        machineFrame("weapon", Pose17.Klass.ITEM_RENDER_WEAPON);
        machineFrame("weapon_full3d", Pose17.Klass.ITEM_RENDER_WEAPON_FULL3D);
        machineFrame("block", blockFrames());
        weaponFlatFrame();
        spriteFrames();
        handheldFull3D();
        gunFrame();
        teisrFrame();
    }

    /**
     * The {@code teisr} frame: where 1.12 handed a {@code TEISRBase} item renderer its pose, old-hbm
     * {@code BakedModelTransforms.defaultItemTransforms()} (third person {@code 0, 3/16, 1/16} at 0.55, first person
     * rotated {@code 0, -90, 25}, translated {@code 1.13, 3.2, 1.13}/16 at 0.68, ground {@code 0, 2/16, 0} at 0.5, head and
     * frame turned 180, GUI identity) followed by the builtin renderer's {@code translate(-0.5, -0.5, -0.5)}. The
     * renderer's lighting in the GUI was off (the TEISR's layer model is not gui3d), which {@code front} light is.
     */
    private static void teisrFrame() {
        Map<DisplaySlot, Matrix4f> frame = new java.util.EnumMap<>(DisplaySlot.class);
        frame.put(DisplaySlot.GUI, new Matrix4f());
        frame.put(DisplaySlot.GROUND, vanillaPose(DisplaySlot.GROUND, 0, 0, 0, 0, 2, 0, 0.5F));
        frame.put(DisplaySlot.HEAD, vanillaPose(DisplaySlot.HEAD, 0, 180, 0, 0, 13, 7, 1F));
        frame.put(DisplaySlot.FIXED, vanillaPose(DisplaySlot.FIXED, 0, 180, 0, 0, 0, 0, 1F));
        for (DisplaySlot slot : new DisplaySlot[]{DisplaySlot.THIRD_PERSON_RIGHT_HAND, DisplaySlot.THIRD_PERSON_LEFT_HAND}) {
            frame.put(slot, vanillaPose(slot, 0, 0, 0, 0, 3, 1, 0.55F));
        }
        for (DisplaySlot slot : new DisplaySlot[]{DisplaySlot.FIRST_PERSON_RIGHT_HAND, DisplaySlot.FIRST_PERSON_LEFT_HAND}) {
            frame.put(slot, vanillaPose(slot, 0, -90, 25, 1.13F, 3.2F, 1.13F, 0.68F));
        }
        Dejson.item(NS, "teisr_obj_context_base")
                .models(Models.guiLight("front", Models.display(frame,
                        Models.parented("minecraft:block/block", Slots.of("particle", "hbm:blocks/block_steel")))))
                .register();
        Dejson.item(NS, "teisr_obj_context_legacy")
                .models(Models.guiLight("front", Models.display(frame,
                        Models.parented("minecraft:builtin/entity", Slots.EMPTY))))
                .register();
    }

    /**
     * {@code item/generated} and {@code item/handheld}'s own {@code display{}} on a {@code builtin/entity} marker,
     * for an {@code hbm:custom_item} that only adds to a sprite (the hot ingots' glow, the meteorite swords' glint).
     * Below 1.21.4 the item's model must be that marker for the renderer to be reached at all, so the sprite is
     * drawn by the renderer's hook too, in the same frame vanilla would have given it. The numbers are vanilla's
     * files, composed the way {@code ItemTransform.apply} composes them (left hands with the negation it applies)
     * about the block centre, which is where a {@code Pose17} frame puts vanilla's display.
     */
    private static void spriteFrames() {
        Map<DisplaySlot, Matrix4f> generated = new java.util.EnumMap<>(DisplaySlot.class);
        generated.put(DisplaySlot.GUI, new Matrix4f());
        generated.put(DisplaySlot.GROUND, vanillaPose(DisplaySlot.GROUND, 0, 0, 0, 0, 2, 0, 0.5F));
        generated.put(DisplaySlot.HEAD, vanillaPose(DisplaySlot.HEAD, 0, 180, 0, 0, 13, 7, 1F));
        generated.put(DisplaySlot.FIXED, vanillaPose(DisplaySlot.FIXED, 0, 180, 0, 0, 0, 0, 1F));
        for (DisplaySlot slot : new DisplaySlot[]{DisplaySlot.THIRD_PERSON_RIGHT_HAND, DisplaySlot.THIRD_PERSON_LEFT_HAND}) {
            generated.put(slot, vanillaPose(slot, 0, 0, 0, 0, 3, 1, 0.55F));
        }
        for (DisplaySlot slot : new DisplaySlot[]{DisplaySlot.FIRST_PERSON_RIGHT_HAND, DisplaySlot.FIRST_PERSON_LEFT_HAND}) {
            generated.put(slot, vanillaPose(slot, 0, -90, 25, 1.13F, 3.2F, 1.13F, 0.68F));
        }
        Map<DisplaySlot, Matrix4f> handheld = new java.util.EnumMap<>(generated);
        handheld.put(DisplaySlot.THIRD_PERSON_RIGHT_HAND,
                vanillaPose(DisplaySlot.THIRD_PERSON_RIGHT_HAND, 0, -90, 55, 0, 4, 0.5F, 0.85F));
        handheld.put(DisplaySlot.THIRD_PERSON_LEFT_HAND,
                vanillaPose(DisplaySlot.THIRD_PERSON_LEFT_HAND, 0, 90, -55, 0, 4, 0.5F, 0.85F));
        handheld.put(DisplaySlot.FIRST_PERSON_LEFT_HAND,
                vanillaPose(DisplaySlot.FIRST_PERSON_LEFT_HAND, 0, 90, -25, 1.13F, 3.2F, 1.13F, 0.68F));
        for (Map.Entry<String, Map<DisplaySlot, Matrix4f>> frame : Map.of("generated", generated, "handheld", handheld).entrySet()) {
            Dejson.item(NS, frame.getKey() + "_sprite_legacy")
                    .models(Models.guiLight("front", Models.display(frame.getValue(),
                            Models.parented("minecraft:builtin/entity", Slots.EMPTY))))
                    .register();
        }
    }

    /**
     * {@code hbm:item/handheld_full3d}: {@code item/handheld} in 1.7's held poses for a full-3D item (every
     * {@code ItemSword}/{@code ItemTool}/{@code ItemHoe} and {@code setFull3D()} item), which is what
     * {@code Presets.handheld} parents. Only the four hand slots differ from {@code item/handheld}; the inventory, ground,
     * frame and head slots are vanilla's (1.7 drew those the same for every sprite). Vanilla's own handheld pose is
     * 0.85 of the item where 1.7's is 0.9375 and about 20 degrees off, so 1.7's is stated here.
     */
    private static void handheldFull3D() {
        Dejson.model(NS + ":item/handheld_full3d", Models.display(ItemRenderTransformerPoses.full3D(),
                Models.parented("minecraft:item/handheld", Slots.EMPTY)));
    }

    /** One {@code display{}} entry as {@code ItemTransform.apply} poses it: translate, rotate X then Y then Z, scale. */
    private static Matrix4f vanillaPose(DisplaySlot slot, float rx, float ry, float rz, float tx, float ty, float tz,
                                        float scale) {
        boolean left = slot.leftHand();
        return new Matrix4f()
                // a frame lives in block space, corner at the origin; vanilla applies display{} about the centre
                .translate(0.5F, 0.5F, 0.5F)
                .translate((left ? -tx : tx) / 16F, ty / 16F, tz / 16F)
                .rotateXYZ((float) Math.toRadians(rx), (float) Math.toRadians(left ? -ry : ry),
                        (float) Math.toRadians(left ? -rz : rz))
                .scale(scale)
                .translate(-0.5F, -0.5F, -0.5F);
    }

    /**
     * The weapon frame for an {@code hbm:custom_item} whose inventory slot is the sprite (the held weapons whose
     * 1.7 {@code IItemRenderer} declined {@code INVENTORY}). Below 1.21.4 the item is one {@code builtin/entity}
     * model in every slot, so the slot the renderer declines is drawn by the same hook; {@code front} GUI light is
     * what the sprite's own {@code item/handheld} model asks for, and is the only difference from
     * {@code weapon_obj_context_legacy}. 26.1 resolves the sprite model itself and never reads this.
     */
    private static void weaponFlatFrame() {
        Dejson.item(NS, "weapon_obj_flat_legacy")
                .models(Models.guiLight("front", Models.display(Pose17.frames(Pose17.Klass.ITEM_RENDER_WEAPON),
                        Models.parented("minecraft:builtin/entity", Slots.EMPTY))))
                .register();
        // the same for a full-3D held weapon (an ItemSword/ItemTool, 1.7 RenderPlayer's full-3D hand pose)
        Dejson.item(NS, "weapon_full3d_obj_flat_legacy")
                .models(Models.guiLight("front", Models.display(Pose17.frames(Pose17.Klass.ITEM_RENDER_WEAPON_FULL3D),
                        Models.parented("minecraft:builtin/entity", Slots.EMPTY))))
                .register();
    }

    /**
     * The one base model every Sedna gun wears.
     *
     * <p>{@link Pose17.Klass#ITEM_RENDER_WEAPON} is the engine slot alone, so this carries none of
     * {@code ItemRenderWeaponBase}'s setup chain and none of any weapon's own: those are calls, and they
     * live in {@code WeaponRenderer}. What this replaces is a hand-fitted {@code display{}} that stood
     * in for all of it — one pose shared by sixty-six guns, in place of the fifty-two the oracle has.
     */
    private static void gunFrame() {
        Dejson.item(NS, "gun_obj_base")
                .models(Models.guiLight("side", Models.display(Pose17.frames(Pose17.Klass.ITEM_RENDER_WEAPON),
                        Models.parented("hbm:item/gun_obj_legacy", Slots.EMPTY))))
                .register();
    }

    /**
     * One 1.7 item-render class's frame, in the two spellings. {@code missile} is
     * {@code ItemRenderMissileGeneric}, which is not an {@code ItemRenderBase} at all: it wraps the body in
     * nothing, so its GUI frame is the bare 1.7 pixel slot and its hand poses end on that renderer's own
     * equipped preamble.
     */
    private static void machineFrame(String name, Pose17.Klass klass) {
        machineFrame(name, Pose17.frames(klass));
    }

    /**
     * The {@code block} frame: a 1.7 block item with no {@code IItemRenderer}, drawn in 3D by its
     * {@code ISimpleBlockRenderingHandler.renderInventoryBlock} (barrels, anvils, CRTs, rails, poles, ...). The
     * body is that method's own GL calls, written about its origin, the block centre.
     *
     * <p>GUI is 1.7 {@code RenderItem.renderItemIntoGUI}'s 3D-block branch (forge 1.7.10 RenderItem.java:445-463):
     * {@code translate(x - 2, y + 3) scale(10) translate(1, 0.5, 1) scale(1, 1, -1) rotate(210, X) rotate(45, Y)
     * rotate(-90, Y)}, which in this block space is exactly {@code translate(0.5) rotateX(30) rotateY(-45)
     * scale(0.625)} (checked numerically, 4 decimals). Vanilla {@code block/block}'s GUI pose is
     * {@code rotateY(225)}, a quarter turn away, so a nested {@code machine_obj_base} item shows another face.
     * The other slots are vanilla {@code block/block} about the block centre, the hand poses every block item has.
     */
    private static Map<DisplaySlot, Matrix4f> blockFrames() {
        Map<DisplaySlot, Matrix4f> frame = new java.util.EnumMap<>(DisplaySlot.class);
        frame.put(DisplaySlot.GUI, new Matrix4f().translate(0.5F, 0.5F, 0.5F)
                .rotateX((float) Math.toRadians(30)).rotateY((float) Math.toRadians(-45)).scale(0.625F));
        frame.put(DisplaySlot.GROUND, vanillaPose(DisplaySlot.GROUND, 0, 0, 0, 0, 3, 0, 0.25F));
        frame.put(DisplaySlot.FIXED, vanillaPose(DisplaySlot.FIXED, 0, 0, 0, 0, 0, 0, 0.5F));
        frame.put(DisplaySlot.HEAD, vanillaPose(DisplaySlot.HEAD, 0, 0, 0, 0, 0, 0, 1F));
        for (DisplaySlot slot : new DisplaySlot[]{DisplaySlot.THIRD_PERSON_RIGHT_HAND, DisplaySlot.THIRD_PERSON_LEFT_HAND}) {
            frame.put(slot, vanillaPose(slot, 75, 45, 0, 0, 2.5F, 0, 0.375F));
        }
        frame.put(DisplaySlot.FIRST_PERSON_RIGHT_HAND, vanillaPose(DisplaySlot.FIRST_PERSON_RIGHT_HAND, 0, 45, 0, 0, 0, 0, 0.4F));
        frame.put(DisplaySlot.FIRST_PERSON_LEFT_HAND, vanillaPose(DisplaySlot.FIRST_PERSON_LEFT_HAND, 0, 225, 0, 0, 0, 0, 0.4F));
        for (DisplaySlot slot : DisplaySlot.values()) {
            if (slot != DisplaySlot.GUI) frame.get(slot).translate(0.5F, 0.5F, 0.5F); // body origin = block centre
        }
        return frame;
    }

    private static void machineFrame(String name, Map<DisplaySlot, Matrix4f> frame) {
        Dejson.item(NS, name + "_obj_context_base")
                .models(Models.guiLight("side", Models.display(frame,
                        Models.parented("minecraft:block/block",
                                Slots.of("particle", "hbm:blocks/block_steel")))))
                .register();
        Dejson.item(NS, name + "_obj_context_legacy")
                .models(Models.guiLight("side", Models.display(frame,
                        Models.parented("minecraft:builtin/entity", Slots.EMPTY))))
                .register();
    }

    /**
     * The funnel's inventory icon is the old block OBJ with separate Top/Bottom/Side skins. Dejson's
     * ordinary item vocabulary intentionally does not claim renderer-owned special models, so this
     * declaration owns both the legacy marker and the special-model definition from the same registry.
     * {@link Models#blockItem(String)} emits the {@code builtin/entity} marker directly, while the
     * legacy-assets pass also recognises the special definition and keeps that marker authoritative.
     */
    private static void funnelModel() {
        Dejson.item(NS, "machine_funnel")
                .models(Models.blockItem("hbm:item/block_obj_context_legacy"))
                .register();

        // Dejson.itemDefinition returns the object stored in its definition registry (not a copy), and
        // DejsonWriter writes that same object. This is the narrow escape hatch for the custom OBJ
        // definition that the generic model-shape vocabulary deliberately cannot represent.
        JsonObject definition = Dejson.itemDefinition("hbm:item/machine_funnel");
        JsonObject machine = new JsonObject();
        machine.addProperty("type", "hbm:machine_obj");
        machine.addProperty("obj", "hbm:models/blocks/funnel.obj");
        machine.addProperty("texture", "hbm:textures/blocks/machine_funnel_side.png");
        JsonObject partTextures = new JsonObject();
        partTextures.addProperty("Top", "hbm:textures/blocks/machine_funnel_top.png");
        partTextures.addProperty("Bottom", "hbm:textures/blocks/machine_funnel_bottom.png");
        partTextures.addProperty("Side", "hbm:textures/blocks/machine_funnel_side.png");
        machine.add("partTextures", partTextures);
        // 1.7 RenderRail -> MachineFunnel.renderInventory: translate(0, -0.5, 0), Top/Bottom/Side, in the 3D
        // block-item frame (icon-P2 2026-10-01)
        machine.addProperty("frame", "block");
        machine.addProperty("rawLegacyTransform", true);
        machine.addProperty("scale", 1.0F);
        machine.addProperty("yaw", 0.0F);
        machine.addProperty("ty", -0.5F);
        definition.add("model", machine);
        definition.addProperty("oversized_in_gui", true);
    }

    /**
     * Block items whose block still loads its model from a file: the item model is a bare parent
     * pointing at that model, and there is nothing about the block itself to declare.
     */
    private static void blockItems() {
        Dejson.item(NS, "foam_layer_height2").models(Models.blockItem("hbm:block/foam_layer_height2")).register();
        Dejson.item(NS, "jetpack_glider_base").models(Models.blockItem("hbm:item/machine_obj_base")).register();
        Dejson.item(NS, "steel_scaffold_red").models(Models.blockItem("hbm:item/block_obj_context_legacy")).register();
        Dejson.item(NS, "steel_scaffold_steel").models(Models.blockItem("hbm:item/block_obj_context_legacy")).register();
        Dejson.item(NS, "steel_scaffold_white").models(Models.blockItem("hbm:item/block_obj_context_legacy")).register();
        Dejson.item(NS, "steel_scaffold_yellow").models(Models.blockItem("hbm:item/block_obj_context_legacy")).register();
    }
}
