package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ExplosionEffectAmat}: below size 15 the vanilla explosion sound (volume 4, pitch 0.7 around 1.4),
 * from 15 up the mini-nuke boom; then the {@code amat} flash ({@code ParticleAmatFlash}) for everyone within
 * 200 blocks, scaled by the explosion size.
 */
public class ExplosionEffectAmat implements IExplosionSFX {

    @Override
    public void doEffect(ExplosionVNT explosion, Level world, double x, double y, double z, float size) {
        if (world.isClientSide()) return;
        RandomSource rand = world.getRandom();
        if (size < 15) {
            world.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
                    (1.4F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
        } else {
            world.playSound(null, x, y, z, ModSounds.MUKE_EXPLOSION.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
        }
        CompoundTag data = new CompoundTag();
        data.putFloat("scale", size);
        HbmEffectNT.AmatFlash.send(world, x, y, z, data, 200);
    }
}
