package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;

import java.util.Random;

/**
 * 1.7 {@code ParticleSmokePlume} ({@code launchSmoke}): six grey contrail quads (each its own 0.1..0.85 grey,
 * offset by a gaussian times half the scale), fullbright and alpha-blended, fading out over 80..99 ticks while the
 * scale grows from 0.25 to 2.25. It moves with the motion it is given, lifted by the growth each tick, friction
 * 0.925; a vertical collision restores the motion's length upward, as 1.7 did.
 */
public final class ParticleSmokePlume extends CompatParticle {

    private static int seedSeq;
    /** Plumes made since start, for the harness. */
    public static int spawned;
    /** Plume ticks since start and the farthest a plume drifted from where it was made, for the harness. */
    public static int ticked;
    public static double maxDrift;

    private final double originX;
    private final double originZ;

    private final float[] grey = new float[6];
    private final double[] gaussX = new double[6];
    private final double[] gaussY = new double[6];
    private final double[] gaussZ = new double[6];
    private float plumeScale = 0.25F;
    private float fade = 1F;

    public ParticleSmokePlume(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ) {
        super(level, x, y, z, Particles.sprite(ParticleContrail.CONTRAIL));
        this.lifetime = 80 + this.random.nextInt(20);
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.setLayer(ParticleLayer.EMISSIVE);
        // The plume starts inside the launcher's own block. 1.7's moveEntity let an entity leave boxes it started
        // in; modern Particle.move marks a particle blocked on its first collision and never moves it again, which
        // left every plume buried in the launcher. So no block collision: the plumes drift out as 1.7's did.
        this.hasPhysics = false;
        this.originX = x;
        this.originZ = z;
        // 1.7 re-seeded a Random with the entity id every frame: one fixed stream per particle
        spawned++;
        Random urandom = new Random(seedSeq++);
        for (int i = 0; i < 6; i++) {
            grey[i] = urandom.nextFloat() * 0.75F + 0.1F;
            gaussX[i] = urandom.nextGaussian();
            gaussY[i] = urandom.nextGaussian();
            gaussZ[i] = urandom.nextGaussian();
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.fade = 1F - (float) this.age / (float) this.lifetime;
        float prevScale = this.plumeScale;
        this.plumeScale = 0.25F + ((float) this.age / (float) this.lifetime) * 2F;
        ++this.age;
        if (this.age == this.lifetime) this.remove();

        double bak = Math.sqrt(xd * xd + yd * yd + zd * zd);
        double wantY = this.yd + (this.plumeScale - prevScale);
        this.move(this.xd, wantY, this.zd);
        if (this.onGround) this.yd = bak; // 1.7 isCollidedVertically (never true without physics; kept for the record)
        ticked++;
        maxDrift = Math.max(maxDrift, Math.hypot(this.x - originX, this.z - originZ));
        this.xd *= 0.925;
        this.yd *= 0.925;
        this.zd *= 0.925;
    }

    @Override
    protected void emitQuads(QuadSink sink, float partialTick) {
        this.setAlpha(this.fade);
        for (int i = 0; i < 6; i++) {
            this.setColor(grey[i], grey[i], grey[i]);
            sink.quad((float) (gaussX[i] * 0.5 * plumeScale), (float) (gaussY[i] * 0.5 * plumeScale),
                    (float) (gaussZ[i] * 0.5 * plumeScale), plumeScale);
        }
    }
}
