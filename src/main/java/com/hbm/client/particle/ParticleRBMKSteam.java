package com.hbm.client.particle;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.render.CompatParticle;
import dev.golem.core.render.ParticleLayer;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;

/** The 20-frame additive jet an RBMK boiler vents; frames run along the sheet's width. */
public final class ParticleRBMKSteam extends CompatParticle {
    private static final int FRAMES = 20;

    private ParticleRBMKSteam(ClientLevel level, double x, double y, double z) {
        super(level, x, y + 1.75D, z, Particles.sprite(Ids.of(Constants.MOD_ID, "rbmk_jet_steam")));
        lifetime = 10;
        setQuadSize(2F);
        alpha = 0.25F;
        gravity = 0F;
        friction = 1F;
        hasPhysics = false;
        rCol = gCol = bCol = 1F;
        setLayer(ParticleLayer.ADDITIVE);
        keepFaintFragments();
    }

    @Override public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) remove();
    }

    private int frame() { return Math.min(FRAMES - 1, age * FRAMES / lifetime); }
    @Override protected float getU0() { return sprite.getU((float) frame() / FRAMES); }
    @Override protected float getU1() { return sprite.getU((float) (frame() + 1) / FRAMES); }

    public static void spawn(ClientLevel level, double x, double y, double z) {
        Particles.add(new ParticleRBMKSteam(level, x, y, z));
    }
}
