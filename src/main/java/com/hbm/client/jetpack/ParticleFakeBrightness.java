package com.hbm.client.jetpack;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Local-space fresnel light; the old fade changes its size, not its base alpha. */
public final class ParticleFakeBrightness {
    public final Vec3 position;
    public final float[] color;
    private final float scale;
    private final int lifetime;
    private int age;

    public ParticleFakeBrightness(Vec3 position, float scale, int lifetime, float[] color, float alpha) {
        this.position = position;
        this.scale = scale;
        this.lifetime = lifetime;
        this.color = new float[] {color[0], color[1], color[2], alpha};
    }

    public boolean tick() { return ++age < lifetime; }

    public float size(float partial) {
        float time = (age + partial) / lifetime;
        return 0.1F * scale * Mth.clamp(time * 2F, 0F, 1F)
                * Mth.clamp(2F - time * 2F + 0.1F, 0F, 1F);
    }
}
