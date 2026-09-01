package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** The Assembly Factory's looping motor hum while at least one lane is processing. */
public final class AssemblyFactoryLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineAssemblyFactory factory;

    public AssemblyFactoryLoopSound(TileEntityMachineAssemblyFactory factory) {
        super(ModSounds.MOTOR.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.factory = factory;
        BlockPos pos = factory.getBlockPos();
        x = pos.getX() + 0.5D;
        y = pos.getY() + 0.5D;
        z = pos.getZ() + 0.5D;
        looping = true;
        delay = 0;
        pitch = 0.75F;
        volume = factory.getVolume(0.5F);
    }

    @Override
    public void tick() {
        if (factory.isRemoved() || !factory.isProcessing()) {
            stop();
            return;
        }
        volume = factory.getVolume(0.5F);
    }

    /** Stops this loop from the factory sound dispatcher without exposing the base sound API. */
    public void stopLoop() {
        stop();
    }
}
