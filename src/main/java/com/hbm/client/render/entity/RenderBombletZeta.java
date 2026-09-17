package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntityBombletZeta;
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
import net.minecraft.util.Mth;

/**
 * 1.7 render/entity/projectile/RenderBombletTheta, zeta arm; its theta arm binds no entity and its sheet is absent
 * from 1.7.
 */
public class RenderBombletZeta extends CompatEntityRenderer<EntityBombletZeta, RenderBombletZeta.State> {

    private final RenderType body = CompatRenderTypes.entityCutoutCull(ResourceManager.bomblet_zeta_tex);

    public RenderBombletZeta(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntityBombletZeta entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityBombletZeta entity, State state, float partialTicks) {
        // both halves unwrapped by the entity, so a plain lerp is 1.7's
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        pose.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        // 1.7: the zeta halves the shared theta mesh
        pose.scale(0.5F, 0.5F, 0.5F);
        sink.submitCustomGeometry(pose, body, (p, buf) ->
                NtmEntityModels.BOMBLET_THETA.renderAll(p, buf, light, OverlayTexture.NO_OVERLAY));
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public float yaw;
        public float pitch;
    }
}
