package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public class ExplosionEffectStandard implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {

        if (world.isClientSide()) return;
        if (!(world instanceof ServerLevel level)) return;

        RandomSource rand = world.getRandom();
        world.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);

        if (size >= 2.0F) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        } else {
            level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        // TODO: 1.7 :25 broadcasts explosion.affectedBlocks and performClient (:36-62) spawns a directional
        // explode+smoke pair per affected block; the packet and its client half are unported, so only the centre
        // burst ships. BlockProcessorNoDamage's affectedBlocks.clear() exists to suppress that missing pass.
    }
}
