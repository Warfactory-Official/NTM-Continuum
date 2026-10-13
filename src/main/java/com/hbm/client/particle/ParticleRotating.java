package com.hbm.client.particle;

import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * A camera-facing quad that spins about the view axis, the base for HBM's smoke, fire and ash.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.particle.ParticleRotating}, which built the quad from the camera
 * basis vectors and rotated the four corners about its normal with a hand-written Rodrigues formula.
 * {@code SingleQuadParticle} does exactly this (camera rotation, then {@code rotateZ(roll)}), so that
 * method is replaced by advancing {@link #oRoll}/{@link #roll}. Upstream's near-identical
 * {@code ParticleFXRotating} is folded into this class (see {@link ParticleFXRotating}).
 */
public abstract class ParticleRotating extends CompatParticle {

    /**
     * The soft round blob every HBM smoke/fire particle draws with.
     *
     * <p>The id has no {@code particle/} segment: vanilla's particles atlas is declared with
     * {@code "prefix": ""} (unlike {@code blocks.json}, which uses {@code "prefix": "block/"}), so
     * {@code assets/hbm/textures/particle/particle_base.png} stitches as {@code hbm:particle_base}. A wrong
     * id does not throw: the atlas returns its missing sprite and the particle renders as a checkerboard.
     */
    public static final Identifier PARTICLE_BASE = Ids.of("hbm", "particle_base");

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
     * <p>Uses {@link java.awt.Color#HSBtoRGB} rather than {@code Mth.hsvToRgb}: it is the routine
     * {@code Color.getHSBColor} calls, so the ported hue constants land on the same colours on all six
     * targets.
     */
    protected void setColorHSB(float h, float s, float b) {
        int rgb = Color.HSBtoRGB(h, s, b);
        this.setColor(((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F);
    }

    /**
     * Advances the spin by {@code degreesPerTick}, tapering to zero at the end of the particle's life.
     *
     * <p><b>The rate is in degrees, and {@link #roll} is in radians.</b> Every upstream call site states
     * degrees, and the ported constants keep that unit, so the conversion happens here. {@code roll} goes
     * straight to {@code Quaternionf.rotateZ}, which is radians; feeding it a degree constant spins the
     * particle 57 times too fast.
     *
     * <p>The per-particle direction comes from {@code identityHashCode % 2}, a coin flip fixed for the
     * particle's lifetime. That is upstream's trick; drawing from {@code random} each tick would make
     * the spin jitter instead of turn.
     */
    protected void spin(float degreesPerTick) {
        this.oRoll = this.roll;
        float ageScaled = (float) this.age / (float) this.lifetime;
        this.roll += (1 - ageScaled) * degreesPerTick * Mth.DEG_TO_RAD
                * ((System.identityHashCode(this) % 2) - 0.5F);
    }

    /** Advances the spin at a constant rate, with no taper. Degrees per tick, as {@link #spin}. */
    protected void spinConstant(float degreesPerTick) {
        this.oRoll = this.roll;
        this.roll += degreesPerTick * Mth.DEG_TO_RAD * ((System.identityHashCode(this) % 2) - 0.5F);
    }
}
