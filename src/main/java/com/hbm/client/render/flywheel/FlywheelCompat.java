package com.hbm.client.render.flywheel;

import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * The only class the rest of the mod talks to about Flywheel.
 *
 * <p>It is compiled on every node and names <b>no {@code dev.engine_room} type at all</b>, which is
 * what lets it be loaded unconditionally. Every such reference lives in {@link FlywheelVisuals}, a
 * file that does not exist on a node without the engine and which this class names only from inside a
 * {@code //? if flywheel} branch — so the JVM never tries to resolve those classes when the engine is
 * absent. Nothing outside this package may name a bridge class directly; that is the whole discipline.
 *
 * <p>Flywheel is optional twice over and a caller has to survive both: the mod may not be installed,
 * and even when it is the player can switch the backend off. So a Flywheel visual is never the only
 * renderer for anything — it is an acceleration for a machine that must already draw correctly through
 * its vanilla {@code CompatBER}. Gate on {@link #isAvailable} and fall through when it is false.
 *
 * <p><b>26.1 has no engine.</b> Flywheel publishes 1.20.1 and 1.21.1 builds and nothing for 26.1, so
 * {@code deps.flywheel} is blank on both 26.1 nodes and every bridge class compiles out there.
 * {@link #isSupportedOnThisVersion} is a constant-folded {@code false} on those nodes, which makes
 * {@link #registerVisuals} unreachable — and it throws rather than returning quietly, because
 * reaching it means a caller skipped the guard.
 */
public final class FlywheelCompat {

    /** Flywheel's own mod id, on every loader it ships for. */
    public static final String MOD_ID = "flywheel";

    private FlywheelCompat() {}

    /** Whether this node was built against the engine at all. Constant-folded per node. */
    public static boolean isSupportedOnThisVersion() {
        //? if flywheel {
        /*return true;
        *///?} else {
        return false;
        //?}
    }

    /**
     * Whether an instanced visual will actually be drawn for {@code level}: the engine is present,
     * this node has it, and the player has not turned the backend off.
     */
    public static boolean isAvailable(@Nullable LevelAccessor level) {
        //? if flywheel {
        /*return FlywheelVisuals.isAvailable(level);
        *///?} else {
        return false;
        //?}
    }

    /** Binds every HBM instanced visual. Call once from client setup. */
    public static void registerVisuals() {
        //? if flywheel {
        /*FlywheelVisuals.register();
        *///?} else {
        throw new UnsupportedOperationException(
                "Flywheel publishes no build for this Minecraft version, so there are no visuals to "
                        + "register. Guard the call site on FlywheelCompat.isSupportedOnThisVersion() "
                        + "and let the block entity draw through its vanilla CompatBER.");
        //?}
    }
}
