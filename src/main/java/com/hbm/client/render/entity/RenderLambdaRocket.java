package com.hbm.client.render.entity;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.missile.EntityRocketLambda;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code RenderLambdaRocket}: the Lambda-98 and its engine flare, pulled a block towards the eye. */
public class RenderLambdaRocket extends CompatEntityRenderer<EntityRocketLambda, RenderLambdaRocket.State> {

    public static final WavefrontObj MODEL = WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/lambda_rocket.obj"));
    public static final RenderType TYPE = CompatRenderTypes.entityCutoutCull(Ids.of(Constants.MOD_ID, "textures/models/lambda_rocket.png"));

    public RenderLambdaRocket(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public boolean culled(EntityRocketLambda entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityRocketLambda entity, State state, float partialTicks) {
        state.position = entity.getPosition(partialTicks);
        state.flare = RocketFlare.scale(entity.tickCount + partialTicks, entity.getRandom().nextDouble());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        sink.submitCustomGeometry(poseStack, TYPE, (pose, buffer) ->
                MODEL.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));

        poseStack.pushPose();
        RocketFlare.towardCamera(poseStack, state.position, camera, 1D);
        poseStack.scale(2F, 2F, 2F);
        RocketFlare.submit(poseStack, sink, camera, state.flare, 1F, 0.75F, 0.5F);
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        Vec3 position = Vec3.ZERO;
        float flare;
    }
}
