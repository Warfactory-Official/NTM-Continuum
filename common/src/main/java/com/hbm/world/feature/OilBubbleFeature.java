package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Underground oil bubble — 1:1 port of the 1.12.2 {@code world/OilBubble} + its invocation in
 * {@code HbmWorldGen} (L671-686). Per chunk: {@code rand.nextInt(rate) == 0} with rate 100 (the old
 * {@code oilBubbleSpawn} config default {@code 0:100}), divided by 3 (min 1) in hot dry biomes; origin
 * at a random in-chunk x/z, y = {@code rand.nextInt(25)}, radius {@code 10 + rand.nextInt(7)}. The
 * deposit is the ellipsoid {@code xx² + 3·yy² + zz² < r²/2} replacing STONE with {@code ore_oil}
 * (deepslate untouched — Y0-24 is stone in 26.1, matching the old stone-only check), plus the old
 * surface treatment: 150 gaussian dead-vegetation spots and the central oil vent column.
 *
 * <p>Deviations (documented in IMPLEMENTATION.md §20): the old rarity/biome logic lives inside
 * {@code place()} instead of placement modifiers (one placed feature, empty placement); hot-dry
 * detection is {@code baseTemperature >= 2.0 && !hasPrecipitation()} (26.1 has no public downfall
 * getter; this admits savannas, which 1.12 excluded by rainfall); the outer-ring 1/20 {@code
 * plant_dead} placements are skipped (block unported); gaussian spots outside the writable worldgen
 * region are skipped; surface scans start at the WORLD_SURFACE_WG heightmap instead of the old fixed
 * y=127; {@code ore_oil_sand} substrate is skipped (unported with the oil-sand bubble).
 */
public class OilBubbleFeature extends Feature<NoneFeatureConfiguration> {

    /** Old CompatibilityConfig.oilBubbleSpawn overworld default ("0:100"). */
    private static final int RATE = 100;

    public OilBubbleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos chunkOrigin = context.origin();

        int rate = RATE;
        Biome biome = level.getBiome(chunkOrigin).value();
        if (biome.getBaseTemperature() >= 2.0F && !biome.hasPrecipitation()) {
            rate /= 3;
        }
        if (rate == 0) rate = 1;
        if (rand.nextInt(rate) != 0) return false;

        int x = chunkOrigin.getX() + rand.nextInt(16);
        int y = rand.nextInt(25);
        int z = chunkOrigin.getZ() + rand.nextInt(16);
        int radius = 10 + rand.nextInt(7);

        spawnOil(level, x, y, z, radius);
        addSurfaceSpot(level, rand, chunkOrigin, x, z);
        return true;
    }

    /** Old OilBubble.spawnOil: ellipsoid xx² + 3·yy² + zz² < r²/2, STONE → ore_oil (flags 2|16). */
    private static void spawnOil(WorldGenLevel level, int x, int y, int z, int radius) {
        int r22 = radius * radius / 2;
        BlockState oil = ModOilBlocks.ore_oil.get().defaultBlockState();
        MutableBlockPos pos = new MutableBlockPos();

        for (int xx = -radius; xx < radius; xx++) {
            int xd = xx * xx;
            for (int yy = -radius; yy < radius; yy++) {
                int yd = xd + yy * yy * 3;
                for (int zz = -radius; zz < radius; zz++) {
                    if (yd + zz * zz < r22) {
                        pos.set(x + xx, y + yy, z + zz);
                        if (level.getBlockState(pos).is(Blocks.STONE)) {
                            level.setBlock(pos, oil, 2 | 16);
                        }
                    }
                }
            }
        }
    }

    /** Old OilBubble.addSurfaceSpot: 150 gaussian (σ=7) dead-vegetation spots + the central vent. */
    private static void addSurfaceSpot(WorldGenLevel level, RandomSource rand, BlockPos chunkOrigin, int xCoord, int zCoord) {
        int spotCount = 150;
        int spotWidth = 7;
        // Features may only write to the origin chunk ± 1 chunk; gaussian offsets can exceed that.
        int minX = chunkOrigin.getX() - 16, maxX = chunkOrigin.getX() + 31;
        int minZ = chunkOrigin.getZ() - 16, maxZ = chunkOrigin.getZ() + 31;
        MutableBlockPos pos = new MutableBlockPos();

        for (int i = 0; i < spotCount; i++) {
            int offX = (int) (rand.nextGaussian() * spotWidth);
            int offZ = (int) (rand.nextGaussian() * spotWidth);
            int absX = xCoord + offX;
            int absZ = zCoord + offZ;
            if (absX < minX || absX > maxX || absZ < minZ || absZ > maxZ) continue; // outside writable region

            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, absX, absZ);
            for (int y = top; y >= level.getMinY(); y--) {
                pos.set(absX, y, absZ);
                BlockState state = level.getBlockState(pos);

                if (state.isCollisionShapeFullBlock(level, pos)) {
                    for (int oy = 1; oy > -3; oy--) {
                        BlockPos subPos = pos.offset(0, oy, 0);
                        BlockState subState = level.getBlockState(subPos);
                        Block b = subState.getBlock();

                        int distSq = offX * offX + offZ * offZ;
                        boolean inner = distSq < (spotWidth / 2) * (spotWidth / 2);

                        if (b == Blocks.GRASS_BLOCK || b == Blocks.DIRT) {
                            level.setBlock(subPos, (inner ? ModFallingBlocks.dirt_oily : ModFallingBlocks.dirt_dead).get().defaultBlockState(), 2 | 16);
                            // old: outer ring oy==0 1/20 plant_dead above — skipped, block unported
                            break;
                        } else if (b == Blocks.SAND) {
                            level.setBlock(subPos, ModFallingBlocks.sand_dirty.get().defaultBlockState(), 2 | 16);
                            break;
                        } else if (b == Blocks.RED_SAND) {
                            level.setBlock(subPos, ModFallingBlocks.sand_dirty_red.get().defaultBlockState(), 2 | 16);
                            break;
                        } else if (b == Blocks.STONE) {
                            level.setBlock(subPos, ModBlocks.stone_cracked.get().defaultBlockState(), 2 | 16);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        // Central vent (old i=1, UP = the origin column) + the 4 horizontal neighbor columns cracked.
        BlockState cracked = ModBlocks.stone_cracked.get().defaultBlockState();
        ventColumn(level, xCoord, zCoord, true, cracked);
        for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            ventColumn(level, xCoord + dir.getStepX(), zCoord + dir.getStepZ(), false, cracked);
        }
    }

    private static void ventColumn(WorldGenLevel level, int x, int z, boolean center, BlockState cracked) {
        MutableBlockPos pos = new MutableBlockPos();
        int solids = 0;
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

        for (int y = top; y >= level.getMinY(); y--) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (!state.getFluidState().isEmpty()) break;

            if (state.isCollisionShapeFullBlock(level, pos)) {
                solids++;

                if (!center) {
                    level.setBlock(pos, cracked, 2 | 16);
                    if (solids >= 4) break;
                } else {
                    if (solids < 3) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
                    if (solids == 3) level.setBlock(pos, ModOilBlocks.oil_spill.get().defaultBlockState(), 2 | 16);
                    if (solids > 3 && solids < 7) level.setBlock(pos, cracked, 2 | 16);
                    if (solids == 7) break;
                }
            }
        }
    }
}
