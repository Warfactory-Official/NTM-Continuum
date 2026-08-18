package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * {@link ParticleRotating} plus a remembered hue.
 *
 * <p>1.12.2 had two classes for this — {@code com.hbm.particle.ParticleRotating} and
 * {@code com.hbm.entity.particle.ParticleFXRotating} — carrying two copies of the same billboard
 * maths, differing only in that this one kept a {@code hue} field and spun on
 * {@code hashCode()} rather than {@code identityHashCode()}. Both names are kept so ported subclasses
 * read like their originals, but the duplicated rendering is gone: everything lives in the parent.
 */
public abstract class ParticleFXRotating extends ParticleRotating {

    /**
     * Hue in the 0-255 space the 1.12 particles used (they passed {@code hue / 255F} to
     * {@code Color.getHSBColor}). Kept in that space so the ported constants read identically.
     */
    public float hue;

    protected ParticleFXRotating(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    protected ParticleFXRotating(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
    }
}
