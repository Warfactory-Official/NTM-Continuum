package com.hbm.client.render.flywheel;

//? if flywheel {

/*import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.world.level.LevelAccessor;
import org.jspecify.annotations.Nullable;

/^*
 * Every {@code dev.engine_room} reference in the mod lives in this package, and this file is the
 * registry for them. The whole body is compiled out on a node with no {@code deps.flywheel}, which is
 * why {@link FlywheelCompat} may only name it from inside the matching guard.
 *
 * <p>Nothing is registered yet: the machines that would carry an instanced visual are still being
 * ported, and a visual is only ever added alongside a working vanilla {@code CompatBER}, never
 * instead of one.
 ^/
final class FlywheelVisuals {

    private FlywheelVisuals() {}

    /^*
     * The backend can be switched off at runtime ({@code flywheel:off}), in which case the manager
     * still exists but draws nothing — so this asks the manager, not the mod list.
     ^/
    static boolean isAvailable(@Nullable LevelAccessor level) {
        return VisualizationManager.supportsVisualization(level);
    }

    static void register() {
    }
}
*///?}
