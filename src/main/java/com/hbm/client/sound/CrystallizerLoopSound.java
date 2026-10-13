package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * The looping operating sound an ore acidizer plays while it's processing, the chemical-plant loop
 * at the 1.12.2 tile's lower 0.75 pitch. A tickable client sound bound to one machine that stops
 * itself once the machine stops. Client-only, only constructed from {@link CrystallizerSoundHandler}.
 */
public class CrystallizerLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineCrystallizer te;

    public CrystallizerLoopSound(TileEntityMachineCrystallizer te) {
        super(ModSounds.CHEMICAL_PLANT.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.te = te;
        BlockPos pos = te.getBlockPos();
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.looping = true;
        this.delay = 0;
        this.pitch = 0.75F; // the 1.12.2 crystallizer's lower pitch on the shared chem-plant loop
        // Keep the base volume well under 1.0 so it audibly attenuates with distance (see ChemPlantLoopSound).
        this.volume = 0.5F * te.getVolume(0.5F);
    }

    @Override
    public void tick() {
        if (te.isRemoved() || !te.isOn) {
            this.stop();
            return;
        }
        this.volume = 0.5F * te.getVolume(0.5F);
    }
}
