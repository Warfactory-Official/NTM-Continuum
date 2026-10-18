package com.hbm.world.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hbm.config.StructureConfig;
import dev.golem.core.dejson.Data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The data half of 1.7's {@code NTMWorldGenerator}, declared through Dejson (plain JVM, no Minecraft class):
 * <ul>
 *   <li>{@code data/hbm/worldgen/structure/<id>.json}: one {@code hbm:nbt} structure per spawn;</li>
 *   <li>{@code data/hbm/tags/worldgen/biome/has_structure/<id>.json}: the biomes its 1.7 biome predicate accepts;</li>
 *   <li>{@code data/hbm/worldgen/structure_set/ntm_structures.json}: all of them with their 1.7 default weights, placed
 *       by {@code hbm:config_spread} (random_spread with spacing {@code structureMaxChunks}, separation
 *       {@code structureMinChunks}, both read from {@code hbm.cfg} at generation), salt 996996996, which is exactly 1.7's
 *       region pick. The weights that decide are {@link #runtimeWeight}, read from {@code hbm.cfg} by
 *       {@code NBTStructure.GenStructure}; the set's weights only order vanilla's tries;</li>
 *   <li>{@code data/hbm/tags/worldgen/structure/ntm_structures.json}: every real (non-null) one, for
 *       {@code /locate structure #hbm:ntm_structures} and maps.</li>
 * </ul>
 *
 * <p><b>Biomes.</b> 1.7 tested each biome's {@code BiomeDictionary} types, {@code heightVariation}, {@code rootHeight}
 * and {@code canSpawnLightningBolt()}. Modern biomes have none of those, so {@link #LEGACY} gives every vanilla
 * overworld biome the values of its 1.7 counterpart (read from the 1.7.10 sources: {@code BiomeGenBase} heights,
 * mutations' {@code createMutation}, Forge's {@code registerVanillaBiomes}), and the 1.7 predicates run over that table
 * at build time. Biomes 1.7 did not have take their nearest 1.7 analog (noted per row); the three cave biomes have none
 * and never host a surface structure.
 *
 * <p><b>Modded biomes.</b> 1.7 asked a modded biome the same questions, through the types its mod registered in the
 * {@code BiomeDictionary} and the heights it declared. The modern {@code BiomeDictionary} is the loaders' convention
 * tags, so each 1.7 type a predicate reads is an HBM tag {@code hbm:biome_dictionary/<type>} over the convention tags
 * that mean it ({@link #DICTIONARY_TAGS}; the build's legacy pass adds the 1.20.1 spellings, {@code forge:is_*} and
 * Fabric's first convention names). Each {@code has_structure} tag lists the table's vanilla biomes plus a superset
 * for the rest: the dictionary tag of a type the predicate requires, or {@code hbm:biome_dictionary/overworld} when
 * none is required ("not water and flat"). {@code NBTStructure.GenStructure} then runs the 1.7 predicate on the biome
 * at the stub: a table biome through its row (so the superset changes nothing for vanilla), any other overworld biome
 * through {@link com.hbm.world.gen.LegacyBiomes#guess}: its types from the dictionary tags, its rain from the biome, its
 * heights read off the generated terrain.
 *
 * <p>Folder names are the 26.1 ones; the build's legacy pass renames what differs per era (none of these do).
 */
public final class NTMStructureDeclarations {

    public static final String NS = "hbm";
    public static final String NULL_PLAINS = "null_plains";
    public static final String NULL_OCEAN = "null_ocean";
    public static final String SUBMARINE = "submarine";

    /** 1.7 {@code StructureConfig}: region salt, {@code 996996996 - dimension} for the overworld. */
    public static final int SALT = 996996996;

    private static boolean declared;

    private NTMStructureDeclarations() {}

    // 1.7 biome model

    /** The 1.7 {@code BiomeDictionary.Type}s the generator reads. */
    public enum Type { OCEAN, RIVER, PLAINS, SANDY, BEACH, SWAMP, SPARSE, MUSHROOM, SNOWY, FOREST, MOUNTAIN, HILLS, MESA, COLD, HOT, JUNGLE }

    /** A 1.7 biome as the generator saw it: {@code rootHeight}, {@code heightVariation}, types, lightning (rain, no snow). */
    public record LegacyBiome(float rootHeight, float heightVariation, Set<Type> types, boolean lightning) {
        public boolean is(Type type) { return types.contains(type); }

        /** 1.7 {@code isWaterBiome}: {@code Type.WATER} is OCEAN or RIVER. */
        public boolean water() { return is(Type.OCEAN) || is(Type.RIVER); }

        /** 1.7 {@code isFlatBiome}: little height variation, sparse vegetation, not water. */
        public boolean flat() { return heightVariation <= 0.2F && !water() && is(Type.SPARSE); }
    }

    private static LegacyBiome b(float root, float variation, boolean lightning, Type... types) {
        return new LegacyBiome(root, variation, types.length == 0 ? EnumSet.noneOf(Type.class) : EnumSet.of(types[0], types), lightning);
    }

    /** 1.7 {@code BiomeGenBase.Height} presets. */
    private static final float[] LOW_PLAINS = {0.125F, 0.05F}, DEFAULT = {0.1F, 0.2F}, MID_PLAINS = {0.2F, 0.2F},
            LOW_HILLS = {0.45F, 0.3F}, HIGH_PLATEAUS = {1.5F, 0.025F}, MID_HILLS = {1.0F, 0.5F}, SHORES = {0.0F, 0.025F},
            ROCKY_WATERS = {0.1F, 0.8F}, LOW_ISLANDS = {0.2F, 0.3F}, PARTIALLY_SUBMERGED = {-0.2F, 0.1F},
            SHALLOW_WATERS = {-0.5F, 0.0F}, OCEANS = {-1.0F, 0.1F}, DEEP_OCEANS = {-1.8F, 0.1F};

    private static LegacyBiome b(float[] height, boolean lightning, Type... types) {
        return b(height[0], height[1], lightning, types);
    }

    /** Vanilla overworld biome -> its 1.7 counterpart's generator-relevant values. Optional = absent before 1.21.4. */
    static final Map<String, LegacyBiome> LEGACY = new LinkedHashMap<>();
    static final Set<String> OPTIONAL = Set.of("minecraft:pale_garden");

    static {
        Type O = Type.OCEAN, R = Type.RIVER, P = Type.PLAINS, S = Type.SANDY, BE = Type.BEACH, SW = Type.SWAMP,
                SP = Type.SPARSE, MU = Type.MUSHROOM, SN = Type.SNOWY, F = Type.FOREST, MO = Type.MOUNTAIN,
                H = Type.HILLS, ME = Type.MESA, C = Type.COLD, HO = Type.HOT, J = Type.JUNGLE;
        // plains / BiomeGenPlains (LowPlains); sunflower plains is a new BiomeGenPlains, same height
        LEGACY.put("minecraft:plains", b(LOW_PLAINS, true, P));
        LEGACY.put("minecraft:sunflower_plains", b(LOW_PLAINS, true, P));
        // meadow: no 1.7 biome. A grassy flat, but one of the modern mountain biomes (#minecraft:is_mountain): on the
        // harness seed its start chunks average y 110-112 (relief 14-15, i.e. 1.7 rootHeight ~2.7, heightVariation
        // ~0.4). Filed as plains at MidPlains height it took the dish, which 1.7 clamps to y 53..65: about 45 blocks
        // under the meadow. 1.7 had no plains that high, so it is filed as extreme-hills terrain.
        LEGACY.put("minecraft:meadow", b(MID_HILLS, true, MO, H));
        LEGACY.put("minecraft:snowy_plains", b(LOW_PLAINS, false, C, SN));                     // icePlains
        LEGACY.put("minecraft:ice_spikes", b(0.425F, 0.45F, false, C, SN));                    // ice plains spikes (+0.3, +0.4)
        LEGACY.put("minecraft:desert", b(LOW_PLAINS, false, HO, S));
        LEGACY.put("minecraft:swamp", b(PARTIALLY_SUBMERGED, true, SW));
        LEGACY.put("minecraft:mangrove_swamp", b(PARTIALLY_SUBMERGED, true, SW));              // no 1.7 biome: swampland
        LEGACY.put("minecraft:forest", b(DEFAULT, true, F));
        LEGACY.put("minecraft:flower_forest", b(0.1F, 0.4F, true, F));                          // forest mutation, variation + 0.2
        LEGACY.put("minecraft:birch_forest", b(DEFAULT, true, F));
        LEGACY.put("minecraft:old_growth_birch_forest", b(0.2F, 0.4F, true, F));               // BiomeGenMutated (+0.1, +0.2)
        LEGACY.put("minecraft:dark_forest", b(DEFAULT, true, F));                               // roofedForest
        LEGACY.put("minecraft:pale_garden", b(DEFAULT, true, F));                               // no 1.7 biome: roofedForest
        LEGACY.put("minecraft:taiga", b(MID_PLAINS, true, C, F));
        LEGACY.put("minecraft:snowy_taiga", b(MID_PLAINS, false, C, SN, F));                    // coldTaiga
        LEGACY.put("minecraft:old_growth_pine_taiga", b(MID_PLAINS, true, C, F));              // megaTaiga
        LEGACY.put("minecraft:old_growth_spruce_taiga", b(MID_PLAINS, true, C, F));            // mega spruce taiga, same height
        // grove, snowy slopes, cherry grove: modern mountain biomes (#minecraft:is_mountain) that sit high like the meadow
        // (a 1.20.1 snowy-slopes sample averaged y 140); filed as extreme-hills terrain for the same reason.
        LEGACY.put("minecraft:grove", b(MID_HILLS, false, C, SN, F, MO, H));                   // no 1.7 biome: coldTaigaHills
        LEGACY.put("minecraft:savanna", b(LOW_PLAINS, false, HO, P, SP));
        LEGACY.put("minecraft:savanna_plateau", b(HIGH_PLATEAUS, false, HO, P, SP));
        LEGACY.put("minecraft:windswept_savanna", b(0.3625F, 1.225F, false, HO));               // savanna M (root*.5+.3, var*.5+1.2)
        LEGACY.put("minecraft:windswept_hills", b(MID_HILLS, true, MO, H));                     // extremeHills
        LEGACY.put("minecraft:windswept_gravelly_hills", b(MID_HILLS, true, MO, H));            // extremeHills M, same height
        LEGACY.put("minecraft:windswept_forest", b(MID_HILLS, true, MO, F, SP));                // extremeHillsPlus
        LEGACY.put("minecraft:stony_peaks", b(MID_HILLS, true, MO, H));                         // no 1.7 biome: extremeHills
        LEGACY.put("minecraft:jagged_peaks", b(MID_HILLS, false, C, SN, MO));                   // no 1.7 biome: snowy mountain
        LEGACY.put("minecraft:frozen_peaks", b(MID_HILLS, false, C, SN, MO));                   // no 1.7 biome: snowy mountain
        LEGACY.put("minecraft:snowy_slopes", b(MID_HILLS, false, C, SN, MO, H));                // iceMountains, at extreme-hills height
        LEGACY.put("minecraft:cherry_grove", b(MID_HILLS, true, F, MO, H));                     // no 1.7 biome: forestHills, at extreme-hills height
        LEGACY.put("minecraft:jungle", b(DEFAULT, true, HO, J));
        LEGACY.put("minecraft:bamboo_jungle", b(DEFAULT, true, HO, J));                         // no 1.7 biome: jungle
        LEGACY.put("minecraft:sparse_jungle", b(DEFAULT, true, HO, J, F));                      // jungleEdge
        LEGACY.put("minecraft:badlands", b(DEFAULT, false, ME, S));                             // mesa (setDisableRain)
        LEGACY.put("minecraft:eroded_badlands", b(DEFAULT, false, ME, S));                      // mesa (Bryce), same height
        LEGACY.put("minecraft:wooded_badlands", b(HIGH_PLATEAUS, false, ME, SP, S));            // mesaPlateau_F
        LEGACY.put("minecraft:river", b(SHALLOW_WATERS, true, R));
        LEGACY.put("minecraft:frozen_river", b(SHALLOW_WATERS, false, C, R, SN));
        LEGACY.put("minecraft:beach", b(SHORES, true, BE));
        LEGACY.put("minecraft:snowy_beach", b(SHORES, false, C, BE, SN));                       // coldBeach
        LEGACY.put("minecraft:stony_shore", b(ROCKY_WATERS, true, BE));                         // stoneBeach
        LEGACY.put("minecraft:ocean", b(OCEANS, true, O));
        LEGACY.put("minecraft:warm_ocean", b(OCEANS, true, O));                                 // no 1.7 biome: ocean
        LEGACY.put("minecraft:lukewarm_ocean", b(OCEANS, true, O));
        LEGACY.put("minecraft:cold_ocean", b(OCEANS, true, O));
        LEGACY.put("minecraft:frozen_ocean", b(OCEANS, false, C, O, SN));
        LEGACY.put("minecraft:deep_ocean", b(DEEP_OCEANS, true, O));
        LEGACY.put("minecraft:deep_lukewarm_ocean", b(DEEP_OCEANS, true, O));
        LEGACY.put("minecraft:deep_cold_ocean", b(DEEP_OCEANS, true, O));
        LEGACY.put("minecraft:deep_frozen_ocean", b(DEEP_OCEANS, false, C, O, SN));
        LEGACY.put("minecraft:mushroom_fields", b(LOW_ISLANDS, true, MU));
    }

    // The structures

    /**
     * One entry of the structure set: resource id, 1.7 spawn name, weight, 1.7 biome predicate, and the types of which
     * the predicate requires at least one (empty: none, the whole overworld is the candidate set).
     */
    public record Entry(String id, String spawn, int weight, Predicate<LegacyBiome> biome, boolean real, Set<Type> requires) {}

    static final List<Entry> ENTRIES = new ArrayList<>();

    private static void add(String id, String spawn, int weight, Predicate<LegacyBiome> biome, Type... requires) {
        if (weight <= 0) return; // 1.7: weight 0 (a disabled toggle) never wins a pick
        ENTRIES.add(new Entry(id, spawn, weight, biome, true, types(requires)));
    }

    private static Set<Type> types(Type... types) {
        return types.length == 0 ? EnumSet.noneOf(Type.class) : EnumSet.of(types[0], types);
    }

    static {
        add("spire", "spire", StructureConfig.spireSpawnWeight, b -> b.heightVariation() <= 0.05F && !b.water());
        add("features", "features", StructureConfig.featuresSpawnWeight, b -> !b.water()); // WG-C: MapGenNTMFeatures.start
        add("bunker", "bunker", StructureConfig.bunkerSpawnWeight, b -> !b.water()); // WG-C: BunkerComponents.BunkerStart
        add("vertibird", "vertibird", StructureConfig.vertibirdSpawnWeight, b -> !b.water() && b.is(Type.SANDY), Type.SANDY);
        add("crashed_vertibird", "crashed_vertibird", StructureConfig.vertibirdCrashedSpawnWeight, b -> !b.water() && b.is(Type.SANDY), Type.SANDY);
        add("beached_patrol", "beached_patrol", StructureConfig.beachedPatrolSpawnWeight, b -> b.is(Type.BEACH), Type.BEACH);
        add("aircraft_carrier", "aircraft_carrier", StructureConfig.enableOceanStructures ? StructureConfig.aircraftCarrierSpawnWeight : 0, b -> b.is(Type.OCEAN), Type.OCEAN);
        add("oil_rig", "oil_rig", StructureConfig.enableOceanStructures ? StructureConfig.oilRigSpawnWeight : 0, b -> b.is(Type.OCEAN) && b.rootHeight() >= -1.5F, Type.OCEAN);
        add("lighthouse", "lighthouse", StructureConfig.enableOceanStructures ? StructureConfig.lighthouseSpawnWeight : 0, b -> b.is(Type.OCEAN) || b.is(Type.BEACH), Type.OCEAN, Type.BEACH);
        add("dish", "dish", StructureConfig.dishSpawnWeight, b -> b.is(Type.PLAINS), Type.PLAINS);
        add("water_pump", "water_pump", StructureConfig.waterPumpSpawnWeight, b -> b.is(Type.PLAINS) || b.is(Type.SWAMP), Type.PLAINS, Type.SWAMP);
        add("forestchem", "forestchem", StructureConfig.forestChemSpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water());
        add("labolatory", "labolatory", StructureConfig.laboratorySpawnWeight, LegacyBiome::flat, Type.SPARSE);
        add("forest_post", "forest_post", StructureConfig.forestPostSpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water());
        add("radio", "radio", StructureConfig.radioSpawnWeight, LegacyBiome::flat, Type.SPARSE);
        add("factory", "factory", StructureConfig.factorySpawnWeight, b -> b.heightVariation() <= 0.2F && !b.water());
        add("crane", "crane", StructureConfig.craneSpawnWeight, b -> b.heightVariation() <= 0.2F && !b.water());
        add("broadcaster_tower", "broadcaster_tower", StructureConfig.broadcastingTowerSpawnWeight, LegacyBiome::flat, Type.SPARSE);
        add("plane1", "plane1", StructureConfig.plane1SpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water());
        add("plane2", "plane2", StructureConfig.plane2SpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water());
        add("desert_shack_1", "desert_shack_1", StructureConfig.desertShack1SpawnWeight, b -> b.is(Type.SANDY), Type.SANDY);
        add("desert_shack_2", "desert_shack_2", StructureConfig.desertShack2SpawnWeight, b -> b.is(Type.SANDY), Type.SANDY);
        add("desert_shack_3", "desert_shack_3", StructureConfig.desertShack3SpawnWeight, b -> b.is(Type.SANDY), Type.SANDY);
        add("dead_dish_small", "dead_dish_small", StructureConfig.deadDishSmallSpawnWeight, b -> b.is(Type.SANDY), Type.SANDY);
        Predicate<LegacyBiome> ruins = b -> !b.water() && b.lightning();
        int[] ruinWeights = {StructureConfig.ruinsASpawnWeight, StructureConfig.ruinsBSpawnWeight, StructureConfig.ruinsCSpawnWeight,
                StructureConfig.ruinsDSpawnWeight, StructureConfig.ruinsESpawnWeight, StructureConfig.ruinsFSpawnWeight,
                StructureConfig.ruinsGSpawnWeight, StructureConfig.ruinsHSpawnWeight, StructureConfig.ruinsISpawnWeight,
                StructureConfig.ruinsJSpawnWeight};
        for (int i = 0; i < 10; i++) {
            char letter = (char) ('A' + i);
            add("ruin" + Character.toLowerCase(letter), "ruin" + letter, StructureConfig.enableRuins ? ruinWeights[i] : 0, ruins);
        }
        add("tower_base", "tower_base", StructureConfig.towerBaseSpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water() && !b.is(Type.SANDY));
        add("falloutshelter_house", "falloutshelter_house", StructureConfig.falloutShelterHouseSpawnWeight, b -> b.is(Type.PLAINS), Type.PLAINS);
        add("abandoned_arcticstation", "abandoned_arcticstation", StructureConfig.abandonedArcticStationSpawnWeight, b -> b.is(Type.SNOWY), Type.SNOWY);
        add("military_base", "military_base", StructureConfig.militaryBaseSpawnWeight, b -> b.is(Type.SNOWY), Type.SNOWY);
        // 1.7 matched the submarine by identity (biome == ocean || biome == deepOcean): the table's rows for those two
        if (StructureConfig.submarineSpawnWeight > 0)
            ENTRIES.add(new Entry(SUBMARINE, SUBMARINE, StructureConfig.submarineSpawnWeight, null, true, types()));
        add("meteor_dungeon", "meteor_dungeon", 1, b -> b.rootHeight() >= 0); // WG-C: 1.7 left spawnWeight at its default 1

        // 1.7 registerNullWeight: plains only by identity (biome == BiomeGenBase.plains), oceans by type
        if (StructureConfig.plainsNullWeight > 0)
            ENTRIES.add(new Entry(NULL_PLAINS, NULL_PLAINS, StructureConfig.plainsNullWeight, null, false, types()));
        if (StructureConfig.oceanNullWeight > 0)
            ENTRIES.add(new Entry(NULL_OCEAN, NULL_OCEAN, StructureConfig.oceanNullWeight, b -> b.is(Type.OCEAN), false, types(Type.OCEAN)));
    }

    /**
     * The spawn weight of a set entry from the live {@code StructureConfig} (hbm.cfg {@code 5.05}..{@code 5.43}), 0 for an
     * entry the config turns off. Read at generation, not at class initialisation.
     */
    public static int runtimeWeight(String id) {
        boolean ruins = StructureConfig.enableRuins, ocean = StructureConfig.enableOceanStructures;
        return switch (id) {
            case "spire" -> StructureConfig.spireSpawnWeight;
            case "features" -> StructureConfig.featuresSpawnWeight;
            case "bunker" -> StructureConfig.bunkerSpawnWeight;
            case "vertibird" -> StructureConfig.vertibirdSpawnWeight;
            case "crashed_vertibird" -> StructureConfig.vertibirdCrashedSpawnWeight;
            case "beached_patrol" -> StructureConfig.beachedPatrolSpawnWeight;
            case "aircraft_carrier" -> ocean ? StructureConfig.aircraftCarrierSpawnWeight : 0;
            case "oil_rig" -> ocean ? StructureConfig.oilRigSpawnWeight : 0;
            case "lighthouse" -> ocean ? StructureConfig.lighthouseSpawnWeight : 0;
            case "dish" -> StructureConfig.dishSpawnWeight;
            case "water_pump" -> StructureConfig.waterPumpSpawnWeight;
            case "forestchem" -> StructureConfig.forestChemSpawnWeight;
            case "labolatory" -> StructureConfig.laboratorySpawnWeight;
            case "forest_post" -> StructureConfig.forestPostSpawnWeight;
            case "radio" -> StructureConfig.radioSpawnWeight;
            case "factory" -> StructureConfig.factorySpawnWeight;
            case "crane" -> StructureConfig.craneSpawnWeight;
            case "broadcaster_tower" -> StructureConfig.broadcastingTowerSpawnWeight;
            case "plane1" -> StructureConfig.plane1SpawnWeight;
            case "plane2" -> StructureConfig.plane2SpawnWeight;
            case "desert_shack_1" -> StructureConfig.desertShack1SpawnWeight;
            case "desert_shack_2" -> StructureConfig.desertShack2SpawnWeight;
            case "desert_shack_3" -> StructureConfig.desertShack3SpawnWeight;
            case "dead_dish_small" -> StructureConfig.deadDishSmallSpawnWeight;
            case "ruina" -> ruins ? StructureConfig.ruinsASpawnWeight : 0;
            case "ruinb" -> ruins ? StructureConfig.ruinsBSpawnWeight : 0;
            case "ruinc" -> ruins ? StructureConfig.ruinsCSpawnWeight : 0;
            case "ruind" -> ruins ? StructureConfig.ruinsDSpawnWeight : 0;
            case "ruine" -> ruins ? StructureConfig.ruinsESpawnWeight : 0;
            case "ruinf" -> ruins ? StructureConfig.ruinsFSpawnWeight : 0;
            case "ruing" -> ruins ? StructureConfig.ruinsGSpawnWeight : 0;
            case "ruinh" -> ruins ? StructureConfig.ruinsHSpawnWeight : 0;
            case "ruini" -> ruins ? StructureConfig.ruinsISpawnWeight : 0;
            case "ruinj" -> ruins ? StructureConfig.ruinsJSpawnWeight : 0;
            case "tower_base" -> StructureConfig.towerBaseSpawnWeight;
            case "falloutshelter_house" -> StructureConfig.falloutShelterHouseSpawnWeight;
            case "abandoned_arcticstation" -> StructureConfig.abandonedArcticStationSpawnWeight;
            case "military_base" -> StructureConfig.militaryBaseSpawnWeight;
            case SUBMARINE -> StructureConfig.submarineSpawnWeight;
            case "meteor_dungeon" -> 1;
            case NULL_PLAINS -> StructureConfig.plainsNullWeight;
            case NULL_OCEAN -> StructureConfig.oceanNullWeight;
            default -> 0;
        };
    }

    /** The set entries in declaration order (every id the data pack lists). */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** The vanilla biomes a 1.7 predicate accepts, in table order. */
    static List<String> biomes(Entry entry) {
        List<String> out = new ArrayList<>();
        if (entry.id().equals(NULL_PLAINS)) {
            out.add("minecraft:plains");
            return out;
        }
        if (entry.id().equals(SUBMARINE)) {
            // the modern temperature variants are 1.7's ocean and deep ocean (see LEGACY); frozen ones are frozenOcean
            out.addAll(List.of("minecraft:ocean", "minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:cold_ocean",
                    "minecraft:deep_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_cold_ocean"));
            return out;
        }
        LEGACY.forEach((id, legacy) -> {
            if (entry.biome().test(legacy)) out.add(id);
        });
        return out;
    }

    // Biome dictionary

    /** Tag folder of the modern {@code BiomeDictionary}: {@code hbm:biome_dictionary/<type>}. */
    public static final String DICTIONARY = "biome_dictionary/";
    /** The dictionary entry every non-table biome must be in: 1.7 registered these spawns for dimension 0 only. */
    public static final String OVERWORLD = "overworld";

    /**
     * The convention tags that mean each 1.7 type the predicates read (1.7 Forge {@code registerVanillaBiomes}: SANDY is
     * the desert and mesa families, BEACH includes the stone beach, PLAINS includes the savannas, SPARSE is sparse
     * vegetation, WATER is OCEAN + RIVER). 26.1 spellings, plus vanilla's own tag where one exists; the convention ones
     * are optional, so a loader without one loses nothing.
     */
    static final Map<String, List<String>> DICTIONARY_TAGS = new LinkedHashMap<>();

    static {
        DICTIONARY_TAGS.put("ocean", List.of("#minecraft:is_ocean", "#c:is_ocean"));
        DICTIONARY_TAGS.put("river", List.of("#minecraft:is_river", "#c:is_river"));
        DICTIONARY_TAGS.put("beach", List.of("#minecraft:is_beach", "#c:is_beach", "#c:is_stony_shores"));
        DICTIONARY_TAGS.put("plains", List.of("#c:is_plains", "#minecraft:is_savanna", "#c:is_savanna"));
        DICTIONARY_TAGS.put("sandy", List.of("#c:is_sandy", "#c:is_desert", "#minecraft:is_badlands", "#c:is_badlands"));
        DICTIONARY_TAGS.put("swamp", List.of("#c:is_swamp"));
        DICTIONARY_TAGS.put("sparse", List.of("#c:is_sparse_vegetation"));
        DICTIONARY_TAGS.put("snowy", List.of("#c:is_snowy"));
        DICTIONARY_TAGS.put(OVERWORLD, List.of("#minecraft:is_overworld", "#c:is_overworld"));
    }

    /** The dictionary tag path of a type ({@code biome_dictionary/sandy}). */
    public static String dictionary(Type type) {
        return DICTIONARY + type.name().toLowerCase(Locale.ROOT);
    }

    /** The dictionary types the predicates read (the ones {@link #DICTIONARY_TAGS} defines). */
    public static final Set<Type> DICTIONARY_TYPES = types(Type.OCEAN, Type.RIVER, Type.BEACH, Type.PLAINS, Type.SANDY, Type.SWAMP, Type.SPARSE, Type.SNOWY);

    /** The table row of a vanilla biome; null for a biome the table has no row for (modded ones). */
    public static LegacyBiome legacy(String biomeId) {
        return LEGACY.get(biomeId);
    }

    /**
     * The 1.7 biome predicate of a spawn; null when the spawn has none to run at generation (the plains null weight and
     * the submarine, which 1.7 matched by identity and whose tags list exactly those biomes) or is not in the set.
     */
    public static Predicate<LegacyBiome> predicate(String spawn) {
        for (Entry entry : ENTRIES) {
            if (entry.spawn().equals(spawn)) return entry.biome();
        }
        return null;
    }

    /** The has_structure superset for biomes the table does not know: the required types' dictionary tags, or the overworld. */
    static List<String> moddedCandidates(Entry entry) {
        List<String> out = new ArrayList<>();
        if (entry.biome() == null) return out; // by identity: the vanilla biomes only
        if (entry.requires().isEmpty()) {
            out.add("#" + NS + ":" + DICTIONARY + OVERWORLD);
        } else {
            for (Type type : entry.requires()) out.add("#" + NS + ":" + dictionary(type));
        }
        return out;
    }

    /** One tag entry; {@code optional} ones may name something absent. */
    static JsonElement tagEntry(String id, boolean optional) {
        if (!optional) return new JsonPrimitive(id);
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id);
        entry.addProperty("required", false);
        return entry;
    }

    /** {@code hbm:biome_dictionary/<type>}: the tags of {@link #DICTIONARY_TAGS}. */
    static void declareDictionary() {
        DICTIONARY_TAGS.forEach((type, tags) -> {
            JsonArray values = new JsonArray();
            for (String tag : tags) values.add(tagEntry(tag, !tag.startsWith("#minecraft:")));
            JsonObject json = new JsonObject();
            json.addProperty("replace", false);
            json.add("values", values);
            Data.declare("data/" + NS + "/tags/worldgen/biome/" + DICTIONARY + type + ".json", json);
        });
    }

    // Declaration

    public static void declare() {
        if (declared) return;
        declared = true;

        declareDictionary();

        JsonArray setEntries = new JsonArray();
        JsonArray real = new JsonArray();

        for (Entry entry : ENTRIES) {
            List<String> biomes = biomes(entry);
            if (biomes.isEmpty()) throw new IllegalStateException("structure " + entry.id() + " accepts no biome");

            JsonArray values = new JsonArray();
            for (String biome : biomes) values.add(tagEntry(biome, OPTIONAL.contains(biome)));
            // every other biome of the kind; GenStructure runs the predicate on the biome it finds (table biomes by row)
            for (String candidate : moddedCandidates(entry)) values.add(candidate);
            JsonObject tag = new JsonObject();
            tag.addProperty("replace", false);
            tag.add("values", values);
            Data.declare("data/" + NS + "/tags/worldgen/biome/has_structure/" + entry.id() + ".json", tag);

            JsonObject structure = new JsonObject();
            structure.addProperty("type", NS + ":nbt");
            structure.addProperty("biomes", "#" + NS + ":has_structure/" + entry.id());
            structure.add("spawn_overrides", new JsonObject());
            // 1.7 built these in PopulateChunkEvent.Pre, before trees and ores: the surface structure step
            structure.addProperty("step", "surface_structures");
            // 1.7 neither dug nor filled around them: height offsets sink them, ruins follow each column
            structure.addProperty("terrain_adaptation", "none");
            structure.addProperty("spawn", entry.spawn());
            Data.declare("data/" + NS + "/worldgen/structure/" + entry.id() + ".json", structure);

            JsonObject member = new JsonObject();
            member.addProperty("structure", NS + ":" + entry.id());
            member.addProperty("weight", entry.weight());
            setEntries.add(member);
            if (entry.real()) real.add(NS + ":" + entry.id());
        }

        // hbm:config_spread "ntm": random_spread with spacing 5.02 / separation 5.01 read from hbm.cfg at generation
        JsonObject placement = new JsonObject();
        placement.addProperty("type", NS + ":config_spread");
        placement.addProperty("salt", SALT);
        placement.addProperty("key", "ntm");
        JsonObject set = new JsonObject();
        set.add("structures", setEntries);
        set.add("placement", placement);
        Data.declare("data/" + NS + "/worldgen/structure_set/ntm_structures.json", set);

        JsonObject structureTag = new JsonObject();
        structureTag.addProperty("replace", false);
        structureTag.add("values", real);
        Data.declare("data/" + NS + "/tags/worldgen/structure/ntm_structures.json", structureTag);
    }
}
