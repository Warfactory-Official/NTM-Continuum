package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

public final class LargeTurbineLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineLargeTurbine turbine;

    public LargeTurbineLoopSound(TileEntityMachineLargeTurbine turbine, float volume, float pitch) {
        super(ModSounds.TURBOFAN_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.turbine = turbine;
        BlockPos pos = turbine.getBlockPos();
        x = pos.getX() + 0.5D;
        y = pos.getY() + 0.5D;
        z = pos.getZ() + 0.5D;
        looping = true;
        this.volume = volume;
        this.pitch = pitch;
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
        if (turbine.isRemoved() || turbine.fanAcceleration <= 0F) stop();
    }
}
