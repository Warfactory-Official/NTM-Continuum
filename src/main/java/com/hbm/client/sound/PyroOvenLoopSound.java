package com.hbm.client.sound;

import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import com.hbm.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** The pyro oven loop: volume is updated with the legacy muffler calculation every client tick. */
final class PyroOvenLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachinePyroOven oven;

    PyroOvenLoopSound(TileEntityMachinePyroOven oven) {
        super(ModSounds.PYRO_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.oven = oven;
        BlockPos pos = oven.getBlockPos();
        // The old AudioWrapper was positioned at the integer BlockPos, not the block centre.
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.looping = true;
        this.delay = 0;
        this.pitch = 1F;
        this.volume = oven.getVolume(1);
    }

    @Override
    public void tick() {
        if (oven.isRemoved() || !oven.isProgressing) {
            stop();
            return;
        }
        this.volume = oven.getVolume(1);
    }

    void stopLoop() {
        stop();
    }
}
