package com.hbm.items;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Tints;
import dev.golem.core.dejson.Variants;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * Dejson declarations for {@link ModUtilityToolItems} and the {@code book_guide_block}: item models on the 1.7
 * texture names, the legacy manual block's six-faced model, and the 1.7 {@code ToolRecipes} /
 * {@code CraftingManager} recipes of every item that has one. Touches no Minecraft class.
 *
 * <p>The holotape restoration and the restored-holotape-to-damaged recipes take one holotape variant only;
 * they are component shapeless recipes in {@link com.hbm.inventory.recipes.ModContainerRecipes}.
 */
public final class UtilityToolDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private UtilityToolDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        models();
        block();
        recipes();
    }

    private static void flat(String id, String texture) {
        Dejson.item(NS, id).models(Models.generated("hbm:items/" + texture)).register();
    }

    private static void models() {
        for (String id : new String[]{"analysis_tool", "containment_box", "digamma_diagnostic", "meltdown_tool",
                "ore_density_scanner", "survey_scanner", "pollution_detector", "power_net_tool", "settings_tool",
                "rtty_pager", "clay_tablet", "book_guide", "book_lemegeton"}) {
            flat(id, id);
        }
        flat("holotape_image", "holotape");
        // setFull3D
        Dejson.item(NS, "meteor_remote").models(Models.handheld("hbm:items/meteor_remote")).register();

        // pass 1 tinted with the primary fluid
        Dejson.item(NS, "fluid_identifier_multi")
                .models(Models.generated("hbm:items/fluid_identifier_multi", "hbm:items/fluid_identifier_overlay"))
                .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF))
                .register();

        // 1.7 ItemPipette.getIcon: pass 1 is pipette_empty at fill 0, else the (laboratory) overlay, tinted
        pipette("pipette", "pipette_overlay");
        pipette("pipette_boron", "pipette_overlay");
        pipette("pipette_laboratory", "pipette_laboratory_overlay");

        // 1.7 ItemBookLore: pages, the tinted cover, the tinted title
        Dejson.item(NS, "book_lore")
                .models(Models.generated("hbm:items/book_pages", "hbm:items/book_cover", "hbm:items/book_title"))
                .tints(Tints.constant(-1), Tints.customModelData(0, 0x303030), Tints.customModelData(1, 0xFFFFFF))
                .register();

        // 1.7 TextureColtass: 32 compass frames, the frame chosen by the client's needle
        Models.Group frames = Models.group();
        for (int i = 0; i < 32; i++) {
            // the first case is the item's own model
            frames = frames.and(i == 0 ? "" : String.format("_%02d", i),
                    Models.generated(String.format("hbm:items/coltass_%02d", i)));
        }
        Dejson.item(NS, "coltan_tool").models(frames).selects(NS + ":coltass").register();

        // 1.7 setTextureName("polaroid_" + polaroidID): case n is polaroid id n + 1
        Models.Group polaroids = Models.group();
        for (int i = 1; i <= 18; i++) {
            polaroids = polaroids.and(i == 1 ? "" : "_" + i, Models.generated("hbm:items/polaroid_" + i));
        }
        Dejson.item(NS, "polaroid").models(polaroids).selects(NS + ":polaroid").register();
    }

    private static void pipette(String id, String overlay) {
        Dejson.item(NS, id)
                .models(Models.group()
                        .and("", Models.generated("hbm:items/" + id, "hbm:items/pipette_empty"))
                        .and("_filled", Models.generated("hbm:items/" + id, "hbm:items/" + overlay)))
                .selects(NS + ":item_variant")
                .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF))
                .register();
    }

    /**
     * 1.7 {@code Guide}: bottom/top plus four sides. Modelled for a placer looking north (1.7 metadata 3:
     * north right, south left, east front, west back) and turned with the facing.
     */
    private static void block() {
        String b = "hbm:blocks/";
        Dejson.block(NS, "book_guide_block")
                .models(Models.parented("minecraft:block/cube",
                        Slots.of("down", b + "guide_bottom", "up", b + "guide_top", "north", b + "guide_side_right")
                                .and("south", b + "guide_side_left").and("east", b + "guide_front")
                                .and("west", b + "guide_back").and("particle", b + "guide_front")))
                .states(Variants.facing())
                .blockItem()
                .register();
        Drops.self(NS, "book_guide_block");
    }

    private static void recipes() {
        // 1.7 ToolRecipes
        shaped(NS, "survey_scanner", "hbm:survey_scanner", rows("SWS", " G ", "PCP"),
                "W", "#c:wires/gold", "P", "#c:ingots/any_plastic", "C", "hbm:circuit_advanced",
                "S", "#c:plates/steel", "G", "#c:ingots/gold");
        shapeless(NS, "digamma_diagnostic", "hbm:digamma_diagnostic",
                "hbm:geiger_counter", "#c:billets/polonium", "#c:ingots/asbestos");
        shaped(NS, "pollution_detector", "hbm:pollution_detector", rows("SFS", "SCS", " S "),
                "S", "#c:plates/steel", "F", "hbm:filter_coal", "C", "hbm:circuit_vacuum_tube");
        shaped(NS, "ore_density_scanner", "hbm:ore_density_scanner", rows("VVV", "CSC", "GGG"),
                "V", "hbm:circuit_vacuum_tube", "C", "hbm:circuit_capacitor", "S", "hbm:circuit_controller_chassis",
                "G", "#c:plates/gold");
        shaped(NS, "coltan_tool", "hbm:coltan_tool", rows("ACA", "CXC", "ACA"),
                "A", "#c:ingots/copper", "C", "#c:crystals/cinnabar", "X", "minecraft:compass");
        shaped(NS, "settings_tool", "hbm:settings_tool", rows(" P ", "PCP", "III"),
                "P", "#c:plates/iron", "C", "hbm:circuit_analog", "I", "hbm:plate_polymer");
        shaped(NS, "pipette", "hbm:pipette", rows("  L", " G ", "G  "),
                "L", "#c:ingots/any_rubber", "G", "#c:glass_blocks/colorless");
        shaped(NS, "pipette_boron", "hbm:pipette_boron", rows("  P", " B ", "B  "),
                "P", "#c:ingots/rubber", "B", "hbm:glass_boron");
        shaped(NS, "pipette_laboratory", "hbm:pipette_laboratory", rows("  C", " R ", "P  "),
                "C", "hbm:circuit_chip", "R", "#c:ingots/rubber", "P", "hbm:pipette_boron");
        shaped(NS, "power_net_tool", "hbm:power_net_tool", rows("WRW", " I ", " B "),
                "W", "#c:wires/red_copper", "R", "#c:dusts/redstone", "I", "#c:ingots/iron", "B", "hbm:battery_lead");
        shaped(NS, "analysis_tool", "hbm:analysis_tool", rows("  G", " S ", "S  "),
                "G", "#c:glass_panes", "S", "#c:ingots/steel");

        // 1.7 CraftingManager
        shaped(NS, "polaroid", "hbm:polaroid", rows(" C ", "RPY", " B "),
                "B", "#c:dusts/lapis", "C", "#c:dusts/coal", "R", "#c:dusts/red_copper", "Y", "#c:dusts/gold",
                "P", "minecraft:paper");
        shaped(NS, "containment_box", "hbm:containment_box", rows("LUL", "UCU", "LUL"),
                "L", "#c:plates/lead", "U", "#c:ingots/ferrouranium", "C", "hbm:crate_steel");
        shaped(NS, "rtty_pager", "hbm:rtty_pager", rows("R", "C", "S"),
                "R", "hbm:radio_torch_receiver", "C", "hbm:circuit_basic", "S", "#c:plates/steel");
        shaped(NS, "fluid_identifier_multi", "hbm:fluid_identifier_multi", rows("D", "C", "P"),
                "D", "#c:dyes", "C", "hbm:circuit_vacuum_tube", "P", "#c:plates/iron");
        shapeless(NS, "book_guide_rbmk", stack("hbm:book_guide").with("hbm:item_variant", 1),
                "minecraft:book", "minecraft:potato");
        shapeless(NS, "book_guide_starter", stack("hbm:book_guide").with("hbm:item_variant", 3),
                "minecraft:book", "minecraft:iron_ingot");
        shaped(NS, "book_guide_block", "hbm:book_guide_block", rows("IBI", "LBL", "IBI"),
                "B", "minecraft:book", "I", "#c:dyes/black", "L", "#c:dyes/blue");
    }
}
