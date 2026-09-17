package com.hbm.config;

/**
 * Client-only toggles. Plain static fields, matching the port's other config classes ({@link BombConfig},
 * {@code GeneralConfig}) — a real cross-loader config layer is still deferred.
 */
public final class ClientConfig {

    private ClientConfig() {}

    /** The full-screen white-out on a nearby nuclear detonation (old {@code NUKE_HUD_FLASH}). */
    public static boolean NUKE_HUD_FLASH = true;

    /** The HUD/camera shake on a nearby nuclear detonation (old {@code NUKE_HUD_SHAKE}). */
    public static boolean NUKE_HUD_SHAKE = true;

    /** The first-person camera kick after firing a gun (old {@code GUN_VISUAL_RECOIL}). */
    public static boolean GUN_VISUAL_RECOIL = true;

    /** Selects the hand-authored gun animation/casing branch, matching upstream's client setting. */
    public static boolean GUN_ANIMS_LEGACY = false;
}
