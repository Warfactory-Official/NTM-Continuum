package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * One fireball of a small explosion: a hot orange blob that swells, cools toward yellow-white and
 * fades out while tumbling.
 *
 * <p>Ported from 1.12.2 {@code ParticleExplosionSmall}. The colour ramp, alpha curve and swell curve
 * are carried over verbatim; only where they run has moved (see {@link ParticleFXRotating}).
 */
public class ParticleExplosionSmall extends ParticleFXRotating {

    private final float baseScale;

    public ParticleExplosionSmall(ClientLevel level, double x, double y, double z, float scale, float speedMult) {
        super(level, x, y, z);

        this.lifetime = 25 + this.random.nextInt(10);
        this.baseScale = scale * 0.9F + this.random.nextFloat() * 0.2F;
        this.setQuadSize(this.baseScale);

        this.xd = this.random.nextGaussian() * speedMult;
        this.zd = this.random.nextGaussian() * speedMult;
        // Negative gravity: these rise. Upstream then *subtracts* it in tick(), so the sign flips twice.
        this.gravity = this.random.nextFloat() * -0.01F;

        this.hue = 20F + this.random.nextFloat() * 20F;
        this.setColorHSB(this.hue / 255F, 1F, 1F);

        this.hasPhysics = true;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        this.yd -= this.gravity;
        this.spin(5F);

        this.xd *= 0.65D;
        this.zd *= 0.65D;

        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    protected void updateForFrame(float partialTick) {
        double ageScaled = (this.age + partialTick) / (double) this.lifetime;

        this.setColorHSB(
                this.hue / 255F,
                Math.max(1F - (float) ageScaled * 2F, 0F),
                Mth.clamp(1.25F - (float) ageScaled * 2F, this.hue * 0.01F - 0.1F, 1F));

        // The 0.7 factor is upstream's comment-documented correction for modern particles being more
        // opaque than the 1.7 ones the effect was originally tuned against.
        this.setAlpha((float) Math.pow(1 - Math.min(ageScaled, 1), 0.25) * 0.7F);
    }

    @Override
    public float getQuadSize(float partialTick) {
        double ageScaled = (this.age + partialTick) / (double) this.lifetime;
        return (float) (0.25 + 1 - Math.pow(1 - ageScaled, 4) + (this.age + partialTick) * 0.02) * this.baseScale;
    }
}
