package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntityFallingNuke;
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
 * 1.7 RenderFallingNuke: the Little Boy casing tipping onto its nose as the dive steepens, first turned by the facing
 * byte (dataWatcher 20, {@link EntityFallingNuke#getNukeMeta()}) the NukeCustom spawner wrote so it lines up with the
 * block it was dropped from.
 */
public class RenderFallingNuke extends CompatEntityRenderer<EntityFallingNuke, RenderFallingNuke.State> {

    private static final RenderType TYPE = CompatRenderTypes.entityCutout(ResourceManager.custom_nuke_tex);

    public RenderFallingNuke(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(EntityFallingNuke entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityFallingNuke entity, State state, float partialTicks) {
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        // 1.7: the pre-release pitch reads far past vertical; snap it flat rather than draw the casing inverted
        state.pitch = pitch < -80F ? 0F : pitch;
        state.meta = entity.getNukeMeta();
    }

    /** 1.7 facing switch: yaw for metas 2..5, 0 for anything else (which also skips the -2 shift). */
    private static float facingYaw(int meta) {
        return switch (meta) {
            case 2 -> 90F;
            case 4 -> 180F;
            case 3 -> 270F;
            default -> 0F;
        };
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        if (state.meta >= 2 && state.meta <= 5) {
            poseStack.mulPose(Axis.YP.rotationDegrees(facingYaw(state.meta)));
            poseStack.translate(-2.0D, 0.0D, 0.0D);
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch));
        sink.submitCustomGeometry(poseStack, TYPE, (pose, buffer) ->
                NtmEntityModels.LIL_BOY.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float pitch;
        int meta;
    }
}
