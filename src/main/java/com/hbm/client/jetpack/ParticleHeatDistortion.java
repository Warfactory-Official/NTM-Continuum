package com.hbm.client.jetpack;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The state portion of the old shader-backed heat particle.
 *
 * <p>It is not put in Minecraft's ordinary particle atlas: the render layer submits it through
 * {@link dev.golem.core.render.HeatDistortionRenderType}, which has the extra scene/noise/mask samplers
 * ordinary atlas particles cannot express.
 */
public final class ParticleHeatDistortion {

    public double x;
    public double y;
    public double z;
    public double prevX;
    public double prevY;
    public double prevZ;
    public final double motionX;
    public final double motionY;
    public final double motionZ;
    public final float scale;
    public final float heatAmount;
    public final float timeOffset;
    public int age;
    public final int lifetime;

    public ParticleHeatDistortion(double x, double y, double z, float scale, float heatAmount,
            int lifetime, float timeOffset, double motionX, double motionY, double motionZ) {
        this.x = this.prevX = x;
        this.y = this.prevY = y;
        this.z = this.prevZ = z;
        this.scale = scale;
        this.heatAmount = heatAmount;
        this.lifetime = lifetime;
        this.timeOffset = timeOffset;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
    }

    public boolean tick() {
        if (++age >= lifetime) return false;
        prevX = x;
        prevY = y;
        prevZ = z;
        x += motionX;
        y += motionY;
        z += motionZ;
        return true;
    }

    public float fade(float partialTick) {
        float time = (age + partialTick) / (float) lifetime;
        float end = 1F - remap(Mth.clamp(time, 0.8F, 1F), 0.8F, 1F, 0F, 1.1F);
        float start = remap(Mth.clamp(time, 0F, 0.2F), 0F, 0.2F, 0F, 1.1F);
        return Mth.clamp(end, 0F, 1F) * Mth.clamp(start, 0F, 1F);
    }

    public Vec3 position(float partialTick) {
        return new Vec3(Mth.lerp(partialTick, prevX, x), Mth.lerp(partialTick, prevY, y),
                Mth.lerp(partialTick, prevZ, z));
    }

    private static float remap(float value, float from0, float from1, float to0, float to1) {
        return to0 + (value - from0) * (to1 - to0) / (from1 - from0);
    }
}
