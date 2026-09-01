package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** Tickable loop bound to one factory, stopping when its last lane stops or the BE unloads. */
public final class ChemicalFactoryLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineChemicalFactory factory;

    public ChemicalFactoryLoopSound(TileEntityMachineChemicalFactory factory) {
        super(ModSounds.CHEMICAL_PLANT.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.factory = factory;
        BlockPos pos = factory.getBlockPos();
        x = pos.getX() + 0.5;
        y = pos.getY() + 0.5;
        z = pos.getZ() + 0.5;
        looping = true;
        delay = 0;
        pitch = 1F;
        volume = factory.getVolume(1F);
    }

    @Override
    public void tick() {
        BlockPos pos = factory.getBlockPos();
        boolean nearby = Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5) < 50D * 50D;
        if (factory.isRemoved() || !factory.isProcessing() || !nearby) {
            stop();
            return;
        }
        volume = factory.getVolume(1F);
    }
}
