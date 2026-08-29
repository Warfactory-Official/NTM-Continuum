package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** Looped centrifuge sound with the original audio-duration pitch ramp. */
public class CentrifugeLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineCentrifuge te;

    public CentrifugeLoopSound(TileEntityMachineCentrifuge te) {
        super(ModSounds.CENTRIFUGE_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.te = te;
        BlockPos pos = te.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        updateProperties();
    }

    @Override
    public void tick() {
        if (te.isRemoved() || te.audioDuration <= 10) {
            stop();
            return;
        }
        updateProperties();
    }

    public void shutdown() {
        stop();
    }

    private void updateProperties() {
        this.volume = te.getVolume(1F);
        this.pitch = (te.audioDuration - 10) / 100F + 0.5F;
    }
}
