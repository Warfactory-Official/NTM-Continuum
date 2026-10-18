package com.hbm.client.particle;

import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;

/** 1.7 {@code ParticleDebug}: the {@code DEBUG_PORT_VIEW} marker, a fullbright 0.1-block quad drifting along a port. */
public final class ParticleDebug extends CompatParticle {

    private static final Identifier POWER = Ids.of("hbm", "debug_power");
    private static final Identifier FLUID = Ids.of("hbm", "debug_fluid");

    /** A power marker; a fluid one when {@code color} is not negative. */
    public ParticleDebug(ClientLevel level, double x, double y, double z, double mX, double mY, double mZ, int color) {
        super(level, x, y, z, Particles.sprite(color < 0 ? POWER : FLUID));
        this.lifetime = 10;
        this.xd = mX;
        this.yd = mY;
        this.zd = mZ;
        this.hasPhysics = false;
        this.gravity = 0F;
        this.friction = 0.98F; // 1.7 EntityFX.onUpdate
        this.setLayer(ParticleLayer.EMISSIVE);
        this.setQuadSize(0.05F);
        if (color >= 0) this.setColor(((color & 0xff0000) >> 16) / 255F, ((color & 0x00ff00) >> 8) / 255F, (color & 0x0000ff) / 255F);
    }
}
