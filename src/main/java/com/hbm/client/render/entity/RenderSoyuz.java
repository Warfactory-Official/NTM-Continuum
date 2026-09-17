package com.hbm.client.render.entity;

import com.hbm.entity.missile.EntitySoyuz;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** 1.7 RenderSoyuz: the launch vehicle standing on its engines, skinned by the variant byte. */
public class RenderSoyuz extends CompatEntityRenderer<EntitySoyuz, RenderSoyuz.State> {

    public RenderSoyuz(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntitySoyuz entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntitySoyuz entity, State state, float partialTicks) {
        state.skin = SoyuzMesh.wrapSkin(entity.getSkin());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        SoyuzMesh.submit(poseStack, sink, state.lightCoords, state.skin);
    }

    public static final class State extends CompatEntityRenderState {
        int skin;
    }
}
