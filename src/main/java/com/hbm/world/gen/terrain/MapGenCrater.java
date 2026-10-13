package com.hbm.world.gen.terrain;

import com.hbm.blocks.ModFalloutBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/**
 * {@code hbm:sellafield_crater}: 1.7 {@code world/gen/terrain/MapGenCrater} as {@code HbmWorld} registered it: the
 * "radiation hotspot": one origin chunk in {@code radfreq} (5000) whose corner lies in a desert becomes the centre of a
 * crater of radius 8..63 lined with {@code sellafield_slaked}. Range 8 (1.7's {@code MapGenBase} default; the crater
 * never called {@code setSize}).
 *
 * <p>Per column of the chunk, the top opaque or liquid block is found; if the column lies within the (ragged,
 * {@code - rand(3)}) radius, {@code depth(r) = depth - r^2 / radius^2 * depth} blocks are removed ({@code depth} =
 * 0.35 radius) and up to three blocks below are relined, all of them {@code sellafield_slaked}, because 1.7 set both
 * the regolith and the rock to it. The column's jitter draws come from the origin's Random in column order, as in 1.7.
 */
public class MapGenCrater extends MapGenFeature {

    private final int frequency;
    private final int minSize = 8;
    private final int maxSize = 64;
    private final ResourceKey<Biome> targetBiome;

    public MapGenCrater(Codec<NoneFeatureConfiguration> codec, int frequency, ResourceKey<Biome> targetBiome) {
        super(codec, 8);
        this.frequency = frequency;
        this.targetBiome = targetBiome;
    }

    private static double depthFunc(double x, double rad, double depth) {
        return -Math.pow(x, 2) / Math.pow(rad, 2) * depth + depth;
    }

    @Override
    protected boolean generate(WorldGenLevel level, ChunkGenerator generator, Random rand, long originSeed, int seaLevel,
                               int originX, int originZ, int chunkX, int chunkZ, boolean forced) {
        if (frequency <= 0) return false;
        boolean roll = rand.nextInt(frequency) == 0;
        if (!forced) {
            if (!roll) return false;
            if (targetBiome != null) {
                Holder<Biome> biome = originBiome(level, generator, originX, originZ, seaLevel);
                if (!biome.is(targetBiome)) return false;
            }
        }

        int xCoord = -originX + chunkX;
        int zCoord = -originZ + chunkZ;
        double radius = rand.nextInt(maxSize - minSize) + minSize;
        double depth = radius * 0.35D;
        BlockState regolith = ModFalloutBlocks.sellafield_slaked.get().defaultBlockState();
        BlockState rock = regolith;
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
                        BlockState fill = r + rand.nextInt(3) <= radius / 3D ? regolith : rock;
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
