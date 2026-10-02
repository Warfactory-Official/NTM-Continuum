package com.hbm.sound;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/** The one sound this family adds: the suit battery's charge click ({@code item.battery}). */
public final class EnergyNetSounds {

    private EnergyNetSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);
    private static final Reg REG = Reg.of(SOUNDS);

    public static final RegistryObject<SoundEvent> ITEM_BATTERY = REG.sound("item.battery");

    public static void init() {}
}
