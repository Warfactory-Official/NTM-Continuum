package com.hbm.handler;

/**
 * HBM's own keybinds, as a common enum.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.handler.HbmKeybinds}. Only the enum is common: the client half
 * (registering the mappings and diffing their state each tick) lives in
 * {@code com.hbm.client.HbmKeybindsClient}, and the server half in {@link HbmKeybindsServer}. That split
 * exists because the enum is named by {@code ItemGunBaseNT}, which is a common class.
 *
 * <p>Ordinals travel over the wire in the keybind payload, so entries are only ever appended.
 */
public class HbmKeybinds {

    public enum EnumKeybind {
        /** Primary fire. Aliased to the vanilla attack button, which is suppressed while a gun is held. */
        GUN_PRIMARY,
        /** Secondary fire: underbarrel, alt mode. */
        GUN_SECONDARY,
        /** Tertiary: aim down sights on the standard configuration. */
        GUN_TERTIARY,
        RELOAD,
        /** Advanced glider activation (J). */
        JETPACK,
        /** Advanced glider hover toggle (H). */
        JETPACK_HOVER,
        /** Advanced glider compact-HUD toggle (U). */
        JETPACK_HUD,
        /** General backpack enable/disable toggle (C). */
        TOGGLE_JETPACK,
        /** General HUD enable/disable toggle (V). */
        TOGGLE_HEAD,
        /** 1.7 {@code DASH} (left Shift): the bismuth suit's dash. */
        DASH,
        /** 1.7 {@code TOGGLE_MAGNET} (Z): the lodestone/magnet armour modules on and off. */
        TOGGLE_MAGNET;

        public static final EnumKeybind[] VALUES = values();

        public static EnumKeybind byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
        }
    }
}
