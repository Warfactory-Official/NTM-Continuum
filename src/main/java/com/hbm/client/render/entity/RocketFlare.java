package com.hbm.client.render.entity;

import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.Ids;
import dev.golem.core.client.Colors;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code LegoClient.renderFlare}: a rocket's engine glow, an additive fog-free billboard growing over 12 ticks. */
public final class RocketFlare {

    private static final Identifier FLARE = Ids.of(Constants.MOD_ID, "textures/particle/flare.png");
    private static final RenderType TYPE = CompatRenderTypes.flare(FLARE);

    private RocketFlare() {}

    /** The 1.7 scale for an entity this old, or 0 before its third tick. */
    public static float scale(float age, double random) {
        if (age < 2F) return 0F;
        return (float) (Math.min(5D, (age - 2F) * 0.5D) * (0.8D + random * 0.4D));
    }

    /** Shifts the pose {@code distance} blocks from the entity towards the camera, so the flare clears the model. */
    public static void towardCamera(PoseStack pose, Vec3 entity, CameraInfo camera, double distance) {
        Vec3 offset = entity.subtract(camera.pos).normalize().scale(-distance);
        pose.translate(offset.x, offset.y, offset.z);
    }

    public static void submit(PoseStack pose, SubmitSink sink, CameraInfo camera, float scale,
                              float red, float green, float blue) {
        if (scale <= 0F) return;
        pose.pushPose();
        pose.mulPose(camera.orientation);
        sink.submitCustomGeometry(pose, TYPE, (p, buffer) -> {
            quad(p, buffer, scale, red, green, blue, 0.5F);
            quad(p, buffer, scale * 0.5F, 1F, 1F, 1F, 0.75F);
        });
        pose.popPose();
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float size, float red, float green,
                             float blue, float alpha) {
        int color = Colors.colorFromFloat(alpha, red, green, blue);
        Vertexes.vertex(buffer, pose, -size, -size, 0F, color, 1F, 1F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
        Vertexes.vertex(buffer, pose, -size, size, 0F, color, 1F, 0F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
        Vertexes.vertex(buffer, pose, size, size, 0F, color, 0F, 0F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
        Vertexes.vertex(buffer, pose, size, -size, 0F, color, 0F, 1F, OverlayTexture.NO_OVERLAY, 0xF000F0, 0F, 0F, 1F);
    }
}
