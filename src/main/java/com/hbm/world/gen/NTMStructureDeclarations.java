package com.hbm.world.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.config.StructureConfig;
import dev.golem.core.dejson.Data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The data half of 1.7's {@code NTMWorldGenerator}, declared through Dejson (plain JVM, no Minecraft class):
 * <ul>
 *   <li>{@code data/hbm/worldgen/structure/<id>.json}: one {@code hbm:nbt} structure per spawn;</li>
 *   <li>{@code data/hbm/tags/worldgen/biome/has_structure/<id>.json}: the biomes its 1.7 biome predicate accepts;</li>
 *   <li>{@code data/hbm/worldgen/structure_set/ntm_structures.json}: all of them with their 1.7 weights, placed by
 *       {@code random_spread} spacing {@code structureMaxChunks} (16), separation {@code structureMinChunks} (4), salt
 *       996996996, which is exactly 1.7's region pick (see {@code NBTStructure.GenStructure});</li>
 *   <li>{@code data/hbm/tags/worldgen/structure/ntm_structures.json}: every real (non-null) one, for
 *       {@code /locate structure #hbm:ntm_structures} and maps.</li>
 * </ul>
 *
 * <p><b>Biomes.</b> 1.7 tested each biome's {@code BiomeDictionary} types, {@code heightVariation}, {@code rootHeight}
 * and {@code canSpawnLightningBolt()}. Modern biomes have none of those, so {@link #LEGACY} gives every vanilla
 * overworld biome the values of its 1.7 counterpart (read from the 1.7.10 sources: {@code BiomeGenBase} heights,
 * mutations' {@code createMutation}, Forge's {@code registerVanillaBiomes}), and the 1.7 predicates run over that table
 * at build time. Biomes 1.7 did not have take their nearest 1.7 analog (noted per row); the three cave biomes have none
 * and never host a surface structure. Modded biomes are not covered (1.7's {@code makeBestGuess} has no data to run on).
 *
 * <p>Folder names are the 26.1 ones; the build's legacy pass renames what differs per era (none of these do).
 */
public final class NTMStructureDeclarations {

    public static final String NS = "hbm";
    public static final String NULL_PLAINS = "null_plains";
    public static final String NULL_OCEAN = "null_ocean";

    /** 1.7 {@code StructureConfig}: region salt, {@code 996996996 - dimension} for the overworld. */
    public static final int SALT = 996996996;

    private static boolean declared;

    private NTMStructureDeclarations() {}

    // ------------------------------------------------------------------------------------------------ 1.7 biome model

    /** The 1.7 {@code BiomeDictionary.Type}s the generator reads. */
    enum Type { OCEAN, RIVER, PLAINS, SANDY, BEACH, SWAMP, SPARSE, MUSHROOM, SNOWY, FOREST, MOUNTAIN, HILLS, MESA, COLD, HOT, JUNGLE }

    /** A 1.7 biome as the generator saw it: {@code rootHeight}, {@code heightVariation}, types, lightning (rain, no snow). */
    record LegacyBiome(float rootHeight, float heightVariation, Set<Type> types, boolean lightning) {
        boolean is(Type type) { return types.contains(type); }

        /** 1.7 {@code isWaterBiome}: {@code Type.WATER} is OCEAN or RIVER. */
        boolean water() { return is(Type.OCEAN) || is(Type.RIVER); }

        /** 1.7 {@code isFlatBiome}: little height variation, sparse vegetation, not water. */
        boolean flat() { return heightVariation <= 0.2F && !water() && is(Type.SPARSE); }
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
        // meadow: no 1.7 biome; a grassy upland flat -> plains types at MidPlains height
        LEGACY.put("minecraft:meadow", b(MID_PLAINS, true, P));
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
        LEGACY.put("minecraft:grove", b(LOW_HILLS, false, C, SN, F, H));                       // no 1.7 biome: coldTaigaHills
        LEGACY.put("minecraft:savanna", b(LOW_PLAINS, false, HO, P, SP));
        LEGACY.put("minecraft:savanna_plateau", b(HIGH_PLATEAUS, false, HO, P, SP));
        LEGACY.put("minecraft:windswept_savanna", b(0.3625F, 1.225F, false, HO));               // savanna M (root*.5+.3, var*.5+1.2)
        LEGACY.put("minecraft:windswept_hills", b(MID_HILLS, true, MO, H));                     // extremeHills
        LEGACY.put("minecraft:windswept_gravelly_hills", b(MID_HILLS, true, MO, H));            // extremeHills M, same height
        LEGACY.put("minecraft:windswept_forest", b(MID_HILLS, true, MO, F, SP));                // extremeHillsPlus
        LEGACY.put("minecraft:stony_peaks", b(MID_HILLS, true, MO, H));                         // no 1.7 biome: extremeHills
        LEGACY.put("minecraft:jagged_peaks", b(MID_HILLS, false, C, SN, MO));                   // no 1.7 biome: snowy mountain
        LEGACY.put("minecraft:frozen_peaks", b(MID_HILLS, false, C, SN, MO));                   // no 1.7 biome: snowy mountain
        LEGACY.put("minecraft:snowy_slopes", b(LOW_HILLS, false, C, SN, MO));                   // iceMountains
        LEGACY.put("minecraft:cherry_grove", b(LOW_HILLS, true, F, H));                         // no 1.7 biome: forestHills
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

    // ------------------------------------------------------------------------------------------------ the structures

    /** One entry of the structure set: resource id, 1.7 spawn name, weight, 1.7 biome predicate. */
    public record Entry(String id, String spawn, int weight, Predicate<LegacyBiome> biome, boolean real) {}

    static final List<Entry> ENTRIES = new ArrayList<>();

    private static void add(String id, String spawn, int weight, Predicate<LegacyBiome> biome) {
        if (weight <= 0) return; // 1.7: weight 0 (a disabled toggle) never wins a pick
        ENTRIES.add(new Entry(id, spawn, weight, biome, true));
    }

    static {
        add("spire", "spire", StructureConfig.spireSpawnWeight, b -> b.heightVariation() <= 0.05F && !b.water());
        add("features", "features", StructureConfig.featuresSpawnWeight, b -> !b.water()); // WG-C: MapGenNTMFeatures.start
        add("bunker", "bunker", StructureConfig.bunkerSpawnWeight, b -> !b.water()); // WG-C: BunkerComponents.BunkerStart
        add("vertibird", "vertibird", StructureConfig.vertibirdSpawnWeight, b -> !b.water() && b.is(Type.SANDY));
        add("crashed_vertibird", "crashed_vertibird", StructureConfig.vertibirdCrashedSpawnWeight, b -> !b.water() && b.is(Type.SANDY));
        add("beached_patrol", "beached_patrol", StructureConfig.beachedPatrolSpawnWeight, b -> b.is(Type.BEACH));
        add("aircraft_carrier", "aircraft_carrier", StructureConfig.enableOceanStructures ? StructureConfig.aircraftCarrierSpawnWeight : 0, b -> b.is(Type.OCEAN));
        add("oil_rig", "oil_rig", StructureConfig.enableOceanStructures ? StructureConfig.oilRigSpawnWeight : 0, b -> b.is(Type.OCEAN) && b.rootHeight() >= -1.5F);
        add("lighthouse", "lighthouse", StructureConfig.enableOceanStructures ? StructureConfig.lighthouseSpawnWeight : 0, b -> b.is(Type.OCEAN) || b.is(Type.BEACH));
        add("dish", "dish", StructureConfig.dishSpawnWeight, b -> b.is(Type.PLAINS));
        add("water_pump", "water_pump", StructureConfig.waterPumpSpawnWeight, b -> b.is(Type.PLAINS) || b.is(Type.SWAMP));
        add("forestchem", "forestchem", StructureConfig.forestChemSpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water());
        add("labolatory", "labolatory", StructureConfig.laboratorySpawnWeight, LegacyBiome::flat);
        add("forest_post", "forest_post", StructureConfig.forestPostSpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water());
        add("radio", "radio", StructureConfig.radioSpawnWeight, LegacyBiome::flat);
        add("factory", "factory", StructureConfig.factorySpawnWeight, b -> b.heightVariation() <= 0.2F && !b.water());
        add("crane", "crane", StructureConfig.craneSpawnWeight, b -> b.heightVariation() <= 0.2F && !b.water());
        add("broadcaster_tower", "broadcaster_tower", StructureConfig.broadcastingTowerSpawnWeight, LegacyBiome::flat);
        add("plane1", "plane1", StructureConfig.plane1SpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water());
        add("plane2", "plane2", StructureConfig.plane2SpawnWeight, b -> b.heightVariation() <= 0.3F && !b.water());
        add("desert_shack_1", "desert_shack_1", StructureConfig.desertShack1SpawnWeight, b -> b.is(Type.SANDY));
        add("desert_shack_2", "desert_shack_2", StructureConfig.desertShack2SpawnWeight, b -> b.is(Type.SANDY));
        add("desert_shack_3", "desert_shack_3", StructureConfig.desertShack3SpawnWeight, b -> b.is(Type.SANDY));
        add("dead_dish_small", "dead_dish_small", StructureConfig.deadDishSmallSpawnWeight, b -> b.is(Type.SANDY));
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
        add("meteor_dungeon", "meteor_dungeon", 1, b -> b.rootHeight() >= 0); // WG-C: 1.7 left spawnWeight at its default 1

        // 1.7 registerNullWeight: plains only by identity (biome == BiomeGenBase.plains), oceans by type
        if (StructureConfig.plainsNullWeight > 0)
            ENTRIES.add(new Entry(NULL_PLAINS, NULL_PLAINS, StructureConfig.plainsNullWeight, null, false));
        if (StructureConfig.oceanNullWeight > 0)
            ENTRIES.add(new Entry(NULL_OCEAN, NULL_OCEAN, StructureConfig.oceanNullWeight, b -> b.is(Type.OCEAN), false));
    }

    /** The modern biomes a 1.7 predicate accepts, in table order. */
    static List<String> biomes(Entry entry) {
        List<String> out = new ArrayList<>();
        if (entry.id().equals(NULL_PLAINS)) {
            out.add("minecraft:plains");
            return out;
        }
        LEGACY.forEach((id, legacy) -> {
            if (entry.biome().test(legacy)) out.add(id);
        });
        return out;
    }

    // ------------------------------------------------------------------------------------------------ declaration

    public static void declare() {
        if (declared) return;
        declared = true;

        JsonArray setEntries = new JsonArray();
        JsonArray real = new JsonArray();

        for (Entry entry : ENTRIES) {
            List<String> biomes = biomes(entry);
            if (biomes.isEmpty()) throw new IllegalStateException("structure " + entry.id() + " accepts no biome");

            JsonArray values = new JsonArray();
            for (String biome : biomes) {
                if (OPTIONAL.contains(biome)) {
                    JsonObject optional = new JsonObject();
                    optional.addProperty("id", biome);
                    optional.addProperty("required", false);
                    values.add(optional);
                } else {
                    values.add(biome);
                }
            }
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

        JsonObject placement = new JsonObject();
        placement.addProperty("type", "minecraft:random_spread");
        placement.addProperty("salt", SALT);
        placement.addProperty("spacing", StructureConfig.structureMaxChunks);
        placement.addProperty("separation", StructureConfig.structureMinChunks);
        placement.addProperty("spread_type", "linear");
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
