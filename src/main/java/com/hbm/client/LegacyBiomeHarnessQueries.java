package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.world.gen.LegacyBiomes;
import com.hbm.world.gen.LegacyDungeonStructure;
import com.hbm.world.gen.NTMStructureDeclarations;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.terrain.LegacyClimate;
import dev.golem.core.Ids;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Harness queries for the 1.7 biome model on modern and modded biomes (NTMStructureDeclarations, LegacyBiomes). All read
 * the chunk generator's noise only; nothing is loaded or generated.
 * <ul>
 *   <li>{@code hbm_legacy_biome mode=calibrate x0 z0 radius step}: over a grid of start chunks, every vanilla biome's
 *       table {@code heightVariation}/{@code rootHeight} against the terrain {@link LegacyBiomes#terrain} reads there,
 *       and how often the height predicates' thresholds agree for several relief scales.</li>
 *   <li>{@code hbm_modded_biome biome x z spawns dungeons}: a biome (any registered one) as it would be found by a start
 *       at chunk {@code x, z} of this world's terrain: its has_structure tag membership per spawn, the 1.7 predicate's
 *       answer ({@link LegacyBiomes#guess} for a non-table biome), and the structure's own start run with that biome
 *       (a {@link FixedBiomeSource}); per legacy dungeon kind the same with its climate rule.</li>
 * </ul>
 */
public final class LegacyBiomeHarnessQueries {

    private LegacyBiomeHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_legacy_biome", (mc, p) -> ask(mc, "hbm_legacy_biome", p));
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_modded_biome", (mc, p) -> ask(mc, "hbm_modded_biome", p));
    }

    private static JsonObject ask(Minecraft mc, String kind, JsonObject params) {
        return ServerQueries.answer(mc, kind, params.deepCopy(), (level, p) -> {
            try {
                return kind.equals("hbm_legacy_biome") ? calibrate(level, p) : modded(level, p);
            } catch (RuntimeException e) {
                JsonObject error = new JsonObject();
                error.addProperty("error", String.valueOf(e));
                return error;
            }
        });
    }

    private static int i(JsonObject p, String key, int fallback) {
        return p.has(key) ? p.get(key).getAsInt() : fallback;
    }

    private static final int[] SCALES = {12, 16, 20, 24, 28, 32, 40, 48};
    private static final float[] THRESHOLDS = {0.05F, 0.2F, 0.25F, 0.3F};

    private static JsonObject calibrate(ServerLevel level, JsonObject p) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int x0 = i(p, "x0", 0) >> 4, z0 = i(p, "z0", 0) >> 4, radius = i(p, "radius", 32), step = Math.max(1, i(p, "step", 4));
        Map<String, List<int[]>> byBiome = new TreeMap<>(); // relief, average
        int[][] agree = new int[SCALES.length][2];
        int[] rootAgree = new int[2];
        for (int cx = x0 - radius; cx <= x0 + radius; cx += step) {
            for (int cz = z0 - radius; cz <= z0 + radius; cz += step) {
                int x = (cx << 4) + 8, z = (cz << 4) + 8;
                int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
                Holder<Biome> biome = LegacyBiomes.biomeAt(generator.getBiomeSource(), random, x, surface, z);
                String id = biome.unwrapKey().map(k -> Ids.keyId(k).toString()).orElse("?");
                NTMStructureDeclarations.LegacyBiome row = NTMStructureDeclarations.legacy(id);
                if (row == null) continue;
                LegacyBiomes.Terrain terrain = LegacyBiomes.terrain(generator, level, random, cx << 4, cz << 4);
                int relief = terrain.max() - terrain.min();
                byBiome.computeIfAbsent(id, k -> new ArrayList<>()).add(new int[] {relief, terrain.average()});
                rootAgree[0]++;
                if ((terrain.rootHeight() >= 0) == (row.rootHeight() >= 0)) rootAgree[1]++;
                if (row.water()) continue; // every height predicate also requires "not water"
                for (int s = 0; s < SCALES.length; s++) {
                    float variation = Math.max(0.0F, (relief / (float) SCALES[s] - 0.1F) / 0.9F);
                    for (float t : THRESHOLDS) {
                        agree[s][0]++;
                        if ((variation <= t) == (row.heightVariation() <= t)) agree[s][1]++;
                    }
                }
            }
        }
        JsonObject json = new JsonObject();
        JsonObject biomes = new JsonObject();
        byBiome.forEach((id, samples) -> {
            NTMStructureDeclarations.LegacyBiome row = NTMStructureDeclarations.legacy(id);
            int[] reliefs = samples.stream().mapToInt(a -> a[0]).sorted().toArray();
            int[] averages = samples.stream().mapToInt(a -> a[1]).sorted().toArray();
            JsonObject b = new JsonObject();
            b.addProperty("n", reliefs.length);
            b.addProperty("hv", row.heightVariation());
            b.addProperty("root", row.rootHeight());
            b.addProperty("relief_p25", reliefs[reliefs.length / 4]);
            b.addProperty("relief_p50", reliefs[reliefs.length / 2]);
            b.addProperty("relief_p75", reliefs[reliefs.length * 3 / 4]);
            b.addProperty("relief_p90", reliefs[reliefs.length * 9 / 10]);
            b.addProperty("avg_p50", averages[averages.length / 2]);
            biomes.add(id, b);
        });
        json.add("biomes", biomes);
        JsonObject scales = new JsonObject();
        for (int s = 0; s < SCALES.length; s++) {
            scales.addProperty(String.valueOf(SCALES[s]), agree[s][0] == 0 ? 0 : (double) agree[s][1] / agree[s][0]);
        }
        json.add("threshold_agreement", scales);
        json.addProperty("root_agreement", rootAgree[0] == 0 ? 0 : (double) rootAgree[1] / rootAgree[0]);
        json.addProperty("relief_per_unit", LegacyBiomes.RELIEF_PER_UNIT);
        return json;
    }

    /** A structure whose start search the query can run with its own context. */
    private static final class ProbeStructure extends NBTStructure.GenStructure {
        ProbeStructure(String spawn) {
            super(settings(), spawn);
        }

        Optional<GenerationStub> probe(GenerationContext context) {
            return findGenerationPoint(context);
        }
    }

    private static final class ProbeDungeon extends LegacyDungeonStructure {
        ProbeDungeon(Kind kind) {
            super(settings(), kind);
        }

        Optional<GenerationStub> probe(GenerationContext context) {
            return findGenerationPoint(context);
        }
    }

    private static Structure.StructureSettings settings() {
        return new Structure.StructureSettings(HolderSet.direct(List.of()), Map.of(),
                GenerationStep.Decoration.SURFACE_STRUCTURES, TerrainAdjustment.NONE);
    }

    private static TagKey<Biome> hasStructure(String id) {
        return TagKey.create(Registries.BIOME, Ids.of(NTMStructureDeclarations.NS, "has_structure/" + id));
    }

    private static JsonObject modded(ServerLevel level, JsonObject p) {
        String id = p.get("biome").getAsString();
        String[] name = id.split(":", 2);
        Holder<Biome> biome = dev.golem.core.world.Biomes.holder(level.registryAccess(), dev.golem.core.world.Biomes.key(name[0], name[1]));
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        ChunkPos chunk = new ChunkPos(i(p, "x", 0) >> 4, i(p, "z", 0) >> 4);

        JsonObject json = new JsonObject();
        json.addProperty("biome", id);
        json.addProperty("table", NTMStructureDeclarations.legacy(id) != null);
        json.addProperty("overworld", biome.is(LegacyBiomes.OVERWORLD));
        LegacyBiomes.Terrain terrain = LegacyBiomes.terrain(generator, level, random, chunk.getMinBlockX(), chunk.getMinBlockZ());
        NTMStructureDeclarations.LegacyBiome guess = LegacyBiomes.guess(biome, terrain);
        JsonObject g = new JsonObject();
        JsonArray types = new JsonArray();
        guess.types().forEach(t -> types.add(t.name()));
        g.add("types", types);
        g.addProperty("average", terrain.average());
        g.addProperty("relief", terrain.max() - terrain.min());
        g.addProperty("rootHeight", guess.rootHeight());
        g.addProperty("heightVariation", guess.heightVariation());
        g.addProperty("lightning", guess.lightning());
        g.addProperty("temperature", LegacyClimate.temperature(biome));
        g.addProperty("downfall", LegacyClimate.downfall(biome));
        json.add("guess", g);

        JsonObject spawns = new JsonObject();
        if (p.has("spawns")) {
            for (String spawn : p.get("spawns").getAsString().split(",")) {
                if (spawn.isBlank()) continue;
                TagKey<Biome> tag = hasStructure(spawn.toLowerCase(java.util.Locale.ROOT));
                Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), generator,
                        new FixedBiomeSource(biome), random, level.getStructureManager(), level.getSeed(), chunk, level, h -> h.is(tag));
                JsonObject s = new JsonObject();
                s.addProperty("inTag", biome.is(tag));
                s.addProperty("predicate", LegacyBiomes.accepts(spawn, biome, context));
                Optional<Structure.GenerationStub> stub = new ProbeStructure(spawn).probe(context);
                s.addProperty("start", stub.isPresent());
                if (stub.isPresent()) {
                    List<StructurePiece> pieces = stub.get().getPiecesBuilder().build().pieces();
                    s.addProperty("pieces", pieces.size());
                    if (!pieces.isEmpty()) {
                        BoundingBox box = StructurePiece.createBoundingBox(pieces.stream());
                        s.addProperty("box", box.minX() + "," + box.minY() + "," + box.minZ() + " .. " + box.maxX() + "," + box.maxY() + "," + box.maxZ());
                    }
                }
                spawns.add(spawn, s);
            }
        }
        json.add("spawns", spawns);

        JsonObject dungeons = new JsonObject();
        if (p.has("dungeons")) {
            for (String kindName : p.get("dungeons").getAsString().split(",")) {
                if (kindName.isBlank()) continue;
                LegacyDungeonStructure.Kind kind = LegacyDungeonStructure.Kind.valueOf(kindName.toUpperCase(java.util.Locale.ROOT));
                TagKey<Biome> tag = hasStructure(kind.id);
                JsonObject d = new JsonObject();
                d.addProperty("inTag", biome.is(tag));
                d.addProperty("rule", LegacyBiomes.acceptsDungeon(kind, biome));
                Structure.GenerationContext context = new Structure.GenerationContext(level.registryAccess(), generator,
                        new FixedBiomeSource(biome), random, level.getStructureManager(), level.getSeed(), chunk, level,
                        h -> h.is(tag));
                d.addProperty("start", new ProbeDungeon(kind).probe(context).isPresent());
                dungeons.add(kind.id, d);
            }
        }
        json.add("dungeons", dungeons);
        return json;
    }
}
