package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

import java.util.Random;

/**
 * 1.7 six-quad rocket contrail puff: stationary, tinted per propellant, fading linearly over its life.
 * Copied from ntm-next {@code com.hbm.particle.ParticleContrail}; its renderer's six sub-quads are emitted
 * through {@link CompatParticle#emitQuads} so the same geometry draws on every node.
 */
public class ParticleContrail extends CompatParticle {

    public static final Identifier CONTRAIL = dev.golem.core.Ids.of("hbm", "contrail");

    public static final int SUBQUADS = 6;

    // 1.7: getEntityId() seeded; static sequence.
    private static int seedSeq;

    /**
     * Frame-invariant in 1.7 - renderParticle re-seeds from the id every frame, yielding this same stream.
     */
    public final float[] mod = new float[SUBQUADS];
    public final double[] gaussX = new double[SUBQUADS];
    public final double[] gaussY = new double[SUBQUADS];
    public final double[] gaussZ = new double[SUBQUADS];

    private final float scale;
    private final float red, green, blue;
    private float fade = 1F;

    public ParticleContrail(ClientLevel level, double x, double y, double z, float r, float g, float b, float scale) {
        super(level, x, y, z, Particles.sprite(CONTRAIL));
        this.lifetime = 100 + this.random.nextInt(40);
        this.red = r;
        this.green = g;
        this.blue = b;
        this.scale = scale;
        this.xd = this.yd = this.zd = 0;
        this.hasPhysics = false;
        // 1.7: brightness 240, alpha blend, depth mask off - the full-bright translucent layer.
        this.setLayer(ParticleLayer.EMISSIVE);

        Random urandom = new Random(seedSeq++);
        for (int i = 0; i < SUBQUADS; i++) {
            this.mod[i] = urandom.nextFloat() * 0.2F + 0.2F;
            this.gaussX[i] = urandom.nextGaussian();
            this.gaussY[i] = urandom.nextGaussian();
            this.gaussZ[i] = urandom.nextGaussian();
        }
    }

    /**
     * 1.7 sets the alpha from the PREVIOUS age and only then increments, so the fade lags one tick.
     */
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.fade = 1F - (float) this.age / (float) this.lifetime;

        this.age++;
        if (this.age == this.lifetime) this.remove();
    }

    /** 1.7: the quad grows as the puff fades out. */
    public float quadScale() {
        return (this.fade + 0.5F) * this.scale;
    }

    /**
     * 1.7 renderParticle: six quads, each offset by its own Gaussian times half the spread, tinted by the
     * base colour plus its own 0.2..0.4 lift, all at the same fading half-size.
     */
    @Override
    protected void emitQuads(QuadSink sink, float partialTick) {
        this.setAlpha(this.fade);
        float half = quadScale();
        for (int i = 0; i < SUBQUADS; i++) {
            float m = this.mod[i];
            this.setColor(this.red + m, this.green + m, this.blue + m);
            sink.quad((float) (this.gaussX[i] * 0.5D * this.scale), (float) (this.gaussY[i] * 0.5D * this.scale),
                    (float) (this.gaussZ[i] * 0.5D * this.scale), half);
        }
    }
}
