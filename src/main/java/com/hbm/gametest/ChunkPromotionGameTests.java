package com.hbm.gametest;

import com.hbm.Constants;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ChunkPromotions;
import dev.golem.core.world.Chunks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * The contract for the hook the power and fluid graphs hang their per-chunk registration on.
 *
 * <p>Asserts what no unit test can reach: that the hook exists and the announcement arrives. It is a mixin on
 * {@code ChunkMap}, which applies when its target class loads, so a clean server start proves nothing; claiming a
 * chunk and waiting for the announcement does.
 *
 * <p>The edge arithmetic (a repeat is not an edge, a fall re-arms one, one level's chunks are not another's) is
 * covered by Core's {@code RisingEdgesTest}, which needs no server.
 *
 * <p>The chunk used is derived from this test's own plot and sits 200 chunks away, so no concurrently running test
 * can claim it. The counter is armed per chunk for the same reason.
 */
public final class ChunkPromotionGameTests {

    private ChunkPromotionGameTests() {}

    /**
     * Announcements seen for chunks a test has armed. Production arms none, so the listener below
     * reaches its first map lookup only while a test is running.
     */
    private static final Map<Long, AtomicInteger> ARMED = new ConcurrentHashMap<>();

    private static final int AWAY = 200;
    private static final int TRIES = 200;
    private static final int HELD_TICKS = 40;

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function RISING_EDGE = TESTS.function(
            "chunk_promotion_announces_one_rising_edge", 900,
            () -> ChunkPromotionGameTests::announcesOneRisingEdge);

    public static void init() {
        ChunkPromotions.onRise((level, chunk) -> {
            if (ARMED.isEmpty()) return;
            AtomicInteger seen = ARMED.get(Chunks.key(chunk.getPos()));
            if (seen != null) seen.incrementAndGet();
        });
    }

    private static void announcesOneRisingEdge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);

        helper.assertTrue(ChunkPromotions.isUp(level, new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4)),
                "the chunk this test is standing in is fully accessible, so it must be recorded as such;"
                        + " the ChunkMap mixin never reached ChunkPromotions");

        int chunkX = (origin.getX() >> 4) + AWAY;
        int chunkZ = (origin.getZ() >> 4) + AWAY;
        ChunkPos far = new ChunkPos(chunkX, chunkZ);
        long key = Chunks.key(far);

        // Generated up front and on the server thread, because generation costs wall-clock time on
        // worker threads while every budget this test can express is counted in ticks - and a GameTest
        // server runs ticks as fast as it can. What follows only moves a resident chunk's status.
        level.getChunk(chunkX, chunkZ);

        AtomicInteger seen = new AtomicInteger();

        boolean[] holding = {false};
        Runnable claim = () -> {
            if (holding[0]) return;
            holding[0] = true;
            Chunks.claimForLoading(level, chunkX, chunkZ);
        };
        Runnable release = () -> {
            if (!holding[0]) return;
            holding[0] = false;
            Chunks.releaseForLoading(level, chunkX, chunkZ);
        };
        Runnable cleanup = () -> {
            ARMED.remove(key);
            release.run();
        };
        Supplier<String> state = () -> " [recorded full: " + ChunkPromotions.isUp(level, far)
                + ", chunk resident: " + (level.getChunkSource().getChunkNow(chunkX, chunkZ) != null)
                + ", announcements: " + seen.get() + "]";

        // Generating it may have left a short-lived ticket behind; wait that out so the claim below is
        // the edge under test rather than a repeat of one already recorded.
        poll(helper, TRIES, () -> !ChunkPromotions.isUp(level, far),
                () -> "the generated chunk never dropped out of full accessibility, so this test cannot"
                        + " provoke a rising edge of its own" + state.get(),
                () -> {
                    ARMED.put(key, seen);
                    claim.run();
                    poll(helper, TRIES, () -> seen.get() > 0,
                            () -> "a claimed chunk was never announced as newly full" + state.get(),
                            () -> {
                                helper.assertTrue(ChunkPromotions.isUp(level, far),
                                        "a chunk that was announced must also be recorded as up" + state.get());
                                assertSeen(helper, seen, 1, "after the first promotion", cleanup);
                                heldWithoutRepeating(helper, level, far, seen, claim, release, cleanup, state);
                            },
                            cleanup);
                },
                cleanup);
    }

    /**
     * A held chunk is reported to the hook again and again: every promotion past full, and every
     * demotion back down to it, which is what a player crossing the simulation boundary produces,
     * and none of those is a new edge. Then the release, which must be.
     */
    private static void heldWithoutRepeating(GameTestHelper helper, ServerLevel level, ChunkPos far,
                                             AtomicInteger seen, Runnable claim, Runnable release,
                                             Runnable cleanup, Supplier<String> state) {
        helper.runAfterDelay(HELD_TICKS, () -> {
            assertSeen(helper, seen, 1, "while the chunk stayed held", cleanup);
            release.run();
            poll(helper, TRIES, () -> !ChunkPromotions.isUp(level, far),
                    () -> "a released chunk was never recorded as having fallen out of full accessibility"
                            + state.get(),
                    () -> {
                        assertSeen(helper, seen, 1, "a fall is not an announcement", cleanup);
                        becomesFullAgain(helper, seen, claim, cleanup, state);
                    },
                    cleanup);
        });
    }

    /**
     * The reason this hook is not the loader's chunk-load event: a chunk that becomes fully
     * accessible a second time has to be announced a second time.
     */
    private static void becomesFullAgain(GameTestHelper helper, AtomicInteger seen, Runnable claim,
                                         Runnable cleanup, Supplier<String> state) {
        claim.run();
        poll(helper, TRIES, () -> seen.get() > 1,
                () -> "a re-claimed chunk was never announced a second time" + state.get(),
                () -> {
                    assertSeen(helper, seen, 2, "after the chunk became full again", cleanup);
                    cleanup.run();
                    helper.succeed();
                },
                cleanup);
    }

    private static void assertSeen(GameTestHelper helper, AtomicInteger seen, int expected,
                                   String when, Runnable cleanup) {
        int actual = seen.get();
        if (actual == expected) return;
        cleanup.run();
        helper.fail("expected " + expected + " announcement(s) " + when + ", saw " + actual);
    }

    private static void poll(GameTestHelper helper, int triesLeft, BooleanSupplier condition,
                             Supplier<String> stuck, Runnable then, Runnable cleanup) {
        try {
            if (condition.getAsBoolean()) {
                then.run();
                return;
            }
        } catch (RuntimeException | Error error) {
            cleanup.run();
            throw error;
        }
        if (triesLeft <= 0) {
            cleanup.run();
            helper.fail(stuck.get());
            return;
        }
        helper.runAfterDelay(2, () -> poll(helper, triesLeft - 1, condition, stuck, then, cleanup));
    }
}
