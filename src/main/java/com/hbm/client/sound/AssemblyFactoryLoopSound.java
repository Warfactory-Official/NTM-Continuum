package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** The Assembly Factory's motor hum while a lane processes: 1.7's volume 0.5, range 15, pitch 0.75. */
public final class AssemblyFactoryLoopSound extends AudioDynamic {

    private final TileEntityMachineAssemblyFactory factory;

    public AssemblyFactoryLoopSound(TileEntityMachineAssemblyFactory factory) {
        super(ModSounds.MOTOR.get(), SoundSource.BLOCKS);
        this.factory = factory;
        BlockPos pos = factory.getBlockPos();
        setPosition(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
        setRange(15F);
        setPitch(0.75F);
        setVolume(factory.getVolume(0.5F));
        delay = 0;
        super.tick();
    }

    @Override
    public void tick() {
        if (factory.isRemoved() || !factory.isProcessing()) {
            stop();
            return;
        }
        setVolume(factory.getVolume(0.5F));
        super.tick();
    }

    /** Stops this loop from the factory sound dispatcher without exposing the base sound API. */
    public void stopLoop() {
        stop();
    }
}
