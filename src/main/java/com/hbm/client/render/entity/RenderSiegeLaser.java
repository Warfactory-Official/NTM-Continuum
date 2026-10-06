package com.hbm.client.render.entity;

import com.hbm.entity.projectile.EntitySiegeLaser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * 1.7 RenderSiegeLaser: a dart - a solid four-sided tip inside a fading outer envelope, with a tail that bleeds to
 * transparent. 1.7 drew the tip as GL_TRIANGLES; the additive beam pipeline is quad-topology, so each triangle is
 * emitted with its last vertex doubled.
 */
public class RenderSiegeLaser extends CompatEntityRenderer<EntitySiegeLaser, RenderSiegeLaser.State> {

    public RenderSiegeLaser(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static void dart(VertexConsumer buffer, PoseStack.Pose pose, int color) {
        int solid = color | 0xFF000000;
        int clear = color & 0x00FFFFFF;

        for (int s = -1; s <= 1; s += 2) {
            tri(buffer, pose, 6F, 0F, 0F, solid, 3F, -1F, s, clear, 3F, 1F, s, clear);
            tri(buffer, pose, 6F, 0F, 0F, solid, 3F, s, -1F, clear, 3F, s, 1F, clear);
            tri(buffer, pose, 6F, 0F, 0F, solid, 4F, -0.5F, s * 0.5F, solid, 4F, 0.5F, s * 0.5F, solid);
            tri(buffer, pose, 6F, 0F, 0F, solid, 4F, s * 0.5F, -0.5F, solid, 4F, s * 0.5F, 0.5F, solid);
        }

        for (int s = -1; s <= 1; s += 2) {
            quad(buffer, pose, 4F, s * 0.5F, -0.5F, 4F, s * 0.5F, 0.5F, 0F, s * 0.5F, 0.5F, 0F, s * 0.5F, -0.5F,
                    solid, clear);
            quad(buffer, pose, 4F, -0.5F, s * 0.5F, 4F, 0.5F, s * 0.5F, 0F, 0.5F, s * 0.5F, 0F, -0.5F, s * 0.5F,
                    solid, clear);
        }
    }

    private static void tri(VertexConsumer buffer, PoseStack.Pose pose,
                            float ax, float ay, float az, int ac,
                            float bx, float by, float bz, int bc,
                            float cx, float cy, float cz, int cc) {
        Vertexes.color(buffer, pose, ax, ay, az, ac);
        Vertexes.color(buffer, pose, bx, by, bz, bc);
        Vertexes.color(buffer, pose, cx, cy, cz, cc);
        Vertexes.color(buffer, pose, cx, cy, cz, cc);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             int near, int far) {
        Vertexes.color(buffer, pose, ax, ay, az, near);
        Vertexes.color(buffer, pose, bx, by, bz, near);
        Vertexes.color(buffer, pose, cx, cy, cz, far);
        Vertexes.color(buffer, pose, dx, dy, dz, far);
    }

    @Override
    public boolean culled(EntitySiegeLaser entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntitySiegeLaser entity, State state, float partialTicks) {
        state.yaw = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.color = entity.getColor();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        int color = state.color;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.pitch + 180F));
        poseStack.scale(-0.5F, 0.25F, 0.25F);
        sink.submitCustomGeometry(poseStack, CompatRenderTypes.additive(),
                (pose, buffer) -> dart(buffer, pose, color));
        poseStack.popPose();
    }

    public static final class State extends CompatEntityRenderState {
        float yaw;
        float pitch;
        int color;
    }
}
