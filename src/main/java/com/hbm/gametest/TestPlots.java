package com.hbm.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;

/**
 * States which part of its plot a test body needs entity-ticking, and checks it.
 *
 * <p>26.1 starts a test once the chunk of its 1x1x1 structure is entity-ticking, and a position a block or more away
 * can lie in the next chunk, loaded but not entity-ticking: an entity there is not returned by {@code getEntitiesOfClass}
 * and never ticks. Such tests passed or failed at random (a gun's bullets "not fired" on tick 0, a pig the reactor did
 * not see). Golem Core now holds every test declared through {@code GameTests} until all chunks of its plot (structure
 * plus padding, at least 16 blocks) are entity-ticking ({@code dev.golem.core.gametest.TestChunks}), before its first
 * tick; below 26.1 the padded template covers the room. This used to force the chunks and wait inside the body; it is
 * now only the check that the box really is ticking when the body starts, so a box outside the held plot fails with a
 * clear message instead of an entity that silently never ticked.
 */
public final class TestPlots {

    private TestPlots() {}

    /** Runs {@code body} at once, after checking that every chunk under the relative box {@code min}..{@code max} is entity-ticking. */
    public static void whenTicking(GameTestHelper helper, BlockPos min, BlockPos max, Runnable body) {
        ServerLevel level = helper.getLevel();
        BlockPos a = helper.absolutePos(min), b = helper.absolutePos(max);
        int minX = Math.min(a.getX(), b.getX()) >> 4, maxX = Math.max(a.getX(), b.getX()) >> 4;
        int minZ = Math.min(a.getZ(), b.getZ()) >> 4, maxZ = Math.max(a.getZ(), b.getZ()) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                if (!level.isPositionEntityTicking(new BlockPos(cx << 4, a.getY(), cz << 4))) {
                    helper.fail("plot chunk " + cx + ", " + cz + " is not entity-ticking at the start; the box " + min + ".." + max
                            + " lies outside the plot Golem Core holds (padding, at least 16 blocks)");
                }
            }
        }
        body.run();
    }
}
