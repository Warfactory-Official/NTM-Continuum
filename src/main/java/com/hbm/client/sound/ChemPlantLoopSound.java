package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * The chemical plant's loop while it processes, 1.7's {@code getLoopedSound(CHEMPLANT_LOOP, 1, 15, 1)}: full volume
 * at the machine, fading to silence 15 blocks out. Only constructed from {@link ChemPlantSoundHandler}.
 */
public class ChemPlantLoopSound extends AudioDynamic {

    private final TileEntityMachineChemicalPlant te;

    public ChemPlantLoopSound(TileEntityMachineChemicalPlant te) {
        super(ModSounds.CHEMICAL_PLANT.get(), SoundSource.BLOCKS);
        this.te = te;
        BlockPos pos = te.getBlockPos();
        setPosition(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
        setRange(15F);
        setVolume(te.getVolume(1F));
        delay = 0;
        super.tick();
    }

    @Override
    public void tick() {
        if (te.isRemoved() || !te.didProcess) {
            stop();
            return;
        }
        setVolume(te.getVolume(1F));
        super.tick();
    }
}
