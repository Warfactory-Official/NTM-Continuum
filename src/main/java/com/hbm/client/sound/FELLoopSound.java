package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityFEL;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * The FEL's looping laser hum. Unlike the other machine loops this one <b>ramps</b>: the machine keeps
 * an {@code audioDuration} counter that climbs by 2 per firing tick and falls by 3 otherwise (clamped
 * 0–60), the loop plays only above 10, and the pitch follows {@code (n − 10)/100 + 0.5}. The result is
 * a laser that winds up and spins down instead of clicking on and off.
 *
 * <p>Client-only — constructed solely from {@link FELSoundHandler}.
 */
public class FELLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityFEL te;

    public FELLoopSound(TileEntityFEL te) {
        super(ModSounds.FEL.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.te = te;
        BlockPos pos = te.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        update();
    }

    @Override
    public void tick() {
        if (te.isRemoved() || !te.isAudioPlaying()) {
            this.stop();
            return;
        }
        update();
    }

    private void update() {
        // Old: getLoopedSound(..., 2.0F volume, 10F range, 2.0F) with updateVolume(getVolume(2F)).
        this.volume = te.getVolume(2F);
        this.pitch = te.audioPitch();
    }
}
