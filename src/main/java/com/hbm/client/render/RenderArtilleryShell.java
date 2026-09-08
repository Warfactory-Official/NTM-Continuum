package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.client.model.WavefrontObj;
import com.hbm.entity.projectile.EntityArtilleryShell;
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

/** The shell in flight: the grenade mesh stretched fivefold along its axis and laid on the velocity. */
public class RenderArtilleryShell
        extends CompatEntityRenderer<EntityArtilleryShell, RenderArtilleryShell.State> {

    private static final WavefrontObj PROJECTILES =
            WavefrontObj.load(Ids.of(Constants.MOD_ID, "models/projectiles/projectiles.obj"));
    private static final RenderType TYPE = CompatRenderTypes.entityCutout(
            Ids.of(Constants.MOD_ID, "textures/models/projectiles/grenade.png"));

    public RenderArtilleryShell(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityArtilleryShell shell, State state, float partialTicks) {
        state.yaw = net.minecraft.util.Mth.rotLerp(partialTicks, shell.yRotO, shell.getYRot());
        state.pitch = net.minecraft.util.Mth.lerp(partialTicks, shell.xRotO, shell.getXRot());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch - 90F));
        poseStack.scale(2.5F, 5F, 2.5F);
        sink.submitCustomGeometry(poseStack, TYPE, (pose, buffer) ->
                PROJECTILES.renderPart("Grenade", pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public float yaw;
        public float pitch;
    }
}
