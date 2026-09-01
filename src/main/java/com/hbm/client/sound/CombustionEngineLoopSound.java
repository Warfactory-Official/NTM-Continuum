package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** The combustion engine loop: legacy integer-position source, fixed pitch and muffler-scaled volume. */
final class CombustionEngineLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineCombustionEngine engine;

    CombustionEngineLoopSound(TileEntityMachineCombustionEngine engine) {
        super(ModSounds.IGENERATOR_OPERATE.get(), SoundSource.BLOCKS,
                SoundInstance.createUnseededRandom());
        this.engine = engine;
        BlockPos pos = engine.getBlockPos();
        // The old AudioWrapper was positioned at the integer BlockPos, not the block centre.
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.looping = true;
        this.delay = 0;
        this.pitch = 1F;
        this.volume = engine.getVolume(1);
    }

    @Override
    public void tick() {
        if (engine.isRemoved() || !engine.wasOn) {
            stop();
            return;
        }
        this.volume = engine.getVolume(1);
    }

    void stopLoop() {
        stop();
    }
}
