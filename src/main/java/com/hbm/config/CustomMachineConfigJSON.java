package com.hbm.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.Constants;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import dev.golem.core.Ids;
import dev.golem.core.platform.ConfigFiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 1.7 {@code CustomMachineConfigJSON}: the player-defined machines, read from {@code hbmConfig/hbmCustomMachines.json}
 * in the loader's config directory and written with the original Paper Press example when the file is absent.
 * The file format is the original's, key for key.
 *
 * <p>Two things the flattening forced. A component's {@code block} is a registry id, which 1.7 spelled
 * {@code hbm:tile.<name>} and which had {@code metas} to select a variant; the loader accepts that spelling and
 * turns {@code (cm_sheet, meta 0)} into the flattened {@code cm_sheet_steel}, and accepts a plain modern id too.
 * And registries are not open while mods construct, so the blocks behind each component and the controller's
 * crafting ingredients are resolved on first use rather than in {@link #readConfig}.
 */
public final class CustomMachineConfigJSON {

    public static final Gson gson = new Gson();
    public static final HashMap<String, MachineConfiguration> customMachines = new HashMap<>();
    public static final List<MachineConfiguration> niceList = new ArrayList<>();

    /** The client's language code for {@link MachineConfiguration#localization}; stays null on a server. */
    public static Supplier<@Nullable String> languageCode = () -> null;

    /** The 1.7 metadata variants of the flattened {@code BlockEnumMulti} families, in enum order. */
    private static final Map<String, String[]> FLATTENED = Map.of(
            "cm_block", new String[]{"steel", "alloy", "desh", "tcalloy"},
            "cm_sheet", new String[]{"steel", "alloy", "desh", "tcalloy"},
            "cm_tank", new String[]{"steel", "alloy", "desh", "tcalloy"},
            "cm_port", new String[]{"steel", "alloy", "desh", "tcalloy"},
            "cm_engine", new String[]{"standard", "desh", "bismuth"},
            "cm_circuit", new String[]{"aluminium", "copper", "red_copper", "gold", "schrabidium"});

    private static boolean initialized;

    private CustomMachineConfigJSON() {}

    /** 1.7 {@code MainRegistry.configHbmDir}: {@code <config>/hbmConfig}, created on demand. */
    public static Path configHbmDir() {
        Path dir = ConfigFiles.configDir().resolve("hbmConfig");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Constants.LOG.error("Unable to create {}", dir, e);
        }
        return dir;
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        Path config = configHbmDir().resolve("hbmCustomMachines.json");

        if (!Files.exists(config)) {
            writeDefault(config);
        }

        readConfig(config);
    }

    /**
     * Replaces the loaded machines with the ones in {@code config}. 1.7 read the file once at startup and so does
     * {@link #initialize}; this exists for the harness, which stages test machines in a separate file for one
     * scenario and puts the real file back afterwards ({@code hbm_gapsa} {@code custom_machines}).
     */
    public static synchronized void reloadFrom(Path config) {
        customMachines.clear();
        niceList.clear();
        readConfig(config);
    }

    /** The real machine file, {@code hbmConfig/hbmCustomMachines.json}. */
    public static Path configFile() {
        return configHbmDir().resolve("hbmCustomMachines.json");
    }

    public static void writeDefault(Path config) {

        try (Writer out = Files.newBufferedWriter(config, StandardCharsets.UTF_8)) {
            JsonWriter writer = new JsonWriter(out);
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("machines").beginArray();

            writer.beginObject();
            writer.name("recipeKey").value("paperPress");
            writer.name("unlocalizedName").value("paperPress");
            writer.name("localization").beginObject();
            writer.name("de_DE").value("Papierpresse");
            writer.endObject();
            writer.name("localizedName").value("Paper Press");
            writer.name("fluidInCount").value(1);
            writer.name("fluidInCap").value(1_000);
            writer.name("itemInCount").value(1);
            writer.name("fluidOutCount").value(0);
            writer.name("fluidOutCap").value(0);
            writer.name("itemOutCount").value(1);
            writer.name("generatorMode").value(false);
            writer.name("maxPollutionCap").value(100);
            writer.name("fluxMode").value(false);
            writer.name("recipeSpeedMult").value(1.0D);
            writer.name("recipeConsumptionMult").value(1.0D);
            writer.name("maxPower").value(10_000L);
            writer.name("maxHeat").value(0);

            writer.name("recipeShape").beginArray();
            writer.value("IPI").value("PCP").value("IPI");
            writer.endArray();

            // 1.7 wrote STEEL.ingot(), STEEL.plate() and circuit:BASIC through SerializableRecipe.writeAStack; the
            // two dictionary names are unchanged, the circuit is its flattened id.
            writer.name("recipeParts").beginArray().setIndent("");
            writer.value("I");
            CustomMachineRecipes.writeDict("ingotSteel", 1, writer);
            writer.setIndent("");
            writer.value("P");
            CustomMachineRecipes.writeDict("plateSteel", 1, writer);
            writer.setIndent("");
            writer.value("C");
            CustomMachineRecipes.writeItem("hbm:circuit_basic", 1, writer);
            writer.endArray().setIndent("  ");

            writer.name("components").beginArray();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = 0; z <= 2; z++) {
                        if (!(x == 0 && y == 0 && z == 1) && !(x == 0 && z == 0)) {
                            writer.beginObject().setIndent("");
                            writer.name("block").value(y == 0 ? "hbm:tile.cm_sheet" : "hbm:tile.cm_block");
                            writer.name("x").value(x);
                            writer.name("y").value(y);
                            writer.name("z").value(z);
                            writer.name("metas").beginArray();
                            writer.value(0);
                            writer.endArray();
                            writer.endObject().setIndent("  ");
                        }
                    }
                }
            }

            writer.beginObject().setIndent("");
            writer.name("block").value("hbm:tile.cm_port");
            writer.name("x").value(0);
            writer.name("y").value(-1);
            writer.name("z").value(0);
            writer.name("metas").beginArray();
            writer.value(0);
            writer.endArray();
            writer.endObject().setIndent("  ");

            writer.beginObject().setIndent("");
            writer.name("block").value("hbm:tile.cm_port");
            writer.name("x").value(0);
            writer.name("y").value(1);
            writer.name("z").value(0);
            writer.name("metas").beginArray();
            writer.value(0);
            writer.endArray();
            writer.endObject().setIndent("  ");

            writer.endArray();
            writer.endObject();

            writer.endArray();
            writer.endObject();
            writer.flush();
        } catch (IOException e) {
            Constants.LOG.error("Unable to write {}", config, e);
        }
    }

    public static void readConfig(Path config) {

        try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            JsonArray machines = json.get("machines").getAsJsonArray();

            for (int i = 0; i < machines.size(); i++) {
                JsonObject machineObject = machines.get(i).getAsJsonObject();

                MachineConfiguration configuration = new MachineConfiguration();
                configuration.recipeKey = machineObject.get("recipeKey").getAsString();
                configuration.unlocalizedName = machineObject.get("unlocalizedName").getAsString();
                configuration.localizedName = machineObject.get("localizedName").getAsString();
                if (machineObject.has("localization")) {
                    JsonObject localization = machineObject.get("localization").getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : localization.entrySet()) {
                        configuration.localization.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                configuration.fluidInCount = machineObject.get("fluidInCount").getAsInt();
                configuration.fluidInCap = machineObject.get("fluidInCap").getAsInt();
                configuration.itemInCount = machineObject.get("itemInCount").getAsInt();
                configuration.fluidOutCount = machineObject.get("fluidOutCount").getAsInt();
                configuration.fluidOutCap = machineObject.get("fluidOutCap").getAsInt();
                configuration.itemOutCount = machineObject.get("itemOutCount").getAsInt();
                configuration.generatorMode = machineObject.get("generatorMode").getAsBoolean();
                if (machineObject.has("maxPollutionCap")) configuration.maxPollutionCap = machineObject.get("maxPollutionCap").getAsInt();
                if (machineObject.has("fluxMode")) configuration.fluxMode = machineObject.get("fluxMode").getAsBoolean();
                configuration.recipeSpeedMult = machineObject.get("recipeSpeedMult").getAsDouble();
                configuration.recipeConsumptionMult = machineObject.get("recipeConsumptionMult").getAsDouble();
                configuration.maxPower = machineObject.get("maxPower").getAsLong();
                if (machineObject.has("maxHeat")) configuration.maxHeat = machineObject.get("maxHeat").getAsInt();

                if (machineObject.has("recipeShape") && machineObject.has("recipeParts")) {
                    try {
                        JsonArray recipeShape = machineObject.get("recipeShape").getAsJsonArray();
                        JsonArray recipeParts = machineObject.get("recipeParts").getAsJsonArray();

                        String[] shape = new String[recipeShape.size()];
                        for (int j = 0; j < recipeShape.size(); j++) shape[j] = recipeShape.get(j).getAsString();

                        Map<Character, JsonArray> parts = new HashMap<>();
                        for (int j = 0; j + 1 < recipeParts.size(); j += 2) {
                            // god is dead and we killed him
                            parts.put(recipeParts.get(j).getAsString().charAt(0), recipeParts.get(j + 1).getAsJsonArray());
                        }

                        configuration.recipeShape = shape;
                        configuration.recipePartsRaw = parts;
                    } catch (Exception ex) {
                        Constants.LOG.error("Caught exception trying to parse core recipe for custom machine " + configuration.unlocalizedName);
                        Constants.LOG.error("recipeShape was" + machineObject.get("recipeShape").toString());
                        Constants.LOG.error("recipeParts was" + machineObject.get("recipeParts").toString());
                    }
                }

                JsonArray components = machineObject.get("components").getAsJsonArray();
                configuration.components = new ArrayList<>();

                for (int j = 0; j < components.size(); j++) {
                    JsonObject compObject = components.get(j).getAsJsonObject();
                    MachineConfiguration.ComponentDefinition compDef = new MachineConfiguration.ComponentDefinition();
                    compDef.blockName = compObject.get("block").getAsString();
                    compDef.x = compObject.get("x").getAsInt();
                    compDef.y = compObject.get("y").getAsInt();
                    compDef.z = compObject.get("z").getAsInt();
                    compDef.metas = compObject.get("metas").getAsJsonArray();
                    configuration.components.add(compDef);
                }

                customMachines.put(configuration.unlocalizedName, configuration);
                niceList.add(configuration);
            }
            Constants.LOG.info("Loaded {} custom machine(s) from {}", niceList.size(), config);

        } catch (Exception ex) {
            Constants.LOG.error("Unable to read custom machine config {}", config, ex);
        }
    }

    /**
     * The block a component names at one of its metas. A 1.7 {@code hbm:tile.cm_sheet} with meta 0 is the
     * flattened {@code hbm:cm_sheet_steel}; any other name is looked up as it stands, meta ignored.
     */
    public static Block resolveBlock(String name, int meta) {
        String id = name.replace(":tile.", ":");
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = (colon < 0 ? id : id.substring(colon + 1)).toLowerCase(Locale.US);
        if (Constants.MOD_ID.equals(ns)) {
            String[] variants = FLATTENED.get(path);
            if (variants != null) {
                if (meta < 0 || meta >= variants.length) return Blocks.AIR;
                path = path + "_" + variants[meta];
            }
        }
        try {
            return BuiltInRegistries.BLOCK.getOptional(Ids.of(ns, path)).orElse(Blocks.AIR);
        } catch (RuntimeException malformed) {
            return Blocks.AIR;
        }
    }

    /**
     * The configuration a controller item names, or null. 1.7 kept {@code niceList} index + 100 in the item damage; the
     * port saves the configuration's {@code unlocalizedName} ({@code hbm:custom_machine}), which a config edit cannot
     * shift onto another machine.
     */
    public static @Nullable MachineConfiguration byName(@Nullable String unlocalizedName) {
        return unlocalizedName == null ? null : customMachines.get(unlocalizedName);
    }

    public static class MachineConfiguration {

        /** The name of the recipe set that this machine can handle */
        public String recipeKey;
        /** The internal name of this machine */
        public String unlocalizedName;
        /** The display name of this machine */
        public String localizedName;
        public HashMap<String, String> localization = new HashMap<>();

        public int fluidInCount;
        public int fluidInCap;
        public int itemInCount;
        public int fluidOutCount;
        public int fluidOutCap;
        public int itemOutCount;
        /** Whether inputs should be used up when the process begins */
        public boolean generatorMode;
        public int maxPollutionCap;
        public boolean fluxMode;
        public double recipeSpeedMult = 1D;
        public double recipeConsumptionMult = 1D;
        public long maxPower;
        public int maxHeat;

        /** The controller's shaped recipe, if the config has one; ingredients resolve on first use. */
        public String @Nullable [] recipeShape;
        public @Nullable Map<Character, JsonArray> recipePartsRaw;
        private @Nullable Map<Character, AStack> recipeParts;

        /** Definitions of blocks that this machine is composed of */
        public List<ComponentDefinition> components;

        /** The name for the current client language (1.7 compared {@code de_DE}; modern codes are lower case). */
        public String displayName() {
            String lang = languageCode.get();
            if (lang != null) {
                for (Map.Entry<String, String> entry : localization.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(lang)) return entry.getValue();
                }
            }
            return localizedName;
        }

        public @Nullable Map<Character, AStack> recipeParts() {
            if (recipeParts == null && recipePartsRaw != null) {
                Map<Character, AStack> parts = new HashMap<>();
                for (Map.Entry<Character, JsonArray> entry : recipePartsRaw.entrySet()) {
                    parts.put(entry.getKey(), CustomMachineRecipes.readAStack(entry.getValue()));
                }
                recipeParts = parts;
            }
            return recipeParts;
        }

        public static class ComponentDefinition {
            public String blockName;
            public JsonArray metas;
            public int x;
            public int y;
            public int z;
            private @Nullable List<Block> blocks;

            /** The accepted blocks, one per listed meta, in the listed order (1.7 {@code allowedMetas} + {@code block}). */
            public List<Block> blocks() {
                if (blocks == null) {
                    List<Block> list = new ArrayList<>();
                    for (int k = 0; k < metas.size(); k++) list.add(resolveBlock(blockName, metas.get(k).getAsInt()));
                    blocks = list;
                }
                return blocks;
            }

            public boolean accepts(Block block) {
                return block != Blocks.AIR && blocks().contains(block);
            }
        }
    }
}
