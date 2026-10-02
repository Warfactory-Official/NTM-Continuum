package com.hbm.world.gen;

import com.hbm.world.gen.NTMStructureDeclarations.LegacyBiome;
import com.hbm.world.gen.NTMStructureDeclarations.Type;
import com.hbm.world.gen.terrain.LegacyClimate;
import dev.golem.core.Ids;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The generation-time half of {@link NTMStructureDeclarations}' 1.7 biome model: the 1.7 predicate of a spawn run on the
 * biome a start actually finds.
 *
 * <p>A biome with a row in the table (every vanilla overworld biome) is answered by that row, exactly as the build-time
 * tags were, so vanilla generation is unchanged. Any other biome (a modded one) is answered the way 1.7 answered its
 * modded biomes: from the {@code BiomeDictionary} types its mod gave it, which today are the convention tags behind
 * {@code hbm:biome_dictionary/<type>}, its rain ({@link LegacyClimate#lightning}), and its heights. Modern biomes carry no
 * heights (terrain comes from the noise router, not the biome), so {@link #guess} reads them off the generated terrain of
 * the start chunk's populate box, the box 1.7's own height rule averages: {@code rootHeight} from the mean surface
 * (1.7 {@code ChunkProviderGenerate}: base level {@code 64 + 17 * rootHeight}), {@code heightVariation} from the relief
 * (1.7 scales the terrain's noise swing by {@code 0.1 + 0.9 * heightVariation}; {@link #RELIEF_PER_UNIT} is the
 * relief per unit of that factor, calibrated against the vanilla biomes of the harness seed, see its note).
 */
public final class LegacyBiomes {

    private LegacyBiomes() {}

    private static final Map<Type, TagKey<Biome>> TYPES = new EnumMap<>(Type.class);
    /** {@code hbm:biome_dictionary/overworld}: 1.7 registered its spawns in dimension 0 only. */
    public static final TagKey<Biome> OVERWORLD = tag(NTMStructureDeclarations.DICTIONARY + NTMStructureDeclarations.OVERWORLD);

    static {
        for (Type type : NTMStructureDeclarations.DICTIONARY_TYPES) TYPES.put(type, tag(NTMStructureDeclarations.dictionary(type)));
    }

    private static TagKey<Biome> tag(String path) {
        return TagKey.create(Registries.BIOME, Ids.of(NTMStructureDeclarations.NS, path));
    }

    /**
     * Blocks of relief across the 16x16 populate box per unit of 1.7's noise factor {@code 0.1 + 0.9 * heightVariation}.
     * Calibrated on the harness seed (4815162342, 4,225 start chunks, {@code hbm_legacy_biome mode=calibrate}): the
     * median relief of the vanilla land biomes is 7-9 blocks for the table's 0.2 (forests, taigas, jungles), 13-15 for
     * 0.3 (grove, snowy slopes, cherry grove) and 17-18 for 0.5 (windswept hills, peaks), i.e. 28-38 blocks per unit;
     * 32 is the middle. Modern terrain does not follow the biome (plains have forest relief, median 8), so this reads
     * the ground a modded biome's start actually stands on, not a value its biome had: a best guess, as 1.7's own
     * {@code makeBestGuess} was for biomes that registered nothing.
     */
    public static final float RELIEF_PER_UNIT = 32.0F;

    /** The start chunk's populate box ({@code x+8..x+23}) sampled on a 5x5 grid of generator heights (liquids ignored). */
    public record Terrain(int average, int min, int max) {
        /** 1.7 {@code rootHeight}: the base level is {@code 64 + 17 * rootHeight}. */
        public float rootHeight() {
            return (average - 64) / 17.0F;
        }

        /** 1.7 {@code heightVariation}, inverted from the relief: relief = RELIEF_PER_UNIT * (0.1 + 0.9 * variation). */
        public float heightVariation() {
            return Math.max(0.0F, ((max - min) / RELIEF_PER_UNIT - 0.1F) / 0.9F);
        }
    }

    private static final int[] GRID = {0, 4, 8, 12, 15};

    public static Terrain terrain(ChunkGenerator generator, LevelHeightAccessor heights, RandomState random, int chunkMinX, int chunkMinZ) {
        int total = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dz : GRID) {
            for (int dx : GRID) {
                int y = generator.getBaseHeight(chunkMinX + 8 + dx, chunkMinZ + 8 + dz, Heightmap.Types.OCEAN_FLOOR_WG, heights, random);
                total += y;
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return new Terrain(total / (GRID.length * GRID.length), min, max);
    }

    /** The biome of the start chunk's centre column at the surface: where 1.7 read it, and where vanilla checks the stub. */
    public static Holder<Biome> biomeAt(BiomeSource source, RandomState random, int x, int y, int z) {
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), random.sampler());
    }

    static String id(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> Ids.keyId(key).toString()).orElse("");
    }

    /** A biome the table does not know, as 1.7 would have seen it: dictionary types, rain, heights from the terrain. */
    public static LegacyBiome guess(Holder<Biome> biome, Terrain terrain) {
        Set<Type> types = EnumSet.noneOf(Type.class);
        TYPES.forEach((type, tag) -> {
            if (biome.is(tag)) types.add(type);
        });
        return new LegacyBiome(terrain.rootHeight(), terrain.heightVariation(), types, LegacyClimate.lightning(biome));
    }

    /**
     * Whether the 1.7 predicate of {@code spawn} accepts the biome a start finds: the table row for a vanilla biome, a
     * {@link #guess} for any other overworld biome (never for one outside it: 1.7's spawns were dimension 0's).
     */
    public static boolean accepts(String spawn, Holder<Biome> biome, Structure.GenerationContext context) {
        Predicate<LegacyBiome> predicate = NTMStructureDeclarations.predicate(spawn);
        if (predicate == null) return true; // the tag alone decides (the plains null weight: vanilla plains by identity)
        LegacyBiome row = NTMStructureDeclarations.legacy(id(biome));
        if (row != null) return predicate.test(row);
        if (!biome.is(OVERWORLD)) return false;
        Terrain terrain = terrain(context.chunkGenerator(), context.heightAccessor(), context.randomState(),
                context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ());
        return predicate.test(guess(biome, terrain));
    }

    /** 1.7 {@code heightVariation} of the biome at a start: the table's, or read off the terrain for a modded biome. */
    public static float heightVariation(Holder<Biome> biome, Structure.GenerationContext context) {
        LegacyBiome row = NTMStructureDeclarations.legacy(id(biome));
        if (row != null) return row.heightVariation();
        return terrain(context.chunkGenerator(), context.heightAccessor(), context.randomState(),
                context.chunkPos().getMinBlockX(), context.chunkPos().getMinBlockZ()).heightVariation();
    }

    /** A legacy dungeon's 1.7 climate rule on the biome at its site: the table row for a vanilla biome, the biome's own values otherwise. */
    public static boolean acceptsDungeon(LegacyDungeonStructure.Kind kind, Holder<Biome> biome) {
        Predicate<LegacyDungeonDeclarations.Climate> rule = LegacyDungeonDeclarations.rule(kind);
        if (rule == null) return true; // every overworld biome, or the jungles by identity: the tag decides
        LegacyDungeonDeclarations.Climate row = LegacyDungeonDeclarations.climate(id(biome));
        if (row != null) return rule.test(row);
        if (!biome.is(OVERWORLD)) return false;
        return rule.test(new LegacyDungeonDeclarations.Climate(LegacyClimate.temperature(biome), LegacyClimate.downfall(biome),
                LegacyClimate.lightning(biome)));
    }
}
