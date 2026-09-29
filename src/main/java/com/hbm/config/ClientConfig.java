package com.hbm.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.7 {@code ClientConfig}: the client variables {@code /ntmclient} lists, reads and sets, persisted in
 * {@code config/hbmConfig/hbmClient.json}.
 *
 * <p>The toggles the port already reads stay plain static fields ({@link #NUKE_HUD_FLASH} and friends) so hot paths
 * pay nothing; their {@link #configMap} entries are bound to those fields. The map is built by {@link #initConfig},
 * which only the client calls, since some bindings reach client-only classes.
 */
public final class ClientConfig extends RunningConfig {

    private ClientConfig() {}

    /** The full-screen white-out on a nearby nuclear detonation (old {@code NUKE_HUD_FLASH}). */
    public static boolean NUKE_HUD_FLASH = true;

    /** The HUD/camera shake on a nearby nuclear detonation (old {@code NUKE_HUD_SHAKE}). */
    public static boolean NUKE_HUD_SHAKE = true;

    /** The first-person camera kick after firing a gun (old {@code GUN_VISUAL_RECOIL}). */
    public static boolean GUN_VISUAL_RECOIL = true;

    /** Draws reeds down to the floor of the water they stand in (old {@code RENDER_REEDS}); off draws the top only. */
    public static boolean RENDER_REEDS = true;

    /** Selects the hand-authored gun animation/casing branch, matching upstream's client setting. */
    public static boolean GUN_ANIMS_LEGACY = false;

    public static final Map<String, ConfigWrapper<?>> configMap = new LinkedHashMap<>();

    private static final String FILE = "hbmClient.json";

    /** 1.7 {@code initDefaults}, in 1.7 order, with 1.7's defaults. */
    private static synchronized void initDefaults() {
        if (!configMap.isEmpty()) return;
        configMap.put("GEIGER_OFFSET_HORIZONTAL", new ConfigWrapper<>(0));
        configMap.put("GEIGER_OFFSET_VERTICAL", new ConfigWrapper<>(0));
        configMap.put("INFO_OFFSET_HORIZONTAL", new ConfigWrapper<>(0));
        configMap.put("INFO_OFFSET_VERTICAL", new ConfigWrapper<>(0));
        configMap.put("INFO_POSITION", new ConfigWrapper<>(0));
        configMap.put("GUN_ANIMS_LEGACY", ConfigWrapper.bound(() -> GUN_ANIMS_LEGACY, v -> GUN_ANIMS_LEGACY = v));
        configMap.put("GUN_MODEL_FOV", new ConfigWrapper<>(false));
        configMap.put("GUN_VISUAL_RECOIL", ConfigWrapper.bound(() -> GUN_VISUAL_RECOIL, v -> GUN_VISUAL_RECOIL = v));
        configMap.put("GUN_ANIMATION_SPEED", new ConfigWrapper<>(1D));
        configMap.put("ITEM_TOOLTIP_SHOW_OREDICT", new ConfigWrapper<>(true));
        configMap.put("ITEM_TOOLTIP_SHOW_CUSTOM_NUKE", new ConfigWrapper<>(true));
        configMap.put("MAIN_MENU_WACKY_SPLASHES", new ConfigWrapper<>(true));
        configMap.put("DODD_RBMK_DIAGNOSTIC", new ConfigWrapper<>(true));
        configMap.put("RENDER_CABLE_HANG", ConfigWrapper.bound(() -> com.hbm.client.render.RenderPylonBase.RENDER_CABLE_HANG,
                v -> com.hbm.client.render.RenderPylonBase.RENDER_CABLE_HANG = v));
        configMap.put("NUKE_HUD_FLASH", ConfigWrapper.bound(() -> NUKE_HUD_FLASH, v -> NUKE_HUD_FLASH = v));
        configMap.put("NUKE_HUD_SHAKE", ConfigWrapper.bound(() -> NUKE_HUD_SHAKE, v -> NUKE_HUD_SHAKE = v));
        // 1.7: !Compat.isModLoaded(Compat.MOD_ANG); Angelica does not exist on these loaders
        configMap.put("RENDER_REEDS", ConfigWrapper.bound(() -> RENDER_REEDS, v -> RENDER_REEDS = v));
        configMap.put("NEI_HIDE_SECRETS", new ConfigWrapper<>(true));
        configMap.put("COOLING_TOWER_PARTICLES", new ConfigWrapper<>(true));
        configMap.put("RENDER_REBAR_SIMPLE", new ConfigWrapper<>(false));
        configMap.put("RENDER_HELIOSTAT_BEAM_LIMIT", new ConfigWrapper<>(250));
        configMap.put("RENDER_REBAR_LIMIT", new ConfigWrapper<>(250));
        configMap.put("TOOL_HUD_INDICATOR_X", ConfigWrapper.bound(
                () -> com.hbm.client.AbilityToolClientRegistrations.toolHudIndicatorX,
                v -> com.hbm.client.AbilityToolClientRegistrations.toolHudIndicatorX = v));
        configMap.put("TOOL_HUD_INDICATOR_Y", ConfigWrapper.bound(
                () -> com.hbm.client.AbilityToolClientRegistrations.toolHudIndicatorY,
                v -> com.hbm.client.AbilityToolClientRegistrations.toolHudIndicatorY = v));
        configMap.put("SHOW_BLOCK_META_OVERLAY", new ConfigWrapper<>(false));
        configMap.put("BADGES_HUD", new ConfigWrapper<>(true));
    }

    /** Initializes defaults, then reads the config file if it exists, then writes the config file. */
    public static void initConfig() {
        initDefaults();
        Path config = getConfig(FILE);
        if (Files.exists(config)) readConfig(config, configMap);
        refresh();
    }

    /** Writes over the config file using the running config. */
    public static void refresh() {
        writeConfig(getConfig(FILE), configMap, "This file can be edited ingame using the /ntmclient command.");
    }

    /** Writes over the running config using the config file. */
    public static void reload() {
        Path config = getConfig(FILE);
        if (Files.exists(config)) readConfig(config, configMap);
    }
}
