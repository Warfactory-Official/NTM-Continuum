package com.hbm.blocks;

import dev.golem.core.dejson.Crafting;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Tints;
import dev.golem.core.dejson.Variants;
import com.hbm.blocks.bomb.BlockVolcano;
import com.hbm.blocks.generic.BlockBiomeStone;
import com.hbm.blocks.generic.BlockDeadPlant;
import com.hbm.blocks.generic.BlockNTMFlower;
import com.hbm.blocks.generic.BlockOreBasalt;
import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.blocks.generic.BlockTallPlant;

/**
 * Dejson declarations for the terrain family: models, blockstates, item definitions, loot tables and the
 * packing recipes. Touches no Minecraft class (it runs in a plain JVM from {@code dejson.gradle.kts}).
 *
 * <p>Authored files (hand-built {@code elements}): {@code plant_reeds_*}, {@code vine_phosphor_*},
 * {@code ore_volcano} under {@code assets/hbm/models/block/}; the painted-waste, rope and strong-radaway
 * recipes under {@code data/hbm/recipe/}.
 */
public final class TerrainDeclarations {

    private static final String NS = "hbm";
    private static final String CUTOUT = "minecraft:cutout";
    /**
     * 1.7 {@code ColorizerGrass.getGrassColor(0.5, 1.0)} for the tobacco/hemp tint, as a constant: 26.1
     * tints are state-keyed and cannot read the biome (see {@code dev.golem.core.client.BlockTint}).
     */
    public static final int GRASS_TINT = 0x63A948;

    private static boolean declared;

    private TerrainDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        plants();
        ground();
        stone();
        hazards();
        vents();
        recipes();
    }

    private static void plants() {
        for (BlockDeadPlant.Type t : BlockDeadPlant.Type.values()) {
            Dejson.block(NS, t.id()).models(Models.renderType(CUTOUT, Models.cross(t.texture())))
                    .states(Variants.simple()).item(Models.generated(t.texture())).register();
            // 1.7 getItemDropped -> null; the Java getDrops says so, the table exists for completeness
            Drops.nothing(NS, t.id());
        }
        for (BlockNTMFlower.Type t : BlockNTMFlower.Type.values()) {
            Dejson.Declaration flower = Dejson.block(NS, t.id())
                    .models(Models.renderType(CUTOUT, t.tinted()
                            ? Models.parented("minecraft:block/tinted_cross", Slots.of("cross", t.texture()))
                            : Models.cross(t.texture())))
                    .states(Variants.simple());
            if (t.tinted()) {
                flower.register();
                Dejson.item(NS, t.id()).models(Models.generated(t.texture())).tints(Tints.constant(0xFF000000 | GRASS_TINT)).register();
            } else {
                flower.item(Models.generated(t.texture())).register();
            }
            Drops.self(NS, t.id());
        }
        for (BlockTallPlant.Type t : BlockTallPlant.Type.values()) {
            Models.Group group = Models.group()
                    .and("_lower", Models.renderType(CUTOUT, cross(t.tinted(), t.texture("lower"))))
                    .and("_upper", Models.renderType(CUTOUT, cross(t.tinted(), t.texture("upper"))));
            Dejson.Declaration tall = Dejson.block(NS, t.id()).models(group)
                    .states(Variants.select("half", "lower", "_lower", "upper", "_upper"));
            if (t.tinted()) {
                tall.register();
                Dejson.item(NS, t.id()).models(Models.generated(t.texture("lower"))).tints(Tints.constant(0xFF000000 | GRASS_TINT)).register();
            } else {
                tall.item(Models.generated(t.texture("lower"))).register();
            }
            Drops.nothing(NS, t.id()); // drops are computed in Java (flower item + willow leaves)
        }

        // reeds: authored plant_reeds_1/_2/_3 models (top only; top + bottom cap; top + mid section)
        Dejson.block(NS, "plant_reeds").statesOnly().states(Variants.select("depth", "1", "_1", "2", "_2", "3", "_3")).register();
        Dejson.item(NS, "plant_reeds").models(Models.generated("hbm:blocks/reeds_top")).register();
        Drops.only(NS, "plant_reeds", "minecraft:stick");

        // phosphor vines: authored vine_phosphor_ground/_middle/_hang (old-hbm's three models)
        Dejson.block(NS, "vine_phosphor").statesOnly()
                .states(Variants.select("part", "ground", "_ground", "middle", "_middle", "hang", "_hang")).register();
        Dejson.item(NS, "vine_phosphor").models(Models.generated("hbm:blocks/vine_phosphor_item")).register();
        Drops.self(NS, "vine_phosphor");

        Dejson.block(NS, "mush").models(Models.renderType(CUTOUT, Models.cross("hbm:blocks/mush")))
                .states(Variants.simple()).item(Models.generated("hbm:blocks/mush")).register();
        Drops.self(NS, "mush");
        Dejson.block(NS, "mush_block").models(Models.cubeAll("hbm:blocks/mush_block_skin"))
                .states(Variants.simple()).blockItemDirect().register();
        Dejson.block(NS, "mush_block_stem").models(Models.column("hbm:blocks/mush_block_inside", "hbm:blocks/mush_block_stem"))
                .states(Variants.simple()).blockItemDirect().register();

        // fallen leaves: the snow-layer shape over waste_leaves (as oil_spill / foam_layer)
        String leaves = "hbm:blocks/waste_leaves";
        Models.Group layers = Models.group().and("_full", Models.renderType(CUTOUT, Models.cubeAll(leaves)));
        for (int h = 2; h <= 14; h += 2) {
            layers = layers.and("_height" + h, Models.renderType(CUTOUT, Models.parented("minecraft:block/snow_height" + h,
                    Slots.of("particle", leaves, "texture", leaves))));
        }
        Dejson.block(NS, "leaves_layer").models(layers)
                .states(Variants.select("layers", "1", "_height2", "2", "_height4", "3", "_height6", "4", "_height8",
                        "5", "_height10", "6", "_height12", "7", "_height14", "8", "_full"))
                .register();
        Dejson.item(NS, "leaves_layer").models(Models.blockItem("hbm:block/leaves_layer_height2")).register();
        Drops.self(NS, "leaves_layer");

        Dejson.item(NS, "plant_item_tobacco").models(Models.generated("hbm:items/plant_item.tobacco")).register();
        Dejson.item(NS, "plant_item_rope").models(Models.generated("hbm:items/plant_item.rope")).register();
        Dejson.item(NS, "plant_item_mustardwillow").models(Models.generated("hbm:items/plant_item.mustardwillow")).register();
    }

    private static dev.golem.core.dejson.Shape cross(boolean tinted, String texture) {
        return tinted ? Models.parented("minecraft:block/tinted_cross", Slots.of("cross", texture)) : Models.cross(texture);
    }

    private static void ground() {
        Dejson.block(NS, "burning_earth")
                .models(Models.cubeBottomTop("hbm:blocks/waste_earth_bottom", "hbm:blocks/burning_grass_side", "hbm:blocks/burning_grass_top"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.nothing(NS, "burning_earth"); // drops dirt, computed in Java
    }

    private static void stone() {
        for (BlockBiomeStone.Type t : BlockBiomeStone.Type.values()) {
            String top = "hbm:blocks/stone_biome_top" + t.suffix();
            Dejson.block(NS, t.id())
                    .models(Models.group()
                            .and("", Models.cubeBottomTop(top, "hbm:blocks/stone_biome_layer" + t.suffix(), top))
                            .and("_covered", Models.cubeBottomTop(top, "hbm:blocks/stone_biome" + t.suffix(), top)))
                    .states(Variants.select("covered", "false", "", "true", "_covered"))
                    .blockItemDirect().register();
            Drops.self(NS, t.id());
        }
        cube("stone_porous", "hbm:blocks/stone_porous");
        Drops.nothing(NS, "stone_porous"); // cobblestone / silk-touch stone in Java
        for (BlockOreBasalt.Type t : BlockOreBasalt.Type.values()) {
            Dejson.block(NS, t.id())
                    .models(Models.cubeBottomTop(t.texture() + "_top", t.texture(), t.texture() + "_top"))
                    .states(Variants.simple()).blockItemDirect().register();
            Drops.nothing(NS, t.id()); // one drop item, silk touch the block — in Java
        }
        for (String v : new String[]{"iron", "copper", "aluminium", "rareearth", "cobalt"}) {
            cube("ore_meteor_" + v, "hbm:blocks/ore_meteor." + v);
            Drops.self(NS, "ore_meteor_" + v);
        }
        cube("block_meteor_molten", "hbm:blocks/meteor_cobble_molten");
        Drops.nothing(NS, "block_meteor_molten");

        // fissure: authored bedrock + molten overlay model, both crater states
        Dejson.block(NS, "ore_volcano").statesOnly().states(Variants.uniform("crater", "false", "true")).register();
        Dejson.item(NS, "ore_volcano").models(Models.blockItem("hbm:block/ore_volcano")).register();
        Drops.nothing(NS, "ore_volcano");

        for (boolean rad : new boolean[]{false, true}) {
            for (BlockVolcano.Mode mode : BlockVolcano.Mode.values()) {
                cube(mode.id(rad), rad ? "hbm:blocks/volcano_rad_core" : "hbm:blocks/volcano_core");
                Drops.self(NS, mode.id(rad));
            }
        }

        Dejson.block(NS, "geysir_chlorine")
                .models(Models.cubeBottomTop("minecraft:block/stone", "minecraft:block/stone", "hbm:blocks/geysir_stone"))
                .states(Variants.uniform("active", "false", "true")).blockItemDirect().register();
        Dejson.block(NS, "geysir_nether")
                .models(Models.cubeBottomTop("minecraft:block/netherrack", "minecraft:block/netherrack", "hbm:blocks/geysir_nether"))
                .states(Variants.uniform("active", "false", "true")).blockItemDirect().register();
        Drops.nothing(NS, "geysir_chlorine");
        Drops.nothing(NS, "geysir_nether");

        for (int i = 0; i < BlockSellafield.LEVELS; i++) {
            cube("sellafield_" + i, "hbm:blocks/sellafield_" + i);
            Drops.self(NS, "sellafield_" + i);
        }

        for (String base : new String[]{"stalagmite", "stalactite"}) {
            for (String v : new String[]{"ice", "snow", "glyphid1", "glyphid2", "glyphid3"}) {
                String id = base + "_" + v;
                Dejson.block(NS, id).models(Models.renderType(CUTOUT, Models.cross("hbm:blocks/" + base + "." + v)))
                        .states(Variants.simple()).item(Models.generated("hbm:blocks/" + base + "." + v)).register();
                Drops.nothing(NS, id); // BlockStalagmite computes its own drops
            }
        }
    }

    private static void hazards() {
        for (String id : new String[]{"crystal_virus", "crystal_pulsar", "block_tcalloy", "block_cdalloy", "block_australium",
                "block_lithium", "block_waste", "block_waste_painted", "block_waste_vitrified", "block_yellowcake"}) {
            cube(id, "hbm:blocks/" + id);
            Drops.self(NS, id);
        }
        for (String v : new String[]{"coal", "lignite", "petroleum"}) {
            cube("block_coke_" + v, "hbm:blocks/block_coke." + v);
            Drops.self(NS, "block_coke_" + v);
        }
        cube("block_fallout", "hbm:blocks/ash");
        Drops.self(NS, "block_fallout");

        // gas cells: invisible coal dust (smoke particles only), translucent chlorine
        Dejson.block(NS, "gas_coal").statesOnly("hbm:block/gas_radon").states(Variants.simple()).register();
        Dejson.block(NS, "chlorine_gas")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/chlorine_gas")))
                .states(Variants.simple()).register();
    }

    private static void vents() {
        for (String id : new String[]{"vent_chlorine", "vent_cloud", "vent_pink_cloud"}) {
            Dejson.block(NS, id).models(Models.column("hbm:blocks/vent_blank", "hbm:blocks/" + id))
                    .states(Variants.simple()).blockItemDirect().register();
            Drops.nothing(NS, id);
        }
        Dejson.block(NS, "vent_chlorine_seal")
                .models(Models.cubeBottomTop("hbm:blocks/vent_chlorine_seal_side", "hbm:blocks/vent_chlorine_seal_side", "hbm:blocks/vent_chlorine_seal_top"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "vent_chlorine_seal");
    }

    /** {@code MineralRecipes.add1To9Pair} and the block/nine-item pairs of this family. */
    private static void recipes() {
        pair("block_tcalloy", "hbm:ingot_tcalloy");
        pair("block_cdalloy", "hbm:ingot_cdalloy");
        pair("block_coke_coal", "hbm:coke_coal");
        pair("block_coke_lignite", "hbm:coke_lignite");
        pair("block_coke_petroleum", "hbm:coke_petroleum");
        pair("block_fallout", "hbm:falloutitem");
        pair("block_waste", "hbm:nuclear_waste");
        pair("block_australium", "hbm:ingot_australium");
        pair("block_yellowcake", "hbm:powder_yellowcake");
        pair("block_lithium", "hbm:lithium");
    }

    private static void pair(String block, String item) {
        Crafting.compress(NS, "terrain_" + block, item, "hbm:" + block);
        Crafting.decompress(NS, "terrain_" + block + "_unpack", "hbm:" + block, item);
    }

    private static void cube(String id, String texture) {
        Dejson.block(NS, id).models(Models.cubeAll(texture)).states(Variants.simple()).blockItemDirect().register();
    }
}
