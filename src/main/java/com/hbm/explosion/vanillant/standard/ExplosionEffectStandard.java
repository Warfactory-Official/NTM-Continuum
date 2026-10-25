package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.packet.ExplosionBlocksPayload;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class ExplosionEffectStandard implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {

        if (!(world instanceof ServerLevel level)) return;

        RandomSource rand = world.getRandom();
        world.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
                (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);

        ExplosionBlocksPayload.send(level, x, y, z, explosion.size, explosion.affectedBlockPositions);
    }

    /** 1.7's client half: 1.7 "explode" is the modern poof, "hugeexplosion" the emitter. */
    public static void performClient(Level world, double x, double y, double z, float size, List<BlockPos> affectedBlocks) {

        if (size >= 2.0F) {
            world.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0D, 0.0D, 0.0D);
        } else {
            world.addParticle(ParticleTypes.EXPLOSION, x, y, z, 1.0D, 0.0D, 0.0D);
        }

        RandomSource rand = world.getRandom();
        for (BlockPos pos : affectedBlocks) {
            double oX = pos.getX() + rand.nextFloat();
            double oY = pos.getY() + rand.nextFloat();
            double oZ = pos.getZ() + rand.nextFloat();
            double dX = oX - x;
            double dY = oY - y;
            double dZ = oZ - z;
            double delta = (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
            dX /= delta;
            dY /= delta;
            dZ /= delta;
            double mod = 0.5D / (delta / size + 0.1D);
            mod *= rand.nextFloat() * rand.nextFloat() + 0.3F;
            dX *= mod;
            dY *= mod;
            dZ *= mod;
            world.addParticle(ParticleTypes.POOF, (oX + x) / 2.0D, (oY + y) / 2.0D, (oZ + z) / 2.0D, dX, dY, dZ);
            world.addParticle(ParticleTypes.SMOKE, oX, oY, oZ, dX, dY, dZ);
        }
    }
}
