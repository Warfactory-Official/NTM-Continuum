package com.hbm.gametest;

import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOutgasBlocks;
import com.hbm.blocks.generic.BlockOutgas;
import com.hbm.Constants;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.decoration.ArmorStand;

/** GameTest contracts for old BlockOutgas mappings and emission triggers. */
public final class OutgasGameTests {
    private OutgasGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function RATES_AND_MAPPING =
            TESTS.function("outgas_rates_and_mapping", 20, () -> OutgasGameTests::ratesAndMapping);
    public static final GameTests.Function BREAK_EMISSION =
            TESTS.function("outgas_break_emission", 20, () -> OutgasGameTests::breakEmission);
    public static final GameTests.Function RANDOM_EMISSION =
            TESTS.function("outgas_random_emission", 20, () -> OutgasGameTests::randomEmission);
    public static final GameTests.Function WALKING_EMISSION =
            TESTS.function("outgas_walking_emission", 20, () -> OutgasGameTests::walkingEmission);
    public static final GameTests.Function NEIGHBOUR_EMISSION =
            TESTS.function("outgas_neighbour_emission", 20, () -> OutgasGameTests::neighbourEmission);
    public static final GameTests.Function ANCIENT_SPREAD =
            TESTS.function("outgas_ancient_spread", 20, () -> OutgasGameTests::ancientSpread);

    public static void init() {}

    public static void ratesAndMapping(GameTestHelper helper) {
        assertMapping(helper, ModOutgasBlocks.brick_asbestos.get(), ModNuclearBlocks.gas_asbestos.get(), 20);
        assertMapping(helper, ModOutgasBlocks.tile_lab_broken.get(), ModNuclearBlocks.gas_asbestos.get(), 40);
        assertMapping(helper, ModOutgasBlocks.deco_asbestos.get(), ModNuclearBlocks.gas_asbestos.get(), 40);
        assertMapping(helper, ModOutgasBlocks.block_asbestos.get(), ModNuclearBlocks.gas_asbestos.get(), 4);
        assertMapping(helper, ModOutgasBlocks.block_corium_cobble.get(), ModNuclearBlocks.gas_radon_dense.get(), 1);
        assertMapping(helper, ModOutgasBlocks.ancient_scrap.get(), ModNuclearBlocks.gas_radon_tomb.get(), 1);
        assertMapping(helper, ModNuclearBlocks.ore_uranium.get(), ModNuclearBlocks.gas_radon.get(), 20);
        assertMapping(helper, ModNuclearBlocks.ore_uranium_scorched.get(), ModNuclearBlocks.gas_radon.get(), 15);
        assertMapping(helper, ModNuclearBlocks.ore_gneiss_uranium.get(), ModNuclearBlocks.gas_radon.get(), 20);
        assertMapping(helper, ModNuclearBlocks.ore_gneiss_uranium_scorched.get(), ModNuclearBlocks.gas_radon.get(), 20);
        assertMapping(helper, ModNuclearBlocks.ore_nether_uranium.get(), ModNuclearBlocks.gas_radon.get(), 20);
        assertMapping(helper, ModNuclearBlocks.ore_nether_uranium_scorched.get(), ModNuclearBlocks.gas_radon.get(), 20);
        helper.succeed();
    }

    public static void breakEmission(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        level.setBlock(pos, ModOutgasBlocks.brick_asbestos.get().defaultBlockState(), 3);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pos).is(ModNuclearBlocks.gas_asbestos.get()),
                "breaking asbestos brick did not leave asbestos gas");
        helper.succeed();
    }

    public static void randomEmission(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        BlockOutgas ore = ModNuclearBlocks.ore_uranium.get();
        level.setBlock(pos, ore.defaultBlockState(), 3);
        ore.randomTick(ore.defaultBlockState(), level, pos, RandomSource.create(0x0A76A5L));

        int gasCount = 0;
        for (BlockPos neighbour : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(neighbour).is(ModNuclearBlocks.gas_radon.get())) gasCount++;
        }
        helper.assertTrue(gasCount == 1, "uranium random tick emitted " + gasCount + " radon cells");
        helper.succeed();
    }

    public static void walkingEmission(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        BlockOutgas block = ModOutgasBlocks.brick_asbestos.get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        for (int i = 0; i < 100 && !level.getBlockState(pos.above()).is(ModNuclearBlocks.gas_asbestos.get()); i++) {
            level.removeBlock(pos.above(), false);
            block.stepOn(level, pos, block.defaultBlockState(), stand);
        }
        helper.assertTrue(level.getBlockState(pos.above()).is(ModNuclearBlocks.gas_asbestos.get()),
                "walking asbestos brick did not emit an asbestos gas cell");
        helper.succeed();
    }

    public static void neighbourEmission(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        BlockOutgas block = ModOutgasBlocks.block_corium_cobble.get();
        BlockPos trigger = pos.above();
        level.setBlock(pos, block.defaultBlockState(), 3);
        for (int i = 0; i < 32; i++) {
            level.setBlock(trigger, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(trigger, Blocks.AIR.defaultBlockState(), 3);
        }
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.getBlockState(trigger).is(ModNuclearBlocks.gas_radon_dense.get()),
                    "corium cobble neighbour change did not emit dense radon");
            helper.succeed();
        });
    }

    public static void ancientSpread(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlock(pos, ModOutgasBlocks.ancient_scrap.get().defaultBlockState(), 3);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        helper.runAfterDelay(2, () -> {
            int gasCount = 0;
            for (BlockPos neighbour : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
                if (level.getBlockState(neighbour).is(ModNuclearBlocks.gas_radon_tomb.get())) gasCount++;
            }
            helper.assertTrue(gasCount > 1, "ancient scrap did not spread tomb radon after breaking");
            helper.succeed();
        });
    }

    private static void assertMapping(GameTestHelper helper, BlockOutgas block, Block expectedGas, int rate) {
        helper.assertTrue(block.getGas() == expectedGas,
                block + " mapped to " + block.getGas() + ", expected " + expectedGas);
        helper.assertTrue(block.outgasRate() == rate,
                block + " rate was " + block.outgasRate() + ", expected " + rate);
    }
}
