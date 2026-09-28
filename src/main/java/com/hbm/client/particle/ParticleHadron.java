package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ParticleHadron}: the {@code hadron} flash, a camera-facing white quad drawn additively at full
 * brightness that grows by 0.15 blocks a tick and fades out over its ten-tick life. The ICF emits one every tick
 * a pellet fuses.
 */
public final class ParticleHadron extends CompatParticle {

    private static final Identifier HADRON = dev.golem.core.Ids.of("hbm", "hadron");
    private float particleScale;

    public ParticleHadron(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(HADRON));
        this.lifetime = 10;
        this.particleScale = 1F;
        this.setLayer(ParticleLayer.ADDITIVE);
        this.setColor(1F, 1F, 1F);
    }

    /** 1.7 {@code makeSmall}: half the growth. */
    public ParticleHadron makeSmall(boolean small) {
        if (small) this.particleScale = 0.5F;
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.remove();
    }

    @Override
    protected void updateForFrame(float partialTick) {
        this.setAlpha(1F - (this.age + partialTick) / (float) this.lifetime);
    }

    /** 1.7 {@code scale = (age + interp) * 0.15 * particleScale}, the quad's half-width. */
    @Override
    public float getQuadSize(float partialTick) {
        return (this.age + partialTick) * 0.15F * particleScale;
    }
}
