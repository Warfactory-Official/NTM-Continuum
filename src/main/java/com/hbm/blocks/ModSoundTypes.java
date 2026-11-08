package com.hbm.blocks;

import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

/**
 * 1.7 {@code ModSoundTypes.pipe}. Only properties lambdas may touch this class: a {@code SoundType} needs a running
 * game, and the registration classes also initialise in DejsonMain's plain JVM.
 */
public final class ModSoundTypes {

    private ModSoundTypes() {}

    /** 1.7 {@code customDig(soundTypeMetal, "hbm:block.pipePlaced", 0.85, 0.85)}: breaking sounds 0.15 lower. */
    public static final SoundType PIPE = new ModSoundType(0.85F, 0.85F,
            ModSounds.PIPE_PLACED_EVENT, SoundEvents.METAL_STEP, ModSounds.PIPE_PLACED_EVENT,
            SoundEvents.METAL_HIT, SoundEvents.METAL_FALL, (in, rand, type) -> {
                if (type == ModSoundType.SubType.BREAK) in -= 0.15F;
                return in + rand.nextFloat() * 0.2F;
            });
}
