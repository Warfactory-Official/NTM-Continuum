package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * The 1.12.2 ore-vein placer: {@code DungeonToolbox.generateOre} driving
 * {@code WorldGenMinableNonCascade}, whose body is a copy of vanilla 1.12's
 * {@code WorldGenMinable} ellipsoid.
 *
 * <p>The algorithm sweeps a line at a random horizontal angle and fills a swelling ellipsoid around it:
 *
 * <pre>
 *   f  = rand.nextFloat() * PI                      // vein bearing
 *   d0/d1 = x + 8 ± sin(f) * n / 8                  // line endpoints (note the +8!)
 *   d2/d3 = z + 8 ± cos(f) * n / 8
 *   d4/d5 = y + rand.nextInt(3) - 2                 // independent endpoint heights
 *   per step i:  d9  = rand.nextDouble() * n / 16
 *                d10 = (sin(PI * i/n) + 1) * d9 + 1 // thickest in the middle
 * </pre>
 *
 * <p><b>The {@code +8} offsets are upstream behaviour, kept as is.</b> They came along when HBM
 * copied vanilla's generator, where the caller passed a chunk-corner position; here the caller already
 * picks a random in-chunk x/z, so the effective vein centre sits 8 blocks off the chosen point. Removing
 * it would shift every vein in the mod.
 */
public final class OreVeinPlacer {

    private OreVeinPlacer() {}

    // Replaceable predicates

    /**
     * The old {@code WorldUtil.STONE_PREDICATE}: stone plus the <i>natural</i> stone variants
     * (granite/diorite/andesite), never their polished forms.
     *
     * <p><b>Documented deviation:</b> deepslate and tuff are accepted too. The old y-bands were written
     * for a 0..255 world and reach down to y=4; in 26.1 the deepslate blend zone starts at y=8 and is
     * solid below y=0, so a stone-only predicate would silently thin out or delete the deepest veins
     * (cobalt at 4–11, uranium at 5–24, cinnabar at 8–23). Accepting deepslate keeps the vein counts
     * the config asks for.
     */
    public static final Predicate<BlockState> STONE = state ->
            state.is(Blocks.STONE) || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE)
                    || state.is(Blocks.ANDESITE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF);

    /** Nether veins target netherrack exactly, as upstream. */
    public static final Predicate<BlockState> NETHERRACK = state -> state.is(Blocks.NETHERRACK);

    /** End veins target end stone exactly, as upstream. */
    public static final Predicate<BlockState> END_STONE = state -> state.is(Blocks.END_STONE);

    /** A vein that replaces one exact block: the gneiss family targets {@code stone_gneiss}. */
    public static Predicate<BlockState> exactly(Block block) {
        return state -> state.is(block);
    }

    // The driver

    /**
     * {@code DungeonToolbox.generateOre}: {@code count} veins, each at a random in-chunk x/z and
     * {@code y = minHeight + rand.nextInt(variance)}. A {@code count} of 0 or less generates nothing,
     * which is how the config gates switch a vein off.
     */
    public static void generateOre(WorldGenLevel level, RandomSource rand, int chunkMinX, int chunkMinZ,
                                   int count, int size, int minHeight, int variance,
                                   BlockState ore, Predicate<BlockState> target) {
        if (count <= 0) return;
        for (int i = 0; i < count; i++) {
            int x = chunkMinX + rand.nextInt(16);
            int y = minHeight + (variance > 0 ? rand.nextInt(variance) : 0);
            int z = chunkMinZ + rand.nextInt(16);
            placeVein(level, rand, x, y, z, size, ore, target);
        }
    }

    /**
     * One vein: the vanilla ellipsoid sweep. Writes with flags {@code 2 | 16} (send to
     * clients, skip neighbour updates), matching upstream.
     */
    public static void placeVein(WorldGenLevel level, RandomSource rand, int x, int y, int z, int numberOfBlocks,
                                 BlockState ore, Predicate<BlockState> target) {
        float f = rand.nextFloat() * (float) Math.PI;

        double d0 = x + 8.0F + Mth.sin(f) * numberOfBlocks / 8.0F;
        double d1 = x + 8.0F - Mth.sin(f) * numberOfBlocks / 8.0F;
        double d2 = z + 8.0F + Mth.cos(f) * numberOfBlocks / 8.0F;
        double d3 = z + 8.0F - Mth.cos(f) * numberOfBlocks / 8.0F;
        double d4 = y + rand.nextInt(3) - 2;
        double d5 = y + rand.nextInt(3) - 2;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < numberOfBlocks; ++i) {
            float f1 = (float) i / (float) numberOfBlocks;
            double d6 = d0 + (d1 - d0) * f1;
            double d7 = d4 + (d5 - d4) * f1;
            double d8 = d2 + (d3 - d2) * f1;
            double d9 = rand.nextDouble() * numberOfBlocks / 16.0D;
            double d10 = (Mth.sin((float) Math.PI * f1) + 1.0F) * d9 + 1.0D;
            double d11 = (Mth.sin((float) Math.PI * f1) + 1.0F) * d9 + 1.0D;
            int j = Mth.floor(d6 - d10 / 2.0D);
            int k = Mth.floor(d7 - d11 / 2.0D);
            int l = Mth.floor(d8 - d10 / 2.0D);
            int i1 = Mth.floor(d6 + d10 / 2.0D);
            int j1 = Mth.floor(d7 + d11 / 2.0D);
            int k1 = Mth.floor(d8 + d10 / 2.0D);

            for (int l1 = j; l1 <= i1; ++l1) {
                double d12 = (l1 + 0.5D - d6) / (d10 / 2.0D);
                if (d12 * d12 >= 1.0D) continue;

                for (int i2 = k; i2 <= j1; ++i2) {
                    double d13 = (i2 + 0.5D - d7) / (d11 / 2.0D);
                    if (d12 * d12 + d13 * d13 >= 1.0D) continue;

                    for (int j2 = l; j2 <= k1; ++j2) {
                        double d14 = (j2 + 0.5D - d8) / (d10 / 2.0D);
                        if (d12 * d12 + d13 * d13 + d14 * d14 >= 1.0D) continue;

                        pos.set(l1, i2, j2);
                        if (!isWritable(level, pos)) continue;
                        if (target.test(level.getBlockState(pos))) {
                            level.setBlock(pos, ore, 2 | 16);
                        }
                    }
                }
            }
        }
    }

    /**
     * A 26.1 feature may only write inside the origin chunk ± 1 (writing further corrupts neighbouring
     * chunks or deadlocks generation). The 1.12 generator had no such rule: it used a deferred
     * "phased structure" scheduler to write across chunk borders later. Veins are small enough that
     * clamping is close enough to 1.12; the oil-sand bubble uses the same clamp.
     */
    private static boolean isWritable(WorldGenLevel level, BlockPos pos) {
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && pos.getY() >= level.getMinY() && pos.getY() <= level.getMaxY();
    }
}
