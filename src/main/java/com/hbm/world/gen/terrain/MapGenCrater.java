package com.hbm.world.gen.terrain;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.world.feature.FeatureCodecs;
import com.hbm.world.gen.ConfigInt;
import com.hbm.world.gen.WorldgenKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;
import java.util.Random;

import static com.hbm.world.feature.FeatureCodecs.field;
import static com.hbm.world.feature.FeatureCodecs.lazy;

/**
 * {@code hbm:sellafield_crater}: 1.7 {@code world/gen/terrain/MapGenCrater} as {@code HbmWorld} registered it: the
 * "radiation hotspot": one origin chunk in {@code frequency} ({@code radfreq}, 5000) whose corner lies in one of
 * {@code biomes} (any biome when empty) becomes the centre of a crater of radius {@code min_size..max_size - 1}. Range 8
 * (1.7's {@code MapGenBase} default; the crater never called {@code setSize}).
 *
 * <p>Per column of the chunk, the top opaque or liquid block is found; if the column lies within the (ragged,
 * {@code - rand(3)}) radius, {@code depth(r) = depth - r^2 / radius^2 * depth} blocks are removed ({@code depth} =
 * {@code depth_factor} x radius) and up to three blocks below are relined: {@code regolith} within a third of the
 * radius, {@code rock} beyond. 1.7 set both to {@code sellafield_slaked}. The column's jitter draws come from the
 * origin's Random in column order, as in 1.7.
 */
public class MapGenCrater extends MapGenFeature<MapGenCrater.Config> {

    public record Config(ConfigInt frequency, List<ResourceKey<Biome>> biomes, int minSize, int maxSize, double depthFactor,
                         BlockState regolith, BlockState rock) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = FeatureCodecs.sizeRange(RecordCodecBuilder.<Config>create(i -> i.group(
                lazy(ConfigInt.CODEC, "frequency", () -> ConfigInt.key(WorldgenKeys.RAD_HOTSPOT_RATE)).forGetter(Config::frequency),
                lazy(ResourceKey.codec(Registries.BIOME).listOf(), "biomes", () -> List.of(Biomes.DESERT)).forGetter(Config::biomes),
                field(Codec.INT, "min_size", 8).forGetter(Config::minSize),
                field(Codec.INT, "max_size", 64).forGetter(Config::maxSize),
                field(Codec.DOUBLE, "depth_factor", 0.35D).forGetter(Config::depthFactor),
                FeatureCodecs.state("regolith", ModFalloutBlocks.sellafield_slaked).forGetter(Config::regolith),
                FeatureCodecs.state("rock", ModFalloutBlocks.sellafield_slaked).forGetter(Config::rock)
        ).apply(i, Config::new)), Config::minSize, Config::maxSize);
    }

    public MapGenCrater(Codec<Config> codec) {
        super(codec);
    }

    @Override
    protected int range(Config config) {
        return 8;
    }

    private static double depthFunc(double x, double rad, double depth) {
        return -Math.pow(x, 2) / Math.pow(rad, 2) * depth + depth;
    }

    private static boolean inBiomes(Holder<Biome> biome, List<ResourceKey<Biome>> biomes) {
        if (biomes.isEmpty()) return true;
        for (ResourceKey<Biome> key : biomes) if (biome.is(key)) return true;
        return false;
    }

    @Override
    protected boolean generate(Config c, WorldGenLevel level, ChunkGenerator generator, Random rand, long originSeed, int seaLevel,
                               int originX, int originZ, int chunkX, int chunkZ, boolean forced) {
        int frequency = c.frequency().get();
        if (frequency <= 0) return false;
        boolean roll = rand.nextInt(frequency) == 0;
        if (!forced) {
            if (!roll) return false;
            if (!inBiomes(originBiome(level, generator, originX, originZ, seaLevel), c.biomes())) return false;
        }

        int xCoord = -originX + chunkX;
        int zCoord = -originZ + chunkZ;
        double radius = rand.nextInt(c.maxSize() - c.minSize()) + c.minSize();
        double depth = radius * c.depthFactor();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = level.getMinY();
        boolean placed = false;

        for (int bx = 15; bx >= 0; bx--) {
            for (int bz = 15; bz >= 0; bz--) {
                int ax = (chunkX << 4) + bx, az = (chunkZ << 4) + bz;
                for (int y = top(level, ax, az); y >= floor; y--) {
                    pos.set(ax, y, az);
                    BlockState state = level.getBlockState(pos);
                    if (!opaque(level, pos, state) && state.getFluidState().isEmpty()) continue;

                    int x = xCoord * 16 + bx;
                    int z = zCoord * 16 + bz;
                    double r = Math.sqrt(x * x + z * z);

                    if (r - rand.nextInt(3) <= radius) {
                        // carve out to the intended depth (1.7 clamped against y - 1 above its world floor)
                        int dep = (int) Mth.clamp(depthFunc(r, radius, depth), 0, y - floor - 1);
                        for (int i = 0; i < dep; i++) set(level, pos.setY(y - i), Blocks.AIR.defaultBlockState());
                        y -= dep;
                        dep = Math.min(3, y - floor - 1);
                        BlockState fill = r + rand.nextInt(3) <= radius / 3D ? c.regolith() : c.rock();
                        for (int i = 0; i < dep; i++) set(level, pos.setY(y - i), fill);
                        placed = true;
                    }
                    break;
                }
            }
        }
        return placed;
    }
}
