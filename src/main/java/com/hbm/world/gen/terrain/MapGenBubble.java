package com.hbm.world.gen.terrain;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockDeadPlant;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;
import java.util.function.Predicate;

/**
 * 1.7 {@code world/gen/terrain/MapGenBubble}: an oblate (3 x 1 x 3) sphere of ore replacing one host block, rolled per
 * origin chunk, plus the blighted surface spot and vent hole above it. Registered twice, as 1.7's {@code HbmWorld} did:
 * <ul>
 *   <li>{@code hbm:oil_bubble} — {@code ore_oil} in stone, one origin in {@code oilSpawn} (100), y 15..39, radius 8..15,
 *       range 3;</li>
 *   <li>{@code hbm:oil_sand_bubble} — {@code ore_oil_sand} in sand, one in 200 where it is hot (>= 1.5) and never
 *       storms, y 56..71, radius 16..47, fuzzy edge, range 7.</li>
 * </ul>
 * Where it is hot and dry ({@code temperature >= 2}, {@code rainfall < 0.1}) either rate is divided by three.
 *
 * <p>Verbatim quirks: the roll is {@code rand.nextInt(f) == f - 1}; the bubble centre sits at the origin chunk's corner
 * minus {@code rand(16)}; the radius test is against {@code r^2 / 2} (1.7's comment: "the original OilBubble divided the
 * square by 2 for some reason"); a fuzzy edge draws its jitter only for host blocks; {@code rand.nextInt(1) == 0}
 * always adds the surface spot but still consumes a draw.
 *
 * <p>Deviations: 1.7's {@code blocks[] == Blocks.sand} also matched red sand (sand metadata 1), so red sand is a host
 * here too; scans start at the column's generated top instead of y 127.
 */
public class MapGenBubble extends MapGenFeature {

    private final int frequency;
    private final int minSize;
    private final int maxSize;
    private final int minY;
    private final int rangeY;
    private final boolean fuzzy;
    private final Predicate<BlockState> replace;
    private final java.util.function.Supplier<BlockState> block;
    private final Predicate<Holder<Biome>> canSpawn;

    public MapGenBubble(Codec<NoneFeatureConfiguration> codec, int frequency, int minSize, int maxSize, int minY, int rangeY,
                        boolean fuzzy, Predicate<BlockState> replace, java.util.function.Supplier<BlockState> block,
                        Predicate<Holder<Biome>> canSpawn) {
        super(codec, maxSize / 8 + 1); // 1.7 setSize
        this.frequency = frequency;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.minY = minY;
        this.rangeY = rangeY;
        this.fuzzy = fuzzy;
        this.replace = replace;
        this.block = block;
        this.canSpawn = canSpawn;
    }

    /** {@code hbm:oil_bubble}, 1.7 {@code HbmWorld.registerNTMTerrain}. */
    public static MapGenBubble oil(Codec<NoneFeatureConfiguration> codec, int frequency) {
        return new MapGenBubble(codec, frequency, 8, 16, 15, 25, false, s -> s.is(Blocks.STONE),
                () -> ModOilBlocks.ore_oil.get().defaultBlockState(), null);
    }

    /** {@code hbm:oil_sand_bubble}, 1.7 {@code HbmWorld.registerNTMTerrain} (its rate was a hardcoded 200). */
    public static MapGenBubble oilSand(Codec<NoneFeatureConfiguration> codec) {
        return new MapGenBubble(codec, 200, 16, 48, 56, 16, true, s -> s.is(Blocks.SAND) || s.is(Blocks.RED_SAND),
                () -> ModFallingBlocks.ore_oil_sand.get().defaultBlockState(),
                biome -> !LegacyClimate.lightning(biome) && LegacyClimate.temperature(biome) >= 1.5F);
    }

    @Override
    protected boolean generate(WorldGenLevel level, net.minecraft.world.level.chunk.ChunkGenerator generator, Random rand, long originSeed, int seaLevel,
                               int originX, int originZ, int chunkX, int chunkZ, boolean forced) {
        if (frequency <= 0) return false;
        Holder<Biome> biome = null;
        int effecFreq = frequency;
        if (!forced) {
            // Exact short-cut: the biome only matters if the roll can pass at one of the two rates it could pick.
            int slow = Math.max(1, frequency / 3);
            boolean full = new Random(originSeed).nextInt(frequency) == frequency - 1;
            boolean third = new Random(originSeed).nextInt(slow) == slow - 1;
            if (!full && !third) return false;
            biome = originBiome(level, generator, originX, originZ, seaLevel);
            if (LegacyClimate.hotAndDry(biome)) effecFreq /= 3;
            if (effecFreq <= 0) effecFreq = 1;
        }
        boolean roll = rand.nextInt(effecFreq) == effecFreq - 1;
        if (!forced && !(roll && (canSpawn == null || canSpawn.test(biome)))) return false;

        int xCoord = (chunkX - originX) * 16 + rand.nextInt(16);
        int zCoord = (chunkZ - originZ) * 16 + rand.nextInt(16);
        int yCoord = rand.nextInt(rangeY) + minY;

        double radius = rand.nextInt(maxSize - minSize) + minSize;
        double radiusSqr = (radius * radius) / 2; // 1.7: "original OilBubble implementation divided the square by 2 for some reason"

        int yMin = Math.max(Math.max(1, level.getMinY()), Mth.floor(yCoord - radius));
        int yMax = Math.min(Math.min(127, level.getMaxY()), Mth.ceil(yCoord + radius));

        BlockState ore = block.get();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int bx = 15; bx >= 0; bx--) {
            for (int bz = 15; bz >= 0; bz--) {
                for (int by = yMin; by < yMax; by++) {
                    pos.set((chunkX << 4) + bx, by, (chunkZ << 4) + bz);
                    if (!replace.test(level.getBlockState(pos))) continue;
                    int x = xCoord + bx;
                    int z = zCoord + bz;
                    int y = yCoord - by;
                    double rSqr = x * x + z * z + y * y * 3;
                    if (fuzzy) rSqr -= rand.nextDouble() * radiusSqr / 3;
                    if (rSqr < radiusSqr) {
                        set(level, pos, ore);
                        placed = true;
                    }
                }
            }
        }

        if (rand.nextInt(1) == 0) {
            addSurfaceSpot(level, rand, xCoord, zCoord, chunkX, chunkZ);
        }
        return placed;
    }

    /** 1.7 {@code addSurfaceSpot}: 150 gaussian (sigma 7) blight spots and the vent hole, this chunk's share of them. */
    private static void addSurfaceSpot(WorldGenLevel level, Random rand, int xCoord, int zCoord, int chunkX, int chunkZ) {
        BlockDeadPlant.Type[] deadTypes = BlockDeadPlant.Type.values();
        int spotCount = 150;
        int spotWidth = 7;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < spotCount; i++) {
            int offX = (int) (rand.nextGaussian() * spotWidth);
            int offZ = (int) (rand.nextGaussian() * spotWidth);
            int rx = offX - xCoord;
            int rz = offZ - zCoord;
            if (rx < 0 || rx >= 16 || rz < 0 || rz >= 16) continue;

            int ax = (chunkX << 4) + rx, az = (chunkZ << 4) + rz;
            for (int y = top(level, ax, az); y >= level.getMinY(); y--) {
                pos.set(ax, y, az);
                if (!opaque(level, pos, level.getBlockState(pos))) continue;
                for (int oy = 1; oy > -3; oy--) {
                    BlockPos sub = pos.offset(0, oy, 0);
                    BlockState state = level.getBlockState(sub);
                    int distSq = offX * offX + offZ * offZ;
                    boolean inner = distSq < (spotWidth / 2) * (spotWidth / 2);

                    if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                        set(level, sub, (inner ? ModFallingBlocks.dirt_oily : ModFallingBlocks.dirt_dead).get().defaultBlockState());
                        // 1.7: generation runs before decoration, so it plants its own dead plants
                        if (!inner && oy == 0 && rand.nextInt(20) == 0) {
                            set(level, sub.above(), ModTerrainBlocks.plant_dead(deadTypes[rand.nextInt(deadTypes.length)]).defaultBlockState());
                        }
                        break;
                    } else if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(ModFallingBlocks.ore_oil_sand.get())) {
                        // 1.7 told red sand apart by sand's metadata 1; tar sand kept the metadata of the sand it replaced
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

        // and now for the hole(tm): ForgeDirection 1..5 = up (the centre column), north, south, west, east
        int[][] dirs = {{0, 0}, {0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int i = 1; i < 6; i++) {
            int x = dirs[i - 1][0] - xCoord;
            int z = dirs[i - 1][1] - zCoord;
            if (x < 0 || x >= 16 || z < 0 || z >= 16) continue;
            int ax = (chunkX << 4) + x, az = (chunkZ << 4) + z;
            int solids = 0;
            for (int y = top(level, ax, az); y >= level.getMinY(); y--) {
                pos.set(ax, y, az);
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) continue;
                if (!state.getFluidState().isEmpty()) break;
                if (!opaque(level, pos, state)) continue;
                solids++;
                if (i > 1) {
                    set(level, pos, ModBlocks.stone_cracked.get().defaultBlockState());
                    if (solids >= 4) break;
                } else {
                    if (solids < 3) set(level, pos, Blocks.AIR.defaultBlockState());
                    if (solids == 3) set(level, pos, ModOilBlocks.oil_spill.get().defaultBlockState());
                    if (solids > 3 && solids < 7) set(level, pos, ModBlocks.stone_cracked.get().defaultBlockState());
                    if (solids >= 7) break;
                }
            }
        }
    }
}
