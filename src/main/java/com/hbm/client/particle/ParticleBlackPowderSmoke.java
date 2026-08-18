package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * The puff of muzzle smoke from a gunpowder discharge: brief, bright at the mouth, greying as it
 * expands and slows.
 *
 * <p>Ported from 1.12.2 {@code ParticleBlackPowderSmoke}. Unlike {@link ParticleExplosionSmall} this
 * one does not collide and damps on all three axes, so it hangs in place rather than arcing.
 */
public class ParticleBlackPowderSmoke extends ParticleFXRotating {

    private final float baseScale;

    public ParticleBlackPowderSmoke(ClientLevel level, double x, double y, double z, float scale) {
        super(level, x, y, z);

        this.lifetime = 30 + this.random.nextInt(15);
        this.baseScale = scale * 0.9F + this.random.nextFloat() * 0.2F;
        this.setQuadSize(this.baseScale);

        this.gravity = 0F;

        this.hue = 20F + this.random.nextFloat() * 20F;
        this.setColorHSB(this.hue / 255F, 1F, 1F);

        this.hasPhysics = false;
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
        this.spin(2F);

        this.xd *= 0.65D;
        this.yd *= 0.65D;
        this.zd *= 0.65D;

        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    protected void updateForFrame(float partialTick) {
        double ageScaled = (this.age + partialTick) / (double) this.lifetime;

        this.setColorHSB(
                this.hue / 255F,
                Math.max(1F - (float) ageScaled * 4F, 0F),
                Mth.clamp(1.25F - (float) ageScaled * 2F, 0.7F, 1F));

        this.setAlpha((float) Math.pow(1 - Math.min(ageScaled, 1), 0.25) * 0.7F);
    }

    @Override
    public float getQuadSize(float partialTick) {
        double ageScaled = (this.age + partialTick) / (double) this.lifetime;
        return (float) (0.25 + ageScaled + (this.age + partialTick) * 0.025) * this.baseScale;
    }
}
