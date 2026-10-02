package com.hbm.sound;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events used by the door family. Ids are the original {@code sounds.json} keys lowercased
 * ({@code door.TransitionSealOpen} → {@code door.transition_seal_open}); the original declared
 * {@code door.wghStart}/{@code door.wgh_start} as two keys over one file, which collapse to one here.
 */
public final class ModDoorSounds {
    private ModDoorSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);
    private static final Reg REG = Reg.of(SOUNDS);

    public static final RegistryObject<SoundEvent> REACTOR_STOP = register("block.reactor_stop");
    public static final RegistryObject<SoundEvent> VAULT_SCRAPE_NEW = register("block.vault_scrape_new");
    public static final RegistryObject<SoundEvent> VAULT_THUD_NEW = register("block.vault_thud_new");
    public static final RegistryObject<SoundEvent> LOCK_OPEN = register("block.lock_open");
    /**
     * The vanilla-style mod doors' toggle sound (old {@code hbm:block.openDoor}). Built eagerly because
     * the doors' {@code BlockSetType} needs the event object while blocks are being constructed, before
     * the sound registry has necessarily flushed; the registration hands out this same instance.
     */
    public static final SoundEvent OPEN_DOOR_EVENT = SoundEvent.createVariableRangeEvent(Ids.of(Constants.MOD_ID, "block.open_door"));
    public static final RegistryObject<SoundEvent> OPEN_DOOR = REG.register(Registries.SOUND_EVENT, "block.open_door", () -> OPEN_DOOR_EVENT);
    public static final RegistryObject<SoundEvent> PIN_UNLOCK = register("item.pin_unlock");
    public static final RegistryObject<SoundEvent> PIN_BREAK = register("item.pin_break");

    public static final RegistryObject<SoundEvent> TRANSITION_SEAL_OPEN = register("door.transition_seal_open");
    public static final RegistryObject<SoundEvent> WGH_START = register("door.wgh_start");
    public static final RegistryObject<SoundEvent> WGH_STOP = register("door.wgh_stop");
    public static final RegistryObject<SoundEvent> ALARM6 = register("door.alarm6");
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_SHUT = register("door.sliding_door_shut");
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_OPENED = register("door.sliding_door_opened");
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_OPENING = register("door.sliding_door_opening");
    public static final RegistryObject<SoundEvent> GARAGE_MOVE = register("door.garage_move");
    public static final RegistryObject<SoundEvent> GARAGE_STOP = register("door.garage_stop");
    public static final RegistryObject<SoundEvent> LEVER = register("door.lever");
    public static final RegistryObject<SoundEvent> WGH_BIG_START = register("door.wgh_big_start");
    public static final RegistryObject<SoundEvent> WGH_BIG_STOP = register("door.wgh_big_stop");
    public static final RegistryObject<SoundEvent> QE_SLIDING_SHUT = register("door.qe_sliding_shut");
    public static final RegistryObject<SoundEvent> QE_SLIDING_OPENED = register("door.qe_sliding_opened");
    public static final RegistryObject<SoundEvent> QE_SLIDING_OPENING = register("door.qe_sliding_opening");
    public static final RegistryObject<SoundEvent> SLIDING_SEAL_OPEN = register("door.sliding_seal_open");
    public static final RegistryObject<SoundEvent> SLIDING_SEAL_STOP = register("door.sliding_seal_stop");

    private static RegistryObject<SoundEvent> register(String path) {
        return REG.sound(path);
    }

    public static void init() {}
}
