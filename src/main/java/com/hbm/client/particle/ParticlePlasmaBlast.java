package com.hbm.client.particle;

import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

/** The source's oriented 20-tick shockwave quad. */
public final class ParticlePlasmaBlast extends CompatParticle {
    private static final Identifier SHOCKWAVE = Ids.of("hbm", "shockwave");
    private final float red;
    private final float green;
    private final float blue;
    private final float scale;

    public ParticlePlasmaBlast(ClientLevel level, double x, double y, double z, float red, float green,
            float blue, float pitch, float yaw, float scale) {
        super(level, x, y, z, Particles.sprite(SHOCKWAVE));
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.scale = scale;
        this.lifetime = 20;
        this.setLayer(ParticleLayer.ADDITIVE);
        this.setPlaneOrientation(yaw, pitch);
        this.setColor(red, green, blue);
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
        this.setColor(red, green, blue);
        this.setAlpha(1F - (this.age + partialTick) / this.lifetime);
    }

    @Override
    public float getQuadSize(float partialTick) {
        return (float) (1D - Math.exp(-(this.age + partialTick) * 0.125D)) * scale;
    }
}
