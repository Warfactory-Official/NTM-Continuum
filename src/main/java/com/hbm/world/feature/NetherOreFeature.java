package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.config.WorldConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import static com.hbm.world.feature.OverworldOreFeature.state;

/**
 * The nether half of 1.12.2's {@code HbmWorldGen.generateOres} — everything it placed when
 * {@code dimID == -1}: the seven netherrack-hosted ores, the smoldering-netherrack surface pass, the
 * floor and roof depth deposits, and (because upstream's limestone count was not dimension-mapped)
 * limestone.
 *
 * <p><b>Upstream quirks preserved:</b>
 * <ul>
 *   <li>{@code ore_nether_cobalt} is called with the min-height and variance arguments effectively
 *       swapped relative to every other nether ore, putting it at y 100–125, up by the roof.</li>
 *   <li>Nether plutonium is behind {@link GeneralConfig#enablePlutoniumOre}, which is <b>off</b> by
 *       default, so it does not generate in an unmodified config.</li>
 *   <li>The depth deposits generate twice — once at the floor and once at y≈125 against the roof
 *       bedrock, which they are allowed to eat into.</li>
 * </ul>
 */
public class NetherOreFeature extends Feature<NoneFeatureConfiguration> {

    public NetherOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        int cx = context.origin().getX();
        int cz = context.origin().getZ();
        var netherrack = OreVeinPlacer.NETHERRACK;

        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.netherUraniumSpawn, 6, 0, 127,
                state(ModNuclearBlocks.ore_nether_uranium), netherrack);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.netherTungstenSpawn, 10, 0, 127,
                state(ModBlocks.ore_nether_tungsten), netherrack);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.netherSulfurSpawn, 12, 0, 127,
                state(ModBlocks.ore_nether_sulfur), netherrack);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.netherPhosphorusSpawn, 6, 0, 127,
                state(ModBlocks.ore_nether_fire), netherrack);
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.netherCoalSpawn, 32, 16, 96,
                state(ModOreBlocks.ore_nether_coal), netherrack);
        // UPSTREAM QUIRK, kept: (minHeight=100, variance=26) -> y 100..125, hugging the nether roof,
        // unlike every other nether vein which spans the full 0..126.
        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.netherCobaltSpawn, 6, 100, 26,
                state(ModBlocks.ore_nether_cobalt), netherrack);
        if (GeneralConfig.enablePlutoniumOre) {
            OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.netherPlutoniumSpawn, 4, 0, 127,
                    state(ModBlocks.ore_nether_plutonium), netherrack);
        }

        smolderingNetherrack(level, rand, cx, cz);
        depthDeposits(level, rand, cx, cz);
        OverworldOreFeature.limestone(level, rand, cx, cz);
        // Upstream ran the dimension-agnostic density deposit and then the Nether-specific weighted one.
        BedrockOreFeature.generateAutoChunk(level, rand, cx, cz);
        BedrockOreFeature.generateNetherChunk(level, rand, cx, cz);
        return true;
    }

    /**
     * 30 attempts per chunk: pick a column and a depth {@code d = 16 + rand.nextInt(96)}, then scan the
     * five blocks {@code d-5 … d} and convert any netherrack with air directly above it. This is what
     * paints the burning crust onto cave ceilings' undersides and open ledges.
     *
     * <p>Public only so {@code OreWorldgenGameTests} can drive it directly.
     */
    public static void smolderingNetherrack(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        BlockState smoldering = state(ModOreBlocks.ore_nether_smoldering);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 30; i++) {
            int x = cx + rand.nextInt(16);
            int z = cz + rand.nextInt(16);
            int d = 16 + rand.nextInt(96);
            for (int y = d - 5; y <= d; y++) {
                if (y < level.getMinY() || y >= level.getMaxY()) continue;
                pos.set(x, y, z);
                if (!level.getBlockState(pos).is(Blocks.NETHERRACK)) continue;
                if (!level.getBlockState(pos.above()).isAir()) continue;
                level.setBlock(pos, smoldering, 2 | 16);
            }
        }
    }

    /** Neodymium and nitan, once at the nether floor and once against the roof bedrock (y≈125). */
    private static void depthDeposits(WorldGenLevel level, RandomSource rand, int cx, int cz) {
        BlockState neodymium = state(ModOreBlocks.ore_depth_nether_neodymium);
        BlockState nitan = state(ModOreBlocks.ore_depth_nether_nitan);
        int floor = level.getMinY();

        DepthDepositPlacer.nether(level, rand, cx, cz, floor, 7, 0.6D, neodymium, 16);
        DepthDepositPlacer.nether(level, rand, cx, cz, 125, 7, 0.6D, neodymium, 16);
        DepthDepositPlacer.nether(level, rand, cx, cz, floor, 7, 0.6D, nitan, 16);
        DepthDepositPlacer.nether(level, rand, cx, cz, 125, 7, 0.6D, nitan, 16);
    }
}
