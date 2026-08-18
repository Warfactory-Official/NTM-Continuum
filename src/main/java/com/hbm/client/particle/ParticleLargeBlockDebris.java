package com.hbm.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chunk of a real block thrown out by a blast — a block-textured particle that lives far longer
 * than vanilla's break dust.
 *
 * <p>Ported from 1.12.2 {@code ParticleLargeBlockDebris}, which subclassed vanilla
 * {@code ParticleBlockDust}; the modern equivalent is {@code TerrainParticle}. Upstream also called an
 * {@code init()} afterwards to record the source block position for texture lookup — the modern class
 * does that in its constructor, so the call is gone rather than dropped.
 */
public class ParticleLargeBlockDebris extends TerrainParticle {

    public ParticleLargeBlockDebris(ClientLevel level, double x, double y, double z,
            double xa, double ya, double za, BlockState state, int maxAge) {
        super(level, x, y, z, xa, ya, za, state);
        this.lifetime = maxAge;
    }
}
