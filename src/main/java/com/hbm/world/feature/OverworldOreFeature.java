package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.config.WorldConfig;
import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/**
 * The overworld half of 1.12.2's {@code HbmWorldGen.generateOres} — every vein it placed when
 * {@code dimID == 0}, in the original order, with the original counts, sizes and height bands
 * (see {@link WorldConfig}).
 *
 * <p>Structure of the original, preserved here section by section:
 * <ol>
 *   <li>the chance-gated gas bubbles and alexandrite;</li>
 *   <li>the {@link DepthDepositPlacer} overworld deposits;</li>
 *   <li>the gneiss family, which targets the {@code stone_gneiss} band laid down by
 *       {@link SchistStratumFeature} — hence that feature runs in an earlier decoration step;</li>
 *   <li>the 16 plain stone-hosted ores;</li>
 *   <li>the four clusters and limestone;</li>
 *   <li>the 528 coltan scatter/deposit and the hardcoded australium pocket.</li>
 * </ol>
 *
 * <p><b>Upstream quirks preserved verbatim</b> (each flagged at its call site):
 * <ul>
 *   <li>the {@code rareSpawn} slot in the gneiss block places <b>asbestos again</b>, not rare earths,
 *       which is why {@code ore_gneiss_rare} is unobtainable from worldgen;</li>
 *   <li>limestone generates in every dimension because its count came off a non-dimension-mapped
 *       config field — so it is repeated in the nether and end features too;</li>
 *   <li>the coltan deposit field is centred on a gaussian drawn from {@code new Random(seed + 5)}.</li>
 * </ul>
 */
public class OverworldOreFeature extends Feature<NoneFeatureConfiguration> {

    public OverworldOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        int cx = context.origin().getX();
        int cz = context.origin().getZ();

        gasBubbles(level, rand, cx, cz);
        depthDeposits(level, rand, cx, cz);
        gneissOres(level, rand, cx, cz);
        stoneOres(level, rand, cx, cz);
        clustersAndLimestone(level, rand, cx, cz);
        BedrockOreFeature.generateAutoChunk(level, rand, cx, cz);
        coltanAndAustralium(level, rand, cx, cz);
        return true;
    }

    // ---------------------------------------------------------------- 1. chance-gated pockets

    private static void gasBubbles(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        if (GeneralConfig.enableFlammableGas && chance(rand, WorldConfig.gasBubbleChance)) {
            OreVeinPlacer.generateOre(level, rand, cx, cz, 1, 32, 30, 10,
                    state(ModOreBlocks.gas_flammable), OreVeinPlacer.STONE);
        }
        if (GeneralConfig.enableExplosiveGas && chance(rand, WorldConfig.explosiveBubbleChance)) {
            OreVeinPlacer.generateOre(level, rand, cx, cz, 1, 32, 30, 10,
                    state(ModOreBlocks.gas_explosive), OreVeinPlacer.STONE);
        }
        if (chance(rand, WorldConfig.alexandriteChance)) {
            OreVeinPlacer.generateOre(level, rand, cx, cz, 1, 3, 10, 5,
                    state(ModOreBlocks.ore_alexandrite), OreVeinPlacer.STONE);
        }
    }

    // ---------------------------------------------------------------- 2. depth deposits

    private static void depthDeposits(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        DepthDepositPlacer.overworld(level, rand, cx, cz, 5, 0.6D, state(ModOreBlocks.cluster_depth_iron), 24);
        DepthDepositPlacer.overworld(level, rand, cx, cz, 5, 0.6D, state(ModOreBlocks.cluster_depth_titanium), 32);
        DepthDepositPlacer.overworld(level, rand, cx, cz, 5, 0.6D, state(ModOreBlocks.cluster_depth_tungsten), 32);
        DepthDepositPlacer.overworld(level, rand, cx, cz, 5, 0.8D, state(ModOreBlocks.ore_depth_cinnabar), 16);
        DepthDepositPlacer.overworld(level, rand, cx, cz, 5, 0.8D, state(ModOreBlocks.ore_depth_zirconium), 16);
        DepthDepositPlacer.overworld(level, rand, cx, cz, 5, 0.8D, state(ModOreBlocks.ore_depth_borax), 16);
    }

    // ---------------------------------------------------------------- 3. gneiss family

    private static void gneissOres(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        var gneiss = OreVeinPlacer.exactly(ModBlocks.stone_gneiss.get());
        // hardcoded overworld-only counts upstream (not config-driven)
        OreVeinPlacer.generateOre(level, rand, cx, cz, 25, 6, 30, 10, state(ModBlocks.ore_gneiss_iron), gneiss);
        OreVeinPlacer.generateOre(level, rand, cx, cz, 10, 6, 30, 10, state(ModBlocks.ore_gneiss_gold), gneiss);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.uraniumSpawn * 3, 6, 30, 10,
                state(ModNuclearBlocks.ore_gneiss_uranium), gneiss);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.copperSpawn * 3, 6, 30, 10,
                state(ModBlocks.ore_gneiss_copper), gneiss);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.asbestosSpawn * 3, 6, 30, 10,
                state(ModBlocks.ore_gneiss_asbestos), gneiss);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.lithiumSpawn, 6, 30, 10,
                state(ModBlocks.ore_gneiss_lithium), gneiss);
        // UPSTREAM QUIRK, kept: the rare-earth budget places ASBESTOS a second time. ore_gneiss_rare is
        // registered but has no worldgen path in 1.12.2 either — do not "fix" this silently.
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.rareSpawn, 6, 30, 10,
                state(ModBlocks.ore_gneiss_asbestos), gneiss);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.gasShaleSpawn * 3, 10, 30, 10,
                state(ModBlocks.ore_gneiss_gas), gneiss);
    }

    // ---------------------------------------------------------------- 4. plain stone ores

    private static void stoneOres(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        var stone = OreVeinPlacer.STONE;
        ore(level, rand, cx, cz, WorldConfig.uraniumSpawn,    5, 5, 20, state(ModNuclearBlocks.ore_uranium), stone);
        ore(level, rand, cx, cz, WorldConfig.thoriumSpawn,    5, 5, 25, state(ModBlocks.ore_thorium), stone);
        ore(level, rand, cx, cz, WorldConfig.titaniumSpawn,   6, 5, 30, state(ModBlocks.ore_titanium), stone);
        ore(level, rand, cx, cz, WorldConfig.sulfurSpawn,     8, 5, 30, state(ModBlocks.ore_sulfur), stone);
        ore(level, rand, cx, cz, WorldConfig.aluminiumSpawn,  6, 5, 40, state(ModBlocks.ore_aluminium), stone);
        ore(level, rand, cx, cz, WorldConfig.copperSpawn,     6, 5, 45, state(ModBlocks.ore_copper), stone);
        ore(level, rand, cx, cz, WorldConfig.fluoriteSpawn,   4, 5, 45, state(ModBlocks.ore_fluorite), stone);
        ore(level, rand, cx, cz, WorldConfig.niterSpawn,      6, 5, 30, state(ModBlocks.ore_niter), stone);
        ore(level, rand, cx, cz, WorldConfig.tungstenSpawn,   8, 5, 30, state(ModBlocks.ore_tungsten), stone);
        ore(level, rand, cx, cz, WorldConfig.leadSpawn,       9, 5, 30, state(ModBlocks.ore_lead), stone);
        ore(level, rand, cx, cz, WorldConfig.berylliumSpawn,  4, 5, 30, state(ModBlocks.ore_beryllium), stone);
        ore(level, rand, cx, cz, WorldConfig.rareSpawn,       5, 5, 20, state(ModBlocks.ore_rare), stone);
        ore(level, rand, cx, cz, WorldConfig.ligniteSpawn,   24, 35, 25, state(ModBlocks.ore_lignite), stone);
        ore(level, rand, cx, cz, WorldConfig.asbestosSpawn,   4, 16, 16, state(ModBlocks.ore_asbestos), stone);
        ore(level, rand, cx, cz, WorldConfig.cinnabarSpawn,   4, 8, 16, state(ModBlocks.ore_cinnabar), stone);
        ore(level, rand, cx, cz, WorldConfig.cobaltSpawn,     4, 4, 8, state(ModBlocks.ore_cobalt), stone);
    }

    // ---------------------------------------------------------------- 5. clusters + limestone

    private static void clustersAndLimestone(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        var stone = OreVeinPlacer.STONE;
        ore(level, rand, cx, cz, WorldConfig.ironClusterSpawn,      6, 15, 45, state(ModOreBlocks.cluster_iron), stone);
        ore(level, rand, cx, cz, WorldConfig.titaniumClusterSpawn,  6, 15, 30, state(ModOreBlocks.cluster_titanium), stone);
        ore(level, rand, cx, cz, WorldConfig.aluminiumClusterSpawn, 6, 15, 35, state(ModOreBlocks.cluster_aluminium), stone);
        ore(level, rand, cx, cz, WorldConfig.copperClusterSpawn,    6, 15, 20, state(ModOreBlocks.cluster_copper), stone);
        limestone(level, rand, cx, cz);
    }

    /**
     * Limestone. Upstream read a plain {@code WorldConfig.limestoneSpawn} int rather than a
     * per-dimension map, so this vein generated in <b>every</b> dimension — hence the nether and end
     * features call this same method.
     */
    static void limestone(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.limestoneSpawn, 6, 15, 20,
                state(ModOreBlocks.stone_resource_limestone), OreVeinPlacer.STONE);
    }

    // ---------------------------------------------------------------- 6. coltan + australium

    private static void coltanAndAustralium(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        BlockState coltan = state(ModBlocks.ore_coltan);

        if (GeneralConfig.enable528ColtanSpawn) {
            OreVeinPlacer.generateOre(level, rand, cx, cz, GeneralConfig.coltanRate, 4, 15, 40, coltan,
                    OreVeinPlacer.STONE);
        }

        if (GeneralConfig.enable528ColtanDeposit) {
            // Deposit field: a single gaussian centre per world, drawn from a seed-derived Random, with
            // five concentric bands of decreasing density. Verbatim from HbmWorldGen.
            long seed = level.getSeed();
            Random centreRand = new Random(seed + 5);
            int colX = (int) (centreRand.nextGaussian() * 1500);
            int colZ = (int) (centreRand.nextGaussian() * 1500);
            int colRange = 750;
            for (int r = 1; r <= 5; r++) {
                int band = colRange / r;
                if (Math.abs(cx - colX) > band || Math.abs(cz - colZ) > band) continue;
                for (int i = 0; i < 5; i++) {
                    OreVeinPlacer.placeVein(level, rand, cx + rand.nextInt(16), 15 + rand.nextInt(25),
                            cz + rand.nextInt(16), 4, coltan, OreVeinPlacer.STONE);
                }
            }
        }

        // The hardcoded australium pocket: a single 100×100-block patch of the overworld, size 50.
        if (cx >= -450 && cx <= -350 && cz >= -450 && cz <= -350) {
            int attempts = rand.nextInt(4);
            for (int i = 0; i < attempts; i++) {
                OreVeinPlacer.placeVein(level, rand, cx + rand.nextInt(16), 15 + rand.nextInt(15),
                        cz + rand.nextInt(16), 50, state(ModBlocks.ore_australium), OreVeinPlacer.STONE);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private static void ore(WorldGenLevel level, RandomSource rand, int cx, int cz, int count, int size,
                            int minY, int variance, BlockState ore, java.util.function.Predicate<BlockState> target) {
        OreVeinPlacer.generateOre(level, rand, cx, cz, count, size, minY, variance, ore, target);
    }

    static BlockState state(dev.golem.core.registry.RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return block.get().defaultBlockState();
    }

    /** The old {@code rand.nextInt(n) == 0} rarity gate; {@code n <= 0} disables the feature. */
    static boolean chance(RandomSource rand, int n) {
        return n > 0 && rand.nextInt(n) == 0;
    }
}
