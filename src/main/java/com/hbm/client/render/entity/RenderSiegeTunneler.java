package com.hbm.client.render.entity;

import com.hbm.entity.mob.siege.EntitySiegeTunneler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** The drill body pointed along its motion, the bit spinning on the wall clock. */
public class RenderSiegeTunneler extends CompatEntityRenderer<EntitySiegeTunneler, RenderSiegeTunneler.State> {

    // port: 1.7 binds siege_drill_<tier>.png, which it never shipped; its one drill skin stands in for all tiers
    private static final RenderType TYPE = CompatRenderTypes.entityCutoutNoCull(Ids.of("hbm", "textures/entity/siege_drill.png"));

    public RenderSiegeTunneler(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
        this.shadowStrength = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntitySiegeTunneler entity, State state, float partialTicks) {
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
    }

    @Override
    public void submit(State state, PoseStack ps, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;

        ps.pushPose();
        // 1.7's yOffset 0.5 drew it from the box centre
        ps.translate(0D, 0.5D, 0D);

        ps.mulPose(Axis.YP.rotationDegrees(state.yaw - 90.0F));
        ps.mulPose(Axis.ZP.rotationDegrees(state.pitch - 90.0F));

        sink.submitCustomGeometry(ps, TYPE,
                (pose, buffer) -> NtmEntityModels.TUNNELER.renderPart("Body", pose, buffer, light, OverlayTexture.NO_OVERLAY));
        ps.mulPose(Axis.YN.rotationDegrees(System.currentTimeMillis() / 3L % 360));
        sink.submitCustomGeometry(ps, TYPE,
                (pose, buffer) -> NtmEntityModels.TUNNELER.renderPart("Drill", pose, buffer, light, OverlayTexture.NO_OVERLAY));

        ps.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float yaw;
        float pitch;
    }
}
