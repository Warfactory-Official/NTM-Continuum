package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityChungus;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** Tickable Leviathan turbine loop; the handler supplies its speed-dependent volume and pitch. */
public final class ChungusLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityChungus chungus;

    public ChungusLoopSound(TileEntityChungus chungus, float volume, float pitch) {
        super(ModSounds.CHUNGUS_TURBINE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.chungus = chungus;
        BlockPos pos = chungus.getBlockPos();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.looping = true;
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
        if (chungus.isRemoved() || chungus.getTurbineSpeed() <= 0F) stop();
    }
}
