package com.hbm.world.feature;

import com.hbm.blocks.ModOreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.12.2 {@code DepthDeposit}: a sphere of depth ore wrapped in a shell of depth rock, buried at
 * the very bottom of the world.
 *
 * <p>Per cell, at distance {@code len} from the centre:
 * <pre>
 *   len + rand.nextInt(2) &lt;  size * fill  -&gt; the ore
 *   len + rand.nextInt(2) &lt;= size         -&gt; the depth-rock filler
 * </pre>
 * A cell is only touched if it is the generation target (stone / netherrack) <b>or bedrock</b> — the
 * original's comment on that line reads "yes you've heard right, bedrock", and it is what lets a
 * deposit chew a pocket out of the world floor. Preserved.
 *
 * <p><b>Y rebasing (deviation):</b> upstream clamped to {@code 1 <= y <= 126} and centred deposits at
 * {@code y = 0..2}, which were the floor and ceiling of a 1.12 world. 26.1's overworld floor is −64 and
 * the nether roof is still 127, so the y band is expressed relative to {@link WorldGenLevel#getMinY()}
 * and the guard uses the level's real bounds — the same adjustment {@code MapGenBedrockOil} makes.
 */
public final class DepthDepositPlacer {

    private DepthDepositPlacer() {}

    /**
     * The overworld variant: replaces stone (or bedrock), fills with {@code stone_depth}, centred
     * {@code minY + rand.nextInt(3)} above the world floor.
     */
    public static void overworld(WorldGenLevel level, RandomSource rand, int chunkMinX, int chunkMinZ,
                                 int size, double fill, BlockState ore, int chance) {
        generate(level, rand, chunkMinX, chunkMinZ, level.getMinY(), 3, size, fill, ore,
                Blocks.STONE.defaultBlockState(), ModOreBlocks.stone_depth.get().defaultBlockState(), chance);
    }

    /**
     * The nether variant: replaces netherrack (or bedrock), fills with {@code stone_depth_nether}.
     * {@code yBase} is an absolute y — upstream generated one set at the nether floor and one at the
     * roof (y = 125).
     */
    public static void nether(WorldGenLevel level, RandomSource rand, int chunkMinX, int chunkMinZ,
                              int yBase, int size, double fill, BlockState ore, int chance) {
        generate(level, rand, chunkMinX, chunkMinZ, yBase, 3, size, fill, ore,
                Blocks.NETHERRACK.defaultBlockState(), ModOreBlocks.stone_depth_nether.get().defaultBlockState(), chance);
    }

    private static void generate(WorldGenLevel level, RandomSource rand, int chunkMinX, int chunkMinZ,
                                 int yMin, int yDev, int size, double fill, BlockState ore,
                                 BlockState genTarget, BlockState filler, int chance) {
        if (chance <= 0 || rand.nextInt(chance) != 0) return;

        int cx = chunkMinX + rand.nextInt(16);
        int cy = yMin + rand.nextInt(yDev);
        int cz = chunkMinZ + rand.nextInt(16);
        generateSphere(level, rand, cx, cy, cz, size, fill, ore, genTarget, filler);
    }

    /** The sphere itself. Public only so {@code OreWorldgenGameTests} can drive it deterministically. */
    public static void generateSphere(WorldGenLevel level, RandomSource rand, int cx, int cy, int cz,
                                      int size, double fill, BlockState ore, BlockState genTarget, BlockState filler) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = level.getMinY();
        int ceiling = level.getMaxY();

        for (int ix = cx - size; ix <= cx + size; ix++) {
            int dx = ix - cx;
            for (int jy = cy - size; jy <= cy + size; jy++) {
                if (jy < floor || jy > ceiling) continue;
                int dy = jy - cy;
                for (int kz = cz - size; kz <= cz + size; kz++) {
                    int dz = kz - cz;
                    pos.set(ix, jy, kz);
                    if (!level.hasChunk(ix >> 4, kz >> 4)) continue;

                    BlockState current = level.getBlockState(pos);
                    // "yes you've heard right, bedrock" — the deposit eats the world floor on purpose
                    if (!current.is(Blocks.BEDROCK) && !current.is(genTarget.getBlock())) continue;

                    double len = Math.sqrt(dx * (double) dx + dy * (double) dy + dz * (double) dz);
                    if (len + rand.nextInt(2) < size * fill) {
                        level.setBlock(pos, ore, 2 | 16);
                    } else if (len + rand.nextInt(2) <= size) {
                        level.setBlock(pos, filler, 2 | 16);
                    }
                }
            }
        }
    }
}
