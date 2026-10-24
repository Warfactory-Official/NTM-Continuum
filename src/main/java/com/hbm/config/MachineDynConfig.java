package com.hbm.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.Constants;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.tileentity.machine.GroundwaterPumps;
import com.hbm.tileentity.machine.TileEntityAshpit;
import com.hbm.tileentity.machine.TileEntityChungus;
import com.hbm.tileentity.machine.TileEntityCondenser;
import com.hbm.tileentity.machine.TileEntityCondenserPowered;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.hbm.tileentity.machine.TileEntityForceField;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import com.hbm.tileentity.machine.TileEntityHeaterOven;
import com.hbm.tileentity.machine.TileEntityICFController;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import com.hbm.tileentity.machine.TileEntityMachineRadarLarge;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineRotaryFurnace;
import com.hbm.tileentity.machine.TileEntityMachineSteamEngine;
import com.hbm.tileentity.machine.TileEntityStirling;
import com.hbm.tileentity.machine.TileEntityTowerLarge;
import com.hbm.tileentity.machine.TileEntityTowerSmall;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 1.7 {@code MachineDynConfig}: {@code <config>/hbmConfig/hbmMachines.json}. Read once at start: every registered
 * machine takes the values present in the file and keeps its defaults for the rest, then the file is rewritten with
 * the full set, so missing entries appear with their defaults and unknown ones vanish, as in 1.7. A server sends its
 * values to each joining client ({@link #values}, {@link #apply}); a starting server puts back the local file's.
 */
public final class MachineDynConfig {

    public static final Gson gson = new Gson();

    /** Every machine in {@code hbmMachines.json}, in 1.7's file order, then any an addon {@link #register}s. */
    public static final List<IConfigurableMachine> CONFIGURABLES = new CopyOnWriteArrayList<>(List.of(
            TileEntityMachineCentrifuge.CONFIG,
            TileEntityMachineDiesel.CONFIG,
            TileEntityMachineRadarNT.CONFIG,
            TileEntityMachineRadarLarge.CONFIG,
            TileEntityForceField.CONFIG,
            TileEntityMachineRotaryFurnace.CONFIG,
            TileEntityICFController.CONFIG,
            TileEntityHeaterFirebox.CONFIG,
            TileEntityHeaterOven.CONFIG,
            TileEntityAshpit.CONFIG,
            TileEntityStirling.CONFIG,
            TileEntityCrucible.CONFIG,
            TileEntityHeatBoiler.CONFIG,
            TileEntityHeatBoilerIndustrial.CONFIG,
            GroundwaterPumps.CONFIG,
            TileEntityCondenser.CONFIG,
            TileEntityTowerSmall.CONFIG,
            TileEntityTowerLarge.CONFIG,
            TileEntityCondenserPowered.CONFIG,
            TileEntityMachineSteamEngine.CONFIG,
            TileEntityMachineIndustrialTurbine.CONFIG,
            TileEntityChungus.CONFIG,
            TileEntityMachineOilWell.CONFIG,
            TileEntityMachinePumpjack.CONFIG,
            TileEntityMachineFrackingTower.CONFIG,
            TileEntityFusionMHDT.CONFIG));

    /** The values this game read from its own file, put back whenever a server starts here. */
    private static JsonObject local;

    /**
     * Adds an addon's machine to {@code hbmMachines.json} (1.7 {@code CustomMachineConfigJSON}-style dynamic config). Call
     * before the config is read ({@code initialize}, at common setup), e.g. from the addon's {@code declare}.
     */
    public static void register(IConfigurableMachine machine) {
        CONFIGURABLES.add(machine);
    }

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
                apply(json);
            }

            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                JsonWriter writer = new JsonWriter(out);
                writer.setIndent("  ");
                writer.beginObject();
                writer.name("info").beginArray();
                for (String line : COMMENT) writer.value(line);
                writer.endArray();
                writeMachines(writer);
                writer.endObject();
                writer.flush();
            }
            Constants.LOG.info("Machine config {} holds {} machine(s)", file.getFileName(), CONFIGURABLES.size());
        } catch (Exception ex) {
            Constants.LOG.error("Unable to process {}", file, ex);
        }
        local = gson.fromJson(values(), JsonObject.class);
    }

    /** Every machine's current values as one JSON object keyed by config name. */
    public static String values() {
        StringWriter out = new StringWriter();
        try {
            JsonWriter writer = new JsonWriter(out);
            writer.beginObject();
            writeMachines(writer);
            writer.endObject();
            writer.flush();
        } catch (IOException ex) {
            // a StringWriter does not throw
            throw new IllegalStateException(ex);
        }
        return out.toString();
    }

    /** Reads each machine's entry of {@code json}; a machine without one keeps its values. */
    public static void apply(JsonObject json) {
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

    /** Puts back the values read from this game's own file, undoing a remote server's. */
    public static void restoreLocal() {
        if (local != null) apply(local);
    }

    private static void writeMachines(JsonWriter writer) {
        for (IConfigurableMachine machine : CONFIGURABLES) {
            try {
                writer.name(machine.getConfigName()).beginObject();
                machine.writeConfig(writer);
                writer.endObject();
            } catch (Exception ex) {
                Constants.LOG.error("Unable to write machine config {}", machine.getConfigName(), ex);
            }
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
