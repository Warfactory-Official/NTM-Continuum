package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityHeaterElectric;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * The electric heater's hum loop (the 1.12.2 {@code electricHum} AudioWrapper at volume 0.25). A
 * tickable client sound bound to one heater that stops itself once the heater stops. Client-only —
 * only constructed from {@link HeaterSoundHandler}.
 */
public class HeaterLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityHeaterElectric te;

    public HeaterLoopSound(TileEntityHeaterElectric te) {
        super(ModSounds.ELECTRIC_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.te = te;
        BlockPos pos = te.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.25F;
    }

    @Override
    public void tick() {
        if (te.isRemoved() || !te.isOn) {
            this.stop();
            return;
        }
        this.volume = 0.25F;
    }
}
