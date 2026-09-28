package com.hbm.world.gen;

import com.google.gson.JsonObject;
import com.hbm.config.GeneralConfig;
import com.hbm.config.MobConfig;
import com.hbm.config.WorldConfig;
import dev.golem.core.dejson.Features;

import java.util.ArrayList;
import java.util.List;

import static dev.golem.core.dejson.BiomeModifiers.LOCAL_MODIFICATIONS;
import static dev.golem.core.dejson.BiomeModifiers.RAW_GENERATION;
import static dev.golem.core.dejson.BiomeModifiers.TOP_LAYER_MODIFICATION;
import static dev.golem.core.dejson.BiomeModifiers.UNDERGROUND_ORES;
import static dev.golem.core.dejson.BiomeModifiers.VEGETAL_DECORATION;
import static dev.golem.core.dejson.BiomeModifiers.addFeatures;
import static dev.golem.core.dejson.Features.aboveBottom;
import static dev.golem.core.dejson.Features.absolute;
import static dev.golem.core.dejson.Features.biome;
import static dev.golem.core.dejson.Features.count;
import static dev.golem.core.dejson.Features.heightmap;
import static dev.golem.core.dejson.Features.inSquare;
import static dev.golem.core.dejson.Features.rarity;
import static dev.golem.core.dejson.Features.state;
import static dev.golem.core.dejson.Features.uniformHeight;

/**
 * HBM's world features as data: every configured feature, placed feature, feature biome tag and biome modifier, declared
 * through Golem Core's {@code dejson} vocabulary (plain JVM, no Minecraft class) and written at build time. One
 * declaration serves all three loaders — NeoForge and Forge read the emitted {@code biome_modifier} files, Fabric gets
 * the same list from {@code dev.golem.core.world.BiomeFeatures.install()}.
 *
 * <p>The specification is original-hbm (1.7) {@code HbmWorldGen.generateSurface/generateNether/generateEnd},
 * {@code HbmWorld.registerNTMTerrain} and {@code MainRegistry}'s noise features, at 1.7's config defaults
 * ({@link WorldConfig}). Every 1.7 {@code rand.nextInt(n) == 0} chunk gate is a {@code rarity_filter}, every vein count a
 * {@code count}, every random in-chunk column an {@code in_square}, every {@code minY + rand(variance)} a uniform
 * {@code height_range}, every {@code getHeightValue} a {@code MOTION_BLOCKING} {@code heightmap}, and every biome test a
 * biome modifier over a biome tag plus the {@code biome} filter. So each ore or feature is its own placed feature, and
 * {@code /place feature hbm:<id>} places one of it.
 *
 * <p><b>Steps.</b> 1.7 chain-loaded the oil bubbles and the crater into the cave generator (before decoration):
 * {@code raw_generation}. The noise strata rode {@code DecorateBiomeEvent.Pre}: {@code local_modifications}, before the
 * veins at {@code underground_ores} (the gneiss veins target the schist band). {@code HbmWorldGen} ran after the biome
 * decorator, so its surface features are appended to {@code vegetal_decoration} after vanilla's trees and plants, and
 * the pink tree, which must see the finished trees, goes last at {@code top_layer_modification}.
 *
 * <p><b>Documented deviations from 1.7:</b> ore hosts are {@code #minecraft:base_stone_overworld} instead of plain
 * stone (the old bands reach y 4; the deepslate blend starts at 8); 1.7's {@code +8} decoration offset on flower, hive,
 * meteorite and keyhole columns is not reproduced (the modern decoration grid is already chunk-aligned; the offset only
 * shifted everything by half a chunk and changes no distribution); the vein placer keeps its own {@code +8}.
 */
public final class ModBiomeModifiers {

    private static final String NS = "hbm";

    /** How many data files the pre-2026-09-25 declarations stood in for; kept for {@code DejsonGameTests}. */
    static final int FILES_REPLACED = 18;

    private static boolean declared;

    private ModBiomeModifiers() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        biomeTags();

        // ------------------------------------------------ chain-loaded terrain (1.7 HbmWorld.registerNTMTerrain)
        List<String> terrain = new ArrayList<>();
        if (GeneralConfig.enableRad && WorldConfig.radfreq > 0) terrain.add(once("sellafield_crater", "sellafield_crater"));
        if (WorldConfig.oilSpawn > 0) terrain.add(once("oil_bubble", "oil_bubble"));
        if (WorldConfig.bedrockOilSpawn > 0) terrain.add(once("bedrock_oil", "bedrock_oil"));
        terrain.add(once("oil_sand_bubble", "oil_sand_bubble"));
        addFeatures(NS, "terrain", "#minecraft:is_overworld", terrain, RAW_GENERATION);

        // ------------------------------------------------ noise strata (1.7 MainRegistry, DecorateBiomeEvent.Pre)
        addFeatures(NS, "strata", "#minecraft:is_overworld",
                List.of(once("schist_stratum", "schist_stratum"), once("ore_caves", "ore_caves"), once("ore_layers", "ore_layers")),
                LOCAL_MODIFICATIONS);

        addFeatures(NS, "overworld_ores", "#minecraft:is_overworld", overworldOres(), UNDERGROUND_ORES);
        addFeatures(NS, "nether_ores", "#minecraft:is_nether", netherOres(), UNDERGROUND_ORES);
        addFeatures(NS, "end_ores", "#minecraft:is_end",
                new Generated(vein("ore_tikite", WorldConfig.endTikiteSpawn, 6, 0, 127, blockHost("minecraft:end_stone"))),
                UNDERGROUND_ORES);

        surface();
    }

    // ================================================================================================ ores

    private static JsonObject stoneHost() {
        return Features.tagMatch("minecraft:base_stone_overworld");
    }

    private static JsonObject blockHost(String block) {
        return Features.blockMatch(block);
    }

    /** 1.7 {@code HbmWorldGen.generateSurface}, the ore half, in 1.7's order. */
    private static List<String> overworldOres() {
        List<String> out = new Generated();
        JsonObject stone = stoneHost();
        JsonObject gneiss = blockHost("hbm:stone_gneiss");

        // chance-gated pockets: generateOre(world, rand, i, j, 1, size, minY, variance, block) behind nextInt(n) == 0
        if (WorldConfig.gasBubbleChance > 0) out.add(vein("gas_bubble_flammable", "hbm:gas_flammable", 1, WorldConfig.gasBubbleChance, 32, 30, 10, stone));
        if (WorldConfig.explosiveBubbleChance > 0) out.add(vein("gas_bubble_explosive", "hbm:gas_explosive", 1, WorldConfig.explosiveBubbleChance, 32, 30, 10, stone));
        if (WorldConfig.alexandriteChance > 0) out.add(vein("ore_alexandrite", "hbm:ore_alexandrite", 1, WorldConfig.alexandriteChance, 3, 10, 5, stone));

        // DepthDeposit.generateConditionOverworld(world, i, 0, 3, j, 5, fill, block, rand, chance)
        out.add(depth("cluster_depth_iron", 24, 0.6, false, false));
        out.add(depth("cluster_depth_titanium", 32, 0.6, false, false));
        out.add(depth("cluster_depth_tungsten", 32, 0.6, false, false));
        out.add(depth("ore_depth_cinnabar", 16, 0.8, false, false));
        out.add(depth("ore_depth_zirconium", 16, 0.8, false, false));
        out.add(depth("ore_depth_borax", 16, 0.8, false, false));

        // the schist family; the band itself is schist_stratum at local_modifications
        out.add(vein("ore_gneiss_iron", 25, 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_gold", 10, 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_uranium", WorldConfig.uraniumSpawn * 3, 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_copper", WorldConfig.copperSpawn * 3, 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_asbestos", WorldConfig.asbestosSpawn * 3, 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_lithium", WorldConfig.lithiumSpawn, 6, 30, 10, gneiss));
        // 1.7 places rare earths here. (1.12 placed asbestos a second time, which the port copied until 2026-09-25.)
        out.add(vein("ore_gneiss_rare", WorldConfig.rareSpawn, 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_gas", WorldConfig.gasShaleSpawn * 3, 10, 30, 10, gneiss));

        out.add(vein("ore_uranium", WorldConfig.uraniumSpawn, 5, 5, 20, stone));
        out.add(vein("ore_thorium", WorldConfig.thoriumSpawn, 5, 5, 25, stone));
        out.add(vein("ore_titanium", WorldConfig.titaniumSpawn, 6, 5, 30, stone));
        out.add(vein("ore_sulfur", WorldConfig.sulfurSpawn, 8, 5, 30, stone));
        out.add(vein("ore_aluminium", WorldConfig.aluminiumSpawn, 6, 5, 40, stone));
        out.add(vein("ore_copper", WorldConfig.copperSpawn, 6, 5, 45, stone));
        out.add(vein("ore_fluorite", WorldConfig.fluoriteSpawn, 4, 5, 45, stone));
        out.add(vein("ore_niter", WorldConfig.niterSpawn, 6, 5, 30, stone));
        out.add(vein("ore_tungsten", WorldConfig.tungstenSpawn, 8, 5, 30, stone));
        out.add(vein("ore_lead", WorldConfig.leadSpawn, 9, 5, 30, stone));
        out.add(vein("ore_beryllium", WorldConfig.berylliumSpawn, 4, 5, 30, stone));
        out.add(vein("ore_rare", WorldConfig.rareSpawn, 5, 5, 20, stone));
        out.add(vein("ore_lignite", WorldConfig.ligniteSpawn, 24, 35, 25, stone));
        out.add(vein("ore_asbestos", WorldConfig.asbestosSpawn, 4, 16, 16, stone));
        out.add(vein("ore_cinnabar", WorldConfig.cinnabarSpawn, 4, 8, 16, stone));
        out.add(vein("ore_cobalt", WorldConfig.cobaltSpawn, 4, 4, 8, stone));

        out.add(vein("cluster_iron", WorldConfig.ironClusterSpawn, 6, 15, 45, stone));
        out.add(vein("cluster_titanium", WorldConfig.titaniumClusterSpawn, 6, 15, 30, stone));
        out.add(vein("cluster_aluminium", WorldConfig.aluminiumClusterSpawn, 6, 15, 35, stone));
        out.add(vein("cluster_copper", WorldConfig.copperClusterSpawn, 6, 15, 20, stone));

        // 1.7: stone_resource LIMESTONE, 16 blocks at y 25..54, overworld only
        out.add(vein("limestone", "hbm:stone_resource_limestone", WorldConfig.limestoneSpawn, 0, 16, 25, 30, stone));

        // BedrockOre.generateAuto behind rand.nextInt(10) == 0; the deposit picks its own column
        Features.configured(NS, "bedrock_ore", NS + ":bedrock_ore", null);
        Features.placed(NS, "bedrock_ore", NS + ":bedrock_ore", rarity(10), biome());
        out.add(NS + ":bedrock_ore");

        // 528 coltan: the random scatter (declared for /place feature) is off by default, the deposit is on
        String coltan = vein("ore_coltan", Math.max(1, GeneralConfig.coltanRate), 4, 15, 40, stone);
        if (GeneralConfig.enable528ColtanSpawn && GeneralConfig.coltanRate > 0) out.add(coltan);
        out.add(once("coltan_deposit", "coltan_deposit"));
        out.add(once("australium", "australium"));

        // keyhole stone: rand.nextInt(4) == 0, y 6 + rand(13), only into plain stone (isReplaceableOreGen(stone))
        JsonObject keyhole = new JsonObject();
        JsonObject target = new JsonObject();
        target.add("target", Features.blockMatch("minecraft:stone"));
        target.add("state", state("hbm:stone_keyhole"));
        com.google.gson.JsonArray targets = new com.google.gson.JsonArray();
        targets.add(target);
        keyhole.add("targets", targets);
        Features.configured(NS, "stone_keyhole", "minecraft:replace_single_block", keyhole);
        Features.placed(NS, "stone_keyhole", NS + ":stone_keyhole", rarity(4), inSquare(),
                uniformHeight(absolute(6), absolute(18)), biome());
        out.add(NS + ":stone_keyhole");
        return out;
    }

    /** 1.7 {@code HbmWorldGen.generateNether}, in 1.7's order. */
    private static List<String> netherOres() {
        List<String> out = new Generated();
        JsonObject rack = blockHost("minecraft:netherrack");
        out.add(vein("ore_nether_uranium", WorldConfig.netherUraniumSpawn, 6, 0, 127, rack));
        out.add(vein("ore_nether_tungsten", WorldConfig.netherTungstenSpawn, 10, 0, 127, rack));
        out.add(vein("ore_nether_sulfur", WorldConfig.netherSulfurSpawn, 12, 0, 127, rack));
        out.add(vein("ore_nether_fire", WorldConfig.netherPhosphorusSpawn, 6, 0, 127, rack));
        out.add(vein("ore_nether_coal", WorldConfig.netherCoalSpawn, 32, 16, 96, rack));
        // 1.7 quirk, kept: (minHeight 100, variance 26) puts cobalt at y 100..125, up against the roof
        out.add(vein("ore_nether_cobalt", WorldConfig.netherCobaltSpawn, 6, 100, 26, rack));
        // declared either way (for /place feature), generated only with 1.02_enablePlutoniumNetherOre (off by default)
        String plutonium = vein("ore_nether_plutonium", Math.max(1, WorldConfig.netherPlutoniumSpawn), 4, 0, 127, rack);
        if (GeneralConfig.enablePlutoniumOre && WorldConfig.netherPlutoniumSpawn > 0) out.add(plutonium);

        Features.configured(NS, "bedrock_ore_nether", NS + ":bedrock_ore_nether", null);
        Features.placed(NS, "bedrock_ore_nether", NS + ":bedrock_ore_nether", rarity(10), biome());
        out.add(NS + ":bedrock_ore_nether");

        // DepthDeposit.generateConditionNether: neodymium only (no nitan in 1.7), at the floor and against the roof
        out.add(depth("ore_depth_nether_neodymium", 16, 0.6, true, false));
        out.add(depth("ore_depth_nether_neodymium", 16, 0.6, true, true));

        // 30 smoldering columns and one geyser column, d = 16 + rand(96), scanning d-5..d
        out.add(crust("nether_smoldering", "hbm:ore_nether_smoldering", 30));
        out.add(crust("geysir_nether", "hbm:geysir_nether", 1));
        return out;
    }

    /** One vein family member whose feature name is its block. */
    private static String vein(String block, int count, int size, int minY, int variance, JsonObject host) {
        return vein(block, NS + ":" + block, count, 0, size, minY, variance, host);
    }

    /**
     * 1.7 {@code DungeonToolbox.generateOre(..., veinCount, amount, minHeight, variance, block, target)}, or with
     * {@code chance > 0} the one-vein pocket behind {@code rand.nextInt(chance) == 0}. Always declared (so
     * {@code /place feature} has it); returns null, i.e. not generated, when the config count is 0.
     */
    private static String vein(String name, String block, int count, int chance, int size, int minY, int variance, JsonObject host) {
        Features.configured(NS, name, NS + ":legacy_ore", Features.ore(host, state(block), size));
        List<JsonObject> placement = new ArrayList<>();
        if (chance > 0) placement.add(rarity(chance));
        if (count > 1) placement.add(count(count));
        placement.add(inSquare());
        placement.add(uniformHeight(absolute(minY), absolute(minY + Math.max(variance, 1) - 1)));
        placement.add(biome());
        Features.placed(NS, name, NS + ":" + name, placement.toArray(new JsonObject[0]));
        return count > 0 ? NS + ":" + name : null;
    }

    /** A feature list in generation order that skips what a config switch turned off (a null id). */
    private static final class Generated extends ArrayList<String> {
        Generated(String... ids) {
            for (String id : ids) add(id);
        }

        @Override
        public boolean add(String id) {
            return id != null && super.add(id);
        }
    }

    /** 1.7 {@code DepthDeposit.generateCondition*}: size 5 (overworld) or 7 (nether), y floor + rand(3) or 125 + rand(3). */
    private static String depth(String block, int chance, double fill, boolean nether, boolean roof) {
        String name = block + (nether ? (roof ? "_roof" : "_floor") : "");
        JsonObject config = new JsonObject();
        config.add("ore", state("hbm:" + block));
        config.add("filler", state(nether ? "hbm:stone_depth_nether" : "hbm:stone_depth"));
        config.add("host", state(nether ? "minecraft:netherrack" : "minecraft:stone"));
        config.addProperty("size", nether ? 7 : 5);
        config.addProperty("fill", fill);
        Features.configured(NS, name, NS + ":depth_deposit", config);
        Features.placed(NS, name, NS + ":" + name, rarity(chance), inSquare(),
                roof ? uniformHeight(absolute(125), absolute(127)) : uniformHeight(aboveBottom(0), aboveBottom(2)), biome());
        return NS + ":" + name;
    }

    private static String crust(String name, String block, int n) {
        Features.configured(NS, name, NS + ":crust_column", Features.blockState(state(block)));
        List<JsonObject> placement = new ArrayList<>();
        if (n > 1) placement.add(count(n));
        placement.add(inSquare());
        placement.add(uniformHeight(absolute(16), absolute(111)));
        placement.add(biome());
        Features.placed(NS, name, NS + ":" + name, placement.toArray(new JsonObject[0]));
        return NS + ":" + name;
    }

    /** A feature that runs once per chunk at the chunk corner: the noise strata, the chain-loaded terrain, the deposits. */
    private static String once(String name, String type) {
        Features.configured(NS, name, NS + ":" + type, null);
        Features.placed(NS, name, NS + ":" + name);
        return NS + ":" + name;
    }

    // ================================================================================================ surface

    /**
     * Biome tags for 1.7's biome tests. 1.7 tested classes ({@code instanceof BiomeGenForest}) or identities
     * ({@code == BiomeGenBase.roofedForest}); these list the modern biomes that are those 1.7 biomes. The pale garden
     * (1.21.4+) stands in for the roofed forest, as in the structure tables; the grove, which vanilla files under
     * {@code #minecraft:is_forest}, was no 1.7 forest and is left out.
     */
    private static void biomeTags() {
        Features.biomeTag(NS, "has_feature/foxglove", new String[] {"minecraft:forest", "minecraft:flower_forest",
                "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest"}, "minecraft:pale_garden");
        Features.biomeTag(NS, "has_feature/nightshade", new String[] {"minecraft:dark_forest"}, "minecraft:pale_garden");
    }

    /** 1.7 {@code HbmWorldGen.generateSurface}, the surface half. */
    private static void surface() {
        JsonObject[] onSurface = {inSquare(), heightmap("MOTION_BLOCKING"), biome()};

        // flowers and reeds (skipped after a Tom impact, in the feature)
        flowers("foxglove", "hbm:plant_flower_foxglove", 16, "#" + NS + ":has_feature/foxglove", onSurface);
        flowers("nightshade", "hbm:plant_flower_nightshade", 8, "#" + NS + ":has_feature/nightshade", onSurface);
        flowers("tobacco", "hbm:plant_flower_tobacco", 8, "#minecraft:is_jungle", onSurface);
        flowers("weed", "hbm:plant_flower_weed", 64, "#minecraft:is_overworld", onSurface);
        flowers("reeds_river", "hbm:plant_reeds", 4, "#minecraft:is_river", onSurface);
        flowers("reeds_beach", "hbm:plant_reeds", 8, "#minecraft:is_beach", onSurface);

        List<String> surface = new ArrayList<>();
        // glyphid hive: MobConfig.enableHives && rand.nextInt(hiveSpawn) == 0
        if (MobConfig.enableHives && MobConfig.hiveSpawn > 0) surface.add(gated("glyphid_hive", "glyphid_hive", MobConfig.hiveSpawn, onSurface));
        // fallen meteorite: rand.nextInt(meteoriteSpawn) == 0
        if (WorldConfig.meteoriteSpawn > 0) surface.add(gated("fallen_meteorite", "fallen_meteorite", WorldConfig.meteoriteSpawn, onSurface));
        addFeatures(NS, "surface", "#minecraft:is_overworld", surface, VEGETAL_DECORATION);

        // chlorine geyser: biome == BiomeGenBase.plains (not its sunflower mutation), on grass
        if (WorldConfig.geyserChlorine > 0) {
            addFeatures(NS, "geyser_chlorine", "minecraft:plains",
                    List.of(gated("geyser_chlorine", "geyser_chlorine", WorldConfig.geyserChlorine, onSurface)), VEGETAL_DECORATION);
        }

        // pink tree: rand.nextInt(1000) == 0, a random column (no heightmap: the whole column is scanned)
        Features.configured(NS, "pink_tree", NS + ":pink_tree", null);
        Features.placed(NS, "pink_tree", NS + ":pink_tree", rarity(1000), inSquare(), biome());
        addFeatures(NS, "pink_tree", "#minecraft:is_overworld", List.of(NS + ":pink_tree"), TOP_LAYER_MODIFICATION);
    }

    private static void flowers(String name, String block, int chance, String biomes, JsonObject[] onSurface) {
        Features.configured(NS, name, NS + ":legacy_flowers", Features.blockState(state(block)));
        JsonObject[] placement = new JsonObject[onSurface.length + 1];
        placement[0] = rarity(chance);
        System.arraycopy(onSurface, 0, placement, 1, onSurface.length);
        Features.placed(NS, name, NS + ":" + name, placement);
        addFeatures(NS, name, biomes, List.of(NS + ":" + name), VEGETAL_DECORATION);
    }

    private static String gated(String name, String type, int chance, JsonObject[] onSurface) {
        Features.configured(NS, name, NS + ":" + type, null);
        JsonObject[] placement = new JsonObject[onSurface.length + 1];
        placement[0] = rarity(chance);
        System.arraycopy(onSurface, 0, placement, 1, onSurface.length);
        Features.placed(NS, name, NS + ":" + name, placement);
        return NS + ":" + name;
    }
}
