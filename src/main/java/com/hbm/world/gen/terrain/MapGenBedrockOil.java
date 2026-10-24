package com.hbm.world.gen.terrain;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockDeadPlant;
import com.hbm.blocks.generic.BlockNTMFlower;
import com.hbm.world.feature.FeatureCodecs;
import com.hbm.world.gen.ConfigInt;
import com.hbm.world.gen.WorldgenKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.Random;

import static com.hbm.world.feature.FeatureCodecs.field;
import static com.hbm.world.feature.FeatureCodecs.lazy;

/**
 * {@code hbm:bedrock_oil}: 1.7 {@code world/gen/terrain/MapGenBedrockOil}. One origin in {@code frequency}
 * ({@code bedrockOilSpawn}, 200), rolled as {@code rand.nextInt(f) == f - 2}, turns bedrock and {@code host} within an
 * L1 diamond ({@code |x| < 5}, {@code |z| < 5}, {@code |x| + y + |z| <= 6}, y the height above the floor) into
 * {@code ore}, then blights {@code spot_count} gaussian (sigma {@code spot_width}) surface spots: dead or (1 in 10) oily
 * dirt, a cadmium willow one time in fifty and a dead plant one time in twenty on top, dirty sand, cracked stone. Range 4.
 *
 * <p>1.7's floor was stone and bedrock at y 0..4; the modern floor is bedrock and deepslate (TerraFirmaCraft: its own
 * rock), so the default host is {@code #minecraft:base_stone_overworld}.
 */
public class MapGenBedrockOil extends MapGenFeature<MapGenBedrockOil.Config> {

    public record Config(ConfigInt frequency, BlockState ore, RuleTest host, int spotCount, int spotWidth)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                lazy(ConfigInt.CODEC, "frequency", () -> ConfigInt.key(WorldgenKeys.BEDROCK_OIL)).forGetter(Config::frequency),
                FeatureCodecs.state("ore", ModBlocks.ore_bedrock_oil).forGetter(Config::ore),
                FeatureCodecs.rule("host", () -> FeatureCodecs.tag(BlockTags.BASE_STONE_OVERWORLD)).forGetter(Config::host),
                field(Codec.INT, "spot_count", 50).forGetter(Config::spotCount),
                field(Codec.INT, "spot_width", 5).forGetter(Config::spotWidth)
        ).apply(i, Config::new));
    }

    public MapGenBedrockOil(Codec<Config> codec) {
        super(codec);
    }

    @Override
    protected int range(Config config) {
        return 4;
    }

    @Override
    protected boolean generate(Config c, WorldGenLevel level, ChunkGenerator generator, Random rand, long originSeed, int seaLevel,
                               int originX, int originZ, int chunkX, int chunkZ, boolean forced) {
        int frequency = c.frequency().get();
        if (frequency <= 1) return false;
        boolean roll = rand.nextInt(frequency) == frequency - 2;
        if (!roll && !forced) return false;

        int xCoord = (chunkX - originX) * 16 + rand.nextInt(16);
        int zCoord = (chunkZ - originZ) * 16 + rand.nextInt(16);
        int floor = level.getMinY();
        RandomSource hostRand = RandomSource.create(originSeed);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int bx = 15; bx >= 0; bx--) {
            for (int bz = 15; bz >= 0; bz--) {
                for (int y = 0; y < 5; y++) {
                    pos.set((chunkX << 4) + bx, floor + y, (chunkZ << 4) + bz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(Blocks.BEDROCK) && !c.host().test(state, hostRand)) continue;
                    int x = xCoord + bx;
                    int z = zCoord + bz;
                    if (Math.abs(x) < 5 && Math.abs(z) < 5 && Math.abs(x) + Math.abs(y) + Math.abs(z) <= 6) {
                        set(level, pos, c.ore());
                        placed = true;
                    }
                }
            }
        }

        BlockDeadPlant.Type[] deadTypes = BlockDeadPlant.Type.values();
        for (int i = 0; i < c.spotCount(); i++) {
            int rx = (int) (rand.nextGaussian() * c.spotWidth()) - xCoord;
            int rz = (int) (rand.nextGaussian() * c.spotWidth()) - zCoord;
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
