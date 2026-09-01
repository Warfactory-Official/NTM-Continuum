package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * One cooling-tower smoke puff, ported from 1.12.2 {@code ParticleCoolingTower}.
 *
 * <p>The legacy particle already renders one camera-facing quad, so the modern particle facade can
 * preserve its update maths without a custom render pass.
 */
public final class ParticleCoolingTower extends CompatParticle {

    private float baseScale = 1F;
    private float maxScale = 1F;
    private float lift = 0.3F;
    private float strafe = 0.075F;
    private boolean windDir = true;
    private float alphaMod = 0.25F;

    public ParticleCoolingTower(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(ParticleRotating.PARTICLE_BASE));

        float grey = 0.9F + level.getRandom().nextFloat() * 0.05F;
        this.setColor(grey, grey, grey);
        this.setAlpha(0.30F);
        this.setQuadSize((this.random.nextFloat() * 0.5F + 0.5F) * 2F);
        this.hasPhysics = false;
        this.setLayer(ParticleLayer.TRANSLUCENT);
    }

    public void setBaseScale(float scale) {
        this.baseScale = scale;
    }

    public void setMaxScale(float scale) {
        this.maxScale = scale;
    }

    public void setLift(float lift) {
        this.lift = lift;
    }

    public void setLife(int life) {
        this.lifetime = life;
    }

    public void setStrafe(float strafe) {
        this.strafe = strafe;
    }

    public void noWind() {
        this.windDir = false;
    }

    public void alphaMod(float alpha) {
        this.alphaMod = alpha;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        float ageScale = (float) this.age / (float) this.lifetime;
        this.setAlpha(alphaMod - ageScale * alphaMod);
        this.setQuadSize(baseScale + (float) Math.pow(maxScale * ageScale - baseScale, 2));

        this.age++;
        if (lift > 0F && this.yd < lift) this.yd += 0.01F;
        if (lift < 0F && this.yd > lift) this.yd -= 0.01F;

        this.xd += this.random.nextGaussian() * strafe * ageScale;
        this.zd += this.random.nextGaussian() * strafe * ageScale;

        if (windDir) {
            this.xd += 0.02D * ageScale;
            this.zd -= 0.01D * ageScale;
        }

        if (this.age >= this.lifetime) this.remove();
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.925D;
        this.yd *= 0.925D;
        this.zd *= 0.925D;
    }
}
