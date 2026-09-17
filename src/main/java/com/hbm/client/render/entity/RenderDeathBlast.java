package com.hbm.client.render.entity;

import com.hbm.entity.logic.EntityDeathBlast;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1.7 RenderDeathBlast: the green and magenta pillar pair of {@link RenderLaserColumn}, plus a shrinking green core
 * sphere (0.05, 1, 0.05) wrapped in eight faint additive green shells whose opacity climbs as the blast ages out.
 */
public class RenderDeathBlast extends RenderLaserColumn<EntityDeathBlast> {

    public RenderDeathBlast(EntityRendererProvider.Context context) {
        super(context, 0x00FF00, 0xFF00FF);
    }

    @Override
    public void extract(EntityDeathBlast entity, State state, float partialTicks) {
        state.age = (entity.tickCount + partialTicks) / EntityDeathBlast.maxAge;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        super.submit(state, poseStack, sink, camera);

        float alpha = state.age;
        float scale = Math.max(10F - 10F * state.age, 0F);

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        submitSphere(poseStack, sink, CloudRenderTypes.argb(alpha, 0.05F, 1F, 0.05F), false);
        poseStack.scale(1.25F, 1.25F, 1.25F);
        for (int i = 0; i < 8; i++) {
            submitSphere(poseStack, sink, CloudRenderTypes.argb(alpha * 0.125F, 0F, 1F, 0F), true);
            poseStack.scale(1.05F, 1.05F, 1.05F);
        }
        poseStack.popPose();
    }

    private static void submitSphere(PoseStack poseStack, SubmitSink sink, int color, boolean additive) {
        sink.submitCustomGeometry(poseStack, additive ? CloudRenderTypes.ADDITIVE : CloudRenderTypes.TRANSLUCENT,
                (pose, buffer) -> NtmEntityModels.SPHERE.renderAll(pose, buffer, CloudRenderTypes.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, color));
    }
}
