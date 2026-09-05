package com.hbm.config;

import com.hbm.Constants;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** AUTOCAL server setting, persisted in the legacy shared HBM server config file. */
public final class AutocalConfig {
    private AutocalConfig() {}

    /** Old AUTOCAL_MAX_CLOCK default: clock cycles processed per game tick. */
    public static int AUTOCAL_MAX_CLOCK = 20;

    /** Reads and rewrites the shared config once when a server is ready, preserving unknown keys. */
    public static void load(MinecraftServer server) {
        Path file = dev.golem.core.platform.ConfigFiles.serverConfig(server, "hbmServer.json");
        JsonObject config = new JsonObject();
        try {
            Files.createDirectories(file.getParent());
            if (Files.isRegularFile(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    var parsed = JsonParser.parseReader(reader);
                    if (parsed.isJsonObject()) config = parsed.getAsJsonObject();
                }
            }
            if (config.has("AUTOCAL_MAX_CLOCK") && config.get("AUTOCAL_MAX_CLOCK").isJsonPrimitive()) {
                try {
                    AUTOCAL_MAX_CLOCK = config.getAsJsonPrimitive("AUTOCAL_MAX_CLOCK").getAsInt();
                } catch (RuntimeException ignored) {
                    AUTOCAL_MAX_CLOCK = 20;
                }
            }
            config.addProperty("AUTOCAL_MAX_CLOCK", AUTOCAL_MAX_CLOCK);
            if (!config.has("info")) {
                config.addProperty("info", "This file can be edited ingame using the /ntmserver command.");
            }
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(config, writer);
            }
        } catch (Exception exception) {
            Constants.LOG.warn("Unable to load shared AUTOCAL config from {}", file, exception);
        }
    }
}
