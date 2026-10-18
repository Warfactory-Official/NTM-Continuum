package com.hbm.client.render.entity;

import com.hbm.entity.missile.EntitySoyuzCapsule;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1.7 RenderSoyuzCapsule: the returning lander swinging under its chute, pivoting seven blocks up on two phase-offset
 * sines of world time.
 */
public class RenderSoyuzCapsule extends CompatEntityRenderer<EntitySoyuzCapsule, RenderSoyuzCapsule.State> {

    private static final RenderType LANDER = CompatRenderTypes.entityCutout(ResourceManager.soyuz_lander_tex);
    private static final RenderType CHUTE = CompatRenderTypes.entityCutout(ResourceManager.soyuz_chute_tex);
    private static final int PIVOT = 7;

    public RenderSoyuzCapsule(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntitySoyuzCapsule entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntitySoyuzCapsule entity, State state, float partialTicks) {
        state.time = (entity.level().getGameTime() * 0.05D) % (Math.PI * 2D) + partialTicks * 0.05D;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0D, PIVOT, 0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) (Math.sin(state.time) * 5D)));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (Math.sin(state.time + Math.PI * 0.5D) * 5D)));
        poseStack.translate(0D, -PIVOT, 0D);
        sink.submitCustomGeometry(poseStack, LANDER, (pose, buffer) ->
                NtmEntityModels.SOYUZ_LANDER.renderPart("Capsule", pose, buffer, light, OverlayTexture.NO_OVERLAY));
        sink.submitCustomGeometry(poseStack, CHUTE, (pose, buffer) ->
                NtmEntityModels.SOYUZ_LANDER.renderPart("Chute", pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        double time;
    }
}
