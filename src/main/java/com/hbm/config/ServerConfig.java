package com.hbm.config;

import com.hbm.items.weapon.sedna.factory.GunValues;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.7 {@code ServerConfig}: the server variables {@code /ntmserver} lists, reads and sets, persisted in
 * {@code config/hbmConfig/hbmServer.json}.
 *
 * <p>{@code STRUCTURE_DEBUG} (default false) keeps structure placeholders visible when a structure is built: air
 * becomes {@code wand_air}, jigsaw blocks stay jigsaw blocks and loot/logic placeholders do not replace themselves.
 * The structure loading block switches it on for the duration of one load unless it is powered, exactly as 1.7 did.
 * The mine damages, {@code AUTOCAL_MAX_CLOCK} and {@code ITEM_HAZARD_DROP_TICKRATE} are bound to the fields the
 * landmines, AUTOCAL and the dropped-item hazard tick already read ({@link MineConfig}, {@link AutocalConfig},
 * {@link RadiationConfig}); the others are read where 1.7 read them: {@code DAMAGE_COMPATIBILITY_MODE}
 * ({@code EntityDamageUtil}), {@code ENTITY_TAINT_TRAILS} (tainted creeper, taint effect), {@code CRATE_OPEN_HELD}
 * ({@code ItemBlockStorageCrate}), {@code CRATE_KEEP_CONTENTS} (crate and clutter breaking), {@code ENABLE_MKU}
 * (the MKU contagion).
 */
public final class ServerConfig extends RunningConfig {

    private ServerConfig() {}

    public static final Map<String, ConfigWrapper<?>> configMap = new LinkedHashMap<>();

    public static final ConfigWrapper<Boolean> DAMAGE_COMPATIBILITY_MODE = new ConfigWrapper<>(false);
    public static final ConfigWrapper<Float> MINE_AP_DAMAGE =
            ConfigWrapper.bound(() -> MineConfig.MINE_AP_DAMAGE, v -> MineConfig.MINE_AP_DAMAGE = v);
    public static final ConfigWrapper<Float> MINE_HE_DAMAGE =
            ConfigWrapper.bound(() -> MineConfig.MINE_HE_DAMAGE, v -> MineConfig.MINE_HE_DAMAGE = v);
    public static final ConfigWrapper<Float> MINE_SHRAP_DAMAGE =
            ConfigWrapper.bound(() -> MineConfig.MINE_SHRAP_DAMAGE, v -> MineConfig.MINE_SHRAP_DAMAGE = v);
    public static final ConfigWrapper<Float> MINE_NUKE_DAMAGE =
            ConfigWrapper.bound(() -> MineConfig.MINE_NUKE_DAMAGE, v -> MineConfig.MINE_NUKE_DAMAGE = v);
    public static final ConfigWrapper<Float> MINE_NAVAL_DAMAGE =
            ConfigWrapper.bound(() -> MineConfig.MINE_NAVAL_DAMAGE, v -> MineConfig.MINE_NAVAL_DAMAGE = v);
    public static final ConfigWrapper<Boolean> ENTITY_TAINT_TRAILS = new ConfigWrapper<>(false);
    public static final ConfigWrapper<Boolean> CRATE_OPEN_HELD = new ConfigWrapper<>(true);
    public static final ConfigWrapper<Boolean> CRATE_KEEP_CONTENTS = new ConfigWrapper<>(true);
    public static final ConfigWrapper<Integer> ITEM_HAZARD_DROP_TICKRATE = ConfigWrapper.bound(
            () -> RadiationConfig.itemHazardDropTickRate, v -> RadiationConfig.itemHazardDropTickRate = v);
    public static final ConfigWrapper<Boolean> ENABLE_MKU = new ConfigWrapper<>(true);
    public static final ConfigWrapper<Boolean> STRUCTURE_DEBUG = new ConfigWrapper<>(false);
    public static final ConfigWrapper<Integer> AUTOCAL_MAX_CLOCK =
            ConfigWrapper.bound(() -> AutocalConfig.AUTOCAL_MAX_CLOCK, v -> AutocalConfig.AUTOCAL_MAX_CLOCK = v);
    public static final ConfigWrapper<Integer> MAX_BUFFER_LENGTH = new ConfigWrapper<>(256);
    public static final ConfigWrapper<Integer> MAX_STACK_SIZE = new ConfigWrapper<>(256);
    public static final ConfigWrapper<Integer> CONVEYOR_CRAM_MAX = new ConfigWrapper<>(25);
    public static final ConfigWrapper<Boolean> CONVEYOR_CRAM_EXPLODE = new ConfigWrapper<>(true);
    /** Large doors open by redstone only, except those with {@code DoorDecl.neverDemandRedstone}. */
    public static final ConfigWrapper<Boolean> ULTRA_LARP_MODE = new ConfigWrapper<>(false);
    /** Machine ports show their connections with particles ({@code TilePort.checkSubscribe/checkProvide}). */
    public static final ConfigWrapper<Boolean> DEBUG_PORT_VIEW = new ConfigWrapper<>(false);

    private static final String FILE = "hbmServer.json";

    static {
        configMap.put("DAMAGE_COMPATIBILITY_MODE", DAMAGE_COMPATIBILITY_MODE);
        configMap.put("MINE_AP_DAMAGE", MINE_AP_DAMAGE);
        configMap.put("MINE_HE_DAMAGE", MINE_HE_DAMAGE);
        configMap.put("MINE_SHRAP_DAMAGE", MINE_SHRAP_DAMAGE);
        configMap.put("MINE_NUKE_DAMAGE", MINE_NUKE_DAMAGE);
        configMap.put("MINE_NAVAL_DAMAGE", MINE_NAVAL_DAMAGE);
        configMap.put("ENTITY_TAINT_TRAILS", ENTITY_TAINT_TRAILS);
        configMap.put("CRATE_OPEN_HELD", CRATE_OPEN_HELD);
        configMap.put("CRATE_KEEP_CONTENTS", CRATE_KEEP_CONTENTS);
        configMap.put("ITEM_HAZARD_DROP_TICKRATE", ITEM_HAZARD_DROP_TICKRATE);
        configMap.put("ENABLE_MKU", ENABLE_MKU);
        configMap.put("STRUCTURE_DEBUG", STRUCTURE_DEBUG);
        configMap.put("AUTOCAL_MAX_CLOCK", AUTOCAL_MAX_CLOCK);
        configMap.put("MAX_BUFFER_LENGTH", MAX_BUFFER_LENGTH);
        configMap.put("MAX_STACK_SIZE", MAX_STACK_SIZE);
        configMap.put("CONVEYOR_CRAM_MAX", CONVEYOR_CRAM_MAX);
        configMap.put("CONVEYOR_CRAM_EXPLODE", CONVEYOR_CRAM_EXPLODE);
        configMap.put("ULTRA_LARP_MODE", ULTRA_LARP_MODE);
        configMap.put("DEBUG_PORT_VIEW", DEBUG_PORT_VIEW);

        GunValues.initDefaults();
    }

    /** Reads the config file if it exists, then writes the config file. */
    public static void initConfig() {
        Path config = getConfig(FILE);
        if (Files.exists(config)) readConfig(config, configMap);
        refresh();
    }

    /** Writes over the config file using the running config. */
    public static void refresh() {
        writeConfig(getConfig(FILE), configMap, "This file can be edited ingame using the /ntmserver command.");
    }

    /** Writes over the running config using the config file. */
    public static void reload() {
        Path config = getConfig(FILE);
        if (Files.exists(config)) readConfig(config, configMap);
    }
}
