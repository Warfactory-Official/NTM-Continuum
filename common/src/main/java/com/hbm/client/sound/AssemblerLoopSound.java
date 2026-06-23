package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

public class AssemblerLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineAssemblyMachine te;

    public AssemblerLoopSound(TileEntityMachineAssemblyMachine te) {
        super(ModSounds.MOTOR.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.te = te;
        BlockPos pos = te.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        this.pitch = 0.75F;
        this.volume = 0.5F * te.getVolume(0.5F);
    }

    @Override
    public void tick() {
        if (te.isRemoved() || !te.didProcess) {
            this.stop();
            return;
        }
        this.volume = 0.5F * te.getVolume(0.5F);
    }
}
