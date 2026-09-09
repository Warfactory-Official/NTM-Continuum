package com.hbm.client.particle;

import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
import dev.golem.core.render.Particles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

/** Client handlers for the explosion effects required by the antimatter dependency path. */
public final class SingularityParticleHandlers {
    private SingularityParticleHandlers() {}

    public static void register() {
        HbmEffectNT.BlockBurst.setHandler(SingularityParticleHandlers::blockBurst);
        HbmEffectNT.Smoke_Cloud.setHandler(SingularityParticleHandlers::smokeCloud);
        HbmEffectNT.Smoke_Shock.setHandler(SingularityParticleHandlers::smokeShock);
    }

    private static void blockBurst(net.minecraft.world.level.Level level, double x, double y, double z,
                                   CompoundTag data) {
        if (level instanceof ClientLevel client) {
            BlockPos pos = BlockPos.containing(x, y, z);
            client.addDestroyBlockEffect(pos, Block.stateById(Tags.getIntOr(data, "block", 0)));
        }
    }

    private static void smokeCloud(net.minecraft.world.level.Level level, double x, double y, double z,
                                   CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        int count = Math.max(1, Tags.getIntOr(data, "count", 0));
        for (int i = 0; i < count; i++) {
            net.minecraft.util.RandomSource random = client.getRandom();
            double motionY = random.nextGaussian() * (1D + count / 100D);
            double motionX = random.nextGaussian() * (1D + count / 150D);
            double motionZ = random.nextGaussian() * (1D + count / 150D);
            if (random.nextBoolean()) motionY = Math.abs(motionY);
            Particles.add(new ParticleExSmoke(client, x, y, z).setMotion(motionX, motionY, motionZ));
        }
    }

    private static void smokeShock(net.minecraft.world.level.Level level, double x, double y, double z,
                                   CompoundTag data) {
        if (!(level instanceof ClientLevel client)) return;
        int count = Math.max(1, Tags.getIntOr(data, "count", 0));
        double strength = Tags.getDoubleOr(data, "strength", 0D);
        net.minecraft.util.RandomSource random = client.getRandom();
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
}
