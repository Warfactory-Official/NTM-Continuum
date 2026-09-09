package com.hbm.client.render;

import com.hbm.client.render.turret.TurretResources;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The rocket in flight: the launcher's own rocket meshes, flown nose-first, wearing whichever skin the
 * loaded pod carries.
 */
public class RenderArtilleryRocket
        extends CompatEntityRenderer<EntityArtilleryRocket, RenderArtilleryRocket.State> {

    private static final Map<Identifier, RenderType> TYPES = new HashMap<>();
    private static final String[] MESH = {"RocketStandard", "RocketSingle"};

    public RenderArtilleryRocket(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityArtilleryRocket rocket, State state, float partialTicks) {
        state.yaw = Mth.rotLerp(partialTicks, rocket.yRotO, rocket.getYRot());
        state.pitch = Mth.lerp(partialTicks, rocket.xRotO, rocket.getXRot());
        HIMARSRocketType type = rocket.getRocket();
        state.mesh = MESH[Math.floorMod(type.modelType, MESH.length)];
        state.type = TYPES.computeIfAbsent(type.texture, CompatRenderTypes::entityCutout);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        RenderType type = state.type;
        String mesh = state.mesh;
        if (type == null || mesh == null) return;

        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch - 90F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90F));
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                TurretResources.HIMARS.renderPart(mesh, pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        public float yaw;
        public float pitch;
        public String mesh;
        public RenderType type;
    }
}
