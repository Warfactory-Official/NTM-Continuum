package com.hbm.sound;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/** Sound events used only by the ordinary Gen-3 Sedna firearm batch. */
public final class ModGunSounds {

    private ModGunSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);

    public static final RegistryObject<SoundEvent> FIRE_BLACK_POWDER = register("weapon.fire.blackpowder");
    public static final RegistryObject<SoundEvent> FIRE_ASSAULT = register("weapon.fire.assault");
    public static final RegistryObject<SoundEvent> SWITCH_MODE_1 = register("weapon.switchmode1");
    public static final RegistryObject<SoundEvent> SWITCH_MODE_2 = register("weapon.switchmode2");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                dev.golem.core.Ids.of(Constants.MOD_ID, name)));
    }

    public static void init() {}
}
