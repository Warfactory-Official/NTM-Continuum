package com.hbm.world.gen;

import com.google.gson.JsonObject;
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
        // the MapGen origins read their own chance; hbm:config only keeps 1.7's "> 0" and enableRad guards
        terrain.add(once("sellafield_crater", "sellafield_crater", cfgSwitch(WorldgenKeys.RAD_HOTSPOT), cfgSwitch(WorldgenKeys.RAD_HOTSPOT_RATE)));
        terrain.add(once("oil_bubble", "oil_bubble", cfgSwitch(WorldgenKeys.OIL)));
        terrain.add(once("bedrock_oil", "bedrock_oil", cfgSwitch(WorldgenKeys.BEDROCK_OIL)));
        terrain.add(once("oil_sand_bubble", "oil_sand_bubble"));
        addFeatures(NS, "terrain", "#minecraft:is_overworld", terrain, RAW_GENERATION);

        // ------------------------------------------------ noise strata (1.7 MainRegistry, DecorateBiomeEvent.Pre)
        addFeatures(NS, "strata", "#minecraft:is_overworld",
                List.of(once("schist_stratum", "schist_stratum"), once("ore_caves", "ore_caves"), once("ore_layers", "ore_layers")),
                LOCAL_MODIFICATIONS);

        addFeatures(NS, "overworld_ores", "#minecraft:is_overworld", overworldOres(), UNDERGROUND_ORES);
        addFeatures(NS, "nether_ores", "#minecraft:is_nether", netherOres(), UNDERGROUND_ORES);
        // 1.7 2.D02_endOres gates the trixite
        String tikite = vein("ore_tikite", "hbm:ore_tikite", List.of(cfgSwitch(WorldgenKeys.END_ORES)), cfgCount(WorldgenKeys.END_TIKITE, 1), 6, 0, 127,
                blockHost("minecraft:end_stone"));
        addFeatures(NS, "end_ores", "#minecraft:is_end", List.of(tikite), UNDERGROUND_ORES);

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
        List<String> out = new ArrayList<>();
        JsonObject stone = stoneHost();
        JsonObject gneiss = blockHost("hbm:stone_gneiss");

        // chance-gated pockets: generateOre(world, rand, i, j, 1, size, minY, variance, block) behind nextInt(n) == 0
        out.add(vein("gas_bubble_flammable", "hbm:gas_flammable", List.of(cfgChance(WorldgenKeys.GAS_BUBBLE)), null, 32, 30, 10, stone));
        out.add(vein("gas_bubble_explosive", "hbm:gas_explosive", List.of(cfgChance(WorldgenKeys.EXPLOSIVE_BUBBLE)), null, 32, 30, 10, stone));
        out.add(vein("ore_alexandrite", "hbm:ore_alexandrite", List.of(cfgChance(WorldgenKeys.ALEXANDRITE)), null, 3, 10, 5, stone));

        // 1.7 2.D00_overworldOres gates everything from the depth deposits to australium (not the bubbles,
        // alexandrite or the keyhole): each of these placed features starts with that switch
        List<JsonObject> on = List.of(cfgSwitch(WorldgenKeys.OVERWORLD_ORES));

        // DepthDeposit.generateConditionOverworld(world, i, 0, 3, j, 5, fill, block, rand, chance)
        out.add(depth("cluster_depth_iron", 24, 0.6, false, false, on));
        out.add(depth("cluster_depth_titanium", 32, 0.6, false, false, on));
        out.add(depth("cluster_depth_tungsten", 32, 0.6, false, false, on));
        out.add(depth("ore_depth_cinnabar", 16, 0.8, false, false, on));
        out.add(depth("ore_depth_zirconium", 16, 0.8, false, false, on));
        out.add(depth("ore_depth_borax", 16, 0.8, false, false, on));

        // the schist family; the band itself is schist_stratum at local_modifications
        out.add(vein("ore_gneiss_iron", "hbm:ore_gneiss_iron", on, count(25), 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_gold", "hbm:ore_gneiss_gold", on, count(10), 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_uranium", "hbm:ore_gneiss_uranium", on, cfgCount(WorldgenKeys.URANIUM, 3), 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_copper", "hbm:ore_gneiss_copper", on, cfgCount(WorldgenKeys.COPPER, 3), 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_asbestos", "hbm:ore_gneiss_asbestos", on, cfgCount(WorldgenKeys.ASBESTOS, 3), 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_lithium", "hbm:ore_gneiss_lithium", on, cfgCount(WorldgenKeys.LITHIUM, 1), 6, 30, 10, gneiss));
        // 1.7 places rare earths here. (1.12 placed asbestos a second time, which the port copied until 2026-09-25.)
        out.add(vein("ore_gneiss_rare", "hbm:ore_gneiss_rare", on, cfgCount(WorldgenKeys.RARE_EARTH, 1), 6, 30, 10, gneiss));
        out.add(vein("ore_gneiss_gas", "hbm:ore_gneiss_gas", on, cfgCount(WorldgenKeys.GAS_SHALE, 3), 10, 30, 10, gneiss));

        out.add(vein("ore_uranium", "hbm:ore_uranium", on, cfgCount(WorldgenKeys.URANIUM, 1), 5, 5, 20, stone));
        out.add(vein("ore_thorium", "hbm:ore_thorium", on, cfgCount(WorldgenKeys.THORIUM, 1), 5, 5, 25, stone));
        out.add(vein("ore_titanium", "hbm:ore_titanium", on, cfgCount(WorldgenKeys.TITANIUM, 1), 6, 5, 30, stone));
        out.add(vein("ore_sulfur", "hbm:ore_sulfur", on, cfgCount(WorldgenKeys.SULFUR, 1), 8, 5, 30, stone));
        out.add(vein("ore_aluminium", "hbm:ore_aluminium", on, cfgCount(WorldgenKeys.ALUMINIUM, 1), 6, 5, 40, stone));
        out.add(vein("ore_copper", "hbm:ore_copper", on, cfgCount(WorldgenKeys.COPPER, 1), 6, 5, 45, stone));
        out.add(vein("ore_fluorite", "hbm:ore_fluorite", on, cfgCount(WorldgenKeys.FLUORITE, 1), 4, 5, 45, stone));
        out.add(vein("ore_niter", "hbm:ore_niter", on, cfgCount(WorldgenKeys.NITER, 1), 6, 5, 30, stone));
        out.add(vein("ore_tungsten", "hbm:ore_tungsten", on, cfgCount(WorldgenKeys.TUNGSTEN, 1), 8, 5, 30, stone));
        out.add(vein("ore_lead", "hbm:ore_lead", on, cfgCount(WorldgenKeys.LEAD, 1), 9, 5, 30, stone));
        out.add(vein("ore_beryllium", "hbm:ore_beryllium", on, cfgCount(WorldgenKeys.BERYLLIUM, 1), 4, 5, 30, stone));
        out.add(vein("ore_rare", "hbm:ore_rare", on, cfgCount(WorldgenKeys.RARE_EARTH, 1), 5, 5, 20, stone));
        out.add(vein("ore_lignite", "hbm:ore_lignite", on, cfgCount(WorldgenKeys.LIGNITE, 1), 24, 35, 25, stone));
        out.add(vein("ore_asbestos", "hbm:ore_asbestos", on, cfgCount(WorldgenKeys.ASBESTOS, 1), 4, 16, 16, stone));
        out.add(vein("ore_cinnabar", "hbm:ore_cinnabar", on, cfgCount(WorldgenKeys.CINNABAR, 1), 4, 8, 16, stone));
        out.add(vein("ore_cobalt", "hbm:ore_cobalt", on, cfgCount(WorldgenKeys.COBALT, 1), 4, 4, 8, stone));

        out.add(vein("cluster_iron", "hbm:cluster_iron", on, cfgCount(WorldgenKeys.IRON_CLUSTER, 1), 6, 15, 45, stone));
        out.add(vein("cluster_titanium", "hbm:cluster_titanium", on, cfgCount(WorldgenKeys.TITANIUM_CLUSTER, 1), 6, 15, 30, stone));
        out.add(vein("cluster_aluminium", "hbm:cluster_aluminium", on, cfgCount(WorldgenKeys.ALUMINIUM_CLUSTER, 1), 6, 15, 35, stone));
        out.add(vein("cluster_copper", "hbm:cluster_copper", on, cfgCount(WorldgenKeys.COPPER_CLUSTER, 1), 6, 15, 20, stone));

        // 1.7: stone_resource LIMESTONE, 16 blocks at y 25..54, overworld only
        out.add(vein("limestone", "hbm:stone_resource_limestone", on, cfgCount(WorldgenKeys.LIMESTONE, 1), 16, 25, 30, stone));

        // BedrockOre.generateAuto behind rand.nextInt(10) == 0; the deposit picks its own column
        Features.configured(NS, "bedrock_ore", NS + ":bedrock_ore", null);
        placed("bedrock_ore", on, rarity(10), biome());
        out.add(NS + ":bedrock_ore");

        // 528 coltan: the random scatter (X528_enableColtanSpawning, off by default) and the deposit (its feature reads
        // X528_enableColtanDeposit)
        out.add(vein("ore_coltan", "hbm:ore_coltan", List.of(cfgSwitch(WorldgenKeys.OVERWORLD_ORES), cfgSwitch(WorldgenKeys.COLTAN_SPAWN)),
                cfgCount(WorldgenKeys.COLTAN_RATE, 1), 4, 15, 40, stone));
        out.add(once("coltan_deposit", "coltan_deposit", cfgSwitch(WorldgenKeys.OVERWORLD_ORES)));
        out.add(once("australium", "australium", cfgSwitch(WorldgenKeys.OVERWORLD_ORES)));

        // keyhole stone: rand.nextInt(4) == 0, y 6 + rand(13), only into plain stone (isReplaceableOreGen(stone));
        // y 6..8 is the deepslate blend in a modern world, and deepslate is 1.7's stone at depth (History §19ej-6)
        JsonObject keyhole = new JsonObject();
        com.google.gson.JsonArray targets = new com.google.gson.JsonArray();
        for (String host : new String[] {"minecraft:stone", "minecraft:deepslate"}) {
            JsonObject target = new JsonObject();
            target.add("target", Features.blockMatch(host));
            target.add("state", state("hbm:stone_keyhole"));
            targets.add(target);
        }
        keyhole.add("targets", targets);
        Features.configured(NS, "stone_keyhole", "minecraft:replace_single_block", keyhole);
        Features.placed(NS, "stone_keyhole", NS + ":stone_keyhole", rarity(4), inSquare(),
                uniformHeight(absolute(6), absolute(18)), biome());
        out.add(NS + ":stone_keyhole");
        return out;
    }

    /** 1.7 {@code HbmWorldGen.generateNether}, in 1.7's order. */
    private static List<String> netherOres() {
        List<String> out = new ArrayList<>();
        // 1.7 2.D01_netherOres gates the ores, bedrock ore and depth deposits, not the smoldering crust or geysers
        List<JsonObject> on = List.of(cfgSwitch(WorldgenKeys.NETHER_ORES));
        JsonObject rack = blockHost("minecraft:netherrack");
        out.add(vein("ore_nether_uranium", "hbm:ore_nether_uranium", on, cfgCount(WorldgenKeys.NETHER_URANIUM, 1), 6, 0, 127, rack));
        out.add(vein("ore_nether_tungsten", "hbm:ore_nether_tungsten", on, cfgCount(WorldgenKeys.NETHER_TUNGSTEN, 1), 10, 0, 127, rack));
        out.add(vein("ore_nether_sulfur", "hbm:ore_nether_sulfur", on, cfgCount(WorldgenKeys.NETHER_SULFUR, 1), 12, 0, 127, rack));
        out.add(vein("ore_nether_fire", "hbm:ore_nether_fire", on, cfgCount(WorldgenKeys.NETHER_PHOSPHORUS, 1), 6, 0, 127, rack));
        out.add(vein("ore_nether_coal", "hbm:ore_nether_coal", on, cfgCount(WorldgenKeys.NETHER_COAL, 1), 32, 16, 96, rack));
        // 1.7 quirk, kept: (minHeight 100, variance 26) puts cobalt at y 100..125, up against the roof
        out.add(vein("ore_nether_cobalt", "hbm:ore_nether_cobalt", on, cfgCount(WorldgenKeys.NETHER_COBALT, 1), 6, 100, 26, rack));
        // generated only with 1.02_enablePlutoniumNetherOre (off by default)
        out.add(vein("ore_nether_plutonium", "hbm:ore_nether_plutonium", List.of(cfgSwitch(WorldgenKeys.NETHER_ORES), cfgSwitch(WorldgenKeys.PLUTONIUM_ORE)),
                cfgCount(WorldgenKeys.NETHER_PLUTONIUM, 1), 4, 0, 127, rack));

        Features.configured(NS, "bedrock_ore_nether", NS + ":bedrock_ore_nether", null);
        placed("bedrock_ore_nether", on, rarity(10), biome());
        out.add(NS + ":bedrock_ore_nether");

        // DepthDeposit.generateConditionNether: neodymium only (no nitan in 1.7), at the floor and against the roof
        out.add(depth("ore_depth_nether_neodymium", 16, 0.6, true, false, on));
        out.add(depth("ore_depth_nether_neodymium", 16, 0.6, true, true, on));

        // 30 smoldering columns and one geyser column, d = 16 + rand(96), scanning d-5..d
        out.add(crust("nether_smoldering", "hbm:ore_nether_smoldering", 30));
        out.add(crust("geysir_nether", "hbm:geysir_nether", 1));
        return out;
    }

    /**
     * 1.7 {@code DungeonToolbox.generateOre(..., veinCount, amount, minHeight, variance, block, target)}: {@code gates}
     * (switches and chance gates, in 1.7's order) then {@code count} (null: one vein), a column in the chunk, a uniform
     * height and the biome check. Always declared and always listed: a config value of 0 makes {@code hbm:config} place
     * nothing, so {@code hbm.cfg} works without rebuilding the data pack (History §19ej-8).
     */
    private static String vein(String name, String block, List<JsonObject> gates, JsonObject count, int size, int minY, int variance,
                               JsonObject host) {
        Features.configured(NS, name, NS + ":legacy_ore", Features.ore(host, state(block), size));
        List<JsonObject> placement = new ArrayList<>(gates);
        if (count != null) placement.add(count);
        placement.add(inSquare());
        placement.add(uniformHeight(absolute(minY), absolute(minY + Math.max(variance, 1) - 1)));
        placement.add(biome());
        Features.placed(NS, name, NS + ":" + name, placement.toArray(new JsonObject[0]));
        return NS + ":" + name;
    }

    /** {@code Features.placed} with {@code gates} in front of {@code placement}. */
    private static void placed(String name, List<JsonObject> gates, JsonObject... placement) {
        List<JsonObject> all = new ArrayList<>(gates);
        all.addAll(List.of(placement));
        Features.placed(NS, name, NS + ":" + name, all.toArray(new JsonObject[0]));
    }

    /** {@code hbm:config} ({@code ConfigPlacement}): a step that reads {@code hbm.cfg} key {@code key} at generation. */
    static JsonObject cfg(String key, String mode, int multiplier) {
        if (WorldgenKeys.get(key) == null) throw new IllegalStateException("no WorldgenKeys binding for " + key);
        JsonObject json = new JsonObject();
        json.addProperty("type", NS + ":config");
        json.addProperty("key", key);
        json.addProperty("mode", mode);
        if (multiplier != 1) json.addProperty("multiplier", multiplier);
        return json;
    }

    /** {@code value * multiplier} veins (1.7 {@code for (k < veinCount)}). */
    static JsonObject cfgCount(String key, int multiplier) {
        return cfg(key, "count", multiplier);
    }

    /** Kept one time in {@code value} (1.7 {@code rand.nextInt(value) == 0}); 0 = off. */
    static JsonObject cfgChance(String key) {
        return cfg(key, "chance", 1);
    }

    /** Kept while the value is non-zero (a boolean key, or 1.7's {@code > 0} guard). */
    static JsonObject cfgSwitch(String key) {
        return cfg(key, "switch", 1);
    }

    /** 1.7 {@code DepthDeposit.generateCondition*}: size 5 (overworld) or 7 (nether), y floor + rand(3) or 125 + rand(3). */
    private static String depth(String block, int chance, double fill, boolean nether, boolean roof, List<JsonObject> gates) {
        String name = block + (nether ? (roof ? "_roof" : "_floor") : "");
        JsonObject config = new JsonObject();
        config.add("ore", state("hbm:" + block));
        config.add("filler", state(nether ? "hbm:stone_depth_nether" : "hbm:stone_depth"));
        config.add("host", state(nether ? "minecraft:netherrack" : "minecraft:stone"));
        config.addProperty("size", nether ? 7 : 5);
        config.addProperty("fill", fill);
        Features.configured(NS, name, NS + ":depth_deposit", config);
        placed(name, gates, rarity(chance), inSquare(),
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
    private static String once(String name, String type, JsonObject... gates) {
        Features.configured(NS, name, NS + ":" + type, null);
        Features.placed(NS, name, NS + ":" + name, gates);
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
        surface.add(gated("glyphid_hive", "glyphid_hive", List.of(cfgSwitch(WorldgenKeys.HIVES), cfgChance(WorldgenKeys.HIVE_SPAWN)), onSurface));
        // fallen meteorite: rand.nextInt(meteoriteSpawn) == 0
        surface.add(gated("fallen_meteorite", "fallen_meteorite", List.of(cfgChance(WorldgenKeys.METEORITE)), onSurface));
        addFeatures(NS, "surface", "#minecraft:is_overworld", surface, VEGETAL_DECORATION);

        // chlorine geyser: biome == BiomeGenBase.plains (not its sunflower mutation), on grass
        addFeatures(NS, "geyser_chlorine", "minecraft:plains",
                List.of(gated("geyser_chlorine", "geyser_chlorine", List.of(cfgChance(WorldgenKeys.GEYSER_CHLORINE)), onSurface)), VEGETAL_DECORATION);

        // pink tree: rand.nextInt(1000) == 0, a random column (no heightmap: the whole column is scanned)
        Features.configured(NS, "pink_tree", NS + ":pink_tree", null);
        Features.placed(NS, "pink_tree", NS + ":pink_tree", rarity(1000), inSquare(), biome());
        addFeatures(NS, "pink_tree", "#minecraft:is_overworld", List.of(NS + ":pink_tree"), TOP_LAYER_MODIFICATION);

        // 1.7 ModEventHandlerImpact: after Tom's impact, new chunks lose their grass and decorations (does nothing
        // until TomSaveData.impact is set); last, so it sees every decoration
        addFeatures(NS, "impact_aftermath", "#minecraft:is_overworld",
                List.of(once("impact_aftermath", "impact_aftermath")), TOP_LAYER_MODIFICATION);
    }

    private static void flowers(String name, String block, int chance, String biomes, JsonObject[] onSurface) {
        Features.configured(NS, name, NS + ":legacy_flowers", Features.blockState(state(block)));
        JsonObject[] placement = new JsonObject[onSurface.length + 1];
        placement[0] = rarity(chance);
        System.arraycopy(onSurface, 0, placement, 1, onSurface.length);
        Features.placed(NS, name, NS + ":" + name, placement);
        addFeatures(NS, name, biomes, List.of(NS + ":" + name), VEGETAL_DECORATION);
    }

    private static String gated(String name, String type, List<JsonObject> gates, JsonObject[] onSurface) {
        Features.configured(NS, name, NS + ":" + type, null);
        placed(name, gates, onSurface);
        return NS + ":" + name;
    }
}
