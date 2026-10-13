package com.hbm.world.gen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.config.DungeonConfig;
import dev.golem.core.dejson.Data;
import dev.golem.core.dejson.Features;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static dev.golem.core.dejson.BiomeModifiers.VEGETAL_DECORATION;
import static dev.golem.core.dejson.BiomeModifiers.addFeatures;
import static dev.golem.core.dejson.Features.biome;
import static dev.golem.core.dejson.Features.heightmap;
import static dev.golem.core.dejson.Features.inSquare;
import static dev.golem.core.dejson.Features.rarity;

/**
 * The data half of 1.7 {@code HbmWorldGen.generateSurface}'s legacy dungeons and scattered world blocks, declared
 * through Dejson (plain JVM, no Minecraft class).
 *
 * <p><b>Structures</b> ({@link LegacyDungeonStructure}): per dungeon {@code worldgen/structure/<id>.json}
 * ({@code hbm:legacy_dungeon}), {@code worldgen/structure_set/<id>.json}, the biome tag
 * {@code hbm:has_structure/<id>}, and all of them in the structure tag {@code #hbm:legacy_dungeons}.
 *
 * <p><b>The 1/n chance</b> (read from {@code hbm.cfg} at generation by {@code hbm:config_spread}). 1.7 rolled
 * {@code rand.nextInt(n) == 0} independently in every chunk. A modern structure set offers one candidate chunk per
 * {@code spacing x spacing} region ({@code random_spread}), kept with probability {@code frequency}. With
 * {@code spacing = floor(sqrt(n))}, {@code separation} 0 (the candidate may be any chunk of the region, so
 * neighbours are possible, as in 1.7) and {@code frequency = spacing^2 / n}, every chunk is a site with probability
 * exactly {@code 1/n} (the antenna, n 250: spacing 15, frequency 0.9; the library, n 64: spacing 8, frequency 1).
 * Modern placement cannot give 1.7's independence: two sites never share a region. The biome test runs on the
 * candidate (biome tag), as 1.7's ran before its roll, so the per-chunk rate inside the right biomes is 1/n in both.
 *
 * <p><b>Biomes.</b> 1.7 tested {@code biome.temperature}, {@code biome.rainfall} and {@code canSpawnLightningBolt()}
 * (rain that does not fall as snow). {@link #CLIMATE} gives every vanilla overworld biome its 1.7 counterpart's values
 * (1.7 {@code BiomeGenBase}; modern vanilla kept them except the savannas, which 1.18 moved from 1.2/1.0/1.1 to 2.0),
 * and biomes 1.7 did not have their modern values; the predicates run over that table at build time. 1.7 asked a
 * modded biome the same three questions, so each climate tag also lists {@code #hbm:biome_dictionary/overworld} (see
 * {@code NTMStructureDeclarations}) and {@link LegacyDungeonStructure} runs the rule on the biome at the site: a table
 * biome through its row, any other through its own temperature, rain and downfall ({@code LegacyClimate}). The jungle
 * dungeon keeps 1.7's identity test (three vanilla jungles); the unrestricted ones take every overworld biome.
 *
 * <p><b>Scattered blocks</b> ({@link com.hbm.world.feature.DungeonBlockFeature}): one configured and placed feature
 * each ({@code rarity_filter} = the 1.7 chance, {@code in_square}, {@code heightmap MOTION_BLOCKING} = 1.7's
 * {@code getHeightValue}, {@code biome}), added to the overworld (the capsule to 1.7's {@code BiomeGenBase.beach} only)
 * at {@code vegetal_decoration}, after vanilla's trees and plants, as 1.7 ran after the biome decorator. Features a
 * config switch turns off (the 528 HE mines) are declared for {@code /place feature} but not added to any biome.
 */
public final class LegacyDungeonDeclarations {

    private static final String NS = "hbm";

    private static boolean declared;

    private LegacyDungeonDeclarations() {}

    /** A 1.7 biome's climate: {@code temperature}, {@code rainfall}, {@code canSpawnLightningBolt()}. */
    public record Climate(float temperature, float rainfall, boolean lightning) {}

    static final Map<String, Climate> CLIMATE = new LinkedHashMap<>();
    static final Set<String> OPTIONAL = Set.of("minecraft:pale_garden");

    private static void c(String id, float temperature, float rainfall, boolean lightning) {
        CLIMATE.put("minecraft:" + id, new Climate(temperature, rainfall, lightning));
    }

    static {
        c("plains", 0.8F, 0.4F, true);
        c("sunflower_plains", 0.8F, 0.4F, true);
        c("meadow", 0.5F, 0.8F, true);                // no 1.7 biome: modern values
        c("snowy_plains", 0.0F, 0.5F, false);         // icePlains
        c("ice_spikes", 0.0F, 0.5F, false);
        c("desert", 2.0F, 0.0F, false);
        c("swamp", 0.8F, 0.9F, true);
        c("mangrove_swamp", 0.8F, 0.9F, true);        // no 1.7 biome: modern values
        c("forest", 0.7F, 0.8F, true);
        c("flower_forest", 0.7F, 0.8F, true);
        c("birch_forest", 0.6F, 0.6F, true);
        c("old_growth_birch_forest", 0.6F, 0.6F, true);
        c("dark_forest", 0.7F, 0.8F, true);           // roofedForest
        c("pale_garden", 0.7F, 0.8F, true);           // no 1.7 biome: modern values
        c("taiga", 0.25F, 0.8F, true);
        c("snowy_taiga", -0.5F, 0.4F, false);         // coldTaiga
        c("old_growth_pine_taiga", 0.3F, 0.8F, true); // megaTaiga
        c("old_growth_spruce_taiga", 0.25F, 0.8F, true);
        c("grove", -0.2F, 0.8F, false);               // no 1.7 biome: modern values
        c("savanna", 1.2F, 0.0F, false);              // 1.7 value (modern 2.0)
        c("savanna_plateau", 1.0F, 0.0F, false);      // 1.7 value (modern 2.0)
        c("windswept_savanna", 1.1F, 0.0F, false);    // savanna M, 1.7 value (modern 2.0)
        c("windswept_hills", 0.2F, 0.3F, true);       // extremeHills
        c("windswept_gravelly_hills", 0.2F, 0.3F, true);
        c("windswept_forest", 0.2F, 0.3F, true);      // extremeHillsPlus
        c("stony_peaks", 1.0F, 0.3F, true);           // no 1.7 biome: modern values
        c("jagged_peaks", -0.7F, 0.9F, false);        // no 1.7 biome: modern values
        c("frozen_peaks", -0.7F, 0.9F, false);        // no 1.7 biome: modern values
        c("snowy_slopes", -0.3F, 0.9F, false);        // no 1.7 biome: modern values
        c("cherry_grove", 0.5F, 0.8F, true);          // no 1.7 biome: modern values
        c("jungle", 0.95F, 0.9F, true);
        c("bamboo_jungle", 0.95F, 0.9F, true);        // no 1.7 biome: modern values
        c("sparse_jungle", 0.95F, 0.8F, true);        // jungleEdge
        c("badlands", 2.0F, 0.0F, false);             // mesa
        c("eroded_badlands", 2.0F, 0.0F, false);
        c("wooded_badlands", 2.0F, 0.0F, false);      // mesaPlateau_F
        c("river", 0.5F, 0.5F, true);
        c("frozen_river", 0.0F, 0.5F, false);
        c("beach", 0.8F, 0.4F, true);
        c("snowy_beach", 0.05F, 0.3F, false);         // coldBeach
        c("stony_shore", 0.2F, 0.3F, true);           // stoneBeach
        c("ocean", 0.5F, 0.5F, true);
        c("warm_ocean", 0.5F, 0.5F, true);
        c("lukewarm_ocean", 0.5F, 0.5F, true);
        c("cold_ocean", 0.5F, 0.5F, true);
        c("frozen_ocean", 0.0F, 0.5F, false);
        c("deep_ocean", 0.5F, 0.5F, true);
        c("deep_lukewarm_ocean", 0.5F, 0.5F, true);
        c("deep_cold_ocean", 0.5F, 0.5F, true);
        c("deep_frozen_ocean", 0.0F, 0.5F, false);    // no 1.7 biome: frozenOcean (modern base 0.5 with the frozen modifier)
        c("mushroom_fields", 0.9F, 1.0F, true);
    }

    /** One legacy dungeon: structure id, 1.7 chance, biome rule (null = every overworld biome), salt. */
    record Dungeon(LegacyDungeonStructure.Kind kind, int chance, Predicate<Climate> biome, int salt) {}

    static final List<Dungeon> DUNGEONS = new ArrayList<>();

    static {
        // HbmWorldGen.generateSurface, in 1.7's order
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.ANTENNA, DungeonConfig.antennaStructure,
                b -> b.temperature() >= 0.4F && b.rainfall() <= 0.6F, 1_700_001));
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.DESERT_ATOM, DungeonConfig.atomStructure,
                b -> !b.lightning() && b.temperature() >= 1.5F, 1_700_002));
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.LIBRARY_DUNGEON, DungeonConfig.dungeonStructure, null, 1_700_003));
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.DUD, DungeonConfig.dudStructure, null, 1_700_004));
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.SPACESHIP, DungeonConfig.spaceshipStructure, null, 1_700_005));
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.BARREL, DungeonConfig.barrelStructure,
                b -> b.temperature() >= 1.5F && !b.lightning(), 1_700_006));
        // 1.7 jungle, jungleEdge, jungleHills by identity (no Jungle M); bamboo jungle stands in for the hills
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.JUNGLE_DUNGEON, DungeonConfig.jungleStructure, null, 1_700_007));
        // ArcticVault.trySpawn: getFloatTemperature(x, y <= 47, z) < 0.2, i.e. the biome's own temperature
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.ARCTIC_VAULT, DungeonConfig.arcticStructure,
                b -> b.temperature() < 0.2F, 1_700_008));
        DUNGEONS.add(new Dungeon(LegacyDungeonStructure.Kind.ANCIENT_TOMB, DungeonConfig.pyramidStructure,
                b -> b.temperature() >= 2.0F && !b.lightning(), 1_700_009));
    }

    /** {@code floor(sqrt(n))}: the region side giving one candidate per about n chunks. */
    static int spacing(int chance) {
        return Math.max(1, (int) Math.floor(Math.sqrt(chance)));
    }

    /** {@code spacing^2 / n}: the candidate's keep probability that makes the per-chunk chance exactly 1/n. */
    static double frequency(int chance) {
        // a double, not a float: Gson writes a Float as its short decimal (0.9) but compares it by its widened
        // double (0.8999999761581421), so a float here made the packed file differ from its own declaration
        // (dejson_data_parity). Vanilla reads the field as a float either way.
        int s = spacing(chance);
        return Math.min(1.0D, (double) (s * s) / (double) chance);
    }

    /** The table row of a vanilla biome; null for one the table has no row for (modded ones). */
    public static Climate climate(String biomeId) {
        return CLIMATE.get(biomeId);
    }

    /** The 1.7 climate rule of a dungeon; null when it has none to run (every overworld biome; the jungles by identity). */
    public static Predicate<Climate> rule(LegacyDungeonStructure.Kind kind) {
        for (Dungeon dungeon : DUNGEONS) {
            if (dungeon.kind() == kind) return dungeon.biome();
        }
        return null;
    }

    private static final String OVERWORLD = "#" + NTMStructureDeclarations.NS + ":" + NTMStructureDeclarations.DICTIONARY
            + NTMStructureDeclarations.OVERWORLD;

    static List<String> biomes(Dungeon dungeon) {
        List<String> out = new ArrayList<>();
        if (dungeon.kind() == LegacyDungeonStructure.Kind.JUNGLE_DUNGEON) {
            out.add("minecraft:jungle");
            out.add("minecraft:sparse_jungle");
            out.add("minecraft:bamboo_jungle");
            return out;
        }
        if (dungeon.biome() == null) {
            out.add(OVERWORLD); // 1.7: every biome of dimension 0, modded ones included
            return out;
        }
        CLIMATE.forEach((id, climate) -> {
            if (dungeon.biome().test(climate)) out.add(id);
        });
        out.add(OVERWORLD); // the rest; LegacyDungeonStructure runs the rule on the biome it finds
        return out;
    }

    // Declarations

    public static void declare() {
        if (declared) return;
        declared = true;

        JsonArray all = new JsonArray();
        for (Dungeon dungeon : DUNGEONS) {
            if (dungeon.chance() <= 0) continue; // 1.7: 0 turns the spawn off
            String id = dungeon.kind().id;

            List<String> biomes = biomes(dungeon);
            if (biomes.isEmpty()) throw new IllegalStateException("legacy dungeon " + id + " accepts no biome");
            String biomeRef;
            if (biomes.size() == 1 && biomes.get(0).startsWith("#")) {
                biomeRef = biomes.get(0);
            } else {
                JsonArray values = new JsonArray();
                for (String biome : biomes) values.add(NTMStructureDeclarations.tagEntry(biome, OPTIONAL.contains(biome)));
                JsonObject tag = new JsonObject();
                tag.addProperty("replace", false);
                tag.add("values", values);
                Data.declare("data/" + NS + "/tags/worldgen/biome/has_structure/" + id + ".json", tag);
                biomeRef = "#" + NS + ":has_structure/" + id;
            }

            JsonObject structure = new JsonObject();
            structure.addProperty("type", NS + ":legacy_dungeon");
            structure.addProperty("biomes", biomeRef);
            structure.add("spawn_overrides", new JsonObject());
            // 1.7 HbmWorldGen ran after the biome decorator
            structure.addProperty("step", "top_layer_modification");
            structure.addProperty("terrain_adaptation", "none");
            structure.addProperty("dungeon", id);
            Data.declare("data/" + NS + "/worldgen/structure/" + id + ".json", structure);

            // hbm:config_spread "dungeon:<id>": spacing(n) / separation 0 / keep chance frequency(n), n read from
            // hbm.cfg (4.xx_*Spawn) at generation
            JsonObject placement = new JsonObject();
            placement.addProperty("type", NS + ":config_spread");
            placement.addProperty("salt", dungeon.salt());
            placement.addProperty("key", "dungeon:" + id);
            JsonObject member = new JsonObject();
            member.addProperty("structure", NS + ":" + id);
            member.addProperty("weight", 1);
            JsonArray members = new JsonArray();
            members.add(member);
            JsonObject set = new JsonObject();
            set.add("structures", members);
            set.add("placement", placement);
            Data.declare("data/" + NS + "/worldgen/structure_set/" + id + ".json", set);

            all.add(NS + ":" + id);
        }
        JsonObject structureTag = new JsonObject();
        structureTag.addProperty("replace", false);
        structureTag.add("values", all);
        Data.declare("data/" + NS + "/tags/worldgen/structure/legacy_dungeons.json", structureTag);

        features();
    }

    /** The scattered world blocks. */
    private static void features() {
        JsonObject[] onSurface = {inSquare(), heightmap("MOTION_BLOCKING"), biome()};
        List<String> overworld = new ArrayList<>();

        // every feature is listed; hbm:config reads the hbm.cfg switch and chance when the chunk generates
        overworld.add(gated("locked_safe", onSurface, ModBiomeModifiers.cfgSwitch(WorldgenKeys.VAULTS),
                ModBiomeModifiers.cfgChance(WorldgenKeys.VAULT)));
        overworld.add(gated("landmine_ap", onSurface, ModBiomeModifiers.cfgSwitch(WorldgenKeys.MINES),
                ModBiomeModifiers.cfgChance(WorldgenKeys.LANDMINE)));
        overworld.add(gated("broken_lantern", onSurface, rarity(Math.max(1, DungeonConfig.lanternStructure))));
        overworld.add(gated("landmine_he", onSurface, ModBiomeModifiers.cfgSwitch(WorldgenKeys.BOSNIA), rarity(16)));
        overworld.add(gated("corrupted_broadcaster", onSurface, ModBiomeModifiers.cfgChance(WorldgenKeys.BROADCASTER)));
        // 1.7 genBlueprintChest ran in every chunk; the grid test is the feature's (origin: the chunk corner)
        Features.configured(NS, "blueprint_chest", NS + ":blueprint_chest", null);
        Features.placed(NS, "blueprint_chest", NS + ":blueprint_chest", biome());
        overworld.add(NS + ":blueprint_chest");
        addFeatures(NS, "legacy_dungeon_blocks", "#minecraft:is_overworld", overworld, VEGETAL_DECORATION);

        // 1.7: biome == BiomeGenBase.beach (not the cold or stone beach)
        String capsule = gated("landed_capsule", onSurface, ModBiomeModifiers.cfgChance(WorldgenKeys.CAPSULE));
        addFeatures(NS, "landed_capsule", "minecraft:beach", List.of(capsule), VEGETAL_DECORATION);
    }

    private static String gated(String name, JsonObject[] onSurface, JsonObject... gates) {
        Features.configured(NS, name, NS + ":" + name, null);
        JsonObject[] placement = new JsonObject[gates.length + onSurface.length];
        System.arraycopy(gates, 0, placement, 0, gates.length);
        System.arraycopy(onSurface, 0, placement, gates.length, onSurface.length);
        Features.placed(NS, name, NS + ":" + name, placement);
        return NS + ":" + name;
    }
}
