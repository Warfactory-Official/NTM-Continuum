package com.hbm.world.gen.terrain;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Random;

/**
 * 1.7's {@code MapGenBase} (driven by HBM's {@code MapGenChainloader}) as a modern feature: terrain larger than a chunk,
 * built without ever writing outside the chunk being generated.
 *
 * <p>For the chunk being generated, every chunk within {@link #range} is a potential origin. Each origin gets its own
 * {@code java.util.Random}, seeded exactly as 1.7 seeded it ({@code originX * a ^ originZ * b ^ worldSeed}, with
 * {@code a}, {@code b} the first two longs of a Random seeded with the world seed), so a bubble or crater that
 * straddles many chunks is rolled identically by each of them and each writes only its own share. That is what 1.7
 * did with its chunk primer, and it is why these stay a feature instead of a vanilla feature: a vanilla feature may only
 * write the chunk it is placed in and its direct neighbours, and a sand bubble reaches 47 blocks, a crater 65. A carver
 * could reach that far, but only carves; these also fill.
 *
 * <p>Placed with no placement modifiers, once per chunk, at {@code raw_generation}, as 1.7 chain-loaded them into the
 * cave generator, before any decoration.
 *
 * <p><b>{@code /place feature}</b> runs these in the live level instead of a generation region. There is then no chunk
 * being generated, so the feature treats the target chunk as a forced origin. The rarity roll and the biome test are
 * skipped, and it writes its share into every loaded chunk in range. That is how a single crater or bubble can be put
 * down for inspection; world generation never takes that path.
 */
public abstract class MapGenFeature<C extends FeatureConfiguration> extends Feature<C> {

    protected MapGenFeature(Codec<C> codec) {
        super(codec);
    }

    /** 1.7 {@code MapGenBase.range}: how many chunks away an origin may lie. */
    protected abstract int range(C config);

    @Override
    public boolean place(FeaturePlaceContext<C> context) {
        C config = context.config();
        int range = range(config);
        WorldGenLevel level = context.level();
        int chunkX = context.origin().getX() >> 4;
        int chunkZ = context.origin().getZ() >> 4;
        ChunkGenerator generator = context.chunkGenerator();
        int seaLevel = generator.getSeaLevel();
        Random rand = new Random();
        boolean any = false;

        if (level instanceof WorldGenRegion) {
            long seed = level.getSeed();
            rand.setSeed(seed);
            long a = rand.nextLong();
            long b = rand.nextLong();
            for (int ox = chunkX - range; ox <= chunkX + range; ox++) {
                for (int oz = chunkZ - range; oz <= chunkZ + range; oz++) {
                    long originSeed = (long) ox * a ^ (long) oz * b ^ seed;
                    rand.setSeed(originSeed);
                    any |= generate(config, level, generator, rand, originSeed, seaLevel, ox, oz, chunkX, chunkZ, false);
                }
            }
        } else {
            long originSeed = context.random().nextLong();
            for (int cx = chunkX - range; cx <= chunkX + range; cx++) {
                for (int cz = chunkZ - range; cz <= chunkZ + range; cz++) {
                    if (!level.hasChunk(cx, cz)) continue;
                    rand.setSeed(originSeed);
                    any |= generate(config, level, generator, rand, originSeed, seaLevel, chunkX, chunkZ, cx, cz, true);
                }
            }
        }
        return any;
    }

    /**
     * 1.7 {@code func_151538_a}: the contribution of the origin chunk ({@code originX}, {@code originZ}) to the chunk
     * ({@code chunkX}, {@code chunkZ}). {@code rand} is freshly seeded with {@code originSeed} (kept so a subclass can
     * test a roll on a copy first). Write only inside ({@code chunkX}, {@code chunkZ}).
     */
    protected abstract boolean generate(C config, WorldGenLevel level, ChunkGenerator generator, Random rand, long originSeed,
                                        int seaLevel, int originX, int originZ, int chunkX, int chunkZ, boolean forced);

    // Helpers

    /**
     * 1.7 {@code world.getBiomeGenForCoords(originX * 16, originZ * 16)}: the biome at the origin chunk's corner.
     *
     * <p>The origin lies up to eight chunks away, outside the generation region, and a region refuses to read (or
     * load) a chunk it does not hold ("Requested chunk unavailable during world generation"). So this asks the
     * generator's biome source directly, with the level's climate sampler: the biome that corner will have, without
     * touching its chunk. (Noise biomes are 4 x 4 x 4 cells; 1.7's biomes were per column.)
     */
    protected static Holder<Biome> originBiome(WorldGenLevel level, ChunkGenerator generator, int originX, int originZ, int seaLevel) {
        RandomState random = level.getLevel().getChunkSource().randomState();
        return generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(originX << 4), QuartPos.fromBlock(seaLevel),
                QuartPos.fromBlock(originZ << 4), random.sampler());
    }

    /**
     * The first y to scan down from in a column: 1.7 started at 127; here the column's generated top. A finished chunk
     * (the {@code /place} path) does not carry the worldgen heightmap, so it is asked for its live one.
     */
    protected static int top(WorldGenLevel level, int x, int z) {
        Heightmap.Types type = level instanceof WorldGenRegion ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE;
        return Math.min(level.getMaxY() - 1, level.getHeight(type, x, z));
    }

    /** 1.7 {@code Block.isOpaqueCube()}: an opaque full cube. */
    protected static boolean opaque(WorldGenLevel level, BlockPos pos, BlockState state) {
        return state.canOcclude() && state.isCollisionShapeFullBlock(level, pos);
    }

    /** A primer write: flags 2 | 16, as the other ported world features write. */
    protected static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, 2 | 16);
    }
}
