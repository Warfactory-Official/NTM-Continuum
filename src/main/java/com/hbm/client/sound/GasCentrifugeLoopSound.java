package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineGasCent;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** Looped gas centrifuge sound with the legacy audio-duration pitch ramp. */
public class GasCentrifugeLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineGasCent gasCent;

    public GasCentrifugeLoopSound(TileEntityMachineGasCent gasCent) {
        super(ModSounds.CENTRIFUGE_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.gasCent = gasCent;
        BlockPos pos = gasCent.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        updateProperties();
    }

    @Override
    public void tick() {
        if (gasCent.isRemoved() || gasCent.audioDuration <= 10) {
            stop();
            return;
        }
        updateProperties();
    }

    public void shutdown() { stop(); }

    private void updateProperties() {
        this.volume = gasCent.getVolume(1F);
        this.pitch = (gasCent.audioDuration - 10) / 100F + 0.5F;
    }
}
