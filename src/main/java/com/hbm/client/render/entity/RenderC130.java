package com.hbm.client.render.entity;

import com.hbm.entity.logic.EntityC130;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
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
 * 1.7 render/entity/item/RenderC130: the c130 hull plus four wall-clock-spun props about their hub pivots.
 */
public class RenderC130 extends CompatEntityRenderer<EntityC130, RenderC130.State> {

    private static final String[] PROP = {"Prop1", "Prop2", "Prop3", "Prop4"};
    // 1.7: prop hub pivots in mesh units
    private static final double[][] PROPS = {
            {10, 4.2, -20.5},
            {10, 4.2, -11.16},
            {10, 4.2, 11.16},
            {10, 4.2, 20.5},
    };
    private final RenderType body = CompatRenderTypes.entityCutoutCull(ResourceManager.c130_0_tex);

    public RenderC130(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntityC130 entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityC130 entity, State state, float partialTicks) {
        state.yaw = Mth.rotLerp(partialTicks, entity.renderYawO, entity.renderYaw);
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
        state.spin = (float) (GameTime.now() * 15D % 360D);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.mulPose(Axis.ZP.rotationDegrees(state.pitch));

        sink.submitCustomGeometry(pose, body, (p, buf) ->
                NtmEntityModels.C130.renderPart("Plane", p, buf, light, OverlayTexture.NO_OVERLAY));

        for (int i = 0; i < 4; i++) {
            double[] pivot = PROPS[i];
            String part = PROP[i];
            pose.pushPose();
            pose.translate(pivot[0], pivot[1], pivot[2]);
            pose.mulPose(Axis.XP.rotationDegrees(state.spin));
            pose.translate(-pivot[0], -pivot[1], -pivot[2]);
            sink.submitCustomGeometry(pose, body, (p, buf) ->
                    NtmEntityModels.C130.renderPart(part, p, buf, light, OverlayTexture.NO_OVERLAY));
            pose.popPose();
        }

        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public float yaw;
        public float pitch;
        public float spin;
    }
}
