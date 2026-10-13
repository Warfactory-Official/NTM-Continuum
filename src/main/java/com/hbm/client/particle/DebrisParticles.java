package com.hbm.client.particle;

import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.client.ClientResourceReloads;
import dev.golem.core.client.ClientTicks;
import dev.golem.core.client.Frames;
import dev.golem.core.render.CameraInfo;
import dev.golem.core.render.Jars;
import dev.golem.core.render.Particles;
import dev.golem.core.render.WorldInAJar;
import dev.golem.core.render.WorldEffects;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The chunks of ground a large blast throws, real scoops of the terrain, tumbling.
 *
 * <p>Ported from 1.7 {@code ParticleDebris}. Each chunk carries a {@link WorldInAJar} cut from the
 * blast site and draws it as a block model rather than a sprite, so the rubble is the ground it came
 * from: its ores, its grass tint, its slab and stair shapes.
 *
 * <p>These are not vanilla particles: a particle draws one camera-facing quad from an atlas sprite, and a lump of
 * block models turned about its own middle is outside what the particle engine can express. They submit through
 * {@link WorldEffects} on the client's own tick, as the casings and skeleton pieces do.
 */
public final class DebrisParticles {

    /** 1.7's motion multiplier, applied once to the velocity a blast hands a chunk. */
    private static final double MOTION_MULT = 3D;

    private static final float GRAVITY = 0.15F;

    private static final int MAX_AGE = 100;

    /** How long a chunk passes through blocks before it starts colliding, in ticks. */
    private static final int NO_CLIP_TICKS = 5;

    /** Ticks a trailing flame puff lives. */
    private static final int TRAIL_AGE = 50;

    /**
     * How many chunks may burn at once. Upstream trails every third chunk by entity id; the count
     * here is a share of the whole spread rather than a property of one chunk, which comes to the
     * same thing and does not depend on an id a client-side chunk does not have.
     */
    private static final int TRAIL_EVERY = 3;

    private static final List<Debris> ACTIVE = new ArrayList<>();

    private static boolean initialized;

    private DebrisParticles() {}

    public static void init() {
        if (initialized) return;
        initialized = true;
        WorldEffects.init();
        ClientTicks.onEndTick(DebrisParticles::tick);
        WorldEffects.onBegin(DebrisParticles::submit);
        ClientResourceReloads.register(Constants.MOD_ID,
                "debris_particles", DebrisParticles::clear);
    }

    /**
     * Throws one chunk from {@code (x, y, z)}, cut from the ground around
     * {@code (cX, cY, cZ)}.
     *
     * <p>A scoop that came up empty is dropped rather than thrown: over water and off a cliff edge
     * there is nothing to throw, which upstream also leaves as a chunk drawing nothing.
     */
    public static void add(ClientLevel level, RandomSource rand, double x, double y, double z,
            int cX, int cY, int cZ, int size, int retry, double vx, double vy, double vz) {
        WorldInAJar jar = WorldInAJar.fromLevel(level, cX, cY, cZ, size, retry, rand);
        if (jar.isEmpty()) return;
        boolean trails = ACTIVE.size() % TRAIL_EVERY == 0;
        ACTIVE.add(new Debris(rand, x, y, z, vx, vy, vz, jar, trails));
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            clear();
            return;
        }
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            ACTIVE.get(i).tick(level);
            if (ACTIVE.get(i).expired) ACTIVE.remove(i);
        }
    }

    private static void submit(CameraInfo camera) {
        if (ACTIVE.isEmpty()) return;
        float partial = Frames.partialTick();
        for (Debris debris : List.copyOf(ACTIVE)) debris.submit(camera, partial);
    }

    private static void clear() {
        ACTIVE.clear();
    }

    private static final class Debris {

        private final WorldInAJar jar;
        private final double vx;
        private final double vz;
        private final float spinY;
        private final float spinZ;
        private final boolean trails;
        private final float trailScale;

        private double x, y, z, xo, yo, zo, vy;
        private float turnY, turnZ, turnYo, turnZo;
        private int age;
        private boolean expired;

        private Debris(RandomSource rand, double x, double y, double z, double vx, double vy, double vz,
                WorldInAJar jar, boolean trails) {
            this.x = this.xo = x;
            this.y = this.yo = y;
            this.z = this.zo = z;
            this.vx = vx * MOTION_MULT;
            this.vy = vy * MOTION_MULT;
            this.vz = vz * MOTION_MULT;
            this.jar = jar;
            this.trails = trails;
            this.trailScale = Math.max(jar.sizeY, 6) / 16F;
            this.spinY = rand.nextFloat() * 10F;
            this.spinZ = rand.nextFloat() * 10F;
        }

        private void tick(ClientLevel level) {
            xo = x;
            yo = y;
            zo = z;
            turnYo = turnY;
            turnZo = turnZ;

            age++;
            turnY += spinY;
            turnZ += spinZ;

            if (trails) {
                ParticleRocketFlame flame = new ParticleRocketFlame(level, x, y, z);
                flame.setScale(trailScale);
                flame.setMaxAge(TRAIL_AGE);
                Particles.add(flame);
            }

            vy -= GRAVITY;

            Vec3 from = new Vec3(x, y, z);
            Vec3 to = from.add(vx, vy, vz);

            if (age > NO_CLIP_TICKS) {
                BlockHitResult hit = Compat.clip(level, from, to,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE);
                if (hit.getType() != HitResult.Type.MISS) {
                    Vec3 at = hit.getLocation();
                    x = at.x;
                    y = at.y;
                    z = at.z;
                    expired = true;
                    return;
                }
            }

            x = to.x;
            y = to.y;
            z = to.z;

            if (age >= MAX_AGE) expired = true;
        }

        private void submit(CameraInfo camera, float partial) {
            PoseStack pose = new PoseStack();
            pose.translate(Mth.lerp(partial, xo, x) - camera.pos.x,
                    Mth.lerp(partial, yo, y) - camera.pos.y,
                    Mth.lerp(partial, zo, z) - camera.pos.z);
            // Upstream turns about Y then Z, which is why a chunk tumbles end over end rather than
            // spinning like a top.
            pose.mulPose(new Quaternionf()
                    .rotateY(Mth.lerp(partial, turnYo, turnY) * Mth.DEG_TO_RAD)
                    .rotateZ(Mth.lerp(partial, turnZo, turnZ) * Mth.DEG_TO_RAD));
            pose.translate(-jar.sizeX / 2F, -jar.sizeY / 2F, -jar.sizeZ / 2F);
            Jars.submit(jar, pose, 0);
        }
    }
}
