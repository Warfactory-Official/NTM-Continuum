package com.hbm.client.particle;

import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/** Builds the legacy {@code Tower} effect from its wire payload. */
public final class CoolingTowerCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
            double x, double y, double z, CompoundTag data) {

        Particles.Quality setting = Particles.quality();
        if (setting == Particles.Quality.MINIMAL) return;
        if (setting == Particles.Quality.DECREASED && !rand.nextBoolean()) return;

        ParticleCoolingTower particle = new ParticleCoolingTower(level, x, y, z);
        particle.setLift(Tags.getFloatOr(data, "lift", 0F));
        particle.setBaseScale(Tags.getFloatOr(data, "base", 0F));
        particle.setMaxScale(Tags.getFloatOr(data, "max", 0F));
        particle.setLife(Tags.getIntOr(data, "life", 0) / (setting == Particles.Quality.DECREASED ? 2 : 1));
        if (data.contains("noWind")) particle.noWind();
        if (data.contains("strafe")) particle.setStrafe(Tags.getFloatOr(data, "strafe", 0F));
        if (data.contains("alpha")) particle.alphaMod(Tags.getFloatOr(data, "alpha", 0F));

        if (Tags.hasInt(data, "color")) {
            int color = Tags.getIntOr(data, "color", 0);
            particle.setColor((color >> 16 & 0xFF) / 255F,
                    (color >> 8 & 0xFF) / 255F,
                    (color & 0xFF) / 255F);
        }

        Particles.add(particle);
    }
}
