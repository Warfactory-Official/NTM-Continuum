package com.hbm.sound;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import com.hbm.items.machine.ItemCassette;

/**
 * Feature-local sound events. The Leviathan reuses {@code ModSounds.INDUSTRIAL_TURBINE} (1.7 plays the very
 * same {@code chungusTurbine} file for both turbines) and {@code ModSounds.CHUNGUS_LEVER}; the geothermal loop
 * and the twenty siren tracks are registered here. Keys are the 1.7 {@code sounds.json} keys spelled in
 * snake case, since an identifier may not carry an upper-case letter.
 */
public final class ModPowerGenSounds {
    private ModPowerGenSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);
    /** Declared before the first {@code register} call: static initialisers run in textual order. */
    private static final java.util.Map<String, RegistryObject<SoundEvent>> BY_KEY = new java.util.HashMap<>();

    /** 1.7 {@code hbm:block.hephaestusRunning}. */
    public static final RegistryObject<SoundEvent> HEPHAESTUS_LOOP = register("block.hephaestus_running");

    // 1.7 ItemCassette.TrackType sound locations, in enum order.
    public static final RegistryObject<SoundEvent> ALARM_HATCH = register("alarm.hatch");
    public static final RegistryObject<SoundEvent> ALARM_AUTOPILOT = register("alarm.autopilot");
    public static final RegistryObject<SoundEvent> ALARM_AMS_SIREN = register("alarm.ams_siren");
    public static final RegistryObject<SoundEvent> ALARM_BLAST_DOOR = register("alarm.blast_door_alarm");
    public static final RegistryObject<SoundEvent> ALARM_APC_LOOP = register("alarm.apc_loop");
    public static final RegistryObject<SoundEvent> ALARM_KLAXON = register("alarm.klaxon");
    public static final RegistryObject<SoundEvent> ALARM_FO_KLAXON_A = register("alarm.fo_klaxon_a");
    public static final RegistryObject<SoundEvent> ALARM_FO_KLAXON_B = register("alarm.fo_klaxon_b");
    public static final RegistryObject<SoundEvent> ALARM_REGULAR_SIREN = register("alarm.regular_siren");
    public static final RegistryObject<SoundEvent> ALARM_CLASSIC = register("alarm.classic");
    public static final RegistryObject<SoundEvent> ALARM_BANK_ALARM = register("alarm.bank_alarm");
    public static final RegistryObject<SoundEvent> ALARM_BEEP_SIREN = register("alarm.beep_siren");
    public static final RegistryObject<SoundEvent> ALARM_CONTAINER_ALARM = register("alarm.container_alarm");
    public static final RegistryObject<SoundEvent> ALARM_SWEEP_SIREN = register("alarm.sweep_siren");
    public static final RegistryObject<SoundEvent> ALARM_STRIDER_SIREN = register("alarm.strider_siren");
    public static final RegistryObject<SoundEvent> ALARM_AIR_RAID = register("alarm.air_raid");
    public static final RegistryObject<SoundEvent> ALARM_NOSTROMO_SIREN = register("alarm.nostromo_siren");
    public static final RegistryObject<SoundEvent> ALARM_EAS_ALARM = register("alarm.eas_alarm");
    public static final RegistryObject<SoundEvent> ALARM_APC_PASS = register("alarm.apc_pass");
    public static final RegistryObject<SoundEvent> ALARM_RAZORTRAIN_HORN = register("alarm.razortrain_horn");


    private static RegistryObject<SoundEvent> register(String name) {
        RegistryObject<SoundEvent> event = SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(dev.golem.core.Ids.of(Constants.MOD_ID, name)));
        BY_KEY.put(name, event);
        return event;
    }

    /** The event registered under {@code key} ({@link ItemCassette.TrackType#getSoundKey()}), or null. */
    public static @org.jspecify.annotations.Nullable SoundEvent byKey(@org.jspecify.annotations.Nullable String key) {
        RegistryObject<SoundEvent> event = key == null ? null : BY_KEY.get(key);
        return event == null ? null : event.get();
    }

    public static void init() {}
}
