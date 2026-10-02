package com.hbm.sound;

import com.hbm.registration.Reg;
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
    private static final Reg REG = Reg.of(SOUNDS);

    public static final RegistryObject<SoundEvent> LEVER_LARGE = REG.sound("block.lever_large");

    public static void init() {}
}
