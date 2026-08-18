package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;

/**
 * One tongue of flame from the flamethrower — a fast, rising, spinning blob that swells as it burns
 * out.
 *
 * <p>Ported from 1.12.2 {@code ParticleFlamethrower}. The three fuel types keep their original hue
 * bands: normal fire is orange, balefire green-blue, digamma pushed below zero into magenta (a
 * negative hue wraps, which is what upstream relies on).
 */
public class ParticleFlamethrower extends ParticleRotating {

    public ParticleFlamethrower(ClientLevel level, double x, double y, double z, int type) {
        super(level, x, y, z);

        this.lifetime = 20 + this.random.nextInt(10);
        this.setQuadSize(0.5F);

        this.xd = this.random.nextGaussian() * 0.02;
        this.zd = this.random.nextGaussian() * 0.02;

        float initialColor = 15F + this.random.nextFloat() * 25F;
        if (type == FlameCreator.META_BALEFIRE) initialColor = 65F + this.random.nextFloat() * 35F;
        if (type == FlameCreator.META_DIGAMMA) initialColor = 0F - this.random.nextFloat() * 15F;

        this.setColorHSB(initialColor / 255F, 1F, 1F);
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

        this.xd *= 0.91D;
        this.yd *= 0.91D;
        this.zd *= 0.91D;
        this.yd += 0.01D;

        this.spinConstant(30F);

        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    protected void updateForFrame(float partialTick) {
        double ageScaled = (this.age + partialTick) / (double) this.lifetime;
        this.setAlpha((float) Math.pow(1 - Math.min(ageScaled, 1), 0.5));
    }

    @Override
    public float getQuadSize(float partialTick) {
        double ageScaled = (this.age + partialTick) / (double) this.lifetime;
        return (float) ((ageScaled * 1.25 + 0.25) * 0.5F);
    }
}
