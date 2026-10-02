package com.hbm.client.render.entity;

import com.hbm.entity.mob.EntityBlockSpider;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatBlockRenderState;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * 1.7 RenderBlockSpider: eight OBJ legs lifting and swinging in two alternating banks, carrying the disguise block.
 */
public class RenderBlockSpider extends CompatEntityRenderer<EntityBlockSpider, RenderBlockSpider.State> {

    private static final String[] ODD = {"Leg1", "Leg3", "Leg5", "Leg7"};
    private static final String[] EVEN = {"Leg2", "Leg4", "Leg6", "Leg8"};

    private final RenderType type = CompatRenderTypes.entityCutoutNoCull(ResourceManager.blockspider_tex);

    public RenderBlockSpider(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
    }

    private void legPart(PoseStack poseStack, SubmitSink sink, int light, String part) {
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                NtmEntityModels.BLOCK_SPIDER.renderPart(part, pose, buffer, light, OverlayTexture.NO_OVERLAY));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityBlockSpider entity, State state, float partialTicks) {
        state.bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.swing = -(Mth.cos(entity.walkAnimation.position(partialTicks) * 0.6662F * 2F) * 0.4F)
                * entity.walkAnimation.speed(partialTicks) * 57.3F;
        state.disguise.extract(entity.getDisguise());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        // 1.7 ModelBlockSpider returns before the legs when the disguise block id resolves to nothing
        if (state.disguise.isEmpty()) return;

        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - state.bodyYaw));
        // ModelBlockSpider's quarter turn is rotate(90,0,-1,0), but it ran inside RendererLivingEntity's
        // scale(-1,-1,1); that flip conjugates a Y rotation into its opposite, so the turn is +90 out here
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));

        poseStack.pushPose();
        poseStack.translate(0D, state.swing * 0.005D, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.swing));
        for (String leg : ODD) legPart(poseStack, sink, light, leg);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0D, state.swing * -0.005D, 0D);
        poseStack.mulPose(Axis.YN.rotationDegrees(state.swing));
        for (String leg : EVEN) legPart(poseStack, sink, light, leg);
        poseStack.popPose();

        poseStack.pushPose();
        // 1.7's renderBlockAsItem centres the block itself, so 1.7's 0.75 lands the base at 0.25
        poseStack.translate(-0.5D, 0.25D, -0.5D);
        sink.submitBlock(poseStack, state.disguise, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public final CompatBlockRenderState disguise = new CompatBlockRenderState();
        float bodyYaw;
        float swing;
    }
}
