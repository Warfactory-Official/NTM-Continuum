package com.hbm.client.render.entity;

import com.hbm.entity.logic.EntityBomber;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 1.7 RenderBomber: the Dornier for styles 0-4 and the B-29 for 5-8, each style its own skin.
 */
public final class RenderBomber extends CompatEntityRenderer<EntityBomber, RenderBomber.State> {

    public RenderBomber(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0F;
    }

    private static Identifier texture(int style) {
        return switch (style) {
            case 2 -> ResourceManager.dornier_2_tex;
            case 4 -> ResourceManager.dornier_4_tex;
            case 5 -> ResourceManager.b29_0_tex;
            case 6 -> ResourceManager.b29_1_tex;
            case 7 -> ResourceManager.b29_2_tex;
            case 8 -> ResourceManager.b29_3_tex;
            default -> ResourceManager.dornier_1_tex;
        };
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntityBomber entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityBomber entity, State state, float partialTicks) {
        state.yaw = Mth.rotLerp(partialTicks, entity.renderYawO, entity.renderYaw);
        state.pitch = Mth.lerp(partialTicks, entity.renderPitchO, entity.renderPitch);
        state.style = entity.getStyle();
        state.wobble = (float) Math.sin((entity.tickCount + partialTicks) * 0.05D) * 10F;
        state.light = state.lightCoords;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitSink sink, CameraInfo camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        pose.mulPose(Axis.ZP.rotationDegrees(90F));
        pose.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        pose.mulPose(Axis.XP.rotationDegrees(state.wobble));
        RenderType type = CompatRenderTypes.entityCutoutNoCull(texture(state.style));
        int light = state.light;
        if (state.style >= 0 && state.style <= 4) {
            pose.scale(5F, 5F, 5F);
            pose.mulPose(Axis.YP.rotationDegrees(-90F));
            sink.submitCustomGeometry(pose, type, (p, buffer) ->
                    NtmEntityModels.DORNIER.renderAll(p, buffer, light, OverlayTexture.NO_OVERLAY));
        } else {
            pose.scale(30F / 3.1F, 30F / 3.1F, 30F / 3.1F);
            pose.mulPose(Axis.YP.rotationDegrees(180F));
            sink.submitCustomGeometry(pose, type, (p, buffer) ->
                    NtmEntityModels.B29.renderAll(p, buffer, light, OverlayTexture.NO_OVERLAY));
        }
        pose.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float yaw;
        float pitch;
        float wobble;
        int style;
        int light;
    }
}
