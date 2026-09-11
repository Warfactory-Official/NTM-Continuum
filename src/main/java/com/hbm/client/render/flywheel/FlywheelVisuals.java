package com.hbm.client.render.flywheel;

//? if flywheel {

/*import com.hbm.entity.effect.EntityNukeTorex;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.world.level.LevelAccessor;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

/^* Published-Flywheel bridge for the 1.20.1 and 1.21.1 nodes. ^/
final class FlywheelVisuals {

    private FlywheelVisuals() {}

    static boolean isAvailable(@Nullable LevelAccessor level) {
        return VisualizationManager.supportsVisualization(level);
    }

    static void register() {
        // Torex remains on vanilla: Flywheel 1.0.6 does not preserve the required depth-writing row order.
    }

    static boolean renderTorexCloudlets(EntityNukeTorex torex, float partialTicks,
                                         Quaternionfc cameraOrientation) {
        return false;
    }
}
*///?}
