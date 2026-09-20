package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.config.MobConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionSavedData;
import com.hbm.handler.pollution.PollutionSavedData.PollutionData;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.pollution.PollutionWorldHandler;
import net.minecraft.world.level.ChunkPos;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * The pollution field: its region granularity, the store round trip, the diffusion pass and the blight.
 *
 * <p>Every world-facing assertion is scoped to the test's own position or driven against a synthetic
 * {@link PollutionSavedData}. A pollution region is 64 blocks on a side and gametest plots are much
 * closer together than that, so two plots routinely share one region - asserting an absolute level
 * read out of the live store would be asserting on a neighbouring test's soot.
 */
public final class PollutionGameTests {

    private PollutionGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function REGIONS =
            TESTS.function("pollution_regions", 20, () -> PollutionGameTests::regions);
    public static final GameTests.Function STORE =
            TESTS.function("pollution_store", 20, () -> PollutionGameTests::store);
    public static final GameTests.Function DIFFUSION =
            TESTS.function("pollution_diffusion", 20, () -> PollutionGameTests::diffusion);
    public static final GameTests.Function BLIGHT =
            TESTS.function("pollution_blight", 20, () -> PollutionGameTests::blight);

    public static void init() {}

    /** 1.7 keys on {@code x >> 6}: four chunks a side, not one. */
    public static void regions(GameTestHelper helper) {
        helper.assertTrue(PollutionSavedData.region(0, 0).equals(PollutionSavedData.region(63, 63)),
                "a 64-block region must cover its whole square");
        helper.assertTrue(!PollutionSavedData.region(0, 0).equals(PollutionSavedData.region(64, 0)),
                "block 64 belongs to the next region");
        helper.assertTrue(!PollutionSavedData.region(0, 0).equals(PollutionSavedData.region(0, 64)),
                "the region key must separate Z as well as X");
        helper.assertTrue(!PollutionSavedData.region(-1, -1).equals(PollutionSavedData.region(0, 0)),
                "arithmetic shift must not fold negative coordinates into region 0");
        helper.succeed();
    }

    /**
     * {@code setPollution} writes verbatim; {@code incrementPollution} scales by the config multiplier.
     * Asserted as a delta, because a neighbouring plot shares this region.
     */
    public static void store(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 100F);
        helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT) == 100F,
                "setPollution must write the value it was given, unscaled");

        PollutionHandler.incrementPollution(helper.getLevel(), pos, PollutionType.SOOT, 10F);
        float expected = (float) (100F + 10F * MobConfig.pollutionMult);
        helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT) == expected,
                "incrementPollution must scale by MobConfig.pollutionMult");

        PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 0F);
        PollutionHandler.decrementPollution(helper.getLevel(), pos, PollutionType.SOOT, 1F);
        helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT) == 0F,
                "pollution must clamp at zero rather than going negative");

        PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 999_999F);
        PollutionHandler.incrementPollution(helper.getLevel(), pos, PollutionType.SOOT, 1F);
        helper.assertTrue(PollutionHandler.getPollution(helper.getLevel(), pos, PollutionType.SOOT)
                        == PollutionHandler.MAX_POLLUTION,
                "pollution must clamp at MAX_POLLUTION");

        PollutionHandler.setPollution(helper.getLevel(), pos, PollutionType.SOOT, 0F);
        helper.succeed();
    }

    /** One pass decays the source and hands a share to each of the four orthogonal neighbours. */
    public static void diffusion(GameTestHelper helper) {
        PollutionSavedData saved = new PollutionSavedData();
        ChunkPos origin = new ChunkPos(0, 0);
        PollutionData seed = new PollutionData();
        seed.pollution[PollutionType.SOOT.ordinal()] = 1000F;
        seed.pollution[PollutionType.HEAVYMETAL.ordinal()] = 500F;
        saved.pollution.put(origin, seed);

        PollutionHandler.diffuse(saved);

        PollutionData self = saved.pollution.get(origin);
        helper.assertTrue(self != null, "the source region must survive a diffusion pass");
        // 1000 -> *0.8 (spread) -> *0.99 (decay)
        helper.assertTrue(Math.abs(self.get(PollutionType.SOOT) - 792F) < 0.01F,
                "soot above 10 decays by the spread factor and then the decay factor, got "
                        + self.get(PollutionType.SOOT));
        helper.assertTrue(Math.abs(self.get(PollutionType.HEAVYMETAL) - 499.75F) < 0.01F,
                "heavy metal decays by 0.9995 and never spreads, got " + self.get(PollutionType.HEAVYMETAL));

        for (ChunkPos neighbour : new ChunkPos[]{new ChunkPos(1, 0), new ChunkPos(-1, 0),
                new ChunkPos(0, 1), new ChunkPos(0, -1)}) {
            PollutionData data = saved.pollution.get(neighbour);
            helper.assertTrue(data != null, "each orthogonal neighbour must receive a share");
            helper.assertTrue(Math.abs(data.get(PollutionType.SOOT) - 50F) < 0.01F,
                    "a neighbour receives 5% of the pre-decay soot, got " + data.get(PollutionType.SOOT));
            helper.assertTrue(data.get(PollutionType.HEAVYMETAL) == 0F, "heavy metal must not spread");
        }
        helper.assertTrue(saved.pollution.get(new ChunkPos(1, 1)) == null,
                "diffusion is orthogonal only - no diagonal neighbour");

        // Soot at or below the spread threshold decays but does not spread.
        PollutionSavedData quiet = new PollutionSavedData();
        PollutionData small = new PollutionData();
        small.pollution[PollutionType.SOOT.ordinal()] = 5F;
        quiet.pollution.put(origin, small);
        PollutionHandler.diffuse(quiet);
        helper.assertTrue(quiet.pollution.get(new ChunkPos(1, 0)) == null,
                "soot at or below 10 must not spread");
        helper.succeed();
    }

    /** Poison past the threshold turns ground cover to coarse dirt and strips the plants. */
    public static void blight(GameTestHelper helper) {
        helper.assertTrue(RadiationConfig.enablePollution, "pollution must be enabled for the blight to run");

        BlockPos grass = new BlockPos(1, 1, 1);
        BlockPos fern = new BlockPos(2, 1, 1);
        helper.setBlock(grass, Blocks.GRASS_BLOCK);
        helper.setBlock(fern, Blocks.FERN);

        PollutionWorldHandler.destroyBlock(helper.getLevel(), helper.absolutePos(grass));
        PollutionWorldHandler.destroyBlock(helper.getLevel(), helper.absolutePos(fern));

        helper.assertBlockPresent(Blocks.COARSE_DIRT, grass);
        helper.assertBlockPresent(Blocks.AIR, fern);

        // Stone is not ground cover and must be left alone.
        BlockPos stone = new BlockPos(3, 1, 1);
        helper.setBlock(stone, Blocks.STONE);
        PollutionWorldHandler.destroyBlock(helper.getLevel(), helper.absolutePos(stone));
        helper.assertBlockPresent(Blocks.STONE, stone);
        helper.succeed();
    }
}
