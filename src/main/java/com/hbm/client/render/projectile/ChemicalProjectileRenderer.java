package com.hbm.client.render.projectile;

import com.hbm.entity.projectile.EntityChemical;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatEntityRenderState;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.GemEntityRenderer;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Submit-API port of original-hbm's RenderChemical. */
public final class ChemicalProjectileRenderer
        extends GemEntityRenderer<EntityChemical, ChemicalProjectileRenderer.State> {

    private static final Identifier PARTICLE_TEXTURE =
            Ids.of("hbm", "textures/particle/particle_base.png");

    public ChemicalProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override protected void setup(Gem.Model<State> declaration) {
        // Gas grows to ten blocks around the entity and the beam extends along its flight path.
        declaration.offScreen();
    }

    @Override public State createRenderState() { return new State(); }

    @Override public void extract(EntityChemical entity, State state, float partialTicks) {
        state.style = entity.style();
        state.fluidColor = entity.getFluid().getColor();
        state.age = entity.tickCount;
        state.maxAge = entity.getMaxAge();
        state.motion = entity.getDeltaMovement();
        state.entityId = entity.getId();
        state.partialTicks = partialTicks;
    }

    @Override
    protected void submitUndeclared(State state, PoseStack poseStack, SubmitSink collector, CameraInfo camera) {
        float progress = (state.age + state.partialTicks) / (float) state.maxAge;
        switch (state.style) {
            case AMAT, LIGHTNING -> renderBeam(state, poseStack, collector);
            case GAS -> renderCloud(state, progress, camera, poseStack, collector, state.fluidColor);
            case GASFLAME -> {
                int rgb = Color.HSBtoRGB(Math.max((60F - progress * 100F) / 360F, 0F),
                        1F - progress * 0.25F, 1F - progress * 0.5F);
                renderCloud(state, progress, camera, poseStack, collector, rgb & 0xFFFFFF);
            }
            default -> { }
        }
    }

    private static void renderCloud(State state, float progress, CameraInfo camera, PoseStack pose,
                                    SubmitSink collector, int rgb) {
        float size = progress * (state.style == EntityChemical.ChemicalStyle.GAS ? 10F : 2F);
        float alpha = state.style == EntityChemical.ChemicalStyle.GAS
                ? Math.max(127F * (1F - progress), 0F) / 255F
                : Math.max(255F * (1F - progress), 0F) / 255F;
        Random random = new Random(state.entityId);
        int i = random.nextInt(2);
        int j = random.nextInt(2);
        float u0 = 1F - i, v0 = 1F - j, u1 = i, v1 = j;
        Vector3f right = camera.orientation.transform(new Vector3f(1F, 0F, 0F));
        Vector3f up = camera.orientation.transform(new Vector3f(0F, 1F, 0F));
        int color = (Math.round(alpha * 255F) << 24) | (rgb & 0xFFFFFF);

        collector.submitCustomGeometry(pose, CompatRenderTypes.entityTranslucent(PARTICLE_TEXTURE),
                (last, buffer) -> {
                    vertex(buffer, last, -right.x * size - up.x * size,
                            -right.y * size - up.y * size, -right.z * size - up.z * size, color, u0, v0);
                    vertex(buffer, last, right.x * size - up.x * size,
                            right.y * size - up.y * size, right.z * size - up.z * size, color, u1, v0);
                    vertex(buffer, last, right.x * size + up.x * size,
                            right.y * size + up.y * size, right.z * size + up.z * size, color, u1, v1);
                    vertex(buffer, last, -right.x * size + up.x * size,
                            -right.y * size + up.y * size, -right.z * size + up.z * size, color, u0, v1);
                });
    }

    private static void renderBeam(State state, PoseStack pose, SubmitSink collector) {
        if (state.motion.lengthSqr() <= 1.0E-7D) return;
        double length = state.motion.length() * (state.age + state.partialTicks) * 0.75D;
        Vec3 direction = state.motion.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.atan2(direction.y,
                Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-pitch - 90F));
        final float beamLength = (float) length;
        collector.submitCustomGeometry(pose, CompatRenderTypes.dragonRays(), (last, buffer) -> {
            float radius = 0.0625F;
            int near = 0x33FFFFFF;
            int far = 0x00FFFFFF;
            face(buffer, last, -radius, 0F, -radius, radius, 0F, -radius,
                    radius, beamLength, -radius, -radius, beamLength, -radius, near, far);
            face(buffer, last, -radius, 0F, radius, radius, 0F, radius,
                    radius, beamLength, radius, -radius, beamLength, radius, near, far);
            face(buffer, last, -radius, 0F, -radius, -radius, 0F, radius,
                    -radius, beamLength, radius, -radius, beamLength, -radius, near, far);
            face(buffer, last, radius, 0F, -radius, radius, 0F, radius,
                    radius, beamLength, radius, radius, beamLength, -radius, near, far);
        });
        pose.popPose();
    }

    private static void face(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             int near, int far) {
        Vertexes.color(buffer, pose, ax, ay, az, near);
        Vertexes.color(buffer, pose, bx, by, bz, near);
        Vertexes.color(buffer, pose, cx, cy, cz, far);
        Vertexes.color(buffer, pose, ax, ay, az, near);
        Vertexes.color(buffer, pose, cx, cy, cz, far);
        Vertexes.color(buffer, pose, dx, dy, dz, far);
    }

    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z, int color, float u, float v) {
        Vertexes.vertex(buffer, pose, x, y, z, color, u, v, 0, 0x00F000F0, 0F, 1F, 0F);
    }

    public static final class State extends CompatEntityRenderState {
        private EntityChemical.ChemicalStyle style = EntityChemical.ChemicalStyle.NULL;
        private int fluidColor;
        private int age;
        private int maxAge = 1;
        private int entityId;
        private float partialTicks;
        private Vec3 motion = Vec3.ZERO;
    }
}
