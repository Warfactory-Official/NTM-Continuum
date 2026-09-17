package com.hbm.client.render.flywheel;

//? if flywheel {

/*import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.entity.effect.EntityNukeTorex;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import dev.golem.core.render.CompatRenderState;
import dev.golem.core.render.Visuals;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

/^* Published-Flywheel bridge for the 1.20.1 and 1.21.1 nodes. ^/
final class FlywheelVisuals {

    private FlywheelVisuals() {}

    static boolean isAvailable(@Nullable LevelAccessor level) {
        return VisualizationManager.supportsVisualization(level);
    }

    /^*
     * Binds every gem-declared block entity to the engine, reading the same declarations
     * {@link HbmBlockEntityRenderers} gave the vanilla renderer rather than describing them again.
     ^/
    static void register() {
        // Torex remains on vanilla: Flywheel 1.0.6 does not preserve the required depth-writing row order.
        Visuals.ownedBy(GemFlywheelVisual::owns);
        for (HbmBlockEntityRenderers.GemDeclared<?, ?> declared : HbmBlockEntityRenderers.gemDeclared()) {
            bind(declared);
        }
    }

    private static <T extends BlockEntity, S extends CompatRenderState> void bind(
            HbmBlockEntityRenderers.GemDeclared<T, S> declared) {
        VisualizerRegistry.setVisualizer(declared.type(), new BlockEntityVisualizer<T>() {
            @Override
            public BlockEntityVisual<? super T> createVisual(VisualizationContext ctx, T blockEntity,
                                                             float partialTick) {
                return new GemFlywheelVisual<>(ctx, blockEntity, partialTick, declared.renderer().get());
            }

            /^*
             * Never. A gem renderer draws the declared tree, which this visual takes over, and
             * whatever {@code submitUndeclared} builds, which has no resident mesh and must keep being
             * submitted. Skipping the renderer would silently delete the second half;
             * {@code Visuals.owns} is the narrower question and the one gem asks.
             ^/
            @Override
            public boolean skipVanillaRender(T blockEntity) {
                return false;
            }
        });
    }

    static boolean renderTorexCloudlets(EntityNukeTorex torex, float partialTicks,
                                         Quaternionfc cameraOrientation) {
        return false;
    }
}
*///?}
