package com.hbm.client.crucible;

import com.hbm.physics.V3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 1.12.2 {@code ModEventHandlerClient.firstPersonAuxParticles} as the crucible used it: {@code ParticleCrucibleSpark}
 * and {@code ParticleCrucibleLightning}, which live in the first-person blade's own space and are ticked by the client
 * and drawn only by the crucible's blade callback ({@code ItemRenderCrucible}).
 */
public final class FirstPersonParticles {

    private FirstPersonParticles() {}

    static final List<Particle> PARTICLES = new ArrayList<>();
    /** Harness only: the particles stop ticking and nothing new spawns, so two renders can be compared. */
    static boolean frozen;

    /** {@code clientTick}: {@code onUpdate} each, dropping the dead. */
    static void tick() {
        if (frozen) return;
        Iterator<Particle> it = PARTICLES.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.tick();
            if (!p.alive) it.remove();
        }
    }

    abstract static class Particle {
        /** 1.12 {@code Particle.rand}: every particle owned a {@code new Random()}. */
        final Random rand = new Random();
        double posX, posY, posZ, prevPosX, prevPosY, prevPosZ;
        double motionX, motionY, motionZ;
        int age;
        int maxAge;
        boolean alive = true;

        Particle(double x, double y, double z) {
            // 1.12 Particle(world, x, y, z): position set, prev = pos, a random lifetime and a small random motion that
            // the subclasses below overwrite
            posX = prevPosX = x;
            posY = prevPosY = y;
            posZ = prevPosZ = z;
            maxAge = (int) (4.0F / (rand.nextFloat() * 0.9F + 0.1F));
        }

        abstract void tick();
    }

    /**
     * {@code ParticleCrucibleSpark}: a quad stretched along its motion, facing the eye, drifting with {@code am}
     * plus a random motion re-picked inside a 30 degree cone every 1..6 ticks. 1.12.2 read {@code amx/amy/amz} in the
     * constructor before assigning them, so the first motion is always the random {@code ±0.0075} one.
     */
    static final class Spark extends Particle {
        final float stretch;
        final float scale;
        int timeUntilChange;
        final float amx, amy, amz;

        Spark(float stretch, float scale, double x, double y, double z, float mx, float my, float mz) {
            super(x, y, z);
            this.stretch = stretch;
            this.scale = scale;
            this.motionX = (rand.nextFloat() - 0.5) * 0.015;
            this.motionY = (rand.nextFloat() - 0.5) * 0.015;
            this.motionZ = (rand.nextFloat() - 0.5) * 0.015;
            timeUntilChange = rand.nextInt(6) + 1;
            this.amx = mx;
            this.amy = my;
            this.amz = mz;
        }

        Spark lifetime(int lifetime) {
            this.maxAge = lifetime;
            return this;
        }

        @Override
        void tick() {
            age++;
            timeUntilChange--;
            if (age >= maxAge) alive = false;
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            posX += motionX + amx;
            posY += motionY + amy;
            posZ += motionZ + amz;
            if (timeUntilChange == 0) {
                timeUntilChange = rand.nextInt(6) + 1;
                V3 am = new V3(motionX, motionY, motionZ);
                if (am.lengthSquared() != 0) {
                    V3 r = CrucibleLightning.randVecInCone(am.normalize(), 30, rand).scale(am.length());
                    motionX = r.x();
                    motionY = r.y();
                    motionZ = r.z();
                } else {
                    motionX = (rand.nextFloat() - 0.5) * 0.015;
                    motionY = (rand.nextFloat() - 0.5) * 0.015;
                    motionZ = (rand.nextFloat() - 0.5) * 0.015;
                }
            }
        }

        /** {@code renderParticle}: one quad per unit of alpha (alpha 1: one), {@code color} the caller's GL colour. */
        void render(PoseStack.Pose pose, VertexConsumer buffer, V3 playerPos, float partialTicks, int color) {
            float f5 = (float) (prevPosX + (posX - prevPosX) * partialTicks);
            float f6 = (float) (prevPosY + (posY - prevPosY) * partialTicks);
            float f7 = (float) (prevPosZ + (posZ - prevPosZ) * partialTicks);
            float mX = (float) (posX + motionX * partialTicks);
            float mY = (float) (posY + motionY * partialTicks);
            float mZ = (float) (posZ + motionZ * partialTicks);
            V3 axis = new V3(mX, mY, mZ).subtract(f5, f6, f7);
            V3 toPlayer = new V3(mX, mY, mZ).subtract(playerPos);
            V3 p1 = axis.crossProduct(toPlayer).normalize().scale(0.5 * scale);
            V3 p2 = p1.scale(-1);
            p1 = p1.add(f5, f6, f7);
            p2 = p2.add(f5, f6, f7);
            axis = axis.scale(stretch);
            quad(pose, buffer, color,
                    (float) p2.x(), (float) p2.y(), (float) p2.z(), 1, 0,
                    (float) p1.x(), (float) p1.y(), (float) p1.z(), 1, 1,
                    (float) (p1.x() + axis.x()), (float) (p1.y() + axis.y()), (float) (p1.z() + axis.z()), 0, 1,
                    (float) (p2.x() + axis.x()), (float) (p2.y() + axis.y()), (float) (p2.z() + axis.z()), 0, 0);
        }
    }

    /**
     * {@code ParticleCrucibleLightning}: a generated bolt one block long along -z (or +z), ten ticks.
     */
    static final class Lightning extends Particle {
        final CrucibleLightning.Node node;
        /**
         * Third person (port addition): the same bolt (same seed, same main chain) with its forks inside the cone
         * around the chain ({@code CrucibleLightning.generate(..., alongChain = true)}). First person draws {@link #node},
         * 1.12.2's own forks.
         */
        final CrucibleLightning.Node thirdPersonNode;

        Lightning(long worldTime, RandomSource worldRand, double x, double y, double z) {
            this(worldTime, worldRand.nextBoolean(), x, y, z);
        }

        /** With the start/end swap decided by the caller (the harness's fixed bolt). */
        Lightning(long worldTime, boolean swap, double x, double y, double z) {
            super(x, y, z);
            CrucibleLightning.Info info = new CrucibleLightning.Info();
            info.randAmount = 0.03F;
            info.forkRandAmount = 5F;
            info.forkChance = 0.3F;
            info.forkLengthRandom = 2;
            info.forkConeDegrees = 10;
            V3 start = new V3(x, y, z);
            V3 end = start.add(0, 0, -1F);
            if (swap) {
                V3 tmp = start;
                start = end;
                end = tmp;
            }
            node = CrucibleLightning.generate(worldTime, start, end, info);
            thirdPersonNode = CrucibleLightning.generate(worldTime, start, end, info, true);
        }

        Lightning lifetime(int lifetime) {
            this.maxAge = lifetime;
            return this;
        }

        @Override
        void tick() {
            age++;
            if (age > maxAge) alive = false;
        }

        /** The {@code crucible_lightning} shader's {@code time}: {@code (particleAge / 2) * 2}, "purposefully low frame rate". */
        float shaderTime() {
            return (age / 2) * 2;
        }
    }

    /** One textured, coloured quad in the entity format (fullbright, no overlay, normal up). */
    static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color,
                     float x0, float y0, float z0, float u0, float v0,
                     float x1, float y1, float z1, float u1, float v1,
                     float x2, float y2, float z2, float u2, float v2,
                     float x3, float y3, float z3, float u3, float v3) {
        int light = 0xF000F0;
        int overlay = OverlayTexture.NO_OVERLAY;
        Vertexes.vertex(buffer, pose, x0, y0, z0, color, u0, v0, overlay, light, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, x1, y1, z1, color, u1, v1, overlay, light, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, x2, y2, z2, color, u2, v2, overlay, light, 0F, 1F, 0F);
        Vertexes.vertex(buffer, pose, x3, y3, z3, color, u3, v3, overlay, light, 0F, 1F, 0F);
    }

    static @Nullable Particle any() {
        return PARTICLES.isEmpty() ? null : PARTICLES.get(0);
    }
}
