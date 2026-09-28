package com.hbm.config;

import net.minecraft.server.MinecraftServer;

/** AUTOCAL server setting, persisted in 1.7's shared {@code hbmServer.json} through {@link ServerConfig}. */
public final class AutocalConfig {
    private AutocalConfig() {}

    /** Old AUTOCAL_MAX_CLOCK default: clock cycles processed per game tick. */
    public static int AUTOCAL_MAX_CLOCK = 20;

    /**
     * Called by every loader once a server has started. The shared file belongs to {@link ServerConfig} (1.7
     * {@code ServerConfig.initConfig}), which binds its {@code AUTOCAL_MAX_CLOCK} entry to this field.
     */
    public static void load(MinecraftServer server) {
        ServerConfig.initConfig();
    }
}
