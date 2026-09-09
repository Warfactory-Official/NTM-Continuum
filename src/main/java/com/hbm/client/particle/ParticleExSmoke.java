package com.hbm.client.particle;

import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Random;

/** The legacy six-billboard expanding smoke particle used by large explosions. */
public final class ParticleExSmoke extends CompatParticle {
    public static final net.minecraft.resources.Identifier CONTRAIL = Ids.of("hbm", "contrail");

    private final int randomSeed;

    public ParticleExSmoke(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(CONTRAIL));
        lifetime = 100 + random.nextInt(40);
        randomSeed = level.getRandom().nextInt();
        hasPhysics = false;
        // The old particle returned packed light 240 (full block-light channel) from getBrightnessForRender.
        setLayer(ParticleLayer.EMISSIVE);
    }

    public ParticleExSmoke setMotion(double x, double y, double z) {
        xd = x;
        yd = y;
        zd = z;
        return this;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        age++;
        if (age >= lifetime) {
            remove();
            return;
        }
        xd *= 0.7599999785423279D;
        yd *= 0.7599999785423279D;
        zd *= 0.7599999785423279D;
        move(xd, yd, zd);
    }

    @Override
    protected void updateForFrame(float partialTick) {
        setAlpha(1F - (float) age / (float) lifetime);
    }

    @Override
    protected void emitQuads(QuadSink sink, float partialTick) {
        Random random = new Random(randomSeed);
        for (int i = 0; i < 6; i++) {
            float brightness = random.nextFloat() * 0.5F + 0.4F;
            setColor(brightness, brightness, brightness);
            float scale = random.nextFloat() + 0.5F;
            sink.quad((float) ((random.nextGaussian() - 1D) * 0.75F),
                    (float) ((random.nextGaussian() - 1D) * 0.75F),
                    (float) ((random.nextGaussian() - 1D) * 0.75F), scale);
        }
    }
}
