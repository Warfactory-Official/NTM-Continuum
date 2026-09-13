package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.util.TickPhase;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a phase offset must and must not do.
 *
 * <p>It must not change the rate: a machine on a twenty-tick period still works twenty times as
 * often as one on a four-hundred-tick period, and a phase that quietly dropped or doubled a firing
 * would be a balance change wearing a performance change's clothes.
 *
 * <p>It must change the distribution: the whole point is that a hundred machines do not all take
 * their turn on the same tick. That is asserted as a share of the work landing on the busiest tick
 * of the period, which is the number the change exists to move — a revert to a bare
 * {@code getGameTime() % period} puts the entire share on one tick and fails here loudly.
 *
 * <p>And it must be derived from nothing but the subject and the period, because a block entity
 * that gates client work and server work on the same period has two sides that have to agree, and
 * the only thing both can derive is the position.
 */
public final class TickPhaseGameTests {

    private TickPhaseGameTests() {}

    private static final long[] PERIODS = {2L, 3L, 5L, 7L, 20L, 25L, 50L};
    private static final int GRID = 32;
    private static final double SPREAD_TOLERANCE = 2.0D;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function RATE = TESTS.function(
            "tick_phase_preserves_the_rate", 20, () -> TickPhaseGameTests::preservesTheRate);
    public static final GameTests.Function SPREAD = TESTS.function(
            "tick_phase_spreads_the_work", 20, () -> TickPhaseGameTests::spreadsTheWork);
    public static final GameTests.Function DERIVATION = TESTS.function(
            "tick_phase_depends_on_nothing_else", 20, () -> TickPhaseGameTests::dependsOnNothingElse);
    public static final GameTests.Function ENTITIES = TESTS.function(
            "tick_phase_buckets_entity_ids", 20, () -> TickPhaseGameTests::bucketsEntityIds);

    public static void init() {}

    private static void preservesTheRate(GameTestHelper helper) {
        BlockPos[] positions = {
                new BlockPos(0, 64, 0), new BlockPos(1, 64, 0), new BlockPos(17, 71, -3),
                new BlockPos(-40, -59, -2000), new BlockPos(1_000_000, 319, -1_000_000),
        };
        int windows = 50;
        for (long period : PERIODS) {
            for (BlockPos pos : positions) {
                for (long base : new long[]{0L, 1L, 7L, 123_456_789L}) {
                    int fired = 0;
                    for (long t = 0; t < period * windows; t++) {
                        if (TickPhase.every(base + t, pos, period)) fired++;
                    }
                    helper.assertTrue(fired == windows,
                            "period " + period + " at " + pos + " from " + base + " fired " + fired
                                    + " times in " + windows + " periods, not " + windows);
                }
            }
        }

        for (long period : new long[]{-1L, 0L, 1L}) {
            for (long t = 0; t < 8; t++) {
                helper.assertTrue(TickPhase.every(t, new BlockPos(3, 64, 5), period),
                        "a period of " + period + " must fire every tick, and did not at " + t);
            }
        }
        helper.succeed();
    }

    private static void spreadsTheWork(GameTestHelper helper) {
        for (int yBase : new int[]{64, -59}) {
            for (long period : PERIODS) {
                int[] buckets = new int[(int) period];
                int total = 0;
                for (int x = 0; x < GRID; x++) {
                    for (int z = 0; z < GRID; z++) {
                        BlockPos pos = yBase < 0
                                ? new BlockPos(-x, yBase, -z)
                                : new BlockPos(x, yBase, z);
                        for (int t = 0; t < period; t++) {
                            if (TickPhase.every(t, pos, period)) {
                                buckets[t]++;
                                total++;
                                break;
                            }
                        }
                    }
                }
                helper.assertTrue(total == GRID * GRID,
                        "every position must fire exactly once per period; period " + period
                                + " at y " + yBase + " fired for " + total + " of " + (GRID * GRID));

                int busiest = 0;
                int idle = 0;
                for (int count : buckets) {
                    busiest = Math.max(busiest, count);
                    if (count == 0) idle++;
                }
                double mean = (double) total / period;
                helper.assertTrue(idle == 0,
                        "period " + period + " at y " + yBase + " left " + idle
                                + " of its " + period + " ticks with no work at all");
                helper.assertTrue(busiest <= mean * SPREAD_TOLERANCE,
                        "period " + period + " at y " + yBase + " put " + busiest + " of " + total
                                + " on its busiest tick, over the " + (mean * SPREAD_TOLERANCE) + " allowed");
            }
        }
        helper.succeed();
    }

    private static void dependsOnNothingElse(GameTestHelper helper) {
        BlockPos pos = new BlockPos(11, 70, -23);
        for (long period : PERIODS) {
            for (long t = 0; t < period; t++) {
                boolean now = TickPhase.every(t, pos, period);
                helper.assertTrue(now == TickPhase.every(t + period * 1_000L, pos, period),
                        "the same position answered differently a thousand periods later, so two sides"
                                + " asking at different times would disagree");
            }
        }

        BlockState state = Blocks.CHEST.defaultBlockState();
        BlockPos seat = helper.absolutePos(BlockPos.ZERO);
        BlockEntity orphan = ((EntityBlock) Blocks.CHEST).newBlockEntity(seat, state);
        helper.assertTrue(orphan != null, "the test needs a block entity to hold");
        for (long period : PERIODS) {
            helper.assertFalse(TickPhase.every(orphan, period),
                    "a block entity with no level must never fire - callers fold their null check into this");
        }

        orphan.setLevel(helper.getLevel());
        long gameTime = helper.getLevel().getGameTime();
        for (long period : PERIODS) {
            helper.assertTrue(TickPhase.every(orphan, period) == TickPhase.every(gameTime, seat, period),
                    "the block-entity form disagreed with the position form at period " + period);
        }
        helper.succeed();
    }

    private static void bucketsEntityIds(GameTestHelper helper) {
        for (long period : PERIODS) {
            int[] buckets = new int[(int) period];
            int ids = 1024;
            for (int id = 0; id < ids; id++) {
                for (int t = 0; t < period; t++) {
                    if (TickPhase.every(t, id, period)) {
                        buckets[t]++;
                        break;
                    }
                }
            }
            int busiest = 0;
            for (int count : buckets) {
                helper.assertTrue(count > 0,
                        "period " + period + " left one of its ticks with no entity on it");
                busiest = Math.max(busiest, count);
            }
            helper.assertTrue(busiest <= (double) ids / period * SPREAD_TOLERANCE,
                    "period " + period + " put " + busiest + " of " + ids + " entities on one tick");
        }
        helper.succeed();
    }
}
