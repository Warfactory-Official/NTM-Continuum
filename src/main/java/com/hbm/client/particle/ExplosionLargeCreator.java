package com.hbm.client.particle;

import com.hbm.sound.ModSounds;
import dev.golem.core.client.Sounds;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * A large blast: a boiling smoke plume and a spread of the ground itself thrown into the air.
 *
 * <p>Ported from 1.7 {@code ExplosionCreator}. The plume is {@link ParticleRocketFlame} at the
 * preset's scale (the same ten-quad cloud a missile trails, blown up), and the debris is
 * {@link DebrisParticles}, which throws real scoops of the terrain rather than block-dust sprites.
 *
 * <p>The expanding sheet shares the nuke's {@link MukeParticles} shockwave renderer at the supplied
 * {@code waveScale}, without adding a nuclear flash or mushroom cloud.
 */
public class ExplosionLargeCreator implements IParticleCreator {

    /** Upstream's throw: 45 to 70 degrees above the horizon, any bearing. */
    private static final double MIN_ELEVATION = 45D;
    private static final double ELEVATION_SPREAD = 25D;

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
            double x, double y, double z, CompoundTag data) {

        int cloudCount = Tags.getIntOr(data, "cloudCount", 0);
        float cloudScale = Tags.getFloatOr(data, "cloudScale", 1F);
        float cloudSpeedMult = Tags.getFloatOr(data, "cloudSpeedMult", 1F);
        int debrisCount = Tags.getIntOr(data, "debrisCount", 0);
        int debrisSize = Tags.getIntOr(data, "debrisSize", 0);
        int debrisRetry = Tags.getIntOr(data, "debrisRetry", 0);
        float debrisVelocity = Tags.getFloatOr(data, "debrisVelocity", 1F);
        float debrisHDev = Tags.getFloatOr(data, "debrisHorizontalDeviation", 1F);
        float debrisVOff = Tags.getFloatOr(data, "debrisVerticalOffset", -2F);
        float soundRange = Tags.getFloatOr(data, "soundRange", 200F);
        MukeParticles.spawnWave(level, x, y, z, Tags.getFloatOr(data, "waveScale", 1F));

        if (player != null) {
            float distance = (float) Math.sqrt(player.distanceToSqr(x, y, z));
            if (distance <= soundRange) {
                var sound = distance <= soundRange * 0.4F ? ModSounds.EXPLOSION_LARGE_NEAR.get()
                        : ModSounds.EXPLOSION_LARGE_FAR.get();
                Sounds.playDelayed(
                        new ExplosionSmallCreator.PositionalSound(sound, x, y, z),
                        (int) (distance / ExplosionSmallCreator.speedOfSound));
            }
        }

        for (int i = 0; i < cloudCount; i++) {
            ParticleRocketFlame flame = new ParticleRocketFlame(level, x, y, z);
            flame.setScale(cloudScale);
            flame.setMotion(rand.nextGaussian() * 0.5D * cloudSpeedMult,
                    rand.nextDouble() * 3D * cloudSpeedMult,
                    rand.nextGaussian() * 0.5D * cloudSpeedMult);
            flame.setMaxAge(70 + rand.nextInt(20));
            Particles.add(flame);
        }

        for (int i = 0; i < debrisCount; i++) {
            double oX = rand.nextGaussian() * debrisHDev;
            double oZ = rand.nextGaussian() * debrisHDev;
            int cX = (int) Math.floor(x + oX + 0.5D);
            int cY = (int) Math.floor(y + debrisVOff + 0.5D);
            int cZ = (int) Math.floor(z + oZ + 0.5D);

            // The negative Z turn is upstream's and it is what sends the chunk up rather than down:
            // modern Vec3.zRot carries 1.7's rotateAroundZ formula unchanged, sign included.
            Vec3 motion = new Vec3(debrisVelocity, 0D, 0D)
                    .zRot((float) -Math.toRadians(MIN_ELEVATION + rand.nextFloat() * ELEVATION_SPREAD))
                    .yRot((float) (rand.nextDouble() * Math.PI * 2D));

            DebrisParticles.add(level, rand, x, y, z, cX, cY, cZ, debrisSize, debrisRetry,
                    motion.x, motion.y, motion.z);
        }
    }
}
