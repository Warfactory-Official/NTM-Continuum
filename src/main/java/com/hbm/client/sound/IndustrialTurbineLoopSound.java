package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** Tickable industrial turbine loop; the handler supplies its speed-dependent volume and pitch. */
public final class IndustrialTurbineLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineIndustrialTurbine turbine;

    public IndustrialTurbineLoopSound(TileEntityMachineIndustrialTurbine turbine, float volume, float pitch) {
        super(ModSounds.INDUSTRIAL_TURBINE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.turbine = turbine;
        BlockPos pos = turbine.getBlockPos();
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
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
        if (turbine.isRemoved() || turbine.spin <= 0D) stop();
    }
}
