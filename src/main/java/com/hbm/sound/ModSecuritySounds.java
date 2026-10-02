package com.hbm.sound;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import com.hbm.tileentity.machine.TileEntityLockableBase;

/**
 * The padlock and spike sounds (old ids {@code block.lockHang}, {@code entity.slicer}; lower-cased like
 * {@code ModSounds}). {@code block.lock_open} / {@code item.pin_unlock} / {@code item.pin_break} are
 * registered by the door family's {@code ModDoorSounds}, whose {@code TileEntityLockableBase} plays them.
 */
public final class ModSecuritySounds {
    private ModSecuritySounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Constants.MOD_ID);
    private static final Reg REG = Reg.of(SOUNDS);

    /** {@code NTMSounds.PADLOCK}: the padlock snapping shut on a block. */
    public static final RegistryObject<SoundEvent> LOCK_HANG = register("block.lock_hang");
    public static final RegistryObject<SoundEvent> SLICER = register("entity.slicer");

    private static RegistryObject<SoundEvent> register(String name) {
        return REG.sound(name);
    }

    public static void init() {}
}
