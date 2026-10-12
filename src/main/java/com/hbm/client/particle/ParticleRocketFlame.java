package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * One puff of a missile's exhaust trail — 1.12.2's {@code ParticleRocketFlame}.
 *
 * <p><b>This is not one quad.</b> Each puff draws <b>ten</b>, scattered by a Gaussian whose spread
 * grows with age, from a {@link Random} re-seeded with the particle's fixed seed every frame — so the
 * cloud is stable in the world rather than boiling, and the offsets must be recomputed at render time
 * rather than stored. {@code CompatParticle.emitQuads} is the seam that allows it.
 *
 * <p><b>The {@code scale} scales the whole cloud, not one quad.</b> Upstream multiplies three separate
 * things by it — how far the ten quads scatter, how big each one is, and a fixed quarter-block offset
 * on X and Z — so a scale of five is a fireball rather than five contrail puffs. That offset biases the
 * cloud off the spawn point the same way the {@code -1} on each Gaussian does.
 */
public class ParticleRocketFlame extends CompatParticle {

    private static final int QUADS = 10;

    private final int seed;
    private float customRed;
    private float customGreen;
    private float customBlue;

    public ParticleRocketFlame(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(ParticleRotating.PARTICLE_BASE));
        this.lifetime = 300 + this.random.nextInt(50);
        this.seed = this.random.nextInt();
        this.setQuadSize(1F);
        this.setLayer(ParticleLayer.EMISSIVE);
        this.hasPhysics = false;
    }

    public ParticleRocketFlame setMaxAge(int maxAge) {
        this.lifetime = maxAge;
        return this;
    }

    public ParticleRocketFlame setMotion(double x, double y, double z) {
        this.xd = x;
        this.yd = y;
        this.zd = z;
        return this;
    }

    public ParticleRocketFlame setScale(float scale) {
        this.setQuadSize(scale);
        return this;
    }

    /**
     * hbm-space's custom puff colour, an addon API (NTM: Space's DCM geyser): each non-zero channel replaces the
     * default heat colour's (red 1, green 0.6, blue 0) under the same fade and per-quad brightening; a zero channel
     * keeps the default, as in hbm-space. Unset, the puff is 1.7's.
     */
    public ParticleRocketFlame setCustomColor(float red, float green, float blue) {
        this.customRed = red;
        this.customGreen = green;
        this.customBlue = blue;
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= 0.9099999785423279D;
        this.yd *= 0.9099999785423279D;
        this.zd *= 0.9099999785423279D;
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    protected void emitQuads(QuadSink sink, float partialTick) {
        Random urandom = new Random(this.seed);

        float scale = this.quadSize();
        float aged = (float) this.age / (float) this.lifetime;
        // The heat fades over the first quarter of the puff's life, not over all of it.
        float dark = 1F - Math.min((float) this.age / ((float) this.lifetime * 0.25F), 1F);
        float alpha = Mth.clamp((float) Math.pow(1F - Math.min(aged, 1F), 0.5), 0F, 1F) * 0.75F;
        float spread = ((float) Math.pow(aged * 4F, 1.5) + 1F) * scale;
        float sideOffset = 0.25F * scale;

        this.setAlpha(alpha);

        for (int i = 0; i < QUADS; i++) {
            float add = urandom.nextFloat() * 0.3F;
            this.setColor(Mth.clamp((customRed != 0F ? customRed : 1F) * dark + add, 0F, 1F),
                    Mth.clamp((customGreen != 0F ? customGreen : 0.6F) * dark + add, 0F, 1F),
                    Mth.clamp((customBlue != 0F ? customBlue : 0F) * dark + add, 0F, 1F));

            float size = (urandom.nextFloat() * 0.5F + 0.1F + aged * 2F) * scale;
            // The -1 offset on each Gaussian is upstream's; it biases the cloud rather than centring
            // it, which is what gives the trail its lopsided, wind-blown look.
            float dx = sideOffset + (float) ((urandom.nextGaussian() - 1D) * 0.2F * spread);
            float dy = (float) ((urandom.nextGaussian() - 1D) * 0.5F * spread);
            float dz = sideOffset + (float) ((urandom.nextGaussian() - 1D) * 0.2F * spread);
            sink.quad(dx, dy, dz, size);
        }
    }
}
