package com.hbm.client.particle;

import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1.7 {@code ParticleHaze}, the white-phosphorus haze (1.7 {@code effectNT} type {@code haze}, sent
 * by the phosphorus grenade, the phosphorus artillery shell and the white-phosphorus HIMARS rocket).
 *
 * <p>Lives 600..699 ticks and never moves (1.7 damped its motion but never called {@code moveEntity}). Every tick it
 * pops one vanilla lava particle on the surface of a random column within 7 blocks. It draws 25 camera-facing quads
 * of {@code haze.png}, half-size 7.5..10, from a {@code Random(50)} that is re-seeded every frame, so the cluster is
 * the same shape for every haze. 1.7 pushed one matrix and then {@code glTranslatef}'d by each quad's gaussian
 * offset without popping, so the offsets <b>accumulate</b> into a random walk; that is kept, and since the seed is a
 * constant the walk is computed once.
 *
 * <p>The alpha is 1.7's {@code glColor4f(1, 1, 1, sin(age * pi / 400) * 0.25 * 0.1)}: at most 0.025, and clamped to
 * zero (invisible) from age 400 on. 1.7 drew it with {@code glAlphaFunc(GL_GREATER, 0)} and {@code glDepthMask(false)}:
 * vanilla's particle shader discards below alpha 0.1 on every node, which hid every quad, so the haze draws through
 * Core's {@code keepFaintFragments} layer (alpha above 0 kept, no depth write).
 */
public class ParticleHaze extends CompatParticle {

    /** {@code assets/hbm/textures/particle/haze.png} on the particles atlas (no {@code particle/} prefix). */
    public static final Identifier HAZE = Ids.of("hbm", "haze");

    private static final int QUADS = 25;
    private static final float SCALE = 10F;
    private static final float[] OFFSET_X = new float[QUADS];
    private static final float[] OFFSET_Y = new float[QUADS];
    private static final float[] OFFSET_Z = new float[QUADS];
    private static final float[] SIZE = new float[QUADS];

    static {
        // 1.7 call order per quad: dX, dY, dZ, size, then the three per-vertex-block gaussians.
        Random rand = new Random(50);
        double tx = 0, ty = 0, tz = 0;
        for (int i = 0; i < QUADS; i++) {
            tx += rand.nextGaussian() * 2.5D;
            ty += rand.nextGaussian() * 0.15D;
            tz += rand.nextGaussian() * 2.5D;
            SIZE[i] = (float) ((rand.nextDouble() * 0.25 + 0.75) * SCALE);
            OFFSET_X[i] = (float) (tx + rand.nextGaussian() * 0.5);
            OFFSET_Y[i] = (float) (ty + rand.nextGaussian() * 0.5);
            OFFSET_Z[i] = (float) (tz + rand.nextGaussian() * 0.5);
        }
    }

    public ParticleHaze(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, Particles.sprite(HAZE));
        this.lifetime = 600 + this.random.nextInt(100);
        this.xd = this.yd = this.zd = 0;
        this.hasPhysics = false;
        this.setQuadSize(SCALE);
        // 1.7: brightness 240, SRC_ALPHA / ONE_MINUS_SRC_ALPHA, depth mask off, vertex colour from glColor (white)
        this.setLayer(ParticleLayer.EMISSIVE);
        this.keepFaintFragments();
        this.setColor(1F, 1F, 1F);
    }

    /** Harness only (query {@code hbm_haze}): starts the haze at {@code age} ticks, e.g. 200 = 1.7's peak alpha. */
    public ParticleHaze aged(int age) {
        this.age = age;
        this.lifetime = Math.max(this.lifetime, age + 200);
        return this;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
        }

        int bx = (int) Math.floor(this.x) + this.random.nextInt(15) - 7;
        int bz = (int) Math.floor(this.z) + this.random.nextInt(15) - 7;
        // 1.7 getHeightValue: the first air block above the highest light-blocking one
        int by = this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        this.level.addParticle(ParticleTypes.LAVA, bx + this.random.nextDouble(), by + 0.1, bz + this.random.nextDouble(),
                0.0, 0.0, 0.0);
    }

    @Override
    protected void emitQuads(QuadSink sink, float partialTick) {
        float alpha = (float) Math.sin(this.age * Math.PI / 400F) * 0.25F * 0.1F;
        if (alpha <= 0F) return;
        this.setAlpha(alpha);
        for (int i = 0; i < QUADS; i++) {
            sink.quad(OFFSET_X[i], OFFSET_Y[i], OFFSET_Z[i], SIZE[i]);
        }
    }
}
