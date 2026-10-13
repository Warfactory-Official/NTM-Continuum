package com.hbm.client.particle;

import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Muzzle discharge: a cone of smoke plus a scatter of sparks, both aimed along a heading.
 *
 * <p>Ported from 1.12.2 {@code BlackPowderCreator}, minus its {@code composeEffect}. The server-side
 * sender is {@link com.hbm.particle.helper.ParticleEffects#blackPowder}, which common code can reach
 * without loading this client class. This is the client half only.
 */
public class BlackPowderCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
            double x, double y, double z, CompoundTag data) {

        int cloudCount = Tags.getIntOr(data, "cloudCount", 0);
        float cloudScale = Tags.getFloatOr(data, "cloudScale", 1F);
        float cloudSpeedMult = Tags.getFloatOr(data, "cloudSpeedMult", 0F);
        int sparkCount = Tags.getIntOr(data, "sparkCount", 0);
        float sparkSpeedMult = Tags.getFloatOr(data, "sparkSpeedMult", 0F);

        Vec3 heading = new Vec3(
                Tags.getDoubleOr(data, "hX", 0D),
                Tags.getDoubleOr(data, "hY", 0D),
                Tags.getDoubleOr(data, "hZ", 0D)).normalize();

        for (int i = 0; i < cloudCount; i++) {
            ParticleBlackPowderSmoke particle = new ParticleBlackPowderSmoke(level, x, y, z, cloudScale);
            double speedMult = 0.85 + rand.nextDouble() * 0.3;
            particle.setParticleSpeed(
                    heading.x * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05,
                    heading.y * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05,
                    heading.z * cloudSpeedMult * speedMult + rand.nextGaussian() * 0.05);
            Particles.add(particle);
        }

        for (int i = 0; i < sparkCount; i++) {
            double speedMult = 0.85 + rand.nextDouble() * 0.3;
            Particles.add(new ParticleBlackPowderSpark(level, x, y, z,
                    heading.x * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02,
                    heading.y * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02,
                    heading.z * sparkSpeedMult * speedMult + rand.nextGaussian() * 0.02));
        }
    }
}
