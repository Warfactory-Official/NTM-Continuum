package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/**
 * The ore acidizer's loop while it runs: 1.7's chemical plant loop at pitch 0.75, volume 1 and range 15. Only
 * constructed from {@link CrystallizerSoundHandler}.
 */
public class CrystallizerLoopSound extends AudioDynamic {

    private final TileEntityMachineCrystallizer te;

    public CrystallizerLoopSound(TileEntityMachineCrystallizer te) {
        super(ModSounds.CHEMICAL_PLANT.get(), SoundSource.BLOCKS);
        this.te = te;
        BlockPos pos = te.getBlockPos();
        setPosition(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
        setRange(15F);
        setPitch(0.75F);
        setVolume(te.getVolume(1F));
        delay = 0;
        super.tick();
    }

    @Override
    public void tick() {
        if (te.isRemoved() || !te.isOn) {
            stop();
            return;
        }
        setVolume(te.getVolume(1F));
        super.tick();
    }
}
