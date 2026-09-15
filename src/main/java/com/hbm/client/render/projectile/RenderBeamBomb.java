package com.hbm.client.render.projectile;

import com.hbm.entity.projectile.EntityB92Beam;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The original RenderBeam5 shape: eight nested blue additive open boxes. */
public final class RenderBeamBomb extends GemEntityRenderer<EntityB92Beam, RenderBeamBomb.State> {

    private static final float RADIUS = 0.175F;
    private static final float LENGTH = 2F;
    private static final int LAYERS = 8;

    public RenderBeamBomb(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void setup(Gem.Model<State> declaration) {
        declaration.offScreen();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityB92Beam entity, State state, float partialTicks) {
        state.yaw = entity.yRotO + (entity.getYRot() - entity.yRotO) * partialTicks;
        state.pitch = entity.xRotO + (entity.getXRot() - entity.xRotO) * partialTicks;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XN.rotationDegrees(state.pitch));
        collector.submitCustomGeometry(poseStack, dev.golem.core.render.CompatRenderTypes.dragonRays(),
                (pose, buffer) -> {
                    for (int layer = 0; layer <= LAYERS; layer++) {
                        float offset = RADIUS / LAYERS * layer;
                        float shade = Math.max(1F - offset * 8.333F, 0F);
                        int color = 0xFF000000 | ((int) (shade * 255F) << 16)
                                | ((int) (shade * 255F) << 8) | 0xFF;
                        wall(buffer, pose, color, offset, -offset, offset, offset);
                        wall(buffer, pose, color, -offset, -offset, offset, -offset);
                        wall(buffer, pose, color, -offset, offset, -offset, -offset);
                        wall(buffer, pose, color, offset, offset, -offset, offset);
                    }
                });
        poseStack.popPose();
    }

    private static void wall(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose, int color,
                             float x0, float y0, float x1, float y1) {
        Vertexes.color(buffer, pose, x0, y0, 0F, color);
        Vertexes.color(buffer, pose, x1, y1, 0F, color);
        Vertexes.color(buffer, pose, x1, y1, LENGTH, color);
        Vertexes.color(buffer, pose, x0, y0, LENGTH, color);
    }

    public static final class State extends dev.golem.core.render.CompatEntityRenderState {
        private float yaw;
        private float pitch;
    }
}
