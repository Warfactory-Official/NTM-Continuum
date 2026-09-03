package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Surface oil-blight scatter — 1:1 port of the 1.12.2 {@code world/feature/OilSpot}. Drops {@code count}
 * gaussian (σ = {@code width}) samples around (x, z); at each, walks the top 4 blocks and turns the
 * surface into dead/oily debris: grass/dirt → {@code dirt_oily} (1/10) or {@code dirt_dead}, sand →
 * {@code sand_dirty}(_red), stone → {@code stone_cracked}, leaves → air.
 *
 * <p>Called both from worldgen (the oil / bedrock-oil bubbles) and at runtime by the fracking tower
 * (per suck) — hence typed on {@link WorldGenLevel}, which {@code ServerLevel} implements.
 *
 * <p>Deviations (IMPLEMENTATION.md §20): the plant-kill / mustard-willow branches (old
 * {@code BlockPlantEnumMeta}/{@code plant_dead}/{@code plant_flower}) are skipped — those blocks are
 * unported (same skip as {@link OilBubbleFeature}); {@code addWillows} is therefore a no-op. Surface
 * height comes from the WORLD_SURFACE heightmap (old {@code world.getHeight}).
 */
public final class OilSpot {

    private OilSpot() {}

    public static void generateOilSpot(WorldGenLevel level, int x, int z, int width, int count, boolean addWillows) {
        generateOilSpot(level, x, z, width, count, addWillows,
                Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /** Worldgen variant constrained to the origin chunk and its one-chunk writable border. */
    public static void generateOilSpot(WorldGenLevel level, BlockPos chunkOrigin, int x, int z,
                                       int width, int count, boolean addWillows) {
        generateOilSpot(level, x, z, width, count, addWillows,
                chunkOrigin.getX() - 16, chunkOrigin.getX() + 31,
                chunkOrigin.getZ() - 16, chunkOrigin.getZ() + 31);
    }

    private static void generateOilSpot(WorldGenLevel level, int x, int z, int width, int count,
                                        boolean addWillows, int minX, int maxX, int minZ, int maxZ) {
        RandomSource rand = level.getRandom();
        MutableBlockPos pos = new MutableBlockPos();

        for (int i = 0; i < count; i++) {
            int rX = x + (int) (rand.nextGaussian() * width);
            int rZ = z + (int) (rand.nextGaussian() * width);
            if (rX < minX || rX > maxX || rZ < minZ || rZ > maxZ) continue;
            int rY = level.getHeight(Heightmap.Types.WORLD_SURFACE, rX, rZ);

            for (int y = rY; y > rY - 4 && y > level.getMinY(); y--) {

                pos.set(rX, y, rZ);
                BlockState groundState = level.getBlockState(pos);
                Block ground = groundState.getBlock();

                if (ground == Blocks.GRASS_BLOCK || ground == Blocks.DIRT) {
                    BlockState dirt = (rand.nextInt(10) == 0 ? ModFallingBlocks.dirt_oily : ModFallingBlocks.dirt_dead).get().defaultBlockState();
                    level.setBlock(pos, dirt, 2 | 16);
                    // old: addWillows 1/50 mustard-willow placement — skipped (plant blocks unported)
                    break;

                } else if (ground == Blocks.SAND || ground == ModFallingBlocks.ore_oil_sand.get()) {
                    level.setBlock(pos, ModFallingBlocks.sand_dirty.get().defaultBlockState(), 2 | 16);
                    break;

                } else if (ground == Blocks.RED_SAND) {
                    level.setBlock(pos, ModFallingBlocks.sand_dirty_red.get().defaultBlockState(), 2 | 16);
                    break;

                } else if (ground == Blocks.STONE) {
                    level.setBlock(pos, ModBlocks.stone_cracked.get().defaultBlockState(), 2 | 16);
                    break;

                } else if (groundState.is(net.minecraft.tags.BlockTags.LEAVES)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
            }
        }
    }
}
