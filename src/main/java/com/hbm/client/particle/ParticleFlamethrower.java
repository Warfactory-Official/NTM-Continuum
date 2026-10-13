package com.hbm.client.particle;

import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * One tongue of flame from the flamethrower: a fast, rising, spinning blob that swells as it burns
 * out.
 *
 * <p>Ported from 1.7 {@code com.hbm.particle.ParticleFlamethrower}. The fuel types keep their
 * hue bands: normal fire is orange, balefire green-blue, digamma pushed below zero into magenta (a negative hue
 * wraps, which is what upstream relies on). Oxy (meta 3) and black fire (meta 4) start white and are darkened per
 * frame by 1.7's {@code renderParticle} colour ramps; the other metas are brightened by {@code 0.75 - age} and drawn at
 * half alpha. (1.12.2 has white-only fire for black/oxy and no ramp; this follows 1.7.)
 * Like 1.7, the colour, alpha and size follow the whole-tick age ({@code particleAge / particleMaxAge}), and the
 * vertex colours are clamped to 0..1 as 1.7's {@code Tessellator.setColorRGBA} clamped them.
 */
public class ParticleFlamethrower extends ParticleRotating {

    private final int type;
    private final float baseRed;
    private final float baseGreen;
    private final float baseBlue;

    public ParticleFlamethrower(ClientLevel level, double x, double y, double z, int type) {
        super(level, x, y, z);

        this.lifetime = 20 + this.random.nextInt(10);
        this.setQuadSize(0.5F);

        this.xd = this.random.nextGaussian() * 0.02;
        this.zd = this.random.nextGaussian() * 0.02;

        float initialColor = 15F + this.random.nextFloat() * 25F;
        if (type == FlameCreator.META_BALEFIRE) initialColor = 65F + this.random.nextFloat() * 35F;
        if (type == FlameCreator.META_DIGAMMA) initialColor = 0F - this.random.nextFloat() * 15F;

        int rgb = Color.HSBtoRGB(initialColor / 255F, 1F, 1F);
        float red = ((rgb >> 16) & 0xFF) / 255F;
        float green = ((rgb >> 8) & 0xFF) / 255F;
        float blue = (rgb & 0xFF) / 255F;
        if (type == FlameCreator.META_OXY || type == FlameCreator.META_BLACK) red = green = blue = 1F;
        this.type = type;
        this.baseRed = red;
        this.baseGreen = green;
        this.baseBlue = blue;
        this.setColor(red, green, blue);
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
        double ageScaled = (double) this.age / (double) this.lifetime;
        float alpha;
        if (this.type == FlameCreator.META_OXY) {
            alpha = (float) (1 - ageScaled);
            float add = (float) ageScaled * 1.25F - 0.25F;
            this.setColor(clamp(this.baseRed - add), clamp(this.baseGreen - add * 0.75F), clamp(this.baseBlue));
        } else if (this.type == FlameCreator.META_BLACK) {
            alpha = (float) (1 - ageScaled);
            float add = (float) ageScaled * 2F - 0.25F;
            this.setColor(clamp(this.baseRed - add * 0.75F), clamp(this.baseGreen - add), clamp(this.baseBlue - add * 0.5F));
        } else {
            alpha = (float) Math.pow(1 - Math.min(ageScaled, 1), 0.5) * 0.5F;
            float add = 0.75F - (float) ageScaled;
            this.setColor(clamp(this.baseRed + add), clamp(this.baseGreen + add), clamp(this.baseBlue + add));
        }
        this.setAlpha(clamp(alpha));
    }

    private static float clamp(float value) {
        return value < 0F ? 0F : value > 1F ? 1F : value;
    }

    @Override
    public float getQuadSize(float partialTick) {
        double ageScaled = (double) this.age / (double) this.lifetime;
        return (float) ((ageScaled * 1.25 + 0.25) * 0.5F);
    }
}
