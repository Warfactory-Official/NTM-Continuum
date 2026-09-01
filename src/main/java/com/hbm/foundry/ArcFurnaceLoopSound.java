package com.hbm.foundry;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** The old electric-hum loop while the Arc Furnace consumes power. */
public class ArcFurnaceLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineArcFurnaceLarge furnace;

    public ArcFurnaceLoopSound(TileEntityMachineArcFurnaceLarge furnace) {
        super(ModSounds.ELECTRIC_HUM.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.furnace = furnace;
        BlockPos pos = furnace.getBlockPos();
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
        this.looping = true;
        this.delay = 0;
        updateProperties();
    }

    @Override public void tick() {
        if (furnace.isRemoved() || !furnace.isProgressing) stop();
        else updateProperties();
    }

    public void shutdown() { stop(); }

    private void updateProperties() {
        this.volume = 1.5F * furnace.getVolume(1);
        this.pitch = 0.75F;
    }
}
