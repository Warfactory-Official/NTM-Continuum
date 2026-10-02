package com.hbm.client.render.entity;

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
import net.minecraft.world.entity.Entity;

/**
 * 1.7 RenderMinerRocket, shared by the miner lander and the Bobmazon delivery pod - same mesh, different skin, and
 * the pod hangs nose-down.
 */
public class RenderMinerRocket<T extends Entity> extends CompatEntityRenderer<T, CompatEntityRenderState> {

    private final RenderType type;
    private final boolean inverted;

    private RenderMinerRocket(EntityRendererProvider.Context context, RenderType type, boolean inverted) {
        super(context);
        this.type = type;
        this.inverted = inverted;
        this.shadowRadius = 0F;
    }

    // 1.7: ignoreFrustumCheck - the distance gate stays with the entity
    @Override
    public boolean culled(T entity) {
        return false;
    }

    public static <T extends Entity> RenderMinerRocket<T> lander(EntityRendererProvider.Context context) {
        return new RenderMinerRocket<>(context, CompatRenderTypes.entityCutoutNoCull(ResourceManager.miner_rocket_tex), false);
    }

    public static <T extends Entity> RenderMinerRocket<T> bobmazon(EntityRendererProvider.Context context) {
        return new RenderMinerRocket<>(context, CompatRenderTypes.entityCutoutNoCull(ResourceManager.bobmazon_tex), true);
    }

    @Override
    public CompatEntityRenderState createRenderState() {
        return new CompatEntityRenderState();
    }

    @Override
    public void extract(T entity, CompatEntityRenderState state, float partialTicks) {}

    @Override
    public void submit(CompatEntityRenderState state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        if (inverted) poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) ->
                NtmEntityModels.MINER_ROCKET.renderAll(pose, buffer, light, OverlayTexture.NO_OVERLAY));
        poseStack.popPose();
    }
}
