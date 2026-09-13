package com.hbm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Periodic work, spread across its period instead of landing on one tick of it.
 *
 * <p>A bare {@code getGameTime() % 20 == 0} reads the same clock in every subject, so every machine
 * of a kind does its once-a-second work on the same tick and idles for the other nineteen. The cost
 * is invisible in a test world with one machine and is the whole of the cost in a base with two
 * hundred. Nothing about the rate changes here — only which tick of each period a given subject
 * takes.
 *
 * <p>The offset comes from the <b>position</b>, never a counter. Two reasons, both load-bearing: a
 * block entity that gates client work and server work on the same period needs the two sides to
 * agree, and they can only agree on something both can derive; and a position survives a save and
 * reload where a counter does not, so a machine does not jump phase every time its chunk comes back.
 *
 * <p>An entity keys on its <b>id</b> instead, because an entity moves and its phase must not.
 */
public final class TickPhase {

    private TickPhase() {}

    /** True on one tick per period, at this block entity's own offset. */
    public static boolean every(BlockEntity be, long period) {
        Level level = be.getLevel();
        return level != null && every(level, be.getBlockPos(), period);
    }

    public static boolean every(Level level, BlockPos pos, long period) {
        return every(level.getGameTime(), pos, period);
    }

    public static boolean every(long gameTime, BlockPos pos, long period) {
        return period <= 1 || (gameTime + phase(pos, period)) % period == 0;
    }

    /** True on one tick per period, at this entity's own offset. */
    public static boolean every(Entity entity, long period) {
        return every(entity.level().getGameTime(), entity.getId(), period);
    }

    public static boolean every(long gameTime, int entityId, long period) {
        return period <= 1 || Math.floorMod(gameTime + entityId, period) == 0L;
    }

    /**
     * Adjacent positions differ by small amounts in {@code hashCode()}, and a period is usually small,
     * so the low bits have to be stirred before they are reduced or a wall of machines all land on the
     * same tick anyway.
     */
    private static long phase(BlockPos pos, long period) {
        int h = pos.hashCode();
        h ^= h >>> 16;
        h *= 0x7feb352d;
        h ^= h >>> 15;
        return Math.floorMod((long) h, period);
    }
}
