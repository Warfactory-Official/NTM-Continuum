package com.hbm.world.gen.terrain;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockDeadPlant;
import com.hbm.blocks.generic.BlockNTMFlower;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/**
 * {@code hbm:bedrock_oil}: 1.7 {@code world/gen/terrain/MapGenBedrockOil}. One origin in {@code bedrockOilSpawn} (200),
 * rolled as {@code rand.nextInt(f) == f - 2}, turns the floor within an L1 diamond
 * ({@code |x| < 5}, {@code |z| < 5}, {@code |x| + y + |z| <= 6}, y the height above the floor) into
 * {@code ore_bedrock_oil}, then blights 50 gaussian (sigma 5) surface spots: dead or (1 in 10) oily dirt, a cadmium
 * willow one time in fifty and a dead plant one time in twenty on top, dirty sand, cracked stone. Range 4.
 *
 * <p>1.7's floor was stone and bedrock at y 0..4; the modern floor is bedrock and deepslate at the dimension's lowest
 * five layers, so those are the hosts here.
 */
public class MapGenBedrockOil extends MapGenFeature {

    private final int frequency;
    private static final int SPOT_WIDTH = 5;
    private static final int SPOT_COUNT = 50;

    public MapGenBedrockOil(Codec<NoneFeatureConfiguration> codec, int frequency) {
        super(codec, 4);
        this.frequency = frequency;
    }

    @Override
    protected boolean generate(WorldGenLevel level, ChunkGenerator generator, Random rand, long originSeed, int seaLevel,
                               int originX, int originZ, int chunkX, int chunkZ, boolean forced) {
        if (frequency <= 1) return false;
        boolean roll = rand.nextInt(frequency) == frequency - 2;
        if (!roll && !forced) return false;

        int xCoord = (chunkX - originX) * 16 + rand.nextInt(16);
        int zCoord = (chunkZ - originZ) * 16 + rand.nextInt(16);
        int floor = level.getMinY();
        BlockState oil = ModBlocks.ore_bedrock_oil.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int bx = 15; bx >= 0; bx--) {
            for (int bz = 15; bz >= 0; bz--) {
                for (int y = 0; y < 5; y++) {
                    pos.set((chunkX << 4) + bx, floor + y, (chunkZ << 4) + bz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(Blocks.STONE) && !state.is(Blocks.DEEPSLATE) && !state.is(Blocks.BEDROCK)) continue;
                    int x = xCoord + bx;
                    int z = zCoord + bz;
                    if (Math.abs(x) < 5 && Math.abs(z) < 5 && Math.abs(x) + Math.abs(y) + Math.abs(z) <= 6) {
                        set(level, pos, oil);
                        placed = true;
                    }
                }
            }
        }

        BlockDeadPlant.Type[] deadTypes = BlockDeadPlant.Type.values();
        for (int i = 0; i < SPOT_COUNT; i++) {
            int rx = (int) (rand.nextGaussian() * SPOT_WIDTH) - xCoord;
            int rz = (int) (rand.nextGaussian() * SPOT_WIDTH) - zCoord;
            if (rx < 0 || rx >= 16 || rz < 0 || rz >= 16) continue;
            int ax = (chunkX << 4) + rx, az = (chunkZ << 4) + rz;
            for (int y = top(level, ax, az); y >= level.getMinY(); y--) {
                pos.set(ax, y, az);
                if (!opaque(level, pos, level.getBlockState(pos))) continue;
                for (int oy = 1; oy > -3; oy--) {
                    BlockPos sub = pos.offset(0, oy, 0);
                    BlockState state = level.getBlockState(sub);
                    if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                        set(level, sub, (rand.nextInt(10) == 0 ? ModFallingBlocks.dirt_oily : ModFallingBlocks.dirt_dead).get().defaultBlockState());
                        if (oy == 0 && rand.nextInt(50) == 0) {
                            set(level, sub.above(), ModTerrainBlocks.plant_flower(BlockNTMFlower.Type.CD0).defaultBlockState());
                        }
                        // 1.7: generation runs before decoration, so it plants its own dead plants
                        if (oy == 0 && rand.nextInt(20) == 0) {
                            set(level, sub.above(), ModTerrainBlocks.plant_dead(deadTypes[rand.nextInt(deadTypes.length)]).defaultBlockState());
                        }
                        break;
                    } else if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(ModFallingBlocks.ore_oil_sand.get())) {
                        set(level, sub, (state.is(Blocks.RED_SAND) ? ModFallingBlocks.sand_dirty_red : ModFallingBlocks.sand_dirty).get().defaultBlockState());
                        break;
                    } else if (state.is(Blocks.STONE)) {
                        set(level, sub, ModBlocks.stone_cracked.get().defaultBlockState());
                        break;
                    }
                }
                break;
            }
        }
        return placed;
    }
}
