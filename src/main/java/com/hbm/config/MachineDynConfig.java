package com.hbm.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.Constants;
import com.hbm.tileentity.IConfigurableMachine;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 1.7 {@code MachineDynConfig}: {@code <config>/hbmConfig/hbmMachines.json}. Read once at start: every registered
 * machine takes the values present in the file and keeps its defaults for the rest, then the file is rewritten with
 * the full set (the {@code info} comment first), so missing entries appear with their defaults and unknown ones
 * vanish, exactly as 1.7 behaved.
 *
 * <p>Registered so far: the ICF laser controller ({@code icfLaser}). 1.7 listed every {@code IConfigurableMachine}
 * tile; the port's other machines still carry their 1.7 values as plain defaults and join this list as they are
 * converted.
 */
public final class MachineDynConfig {

    public static final Gson gson = new Gson();

    public static final List<IConfigurableMachine> CONFIGURABLES = List.of(
            com.hbm.tileentity.machine.TileEntityICFController.CONFIG);

    private MachineDynConfig() {}

    public static Path file() {
        return CustomMachineConfigJSON.configHbmDir().resolve("hbmMachines.json");
    }

    public static void initialize() {
        Path file = file();
        try {
            if (Files.exists(file)) {
                JsonObject json;
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    json = gson.fromJson(reader, JsonObject.class);
                }
                for (IConfigurableMachine machine : CONFIGURABLES) {
                    try {
                        JsonElement element = json == null ? null : json.get(machine.getConfigName());
                        machine.readIfPresent(element != null ? element.getAsJsonObject() : new JsonObject());
                    } catch (Exception ex) {
                        // 1.7: one broken machine entry must not affect the others
                        Constants.LOG.error("Unable to read machine config {}", machine.getConfigName(), ex);
                    }
                }
            }

            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                JsonWriter writer = new JsonWriter(out);
                writer.setIndent("  ");
                writer.beginObject();
                writer.name("info").beginArray();
                for (String line : COMMENT) writer.value(line);
                writer.endArray();
                for (IConfigurableMachine machine : CONFIGURABLES) {
                    try {
                        writer.name(machine.getConfigName()).beginObject();
                        machine.writeConfig(writer);
                        writer.endObject();
                    } catch (Exception ex) {
                        Constants.LOG.error("Unable to write machine config {}", machine.getConfigName(), ex);
                    }
                }
                writer.endObject();
                writer.flush();
            }
            Constants.LOG.info("Machine config {} holds {} machine(s)", file.getFileName(), CONFIGURABLES.size());
        } catch (Exception ex) {
            Constants.LOG.error("Unable to process {}", file, ex);
        }
    }

    private static final String[] COMMENT = {
            "Unlike other JSON configs, this one does not use a variable amount of options (like recipes), rather all config options are fixed.",
            "This means that there is no distinction between template and used config, you can simply edit this file and it will use the new values.",
            "If you wish to reset one or multiple values to default, simply delete them, the file is re-created every time the game starts (but changed values persist!)",
            "How this works in detail:",
            "- Machines have default values on init",
            "- The config system will try to read the config file. It will replace the default values where applicable, and keep them when an option is missing.",
            "- The config system will then use the full set of values - configured or default if missing - and re-create the config file to include any missing entries.",
            "This final step also means that any custom non-config values added to the JSON, while not causing errors, will be deleted when the config is re-created.",
            "It also means that should an update add more values to an existing machines, those will be retroactively added to the config using the default value."
    };
}
