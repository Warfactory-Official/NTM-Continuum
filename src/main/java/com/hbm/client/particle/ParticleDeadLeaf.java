package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * original-hbm (1.7.10) {@code ParticleDeadLeaf}: a small grey-tinted leaf that drifts down from
 * {@code waste_leaves}. Scale 0.1 (a 0.2-block quad), 200-249 ticks, gravity 0.2, no initial motion; while
 * airborne it wanders by a gaussian 0.002 per tick on both horizontal axes and never falls faster than
 * 0.025 blocks per tick, then rests on the ground until it expires. Lit like terrain (1.7 FX layer 1).
 *
 * <p>1.7 also mirrored the sprite per entity id; the shared quad path draws it unmirrored.
 */
public final class ParticleDeadLeaf extends CompatParticle {

    public ParticleDeadLeaf(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(dev.golem.core.Ids.of("hbm", "dead_leaf")));
        setLayer(ParticleLayer.TRANSLUCENT);
        this.xd = 0D;
        this.yd = 0D;
        this.zd = 0D;
        float grey = 1F - level.getRandom().nextFloat() * 0.2F;
        this.setColor(grey, grey, grey);
        this.setQuadSize(0.1F);
        this.lifetime = 200 + level.getRandom().nextInt(50);
        this.gravity = 0.2F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.onGround) {
            this.xd += this.random.nextGaussian() * 0.002D;
            this.zd += this.random.nextGaussian() * 0.002D;
            if (this.yd < -0.025D) this.yd = -0.025D;
        }
    }
}
