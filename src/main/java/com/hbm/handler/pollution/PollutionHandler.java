package com.hbm.handler.pollution;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The surface of ntm-next's {@code PollutionHandler} that the glyphid AI reads: the per-chunk soot
 * level that raises hive aggression and the rampant-mode target the scouts home on.
 *
 * <p>This port has no pollution system yet (ntm-next's is a per-chunk saved-data simulation fed by
 * chimneys, engines and explosions), so the readings are the un-polluted defaults: no soot anywhere
 * and no rampant target. The glyphids therefore behave as they do in a clean 1.7 world. Recorded in the
 * entity port manifest; the pollution wave replaces this class in place.
 */
public final class PollutionHandler {

    private PollutionHandler() {}

    /** 1.7 {@code PollutionHandler.SOOT_PER_SECOND}, the smokestack baseline the glyphid stats scale against. */
    public static final float SOOT_PER_SECOND = 1F / 25F;

    /** Rampant-mode guidance target for the scouts; null until the pollution system sets one. */
    public static @Nullable BlockPos targetCoords = null;

    /** The pollution of {@code type} in the chunk holding {@code pos}; 0 until the pollution system exists. */
    public static float getPollution(Level level, BlockPos pos, PollutionType type) {
        return 0F;
    }
}
