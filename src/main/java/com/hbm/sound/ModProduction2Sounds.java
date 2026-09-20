package com.hbm.sound;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/** Feature-local sound events: the excavator GUI's large lever click (old {@code hbm:block.leverLarge}). */
public final class ModProduction2Sounds {
    private ModProduction2Sounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);

    public static final RegistryObject<SoundEvent> LEVER_LARGE = SOUNDS.register("block.lever_large",
            () -> SoundEvent.createVariableRangeEvent(dev.golem.core.Ids.of(Constants.MOD_ID, "block.lever_large")));

    public static void init() {}
}
