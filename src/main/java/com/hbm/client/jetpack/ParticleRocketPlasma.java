package com.hbm.client.jetpack;

import net.minecraft.world.phys.Vec3;

/** Five-tick fresnel plume in the glider's local space, as in the old deferred equipment layer. */
public final class ParticleRocketPlasma {
    private Vec3 position;
    private Vec3 previous;
    private final Vec3 motion;
    private final float scale;
    private final JetpackGradient gradient;
    private int age;

    public ParticleRocketPlasma(Vec3 position, Vec3 motion, float scale, JetpackGradient gradient) {
        this.position = this.previous = position;
        this.motion = motion;
        this.scale = scale;
        this.gradient = gradient;
    }

    public boolean tick() {
        if (++age >= 5) return false;
        previous = position;
        position = position.add(motion);
        return true;
    }

    public Vec3 position(float partial) { return previous.lerp(position, partial); }

    public float size(float partial) {
        float time = (age + partial) / 5F;
        return 0.1F * scale * (1F - time * time);
    }

    public float[] color(float partial) { return gradient.color((age + partial) / 5F); }
}
