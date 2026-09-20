package com.hbm.sound;

import com.hbm.items.machine.ItemCassette.SoundType;
import com.hbm.tileentity.machine.TileEntityMachineSiren;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1.7 {@code SoundLoopSiren} over {@code SoundLoopMachine}: a positioned, un-attenuated sound at the siren's
 * block corner whose volume is {@code 2 − 2·distance/range} ({@code func(f, intendedVolume)}), looping
 * exactly when its track is a {@code LOOP}, and done the moment the tile is gone. The sound category is
 * {@code record}, as {@code sounds.json} filed every alarm.
 */
public final class SoundLoopSiren extends AbstractTickableSoundInstance {

    private final BlockEntity tile;
    private final SoundEvent event;
    public float intendedVolume = 10.0F;
    public SoundType type;

    public SoundLoopSiren(SoundEvent event, BlockEntity tile, SoundType type) {
        super(event, SoundSource.RECORDS, RandomSource.create());
        this.event = event;
        this.tile = tile;
        this.type = type;
        BlockPos pos = tile.getBlockPos();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.looping = true;
        this.volume = 1F;
        this.pitch = 1F;
        this.delay = 0;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (tile.isRemoved()) {
            stop();
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            float f = (float) Math.sqrt(Math.pow(x - player.getX(), 2) + Math.pow(y - player.getY(), 2)
                    + Math.pow(z - player.getZ(), 2));
            volume = func(f, intendedVolume);
        } else {
            volume = intendedVolume;
        }

        if (tile instanceof TileEntityMachineSiren) {
            this.looping = type == SoundType.LOOP;
        } else {
            stop();
        }
    }

    public BlockEntity getTE() {
        return tile;
    }

    public SoundEvent getEvent() {
        return event;
    }

    public void endSound() {
        stop();
    }

    public void setRepeat(boolean repeat) {
        this.looping = repeat;
    }

    /** 1.7: {@code (f / v) * -2 + 2}; the engine clamps a negative volume to silence. */
    public static float func(float f, float v) {
        return (f / v) * -2 + 2;
    }
}
