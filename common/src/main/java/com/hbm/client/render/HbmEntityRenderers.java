package com.hbm.client.render;

import com.hbm.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Cross-loader registration of HBM's entity renderers. Common defines the wiring; each loader passes a
 * {@link Sink} that performs the actual registration (NeoForge via
 * {@code EntityRenderersEvent.RegisterRenderers#registerEntityRenderer}, Fabric via
 * {@code EntityRendererRegistry.register}). Client-only.
 *
 * <p>The nuke explosion entity is invisible, so it uses vanilla's {@code NoopRenderer} — every entity
 * type still needs <em>a</em> renderer registered or the client errors when one spawns.
 */
public final class HbmEntityRenderers {

    private HbmEntityRenderers() {}

    /** Loader-provided registration callback. */
    public interface Sink {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public static void registerAll(Sink sink) {
        sink.register(ModEntities.NUKE_EXPLOSION_MK5.get(), NoopRenderer::new);
        sink.register(ModEntities.FALLOUT_RAIN.get(), NoopRenderer::new);
        sink.register(ModEntities.NUKE_TOREX.get(), TorexRenderer::new);
    }
}
