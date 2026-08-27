package com.hbm.client.render;

import com.hbm.Constants;
import com.hbm.entity.projectile.EntitySawblade;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Renders the ejected blade as two crossed, item-textured planes. */
public class SawbladeRenderer extends CompatEntityRenderer<EntitySawblade, SawbladeRenderState> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(Constants.MOD_ID, "textures/items/sawblade.png");

    public SawbladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SawbladeRenderState createRenderState() {
        return new SawbladeRenderState();
    }

    @Override
    public void extract(EntitySawblade entity, SawbladeRenderState state, float partialTicks) {
        state.rotation = entity.lastRotation + (entity.rotation - entity.lastRotation) * partialTicks;
        state.yaw = entity.getOrientation() % 6 * 60F;
    }

    @Override
    public void submit(SawbladeRenderState state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.scale(0.8F, 0.8F, 0.8F);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.rotation));
        collector.submitCustomGeometry(poseStack, CompatRenderTypes.entityCutout(TEXTURE),
                (pose, buffer) -> {
                    plane(pose, buffer, true);
                    plane(pose, buffer, false);
                });
        poseStack.popPose();
    }

    private static void plane(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                              boolean xAligned) {
        if (xAligned) {
            vertex(buffer, pose, -0.65F, -0.65F, 0F, 0F, 1F, 0F, 0F, 1F);
            vertex(buffer, pose, 0.65F, -0.65F, 0F, 1F, 1F, 0F, 0F, 1F);
            vertex(buffer, pose, 0.65F, 0.65F, 0F, 1F, 0F, 0F, 0F, 1F);
            vertex(buffer, pose, -0.65F, 0.65F, 0F, 0F, 0F, 0F, 0F, 1F);
        } else {
            vertex(buffer, pose, 0F, -0.65F, -0.65F, 0F, 1F, 1F, 0F, 0F);
            vertex(buffer, pose, 0F, -0.65F, 0.65F, 1F, 1F, 1F, 0F, 0F);
            vertex(buffer, pose, 0F, 0.65F, 0.65F, 1F, 0F, 1F, 0F, 0F);
            vertex(buffer, pose, 0F, 0.65F, -0.65F, 0F, 0F, 1F, 0F, 0F);
        }
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        Vertexes.vertex(buffer, pose, x, y, z, 0xFFFFFFFF, u, v,
                OverlayTexture.NO_OVERLAY, 0xF000F0, nx, ny, nz);
    }
}
