package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * The looping operating sound a chemical plant plays while it's processing — a tickable client sound
 * bound to one machine that stops itself once the machine stops processing or is removed (the 1.12.2
 * {@code AudioWrapper} loop). Client-only — only constructed from {@link ChemPlantSoundHandler}.
 */
public class ChemPlantLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineChemicalPlant te;

    public ChemPlantLoopSound(TileEntityMachineChemicalPlant te) {
        super(ModSounds.CHEMICAL_PLANT.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.te = te;
        BlockPos pos = te.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        this.pitch = 1.0F;
        // Keep the base volume well under 1.0 so it audibly attenuates with distance, like the assembly
        // machine's motor loop (volume 1.0 stays loud across the whole 16-block range — "heard everywhere").
        this.volume = 0.5F * te.getVolume(0.5F);
    }

    @Override
    public void tick() {
        if (te.isRemoved() || !te.didProcess) {
            this.stop();
            return;
        }
        this.volume = 0.5F * te.getVolume(0.5F);
    }
}
