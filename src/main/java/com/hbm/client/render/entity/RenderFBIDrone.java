package com.hbm.client.render.entity;

import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.main.ResourceManager;
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

/**
 * 1.7 RenderDrone: the quadcopter, yawed by a fixed per-entity angle so a swarm does not fly in lockstep.
 */
public class RenderFBIDrone extends CompatEntityRenderer<EntityFBIDrone, RenderFBIDrone.State> {

    private final RenderType type = CompatRenderTypes.entityTranslucent(ResourceManager.quadcopter_tex);

    public RenderFBIDrone(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityFBIDrone entity, State state, float partialTicks) {
        state.yaw = (float) (new Random(entity.getId()).nextDouble() * 360D);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0D, 0.25D, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                NtmEntityModels.QUADCOPTER.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY, true));
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float yaw;
    }
}
