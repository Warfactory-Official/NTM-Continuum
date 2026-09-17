package com.hbm.client.render.entity;

import com.hbm.entity.effect.EntityCloudSolinium;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * 1.7 RenderCloudSolinium: one turquoise sphere growing a block per tick, wrapped in three faint additive shells;
 * the core is drawn with culling OFF (1.7 only re-enables GL_CULL_FACE for the shells).
 */
public class RenderCloudSolinium extends CompatEntityRenderer<EntityCloudSolinium, RenderCloudSolinium.State> {

    private static final int COLOR = 0x27FFDA; // 1.7 RenderCloudSolinium
    private static final int CORE = CloudRenderTypes.rgb(fr(COLOR), fg(COLOR), fb(COLOR));
    private static final int SHELL = CloudRenderTypes.argb(0.125F, fr(COLOR), fg(COLOR), fb(COLOR));

    public RenderCloudSolinium(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    private static float fr(int color) {
        return (color >> 16 & 0xFF) / 255F;
    }

    private static float fg(int color) {
        return (color >> 8 & 0xFF) / 255F;
    }

    private static float fb(int color) {
        return (color & 0xFF) / 255F;
    }

    @Override
    public boolean culled(EntityCloudSolinium entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityCloudSolinium entity, State state, float partialTicks) {
        state.age = entity.age + partialTicks;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.scale(state.age, state.age, state.age);

        RenderCloudFleija.submitSphere(poseStack, sink, CloudRenderTypes.OPAQUE_UNCULLED, CORE);

        double outerScale = 1.025;
        for (int i = 0; i < 3; i++) {
            poseStack.scale((float) outerScale, (float) outerScale, (float) outerScale);
            RenderCloudFleija.submitSphere(poseStack, sink, CloudRenderTypes.ADDITIVE, SHELL);
        }
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float age;
    }
}
