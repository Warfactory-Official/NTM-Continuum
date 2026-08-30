package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

public final class TurbofanLoopSound extends AbstractTickableSoundInstance {
    private final TileEntityMachineTurbofan fan;

    public TurbofanLoopSound(TileEntityMachineTurbofan fan, float volume, float pitch) {
        super(ModSounds.TURBOFAN_OPERATE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.fan = fan;
        BlockPos pos = fan.getBlockPos();
        x = pos.getX() + 0.5D; y = pos.getY() + 0.5D; z = pos.getZ() + 0.5D;
        looping = true;
        this.volume = volume; this.pitch = pitch;
    }

    public void update(float volume, float pitch) { this.volume = volume; this.pitch = pitch; }
    public void stopLoop() { stop(); }
    @Override public void tick() { if (fan.isRemoved() || fan.momentum <= 0) stop(); }
}
