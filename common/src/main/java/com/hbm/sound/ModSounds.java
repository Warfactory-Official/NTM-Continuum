package com.hbm.sound;

import com.hbm.Constants;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {

    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);

    public static final RegistryObject<SoundEvent> MOTOR = register("block.motor");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STRIKE = register("block.assembler_strike");
    public static final RegistryObject<SoundEvent> ASSEMBLER_START = register("block.assembler_start");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STOP = register("block.assembler_stop");
    public static final RegistryObject<SoundEvent> CHEMICAL_PLANT = register("block.chemical_plant");
    public static final RegistryObject<SoundEvent> PRESS_OPERATE = register("block.pressoperate");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, name)));
    }

    public static void init() {}
}
