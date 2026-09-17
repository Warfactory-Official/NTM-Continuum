package com.hbm.client.particle;

import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * A single hot ember thrown from a muzzle: a tiny glowing dot that arcs, slows and dies fast.
 *
 * <p>Ported from ntm-next's {@code ParticleBlackPowderSpark}; the direct effect path selects the
 * modern atlas equivalent of the source vanilla sheet's cell zero.
 */
public class ParticleBlackPowderSpark extends CompatParticle {

    public ParticleBlackPowderSpark(ClientLevel level, double x, double y, double z,
            double xa, double ya, double za) {
        super(level, x, y, z, xa, ya, za, Particles.sprite(Ids.of("minecraft", "generic_0")));

        // The super ctor derives motion from the speed arguments with a spread applied; these are
        // aimed sparks, so upstream overwrites it with the exact vector afterwards.
        this.xd = xa;
        this.yd = ya;
        this.zd = za;

        float f = this.random.nextFloat() * 0.1F + 0.2F;
        this.setColor(f + 0.7F, f + 0.5F, f);

        this.setSize(0.02F, 0.02F);
        this.setQuadSize(this.quadSize() * (this.random.nextFloat() * 0.6F + 0.5F));
        this.lifetime = 15 + this.random.nextInt(5);
        this.gravity = 0.01F;

        this.setLayer(ParticleLayer.EMISSIVE);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.yd -= this.gravity;
        this.move(this.xd, this.yd, this.zd);

        // Damping is applied after the move, as upstream does — moving first means the first tick
        // travels the full launch speed.
        this.xd *= 0.95D;
        this.yd *= 0.95D;
        this.zd *= 0.95D;

        // Upstream counted the lifetime *down* (`if (particleMaxAge-- <= 0)`); counting age up is the
        // same number of ticks and matches every other particle here.
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }
}
