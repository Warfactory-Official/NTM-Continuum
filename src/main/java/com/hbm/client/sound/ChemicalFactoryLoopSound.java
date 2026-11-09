package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** Loop bound to one factory, 1.7's volume 1 and range 15, stopping when its last lane stops or the BE unloads. */
public final class ChemicalFactoryLoopSound extends AudioDynamic {

    private final TileEntityMachineChemicalFactory factory;

    public ChemicalFactoryLoopSound(TileEntityMachineChemicalFactory factory) {
        super(ModSounds.CHEMICAL_PLANT.get(), SoundSource.BLOCKS);
        this.factory = factory;
        BlockPos pos = factory.getBlockPos();
        setPosition(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
        setRange(15F);
        setVolume(factory.getVolume(1F));
        delay = 0;
        super.tick();
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
        setVolume(factory.getVolume(1F));
        super.tick();
    }
}
