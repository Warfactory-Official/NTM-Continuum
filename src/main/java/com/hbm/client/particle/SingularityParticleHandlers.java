package com.hbm.client.particle;

import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Client handlers for the explosion effects required by the antimatter dependency path. */
public final class SingularityParticleHandlers {
    private SingularityParticleHandlers() {}

    public static void register() {
        HbmEffectNT.BlockBurst.setHandler(SingularityParticleHandlers::blockBurst);
        HbmEffectNT.Smoke_Cloud.setHandler(SingularityParticleHandlers::smokeCloud);
        HbmEffectNT.Smoke_Shock.setHandler(SingularityParticleHandlers::smokeShock);
        HbmEffectNT.Smoke_Radial.setHandler(SingularityParticleHandlers::smokeRadial);
        HbmEffectNT.Smoke_FoamSplash.setHandler(SingularityParticleHandlers::smokeFoamSplash);
    }

    private static void blockBurst(Level level, double x, double y, double z,
                                   CompoundTag data) {
        if (level instanceof ClientLevel client) {
            BlockPos pos = BlockPos.containing(x, y, z);
            client.addDestroyBlockEffect(pos, Block.stateById(Tags.getIntOr(data, "block", 0)));
        }
    }

    private static void smokeCloud(Level level, double x, double y, double z,
                                   CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        int count = Math.max(1, Tags.getIntOr(data, "count", 0));
        for (int i = 0; i < count; i++) {
            RandomSource random = client.getRandom();
            // 1.7 ClientProxy smoke "cloud": count / 100 and count / 150 are INT divisions
            double motionY = random.nextGaussian() * (1 + (count / 100));
            double motionX = random.nextGaussian() * (1 + (count / 150));
            double motionZ = random.nextGaussian() * (1 + (count / 150));
            if (random.nextBoolean()) motionY = Math.abs(motionY);
            Particles.add(new ParticleExSmoke(client, x, y, z).setMotion(motionX, motionY, motionZ));
        }
    }

    private static void smokeShock(Level level, double x, double y, double z,
                                   CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        int count = Math.max(1, Tags.getIntOr(data, "count", 0));
        double strength = Tags.getDoubleOr(data, "strength", 0D);
        RandomSource random = client.getRandom();
        double angle = random.nextInt(360);
        double vx = strength * Math.cos(angle), vz = -strength * Math.sin(angle);
        for (int i = 0; i < count; i++) {
            Particles.add(new ParticleExSmoke(client, x, y, z).setMotion(vx, 0D, vz));
            double step = Math.PI * 2D / count;
            double nextX = vx * Math.cos(step) + vz * Math.sin(step);
            double nextZ = vz * Math.cos(step) - vx * Math.sin(step);
            vx = nextX;
            vz = nextZ;
        }
    }

    /** 1.7 ClientProxy smoke "radial": every axis on a gaussian scaled by {@code 1 + count / 50} (INT division). */
    private static void smokeRadial(Level level, double x, double y, double z,
                                    CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        int count = Math.max(1, Tags.getIntOr(data, "count", 0));
        RandomSource random = client.getRandom();
        for (int i = 0; i < count; i++) {
            double motionY = random.nextGaussian() * (1 + (count / 50));
            double motionX = random.nextGaussian() * (1 + (count / 50));
            double motionZ = random.nextGaussian() * (1 + (count / 50));
            Particles.add(new ParticleExSmoke(client, x, y, z).setMotion(motionX, motionY, motionZ));
        }
    }

    /**
     * 1.7 ClientProxy smoke "foamSplash": {@code count} foam particles on a ring of radius {@code range}
     * (0 when absent, as every 1.7 sender leaves it), each with max age 50 and no initial motion.
     */
    private static void smokeFoamSplash(Level level, double x, double y, double z,
                                        CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        int count = Math.max(1, Tags.getIntOr(data, "count", 0));
        double strength = Tags.getDoubleOr(data, "range", 0D);
        RandomSource random = client.getRandom();
        double vx = strength, vz = 0D;
        for (int i = 0; i < count; i++) {
            double[] rotated = rotateY(vx, vz, Math.toRadians(random.nextFloat() * 360F));
            vx = rotated[0];
            vz = rotated[1];
            Particles.add(new ParticleFoam(client, x + vx, y, z + vz).setMaxAge(50).setMotion(0D, 0D, 0D));
            // 1.7 rotateAroundY(360 / count): an INT count of radians, kept
            rotated = rotateY(vx, vz, 360 / count);
            vx = rotated[0];
            vz = rotated[1];
        }
    }

    /** 1.7 Vec3.rotateAroundY with float trig, as MathHelper did. */
    private static double[] rotateY(double x, double z, double angle) {
        float cos = Mth.cos((float) angle);
        float sin = Mth.sin((float) angle);
        return new double[] {x * cos + z * sin, z * cos - x * sin};
    }
}
