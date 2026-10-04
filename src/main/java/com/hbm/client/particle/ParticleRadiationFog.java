package com.hbm.client.particle;

import java.util.Random;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

/**
 * 1.7 {@code ParticleRadiationFog} ({@code radFog}): a stationary cloud of 25 camera-facing {@code fog.png}
 * quads tinted (0.85, 0.9, 0.5), fading in and out over 400 ticks with alpha {@code sin(age * pi / 400) / 8},
 * drawn at brightness 240 without depth writes.
 *
 * <p>The 1.7 particle never moved: its {@code onUpdate} only damped the motion it never applied. Its layout
 * came from a {@code Random(50)} re-seeded every frame, and each quad's offset was a {@code glTranslatef}
 * applied without a matrix push, so offsets <i>accumulate</i> from quad to quad (the cloud trails off towards
 * -x/-z, the gaussians being shifted by -1). Both are reproduced.
 */
public final class ParticleRadiationFog extends CompatParticle {

    private static final Identifier FOG = dev.golem.core.Ids.of("hbm", "fog");
    private static final float PARTICLE_SCALE = 7.5F;

    public ParticleRadiationFog(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(FOG));
        // 1.7: maxAge = 100 + rand(40), then onUpdate raises anything below 400 to 400
        this.lifetime = 400;
        this.hasPhysics = false;
        this.xd = this.yd = this.zd = 0D;
        this.setColor(0.85F, 0.9F, 0.5F);
        this.setLayer(ParticleLayer.EMISSIVE);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) this.remove();
    }

    @Override
    protected void updateForFrame(float partialTick) {
        this.setAlpha((float) Math.sin(this.age * Math.PI / 400F) * 0.125F);
    }

    @Override
    protected void emitQuads(QuadSink sink, float partialTick) {
        Random rand = new Random(50);
        float tx = 0F, ty = 0F, tz = 0F;

        for (int i = 0; i < 25; i++) {
            double dX = (rand.nextGaussian() - 1D) * 2.5D;
            double dY = (rand.nextGaussian() - 1D) * 0.15D;
            double dZ = (rand.nextGaussian() - 1D) * 2.5D;
            double size = rand.nextDouble() * PARTICLE_SCALE;

            // glTranslatef without a push: the translation carries over to every later quad
            tx += (float) dX;
            ty += (float) dY;
            tz += (float) dZ;

            float pX = (float) (rand.nextGaussian() * 0.5);
            float pY = (float) (rand.nextGaussian() * 0.5);
            float pZ = (float) (rand.nextGaussian() * 0.5);

            sink.quad(tx + pX, ty + pY, tz + pZ, (float) size);
        }
    }
}
