package com.hbm.client.particle;

import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ParticleAmatFlash}, the {@code amat} aux effect of antimatter explosions: for ten ticks, 100 white
 * rays from the blast centre in fixed pseudo-random directions (seed 432, the rotations accumulate as the GL
 * matrix did), additively blended, growing with age while their centre alpha fades to nothing. The whole burst is
 * scaled by {@code 0.2 * scale}, where the scale is the explosion size. Submitted through Core's late world-effects
 * pass like the muke flash, since a vanilla particle is a camera-facing quad and this is world-fixed geometry.
 */
public final class AmatFlashParticles {

    private static final int MAX_AGE = 10;
    private static final int RAYS = 100;
    private static final List<Flash> ACTIVE = new ArrayList<>();
    private static boolean initialized;
    private static ClientLevel lastLevel;

    private AmatFlashParticles() {}

    public static void register() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        ClientTicks.onEndTick(AmatFlashParticles::tick);
        WorldEffects.onBegin(camera -> {
            if (Minecraft.getInstance().level == null) return;
            float partial = Frames.partialTick();
            for (Flash flash : List.copyOf(ACTIVE)) flash.submit(camera, partial);
        });
        ClientResourceReloads.register(Constants.MOD_ID, "amat_flash", ACTIVE::clear);
    }

    public static void spawn(Level level, double x, double y, double z, float scale) {
        if (!(level instanceof ClientLevel)) return;
        ACTIVE.add(new Flash(x, y, z, scale));
    }

    /** Live flashes, for the harness and tests. */
    public static int active() {
        return ACTIVE.size();
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            ACTIVE.clear();
            lastLevel = level;
        }
        if (level == null) return;
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            // 1.7 EntityFX.onUpdate: setDead once particleAge++ reaches particleMaxAge
            if (ACTIVE.get(i).age++ >= MAX_AGE) ACTIVE.remove(i);
        }
    }

    private static final class Flash {
        private final double x;
        private final double y;
        private final double z;
        private final float scale;
        private int age;

        private Flash(double x, double y, double z, float scale) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.scale = scale;
        }

        private void submit(CameraInfo camera, float partial) {
            PoseStack pose = new PoseStack();
            pose.translate(x - camera.pos.x, y - camera.pos.y, z - camera.pos.z);
            pose.scale(0.2F * scale, 0.2F * scale, 0.2F * scale);

            double intensity = (age + partial) / (double) MAX_AGE;
            float inverse = (float) (1.0D - intensity);
            float grow = (float) (intensity * 0.5F);

            Random random = new Random(432L);
            // The rotations are applied to the pose itself, not inside the geometry callback: each ray is one
            // submission at the accumulated orientation, as each 1.7 tessellator draw was.
            for (int i = 0; i < RAYS; i++) {
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
                pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
                pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));

                float vert1 = (random.nextFloat() * 20.0F + 5.0F + 10.0F) * grow;
                float vert2 = (random.nextFloat() * 2.0F + 1.0F + 2.0F) * grow;
                WorldEffects.submit(pose, CompatRenderTypes.dragonRays(), 1,
                        (drawPose, buffer) -> ray(drawPose, buffer, inverse, vert1, vert2));
            }
        }

        /** 1.7's GL_TRIANGLE_FAN: the centre and the three rim points, as three triangles. */
        private static void ray(PoseStack.Pose pose, VertexConsumer buffer, float centreAlpha, float vert1, float vert2) {
            float ax = -0.866F * vert2, az = -0.5F * vert2;
            float bx = 0.866F * vert2, bz = -0.5F * vert2;
            float cz = 1.0F * vert2;
            triangle(pose, buffer, centreAlpha, ax, vert1, az, bx, vert1, bz);
            triangle(pose, buffer, centreAlpha, bx, vert1, bz, 0F, vert1, cz);
            triangle(pose, buffer, centreAlpha, 0F, vert1, cz, ax, vert1, az);
        }

        private static void triangle(PoseStack.Pose pose, VertexConsumer buffer, float centreAlpha,
                                     float x1, float y1, float z1, float x2, float y2, float z2) {
            Vertexes.color(buffer, pose, 0F, 0F, 0F, 1F, 1F, 1F, centreAlpha);
            Vertexes.color(buffer, pose, x1, y1, z1, 1F, 1F, 1F, 0F);
            Vertexes.color(buffer, pose, x2, y2, z2, 1F, 1F, 1F, 0F);
        }
    }
}
