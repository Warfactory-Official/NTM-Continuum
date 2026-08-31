package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** Tickable fensuHum loop; pitch follows the exact REDD speed formula. */
public final class FensuLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityBatteryREDD battery;

    public FensuLoopSound(TileEntityBatteryREDD battery, float volume, float pitch) {
        super(ModSounds.FENSU_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.battery = battery;
        BlockPos pos = battery.getBlockPos();
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
        looping = true;
        update(volume, pitch);
    }

    public void update(float volume, float pitch) {
        this.volume = volume;
        this.pitch = pitch;
    }

    public void stopLoop() {
        stop();
    }

    @Override
    public void tick() {
        if (battery.isRemoved() || battery.getSpeed() <= 0F) {
            stop();
            return;
        }
        update(battery.getVolume(1.5F), 0.5F + battery.getSpeed() / 15F * 1.5F);
    }
}
