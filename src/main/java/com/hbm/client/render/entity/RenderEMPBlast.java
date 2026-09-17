package com.hbm.client.render.entity;

import com.hbm.entity.effect.EntityEMPBlast;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1.7 RenderEMPBlast: the expanding ring, scaled on X/Z only by the entity's own age counter.
 */
public class RenderEMPBlast extends CompatEntityRenderer<EntityEMPBlast, RenderEMPBlast.State> {

    // 1.7: lighting and face culling both off for the flat ring
    private static final RenderType TYPE = CompatRenderTypes.entityTranslucentEmissive(ResourceManager.emp_ring_tex);

    public RenderEMPBlast(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public boolean culled(EntityEMPBlast entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityEMPBlast entity, State state, float partialTicks) {
        state.scale = entity.scale;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.scale(state.scale, 1F, state.scale);
        sink.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> NtmEntityModels.EMP_RING.renderAll(
                pose, buffer, CloudRenderTypes.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, true));
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float scale;
    }
}
