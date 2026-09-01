package com.hbm.client.particle;

import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/** Builds {@code FluidFill} with the vanilla critical-hit frame selected by the legacy handler. */
public final class FluidFillCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
            double x, double y, double z, CompoundTag data) {

        // 1.12's ParticleCrit.Factory starts on the critical-hit frame and nextTextureIndexX() advances
        // to the adjacent enchanted-hit frame. The modern type uses that same vanilla CritParticle
        // implementation and sprite without requiring an atlas or custom render path.
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.ENCHANTED_HIT,
                x, y, z,
                Tags.getDoubleOr(data, "mX", 0D),
                Tags.getDoubleOr(data, "mY", 0D),
                Tags.getDoubleOr(data, "mZ", 0D));
        if (particle == null || !Tags.hasInt(data, "color")) return;

        int color = Tags.getIntOr(data, "color", 0);
        Particles.color(particle, (color >> 16 & 0xFF) / 255F,
                (color >> 8 & 0xFF) / 255F,
                (color & 0xFF) / 255F);
    }
}
