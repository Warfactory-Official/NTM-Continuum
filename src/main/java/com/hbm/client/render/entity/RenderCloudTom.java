package com.hbm.client.render.entity;

import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatEntityRenderer;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 RenderCloudTom: the moonstone blast wall - five nested sixteen-sided curtains that grow with the entity's age,
 * opaque at the base and fading out overhead. 1.7 scrolled the sheet with a GL texture-matrix translate; there is no
 * texture matrix now, so the scroll is folded into the emitted V coordinates.
 */
public class RenderCloudTom extends CompatEntityRenderer<EntityCloudTom, RenderCloudTom.State> {

    private static final RenderType TYPE = CompatRenderTypes.entityTranslucentEmissive(ResourceManager.tomblast_tex);
    private static final int SEGMENTS = 16;
    private static final int HEIGHT = 20;
    private static final int DEPTH = 20;
    private static final int SOLID = 0xFFFFFFFF;
    private static final int CLEAR = 0x00FFFFFF;

    public RenderCloudTom(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static void curtain(VertexConsumer buffer, PoseStack.Pose pose, double scale, float scroll) {
        float angle = (float) Math.toRadians(360D / SEGMENTS);

        for (int i = 0; i < SEGMENTS; i++) {
            for (int j = 0; j < 5; j++) {
                double mod = 1D - j * 0.025D;
                double h = HEIGHT + j * 10D;
                double off = 1D / j;

                Vec3 vector = new Vec3(scale, 0D, 0D).yRot(angle * i);
                double x0 = vector.x * mod;
                double z0 = vector.z * mod;
                vertex(buffer, pose, x0, h, z0, 0F, (float) (1D + off) + scroll, CLEAR);
                vertex(buffer, pose, x0, -DEPTH, z0, 0F, (float) off + scroll, SOLID);

                vector = vector.yRot(angle);
                x0 = vector.x * mod;
                z0 = vector.z * mod;
                vertex(buffer, pose, x0, -DEPTH, z0, 1F, (float) off + scroll, SOLID);
                vertex(buffer, pose, x0, h, z0, 1F, (float) (1D + off) + scroll, CLEAR);
            }
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z,
                               float u, float v, int color) {
        Vertexes.vertex(buffer, pose, (float) x, (float) y, (float) z, color, u, v, OverlayTexture.NO_OVERLAY,
                CloudRenderTypes.FULL_BRIGHT, 0F, 1F, 0F);
    }

    @Override
    public boolean culled(EntityCloudTom entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(EntityCloudTom entity, State state, float partialTicks) {
        state.scale = entity.age + partialTicks;
        state.scroll = -(entity.tickCount + partialTicks) * 0.05F;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        double scale = state.scale;
        float scroll = state.scroll;
        sink.submitCustomGeometry(poseStack, TYPE, (pose, buffer) -> curtain(buffer, pose, scale, scroll));
    }

    public static final class State extends CompatEntityRenderState {
        float scale;
        float scroll;
    }
}
