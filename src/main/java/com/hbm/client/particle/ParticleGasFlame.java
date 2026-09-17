package com.hbm.client.particle;

import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

import java.awt.Color;

/**
 * 1.7 {@code ParticleGasFlame}: a rising puff with a hot HSB gradient (the gas flare, the plane exhaust,
 * the miner lander's retro flame). Copied from ntm-next's class; the sprite-set animation there
 * played the vanilla flame strip, which this port draws from its own round blob like the rest of
 * {@link ParticleRotating}'s family, keeping the 1.7 colour/size/motion curves exactly.
 */
public class ParticleGasFlame extends ParticleRotating {

    private final float colorMod;

    protected ParticleGasFlame(ClientLevel level, double x, double y, double z,
                               double mx, double my, double mz, float scale) {
        super(level, x, y, z);
        this.xd = this.random.nextGaussian() * 0.01D * 0.1D + mx;
        this.yd = this.random.nextGaussian() * 0.01D * 0.1D + my * 1.5D;
        this.zd = this.random.nextGaussian() * 0.01D * 0.1D + mz;
        this.setQuadSize(0.1F * 0.75F * scale);
        this.lifetime = 30 + this.random.nextInt(13);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.colorMod = 0.8F + this.random.nextFloat() * 0.2F;
        this.setLayer(ParticleLayer.EMISSIVE);
        updateColor();
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

        double previousYd = this.yd;
        this.move(this.xd, this.yd + 0.004D, this.zd);
        this.xd *= 0.96D * 0.75D;
        this.yd = previousYd + 0.005D;
        this.zd *= 0.96D * 0.75D;
        updateColor();
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return this.quadSize() * Mth.clamp((this.age + partialTicks) / this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    private void updateColor() {
        float time = (float) this.age / (float) this.lifetime;
        Color color = Color.getHSBColor(Math.max((60 - time * 100) / 360F, 0.0F), 1 - time * 0.25F, 1 - time * 0.5F);
        this.setColor((color.getRed() / 255F) * colorMod, (color.getGreen() / 255F) * colorMod,
                (color.getBlue() / 255F) * colorMod);
    }

    public static void spawn(ClientLevel level, double x, double y, double z,
                             double mx, double my, double mz, float scale) {
        Particles.add(new ParticleGasFlame(level, x, y, z, mx, my, mz, scale));
    }
}
