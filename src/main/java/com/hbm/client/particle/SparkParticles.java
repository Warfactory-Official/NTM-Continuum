package com.hbm.client.particle;

import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.LineRenderTypes;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.WorldEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ParticleSpark}: a white, fullbright {@code GL_LINE_STRIP} three pixels wide through the particle's last
 * few motion steps, falling under gravity 0.5 and bouncing off the ground at 0.8. {@code makeSmall} keeps three steps,
 * lives two to four ticks and always starts downward.
 *
 * <p>A vanilla particle is a camera-facing quad, so like the amat flash this is its own list drawn in Core's late
 * world-effects pass, with {@code LineRenderTypes} at 1.7's {@code glLineWidth(3)}. 1.7 added the strip's vertices
 * one step behind (the newest point was never drawn); that is kept. Movement collides with block shapes along its
 * path the way {@code moveEntity} did for a 0.01 box, hitting a floor ends the fall and bounces.
 */
public final class SparkParticles {

    private static final List<Spark> ACTIVE = new ArrayList<>();
    private static boolean initialized;
    private static ClientLevel lastLevel;

    private SparkParticles() {}

    public static void register() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        ClientTicks.onEndTick(SparkParticles::tick);
        WorldEffects.onBegin(camera -> {
            if (Minecraft.getInstance().level == null || ACTIVE.isEmpty()) return;
            float partial = Frames.partialTick();
            for (Spark spark : List.copyOf(ACTIVE)) spark.submit(camera, partial);
        });
        ClientResourceReloads.register(Constants.MOD_ID, "spark_particles", ACTIVE::clear);
    }

    /** 1.7 {@code new ParticleSpark(world, x, y, z, mX, mY, mZ).makeSmall(small)}. */
    public static void spawn(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ,
                             boolean small) {
        ACTIVE.add(new Spark(level.getRandom(), x, y, z, mX, mY, mZ, small));
        spawned++;
    }

    /** Sparks made since start, for the harness (a small spark lives two to four ticks). */
    public static int spawned;

    /** Live sparks, for the harness. */
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
            if (!ACTIVE.get(i).tick(level)) ACTIVE.remove(i);
        }
    }

    private static final class Spark {
        private final List<double[]> steps = new ArrayList<>();
        private int thresh;
        private int age;
        private int maxAge;
        private double x, y, z, prevX, prevY, prevZ;
        private double motionX, motionY, motionZ;
        private static final float GRAVITY = 0.5F;

        private Spark(RandomSource rand, double x, double y, double z, double mX, double mY, double mZ, boolean small) {
            this.x = this.prevX = x;
            this.y = this.prevY = y;
            this.z = this.prevZ = z;
            // 1.7 EntityFX randomised the motion it was handed; ParticleSpark kept it (it records motionX/Y/Z as the
            // first step), so this uses 1.7's EntityFX spread: (m + (rand*2-1)*0.4), normalised to a random speed.
            double sx = mX + (rand.nextDouble() * 2.0D - 1.0D) * 0.4D;
            double sy = mY + (rand.nextDouble() * 2.0D - 1.0D) * 0.4D;
            double sz = mZ + (rand.nextDouble() * 2.0D - 1.0D) * 0.4D;
            double speed = (rand.nextDouble() + rand.nextDouble() + 1.0D) * 0.15D;
            double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
            this.motionX = sx / len * speed * 0.4D;
            this.motionY = sy / len * speed * 0.4D + 0.1D;
            this.motionZ = sz / len * speed * 0.4D;
            this.thresh = 4 + rand.nextInt(3);
            this.maxAge = 20 + rand.nextInt(10);
            if (small) {
                this.thresh = 3;
                this.maxAge = 2 + rand.nextInt(3);
                this.motionY = -Math.abs(this.motionY);
            }
            steps.add(new double[]{motionX, motionY, motionZ});
        }

        /** 1.7 {@code onUpdate}; false once dead. */
        private boolean tick(ClientLevel level) {
            prevX = x;
            prevY = y;
            prevZ = z;
            if (age++ >= maxAge) return false;

            steps.add(new double[]{motionX, motionY, motionZ});
            while (steps.size() > thresh) steps.remove(0);

            motionY -= 0.04D * GRAVITY;
            double lastY = motionY;
            Vec3 from = new Vec3(x, y, z);
            Vec3 to = from.add(motionX, motionY, motionZ);
            net.minecraft.world.entity.Entity viewer = Minecraft.getInstance().player;
            if (viewer == null) return false;
            // the viewer only supplies the collision context (scaffolding and the like); block shapes are what count
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
            boolean onGround = false;
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 at = hit.getLocation();
                x = at.x;
                y = at.y;
                z = at.z;
                switch (hit.getDirection().getAxis()) {
                    case X -> motionX = 0;
                    case Z -> motionZ = 0;
                    case Y -> {
                        onGround = hit.getDirection() == net.minecraft.core.Direction.UP;
                        motionY = 0;
                    }
                }
            } else {
                x = to.x;
                y = to.y;
                z = to.z;
            }
            // ParticleSpark replaced EntityFX.onUpdate outright: no air friction, no ground friction
            if (onGround) motionY = -lastY * 0.8D;
            return true;
        }

        private void submit(CameraInfo camera, float partial) {
            if (steps.size() < 2) return;
            double px = prevX + (x - prevX) * partial;
            double py = prevY + (y - prevY) * partial;
            double pz = prevZ + (z - prevZ) * partial;
            PoseStack pose = new PoseStack();
            pose.translate(px - camera.pos.x, py - camera.pos.y, pz - camera.pos.z);
            List<double[]> points = new ArrayList<>();
            double[] prev = {0, 0, 0};
            for (int i = 1; i < steps.size(); i++) {
                points.add(prev);
                prev = new double[]{prev[0] + steps.get(i)[0], prev[1] + steps.get(i)[1], prev[2] + steps.get(i)[2]};
            }
            if (points.size() < 2) return; // a one-vertex strip draws nothing
            WorldEffects.submit(pose, LineRenderTypes.lines(3F), 2, (last, buffer) -> strip(last, buffer, points));
        }

        /** The strip as line segments; the normal is the segment direction, as vanilla's line shader wants. */
        private static void strip(PoseStack.Pose pose, VertexConsumer buffer, List<double[]> points) {
            for (int i = 0; i + 1 < points.size(); i++) {
                double[] a = points.get(i), b = points.get(i + 1);
                float nx = (float) (b[0] - a[0]), ny = (float) (b[1] - a[1]), nz = (float) (b[2] - a[2]);
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len <= 0) continue;
                nx /= len;
                ny /= len;
                nz /= len;
                Vertexes.line(buffer, pose, (float) a[0], (float) a[1], (float) a[2], 0xFFFFFFFF, nx, ny, nz, 3F);
                Vertexes.line(buffer, pose, (float) b[0], (float) b[1], (float) b[2], 0xFFFFFFFF, nx, ny, nz, 3F);
            }
        }
    }
}
