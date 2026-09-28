package com.hbm.tileentity;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * 1.7 {@code IConfigurableMachine}: one named object of {@code hbmConfig/hbmMachines.json}. 1.7 built a throwaway
 * tile entity per configurable class to reach its static fields; a modern block entity cannot be built without a
 * position and state, so each machine exposes one static instance of this interface instead
 * ({@link com.hbm.config.MachineDynConfig#CONFIGURABLES}).
 */
public interface IConfigurableMachine {

    /** the name for the JSON object for this machine */
    String getConfigName();

    /** reads the JSON object and sets the machine's parameters, using defaults for anything not yet present */
    void readIfPresent(JsonObject obj);

    /** writes the entire config for this machine using the relevant values */
    void writeConfig(JsonWriter writer) throws IOException;

    static boolean grab(JsonObject obj, String name, boolean def) {
        return obj.has(name) ? obj.get(name).getAsBoolean() : def;
    }

    static int grab(JsonObject obj, String name, int def) {
        return obj.has(name) ? obj.get(name).getAsInt() : def;
    }

    static long grab(JsonObject obj, String name, long def) {
        return obj.has(name) ? obj.get(name).getAsLong() : def;
    }

    static double grab(JsonObject obj, String name, double def) {
        return obj.has(name) ? obj.get(name).getAsDouble() : def;
    }
}
