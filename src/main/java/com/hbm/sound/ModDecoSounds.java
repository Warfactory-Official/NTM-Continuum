package com.hbm.sound;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events of the decorative family: the 1.7 {@code ModSoundTypes.platemetal} step/place sounds
 * ({@code block.platemetalPlace}, {@code step.platemetal} lowercased). Built eagerly because the
 * {@code SoundType} needs the event objects while the blocks are constructed; the registrations hand
 * out the same instances (the {@code ModDoorSounds.OPEN_DOOR} pattern).
 */
public final class ModDecoSounds {
    private ModDecoSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);

    public static final SoundEvent PLATEMETAL_PLACE_EVENT = SoundEvent.createVariableRangeEvent(Ids.of(Constants.MOD_ID, "block.platemetal_place"));
    public static final SoundEvent PLATEMETAL_STEP_EVENT = SoundEvent.createVariableRangeEvent(Ids.of(Constants.MOD_ID, "step.platemetal"));
    public static final RegistryObject<SoundEvent> PLATEMETAL_PLACE = SOUNDS.register("block.platemetal_place", () -> PLATEMETAL_PLACE_EVENT);
    public static final RegistryObject<SoundEvent> PLATEMETAL_STEP = SOUNDS.register("step.platemetal", () -> PLATEMETAL_STEP_EVENT);

    public static void init() {}
}
