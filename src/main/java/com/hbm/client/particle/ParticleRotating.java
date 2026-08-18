package com.hbm.client.particle;

import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

/**
 * A camera-facing quad that spins about the view axis — the base for HBM's smoke, fire and ash.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.particle.ParticleRotating}, whose whole body was a 60-line
 * {@code renderParticleRotated}: build the quad from the camera basis vectors, compute its normal,
 * then rotate the four corners about that normal with a hand-written Rodrigues formula. Modern
 * {@code SingleQuadParticle} does exactly this — camera rotation, then {@code rotateZ(roll)} — so the
 * whole method is replaced by advancing {@link #oRoll}/{@link #roll}.
 *
 * <p>Upstream also had a near-identical {@code ParticleFXRotating} in another package with its own
 * copy of the same maths; both collapse onto this one class here (see {@link ParticleFXRotating}).
 */
public abstract class ParticleRotating extends CompatParticle {

    /**
     * The soft round blob every HBM smoke/fire particle draws with.
     *
     * <p><b>Note the id has no {@code particle/} segment.</b> Vanilla's particles atlas is declared
     * with {@code "prefix": ""} — unlike {@code blocks.json}, which uses {@code "prefix": "block/"} —
     * so a file at {@code assets/hbm/textures/particle/particle_base.png} stitches as
     * {@code hbm:particle_base}. Getting this wrong does not throw: the atlas returns its missing
     * sprite and the particle renders as a checkerboard.
     */
    public static final Identifier PARTICLE_BASE = Identifier.fromNamespaceAndPath("hbm", "particle_base");

    protected ParticleRotating(ClientLevel level, double x, double y, double z) {
        this(level, x, y, z, Particles.sprite(PARTICLE_BASE));
    }

    protected ParticleRotating(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
        this.setLayer(ParticleLayer.EMISSIVE);
    }

    /**
     * Converts a hue/saturation/brightness triple into this particle's colour.
     *
     * <p>Uses {@link java.awt.Color#HSBtoRGB} rather than {@code Mth.hsvToRgb} deliberately: it is the
     * exact routine {@code Color.getHSBColor} called in 1.12, so the ported hue constants land on the
     * same colours, and as a pure JDK static it behaves identically on all six build targets.
     */
    protected void setColorHSB(float h, float s, float b) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, b);
        this.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F);
    }

    /**
     * Advances the spin by {@code amountPerTick}, tapering to zero at the end of the particle's life.
     *
     * <p>The per-particle direction comes from {@code identityHashCode % 2} — effectively a coin flip
     * fixed for the particle's lifetime. That is upstream's trick, kept rather than replaced with the
     * RNG precisely because it is stable: drawing from {@code random} each tick would make the spin
     * jitter instead of turn.
     */
    protected void spin(float amountPerTick) {
        this.oRoll = this.roll;
        float ageScaled = (float) this.age / (float) this.lifetime;
        this.roll += (1 - ageScaled) * amountPerTick * ((System.identityHashCode(this) % 2) - 0.5F);
    }

    /** Advances the spin at a constant rate, with no taper. */
    protected void spinConstant(float amountPerTick) {
        this.oRoll = this.roll;
        this.roll += amountPerTick * ((System.identityHashCode(this) % 2) - 0.5F);
    }
}
