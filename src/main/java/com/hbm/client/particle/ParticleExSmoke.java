package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Random;

/**
 * 1.7 {@code ParticleExSmoke}: the six-billboard expanding smoke of large explosions. Six grey quads
 * (brightness 0.25..0.5) scattered around the particle by a per-particle seeded gaussian, alpha-blended
 * and lit by the world, on the shared {@code particle_base} sprite. (1.12.2 brightened it to 0.4..0.9
 * and drew it fullbright; 1.7 is the specification.)
 */
public final class ParticleExSmoke extends CompatParticle {

    private final int randomSeed;

    public ParticleExSmoke(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(ParticleRotating.PARTICLE_BASE));
        lifetime = 100 + random.nextInt(40);
        randomSeed = level.getRandom().nextInt(); // 1.7 seeds with getEntityId(): any per-particle constant
        setLayer(ParticleLayer.TRANSLUCENT);
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
        // 1.7 onUpdate: alpha from the age before the increment, death on the tick age reaches maxAge
        setAlpha(1F - (float) age / (float) lifetime);
        age++;
        if (age == lifetime) {
            remove();
        }
        xd *= 0.7599999785423279D;
        yd *= 0.7599999785423279D;
        zd *= 0.7599999785423279D;
        move(xd, yd, zd);
    }

    @Override
    protected void emitQuads(QuadSink sink, float partialTick) {
        Random random = new Random(randomSeed);
        for (int i = 0; i < 6; i++) {
            float brightness = random.nextFloat() * 0.25F + 0.25F;
            setColor(brightness, brightness, brightness);
            float scale = random.nextFloat() + 0.5F;
            sink.quad((float) ((random.nextGaussian() - 1D) * 0.75F),
                    (float) ((random.nextGaussian() - 1D) * 0.75F),
                    (float) ((random.nextGaussian() - 1D) * 0.75F), scale);
        }
    }
}
