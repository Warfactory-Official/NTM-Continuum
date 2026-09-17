package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntityBurningFOEQ;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import java.util.Random;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * 1.7 RenderFOEQ: the de-orbiting satellite, drawn 75 blocks below the entity so the re-entry streak reads from the
 * ground, with ten nested additive flame shells trailing it.
 */
public class RenderFOEQ extends CompatEntityRenderer<EntityBurningFOEQ, RenderFOEQ.State> {

    // 1.7:28 enables GL_CULL_FACE for the hull alone; :33 drops it again for the shells
    private static final RenderType BODY = CompatRenderTypes.entityCutoutCull(ResourceManager.sat_foeq_burning_tex);
    private static final int[] FLAME = {
            CloudRenderTypes.rgb(1F, 0.75F, 0.25F), CloudRenderTypes.rgb(1F, 0.5F, 0F),
            CloudRenderTypes.rgb(1F, 0.25F, 0F), CloudRenderTypes.rgb(1F, 0.15F, 0F)};

    public RenderFOEQ(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntityBurningFOEQ entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityBurningFOEQ entity, State state, float partialTicks) {
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
        state.seed = GameTime.millis(entity.level()) / 50L;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0D, -75D, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        sink.submitCustomGeometry(poseStack, BODY, (pose, buffer) ->
                NtmEntityModels.SAT_FOEQ_BURNING.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));

        Random rand = new Random(state.seed);
        poseStack.scale(1.15F, 0.75F, 1.15F);
        poseStack.translate(0D, -0.5D, 0.3D);
        for (int i = 0; i < 10; i++) {
            for (int shell = 0; shell < FLAME.length; shell++) {
                int color = FLAME[shell];
                poseStack.mulPose(Axis.YP.rotationDegrees(rand.nextInt(360)));
                sink.submitCustomGeometry(poseStack, CloudRenderTypes.ADDITIVE, (pose, buffer) ->
                        NtmEntityModels.SAT_FOEQ_FIRE.renderAll(pose, buffer, CloudRenderTypes.FULL_BRIGHT,
                                OverlayTexture.NO_OVERLAY, color));
                // 1.7: rise between shells, not after the last
                if (shell < FLAME.length - 1) poseStack.translate(0D, 2D, 0D);
            }
            poseStack.translate(0D, -3.8D, 0D);
            poseStack.scale(0.95F, 1.2F, 0.95F);
        }
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float yaw;
        float pitch;
        long seed;
    }
}
