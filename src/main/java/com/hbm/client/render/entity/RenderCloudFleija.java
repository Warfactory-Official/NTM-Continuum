package com.hbm.client.render.entity;

import com.hbm.entity.effect.EntityCloudFleija;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * 1.7 RenderCloudFleija: a cyan core sphere that swells then snaps shut, three faint additive shells, and a grey
 * additive shockwave that fades with age.
 */
public class RenderCloudFleija extends CompatEntityRenderer<EntityCloudFleija, RenderCloudFleija.State> {

    // 1.7: glColor3f leaves alpha at 1
    private static final int CORE = CloudRenderTypes.rgb(0F, 1F, 1F);
    private static final int SHELL = CloudRenderTypes.rgb(0F, 0.125F, 0.125F);

    public RenderCloudFleija(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    static void submitSphere(PoseStack poseStack, SubmitSink sink, RenderType type, int color) {
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) -> NtmEntityModels.SPHERE_NEW.renderAll(
                pose, buffer, CloudRenderTypes.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color));
    }

    // 1.7: frustum skipped - shells reach past the box; the distance gate stays with the entity
    @Override
    public boolean culled(EntityCloudFleija entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityCloudFleija entity, State state, float partialTicks) {
        state.age = entity.age + partialTicks;
        state.maxAge = entity.getMaxAge();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        double baseScale = state.age * 2;
        double ageScale = baseScale / state.maxAge;

        poseStack.pushPose();
        {
            double scale = ageScale * 1.2;
            if (scale > 1) scale = Math.max(1 - (scale - 1) * 5, 0);
            scale *= 2 * baseScale;
            poseStack.scale((float) scale, (float) scale, (float) scale);

            submitSphere(poseStack, sink, CloudRenderTypes.OPAQUE, CORE);

            double outerScale = 1.05;
            for (int i = 0; i < 3; i++) {
                poseStack.scale((float) outerScale, (float) outerScale, (float) outerScale);
                submitSphere(poseStack, sink, CloudRenderTypes.ADDITIVE, SHELL);
            }
        }
        poseStack.popPose();

        poseStack.pushPose();
        {
            double shockwave = 5 * baseScale;
            poseStack.scale((float) shockwave, (float) shockwave, (float) shockwave);
            // GL clamps to [0,1] - ageScale 2 bottoms at black
            float shockTint = Mth.clamp((1F - (float) ageScale) * 0.75F, 0F, 1F);
            submitSphere(poseStack, sink, CloudRenderTypes.ADDITIVE, CloudRenderTypes.rgb(shockTint, shockTint, shockTint));
        }
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float age;
        int maxAge;
    }
}
