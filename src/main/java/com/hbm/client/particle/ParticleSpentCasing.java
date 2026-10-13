package com.hbm.client.particle;

import com.hbm.lib.Library;
import com.hbm.particle.SpentCasing;
import dev.golem.core.render.Renderers;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.golem.core.world.Sounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Source {@code ParticleSpentCasing} physics, kept outside vanilla's billboard-only particle class. */
public final class ParticleSpentCasing {
    public static final float SCALE = 0.05F;
    private static final float SMOKE_JITTER = 0.001F;
    private static final Random RANDOM = new Random();

    public final SpentCasing config;
    public final List<SmokeNode> smokeNodes = new ArrayList<>();
    public double x, y, z, xo, yo, zo;
    public double xd, yd, zd;
    public float rotationPitch, rotationYaw, prevRotationPitch, prevRotationYaw;
    private int age;
    private final int lifetime;
    private final boolean smoking;
    private final int maxSmokeGen;
    private final double smokeLift;
    private final int nodeLife;
    private float momentumPitch, momentumYaw;
    private boolean setupDeltas;
    private double prevRenderX, prevRenderY, prevRenderZ;

    public ParticleSpentCasing(ClientLevel level, double x, double y, double z, double mx, double my, double mz,
            float momentumPitch, float momentumYaw, SpentCasing config, boolean smoking, int smokeLife,
            double smokeLift, int nodeLife) {
        this.x = this.xo = x;
        this.y = this.yo = y;
        this.z = this.zo = z;
        this.xd = mx;
        this.yd = my;
        this.zd = mz;
        this.momentumPitch = momentumPitch;
        this.momentumYaw = momentumYaw;
        this.config = config;
        this.lifetime = config.getMaxAge();
        this.smoking = smoking;
        this.maxSmokeGen = smokeLife;
        this.smokeLift = smokeLift;
        this.nodeLife = nodeLife;
    }

    public void tick(ClientLevel level) {
        xo = x;
        yo = y;
        zo = z;
        // The legacy particle marks itself dead, then still performs this tick's physics before the
        // particle engine removes it.  The group removes it after tick(), so do not return here.
        age++;
        yd -= 0.04D;
        move(level, xd, yd, zd);
        xd *= 0.98D;
        yd *= 0.98D;
        zd *= 0.98D;
        if (onGround) {
            xd *= 0.7D;
            zd *= 0.7D;
            rotationPitch = (float) (Math.floor(rotationPitch / 180F + 0.5F) * 180F);
            momentumYaw *= 0.7F;
            onGround = false;
        }
        if (age > maxSmokeGen && !smokeNodes.isEmpty()) smokeNodes.clear();
        if (smoking && age <= maxSmokeGen) {
            for (SmokeNode node : smokeNodes) {
                node.x += RANDOM.nextGaussian() * SMOKE_JITTER;
                node.z += RANDOM.nextGaussian() * SMOKE_JITTER;
                node.y += smokeLift * SCALE;
                node.alpha = Math.max(0D, node.alpha - 1D / nodeLife);
            }
            if (age < maxSmokeGen || isInWater(level)) smokeNodes.add(new SmokeNode(smokeNodes.isEmpty() ? 0D : 1D));
        }
        prevRotationPitch = rotationPitch;
        prevRotationYaw = rotationYaw;
        rotationPitch += momentumPitch;
        rotationYaw += momentumYaw;
        if (Math.abs(prevRotationPitch - rotationPitch) > 180F) prevRotationPitch += rotationPitch > prevRotationPitch ? 360F : -360F;
        if (Math.abs(prevRotationYaw - rotationYaw) > 180F) prevRotationYaw += rotationYaw > prevRotationYaw ? 360F : -360F;
    }

    private boolean onGround;

    private boolean isInWater(ClientLevel level) {
        return level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER);
    }

    private void move(ClientLevel level, double motionX, double motionY, double motionZ) {
        Vec3 initial = new Vec3(motionX, motionY, motionZ);
        AABB bounds =
                new AABB(x - width(), y, z - width(), x + width(), y + height(), z + width());
        List<VoxelShape> collisions = new ArrayList<>();
        level.getBlockCollisions(null, bounds.expandTowards(initial.x, initial.y, initial.z))
                .forEach(collisions::add);
        Vec3 collided = Entity.collideBoundingBox(null, initial, bounds, level, collisions);
        x += collided.x;
        y += collided.y;
        z += collided.z;
        onGround = initial.y != collided.y && initial.y < 0D;
        if (initial.x != collided.x) {
            xd *= -0.25D;
            momentumYaw = Math.abs(momentumYaw) > 1E-7 ? momentumYaw * -0.75F : (float) RANDOM.nextGaussian() * 10F * config.getBounceYaw();
        }
        if (initial.y != collided.y) {
            yd *= -0.5D;
            boolean fromSpeed = Math.abs(yd) > 0.04D;
            if (fromSpeed || Math.abs(momentumPitch) > 1E-7) {
                momentumPitch *= -0.75F;
                if (fromSpeed) {
                    float mult = Math.max(-1F, Math.min(1F, (float) (initial.y / 0.2F)));
                    momentumPitch += (float) RANDOM.nextGaussian() * 10F * config.getBouncePitch() * mult;
                    momentumYaw += (float) RANDOM.nextGaussian() * 10F * config.getBounceYaw() * mult;
                }
            }
            if (config.getSound() != null && Math.abs(initial.y) >= 0.2D) {
                // a bare name is one of HBM's plink sounds; an addon casing names its own sound as "ns:path"
                var sound = Sounds.get(Library.parseId(config.getSound()));
                if (sound != null) level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS,
                        SpentCasing.PLINK_LARGE.equals(config.getSound()) ? 1F : 0.5F,
                        1F + RANDOM.nextFloat() * 0.2F, false);
            }
        }
        if (initial.z != collided.z) {
            zd *= -0.25D;
            momentumYaw = Math.abs(momentumYaw) > 1E-7 ? momentumYaw * -0.75F : (float) RANDOM.nextGaussian() * 10F * config.getBounceYaw();
        }
    }

    private double width() { return SCALE * Math.max(config.getScaleX(), config.getScaleZ()); }
    private double height() { return SCALE * config.getScaleY(); }
    public boolean expired() { return age > lifetime; }
    public int age() { return age; }
    public float heightQuarter() { return (float) height() / 4F; }
    public float scaleLift() { return config.getScaleY() * 0.01F; }
    public double interpX(float partial) { return xo + (x - xo) * partial; }
    public double interpY(float partial) { return yo + (y - yo) * partial; }
    public double interpZ(float partial) { return zo + (z - zo) * partial; }
    public float interpYaw(float partial) { return prevRotationYaw + (rotationYaw - prevRotationYaw) * partial; }
    public float interpPitch(float partial) { return prevRotationPitch + (rotationPitch - prevRotationPitch) * partial; }
    public void dragSmokeNodes(double px, double py, double pz) {
        if (!setupDeltas) { prevRenderX = px; prevRenderY = py; prevRenderZ = pz; setupDeltas = true; }
        double dx = prevRenderX - px, dy = prevRenderY - py, dz = prevRenderZ - pz;
        for (SmokeNode node : smokeNodes) { node.x += dx; node.y += dy; node.z += dz; }
        prevRenderX = px; prevRenderY = py; prevRenderZ = pz;
    }
    public int light(ClientLevel level, float partial) { return Renderers.lightAt(level, BlockPos.containing(interpX(partial), interpY(partial), interpZ(partial))); }
    public static final class SmokeNode { public double x, y, z, alpha; SmokeNode(double alpha) { this.alpha = alpha; } }
}
