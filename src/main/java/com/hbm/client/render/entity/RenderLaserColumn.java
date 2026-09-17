package com.hbm.client.render.entity;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The beam half of 1.7 RenderDeathBlast: two nested eight-sided 250-block columns, additive and untextured.
 */
public class RenderLaserColumn<T extends Entity> extends CompatEntityRenderer<T, RenderLaserColumn.State> {

    private static final int HEIGHT = 250;

    private final int outer;
    private final int inner;

    protected RenderLaserColumn(EntityRendererProvider.Context context, int outer, int inner) {
        super(context);
        this.outer = 0xFF000000 | (outer & 0xFFFFFF);
        this.inner = 0xFF000000 | (inner & 0xFFFFFF);
        this.shadowRadius = 0F;
    }

    // 1.7: RenderDeathBlast:50,62 pass 45 to Vec3.rotateAroundY, which takes RADIANS - 58.31 deg/facet, so
    // 8 facets span 466.5 deg and ~106 deg is drawn twice, with the second column reusing the same mutated
    // vector and landing out of phase. 1.12.2 carries it unchanged. Deliberate divergence: closed prisms.
    static void column(VertexConsumer buffer, PoseStack.Pose pose, double radius, int color) {
        Vec3 vector = new Vec3(radius, 0D, 0D);
        for (int i = 0; i < 8; i++) {
            Vec3 next = vector.yRot((float) Math.toRadians(45D));
            vertex(buffer, pose, vector.x, HEIGHT, vector.z, color);
            vertex(buffer, pose, vector.x, 0D, vector.z, color);
            vertex(buffer, pose, next.x, 0D, next.z, color);
            vertex(buffer, pose, next.x, HEIGHT, next.z, color);
            vector = next;
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y, double z, int color) {
        Vertexes.color(buffer, pose, (float) x, (float) y, (float) z, color);
    }

    @Override
    public boolean culled(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extract(T entity, State state, float partialTicks) {}

    @Override
    public void submit(State state, PoseStack poseStack, SubmitSink sink, CameraInfo camera) {
        RenderType type = CompatRenderTypes.dragonRays();
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) -> column(buffer, pose, 0.5D, outer));
        sink.submitCustomGeometry(poseStack, type, (pose, buffer) -> column(buffer, pose, 0.25D, inner));
    }

    public static class State extends CompatEntityRenderState {
        float age;
    }
}
