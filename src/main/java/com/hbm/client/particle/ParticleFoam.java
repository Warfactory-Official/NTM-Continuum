package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.7 {@code ParticleFoam}: a foam blob that bursts upward, hangs, then settles, trailing up to 15
 * fading copies of itself. Each position draws 8/6/4 white bubbles (burst/peak/settle) scattered by a
 * position-seeded gaussian. Like 1.7 it draws at the current, uninterpolated positions.
 */
public final class ParticleFoam extends CompatParticle {

    private final List<double[]> trail = new ArrayList<>();
    private final int trailLength = 15;
    private final float baseScale = 1.0F;
    private final float maxScale = 1.5F;
    private final float buoyancy = 0.05F;
    private final float jitter = 0.15F;
    private final float drag = 0.96F;
    private final float particleGravity;
    private final int seed;
    private int explosionPhase;
    private float particleScale;
    private float particleAlpha = 1F;

    public ParticleFoam(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(ParticleRotating.PARTICLE_BASE));
        setLayer(ParticleLayer.TRANSLUCENT);
        lifetime = 60 + random.nextInt(60);
        particleGravity = 0.005F + random.nextFloat() * 0.015F;
        // 1.7 rolls an initial velocity and spread here; every 1.7 caller overwrites both with zero
        float initialVelocity = 2.0F + random.nextFloat() * 3.0F;
        double angle = random.nextDouble() * Math.PI * 2;
        double strength = random.nextDouble() * 0.5;
        xd = Math.cos(angle) * strength;
        yd = initialVelocity;
        zd = Math.sin(angle) * strength;
        particleScale = 0.3F + random.nextFloat() * 0.7F;
        seed = level.getRandom().nextInt(); // 1.7 uses getEntityId(): any per-particle constant
    }

    public ParticleFoam setMaxAge(int maxAge) {
        this.lifetime = maxAge;
        return this;
    }

    public ParticleFoam setMotion(double x, double y, double z) {
        xd = x;
        yd = y;
        zd = z;
        return this;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;

        trail.add(0, new double[] {x, y, z});
        while (trail.size() > trailLength) trail.remove(trail.size() - 1);

        age++;
        if (age == lifetime) remove();

        float phaseRatio = (float) age / (float) lifetime;
        if (phaseRatio < 0.3F) {
            explosionPhase = 0;
            if (phaseRatio < 0.15F) {
                yd += buoyancy * 6.0F;
            } else {
                yd += buoyancy * (1.0F - (phaseRatio / 0.3F)) * 2.0F;
            }
            particleScale = baseScale + (maxScale - baseScale) * (phaseRatio / 0.3F);
        } else if (phaseRatio < 0.6F) {
            explosionPhase = 1;
            yd *= 0.98F;
            particleScale = maxScale;
        } else {
            explosionPhase = 2;
            yd -= particleGravity;
            particleScale = maxScale * (1.0F - ((phaseRatio - 0.6F) / 0.4F) * 0.7F);
        }

        particleAlpha = 0.8F * (1.0F - phaseRatio * phaseRatio);

        xd += (random.nextFloat() - 0.5F) * jitter;
        zd += (random.nextFloat() - 0.5F) * jitter;

        xd *= drag;
        yd *= drag;
        zd *= drag;

        move(xd, yd, zd);

        if (onGround) remove(); // 1.7 also died in cobweb; modern particles never report it
    }

    @Override
    protected void emitQuads(QuadSink sink, float partialTick) {
        double baseX = Mth.lerp(partialTick, xo, x);
        double baseY = Mth.lerp(partialTick, yo, y);
        double baseZ = Mth.lerp(partialTick, zo, z);
        bubbles(sink, baseX, baseY, baseZ, x, y, z, particleScale, particleAlpha);
        for (int i = 1; i < trail.size(); i++) {
            double[] point = trail.get(i);
            float trailScale = particleScale * (1.0F - (float) i / trailLength);
            float trailAlpha = particleAlpha * (1.0F - (float) i / trailLength) * 0.7F;
            bubbles(sink, baseX, baseY, baseZ, point[0], point[1], point[2], trailScale, trailAlpha);
        }
    }

    private void bubbles(QuadSink sink, double baseX, double baseY, double baseZ,
                         double px, double py, double pz, float scale, float alpha) {
        Random urandom = new Random(seed + (long) (px * 100) + (long) (py * 10) + (long) pz);
        int bubbleCount = explosionPhase == 0 ? 8 : (explosionPhase == 1 ? 6 : 4);
        for (int i = 0; i < bubbleCount; i++) {
            float whiteness = 0.9F + urandom.nextFloat() * 0.1F;
            setColor(whiteness, whiteness, whiteness);
            setAlpha(alpha);
            float bubbleScale = scale * (urandom.nextFloat() * 0.5F + 0.75F);
            float offset = explosionPhase == 0 ? 0.4F : (explosionPhase == 1 ? 0.6F : 0.9F);
            float dx = (float) ((px - baseX) + urandom.nextGaussian() * offset);
            float dy = (float) ((py - baseY) + urandom.nextGaussian() * offset * 0.7F);
            float dz = (float) ((pz - baseZ) + urandom.nextGaussian() * offset);
            sink.quad(dx, dy, dz, bubbleScale);
        }
    }
}
