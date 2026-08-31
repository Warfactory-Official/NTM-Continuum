package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** Tickable motor hum for one precision assembler. */
public final class PrecisionAssemblerLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachinePrecAss te;

    public PrecisionAssemblerLoopSound(TileEntityMachinePrecAss te) {
        super(ModSounds.MOTOR.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.te = te;
        BlockPos pos = te.getBlockPos();
        x = pos.getX() + 0.5;
        y = pos.getY() + 0.5;
        z = pos.getZ() + 0.5;
        looping = true;
        pitch = 0.75F;
        volume = te.getVolume(0.5F);
    }

    @Override
    public void tick() {
        if (te.isRemoved() || !te.didProcess) {
            stop();
            return;
        }
        volume = te.getVolume(0.5F);
    }
}
