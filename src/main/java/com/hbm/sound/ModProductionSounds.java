package com.hbm.sound;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/** Feature-local sound events; the thresher's diesel engine loop (old {@code hbm:block.engine}). */
public final class ModProductionSounds {
    private ModProductionSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);
    private static final Reg REG = Reg.of(SOUNDS);

    public static final RegistryObject<SoundEvent> ENGINE = REG.sound("block.engine");

    public static void init() {}
}
