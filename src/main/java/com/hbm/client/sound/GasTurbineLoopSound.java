package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

public final class GasTurbineLoopSound extends AbstractTickableSoundInstance {
    private final TileEntityMachineTurbineGas turbine;

    public GasTurbineLoopSound(TileEntityMachineTurbineGas turbine, float volume, float pitch) {
        super(ModSounds.TURBINE_GAS_RUNNING.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.turbine = turbine;
        BlockPos pos = turbine.getBlockPos();
        x = pos.getX() + 0.5D;
        y = pos.getY() + 0.5D;
        z = pos.getZ() + 0.5D;
        looping = true;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void update(float volume, float pitch) { this.volume = volume; this.pitch = pitch; }
    public void stopLoop() { stop(); }
    @Override public void tick() { if (turbine.isRemoved() || turbine.rpm < 10 || turbine.state == -1) stop(); }
}
