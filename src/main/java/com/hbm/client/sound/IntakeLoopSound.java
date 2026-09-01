package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

/** The legacy motor loop used by an operating air intake. */
public final class IntakeLoopSound extends AbstractTickableSoundInstance {

    private final TileEntityMachineIntake intake;

    public IntakeLoopSound(TileEntityMachineIntake intake) {
        super(ModSounds.MOTOR.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.intake = intake;
        BlockPos pos = intake.getBlockPos();
        x = pos.getX() + 0.5D;
        y = pos.getY() + 0.5D;
        z = pos.getZ() + 0.5D;
        looping = true;
        delay = 0;
        volume = intake.getVolume(0.25F);
        pitch = 1.0F;
    }

    @Override
    public void tick() {
        if (intake.isRemoved() || !intake.isMotorRunning()) {
            stop();
            return;
        }
        volume = intake.getVolume(0.25F);
    }

    public void stopLoop() {
        stop();
    }
}
