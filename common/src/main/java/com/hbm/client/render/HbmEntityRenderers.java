package com.hbm.client.render;

import com.hbm.entity.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public final class HbmEntityRenderers {

    private HbmEntityRenderers() {}

    public interface Sink {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public static void registerAll(Sink sink) {
        sink.register(ModEntities.NUKE_EXPLOSION_MK5.get(), NoopRenderer::new);
    }
}
