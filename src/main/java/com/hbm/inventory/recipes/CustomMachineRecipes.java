package com.hbm.inventory.recipes;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.Constants;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.Ids;
import dev.golem.core.platform.ConfigFiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1.7 {@code CustomMachineRecipes}, a {@code SerializableRecipe}: the recipe sets the custom machines run, keyed by
 * {@code recipeKey}. The loader behaves as 1.7's {@code SerializableRecipe.initialize} did for this one handler: a
 * {@code hbmRecipes/hbmCustomMachines.json} in the config directory replaces the defaults; without one the defaults
 * register and {@code _hbmCustomMachines.json} is written beside the "remove underscore" marker file.
 *
 * <p>The JSON stack spelling is the original's. {@code ["dict", "ingotSteel"]} names the {@code c:ingots/steel}
 * tag, which is how the port spells ore dictionary keys; a {@code #namespace:path} tag id is accepted as well. Item
 * ids drop 1.7's {@code item.}/{@code tile.} prefixes; metadata has no meaning after the flattening and is ignored.
 * Loading waits for first use, because the item registry is not open while mods construct.
 */
public final class CustomMachineRecipes {

    public static final HashMap<String, List<CustomMachineRecipe>> recipes = new LinkedHashMap<>();
    private static final Gson gson = new Gson();
    private static boolean loaded;

    private CustomMachineRecipes() {}

    public static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        initialize(ConfigFiles.configDir().resolve("hbmRecipes"));
    }

    /** 1.7 {@code SerializableRecipe.initialize} as {@code /ntmreload} runs it: re-reads the file now. */
    public static synchronized void reload() {
        loaded = true;
        initialize(ConfigFiles.configDir().resolve("hbmRecipes"));
    }

    /** {@link #reload} from another recipe directory: the harness's staged test recipes ({@code hbm_gapsa}). */
    public static synchronized void loadFrom(Path recDir) {
        loaded = true;
        initialize(recDir);
    }

    public static List<CustomMachineRecipe> get(String key) {
        ensureLoaded();
        return recipes.get(key);
    }

    /** 1.7 {@code SerializableRecipe.initialize}, for this handler only. */
    static void initialize(Path recDir) {
        try {
            Files.createDirectories(recDir);
            Path info = recDir.resolve("REMOVE UNDERSCORE TO ENABLE RECIPE LOADING - RECIPES WILL RESET TO DEFAULT OTHERWISE");
            if (!Files.exists(info)) Files.createFile(info);
        } catch (IOException e) {
            Constants.LOG.error("Unable to make recipe directory {}", recDir, e);
        }

        recipes.clear();
        Path recFile = recDir.resolve(getFileName());
        if (Files.isRegularFile(recFile)) {
            Constants.LOG.info("Reading recipe file " + recFile.getFileName());
            try (Reader reader = Files.newBufferedReader(recFile, StandardCharsets.UTF_8)) {
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                for (JsonElement recipe : json.get("recipes").getAsJsonArray()) {
                    if (recipe != null) readRecipe(recipe);
                }
            } catch (Exception ex) {
                Constants.LOG.error("Unable to read recipe file {}", recFile, ex);
            }
        } else {
            Constants.LOG.info("No recipe file found, registering defaults for " + getFileName());
            registerDefaults();
            Path recTemplate = recDir.resolve("_" + getFileName());
            Constants.LOG.info("Writing template file " + recTemplate.getFileName());
            writeTemplateFile(recTemplate);
        }
    }

    public static void registerDefaults() {

        List<CustomMachineRecipe> paperPress = new ArrayList<>();
        CustomMachineRecipe recipe = new CustomMachineRecipe();
        recipe.inputFluids = new FluidStack[]{new FluidStack(Fluids.WATER, 250)};
        recipe.inputItems = new AStack[]{new ComparableStack(item("hbm:powder_sawdust"))};
        recipe.outputFluids = new FluidStack[0];
        recipe.outputItems = new ChanceStack[]{new ChanceStack(new ItemStack(Items.PAPER, 3), 1F)};
        recipe.duration = 60;
        recipe.consumptionPerTick = 10;
        recipe.pollutionType = "SOOT";
        recipe.pollutionAmount = 0.03F;
        recipe.radiationAmount = 0;
        recipe.flux = 0;
        recipe.heat = 0;
        paperPress.add(recipe);
        recipes.put("paperPress", paperPress);
    }

    public static String getFileName() {
        return "hbmCustomMachines.json";
    }

    public static void readRecipe(JsonElement recipe) {
        JsonObject obj = recipe.getAsJsonObject();

        String name = obj.get("recipeKey").getAsString();
        List<CustomMachineRecipe> list = new ArrayList<>();
        JsonArray array = obj.get("recipes").getAsJsonArray();

        for (int i = 0; i < array.size(); i++) {
            JsonObject rec = array.get(i).getAsJsonObject();
            CustomMachineRecipe recipeInstance = new CustomMachineRecipe();
            recipeInstance.inputFluids = readFluidArray(rec.get("inputFluids").getAsJsonArray());
            recipeInstance.inputItems = readAStackArray(rec.get("inputItems").getAsJsonArray());
            recipeInstance.outputFluids = readFluidArray(rec.get("outputFluids").getAsJsonArray());
            recipeInstance.outputItems = readItemStackArrayChance(rec.get("outputItems").getAsJsonArray());
            recipeInstance.duration = rec.get("duration").getAsInt();
            recipeInstance.consumptionPerTick = rec.get("consumptionPerTick").getAsInt();

            if (rec.has("pollutionType") && rec.has("pollutionAmount")) {
                recipeInstance.pollutionType = rec.get("pollutionType").getAsString();
                recipeInstance.pollutionAmount = rec.get("pollutionAmount").getAsFloat();
            } else {
                recipeInstance.pollutionType = "";
            }

            if (rec.has("radiationAmount")) recipeInstance.radiationAmount = rec.get("radiationAmount").getAsFloat();
            if (rec.has("flux")) recipeInstance.flux = rec.get("flux").getAsInt();
            if (rec.has("heat")) recipeInstance.heat = rec.get("heat").getAsInt();

            list.add(recipeInstance);
        }

        recipes.put(name, list);
    }

    /** 1.7 {@code SerializableRecipe.writeTemplateFile} + {@code writeRecipe}. */
    public static void writeTemplateFile(Path template) {
        try (Writer out = Files.newBufferedWriter(template, StandardCharsets.UTF_8)) {
            JsonWriter writer = new JsonWriter(out);
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("recipes").beginArray();

            for (Map.Entry<String, List<CustomMachineRecipe>> entry : recipes.entrySet()) {
                writer.beginObject();
                writer.name("recipeKey").value(entry.getKey());
                writer.name("recipes").beginArray();

                for (CustomMachineRecipe recipeInstance : entry.getValue()) {
                    writer.beginObject();

                    writer.name("inputFluids").beginArray();
                    for (FluidStack stack : recipeInstance.inputFluids) writeFluidStack(stack, writer);
                    writer.endArray();

                    writer.name("inputItems").beginArray();
                    for (AStack stack : recipeInstance.inputItems) writeAStack(stack, writer);
                    writer.endArray();

                    writer.name("outputFluids").beginArray();
                    for (FluidStack stack : recipeInstance.outputFluids) writeFluidStack(stack, writer);
                    writer.endArray();

                    writer.name("outputItems").beginArray();
                    for (ChanceStack stack : recipeInstance.outputItems) writeItemStackChance(stack, writer);
                    writer.endArray();

                    writer.name("duration").value(recipeInstance.duration);
                    writer.name("consumptionPerTick").value(recipeInstance.consumptionPerTick);
                    writer.name("pollutionType").value(recipeInstance.pollutionType);
                    writer.name("pollutionAmount").value(recipeInstance.pollutionAmount);
                    writer.name("radiationAmount").value(recipeInstance.radiationAmount);
                    writer.name("flux").value(recipeInstance.flux);
                    writer.name("heat").value(recipeInstance.heat);

                    writer.endObject();
                }

                writer.endArray();
                writer.endObject();
            }

            writer.endArray();
            writer.endObject();
            writer.flush();
        } catch (Exception ex) {
            Constants.LOG.error("Unable to write recipe template {}", template, ex);
        }
    }

    // --- JSON IO (1.7 SerializableRecipe helpers) ---

    private static String modernId(String id) {
        return id.replace(":item.", ":").replace(":tile.", ":").toLowerCase(Locale.US);
    }

    public static Item item(String id) {
        try {
            return BuiltInRegistries.ITEM.getOptional(Ids.parse(modernId(id))).orElse(Items.AIR);
        } catch (RuntimeException malformed) {
            return Items.AIR;
        }
    }

    /**
     * A 1.7 ore dictionary key as the port's {@code c:} tag: {@code ingotSteel} is {@code c:ingots/steel},
     * {@code plateTripleAnyResistantAlloy} is {@code c:plates_cast/any_resistant_alloy}.
     */
    public static TagKey<Item> dictTag(String dict) {
        if (dict.startsWith("#")) return TagKey.create(Registries.ITEM, Ids.parse(dict.substring(1)));
        if (dict.indexOf(':') >= 0) return TagKey.create(Registries.ITEM, Ids.parse(dict));
        String[][] prefixes = {
                {"plateSextuple", "plates_welded"}, {"plateTriple", "plates_cast"}, {"plate", "plates"},
                {"ingot", "ingots"}, {"nugget", "nuggets"}, {"dustTiny", "dusts_tiny"}, {"tiny", "dusts_tiny"},
                {"dust", "dusts"}, {"gem", "gems"}, {"crystal", "crystals"}, {"billet", "billets"},
                {"wireFine", "wires"}, {"wireDense", "wires_dense"}, {"bolt", "bolts"}, {"shell", "shells"},
                {"ntmpipe", "pipes"}, {"block", "storage_blocks"}, {"ore", "ores"},
                {"barrelLight", "barrels_light"}, {"barrelHeavy", "barrels_heavy"},
                {"receiverLight", "receivers_light"}, {"receiverHeavy", "receivers_heavy"},
                {"gunMechanism", "mechanisms"}, {"stock", "stocks"}, {"grip", "grips"}};
        for (String[] prefix : prefixes) {
            if (dict.length() > prefix[0].length() && dict.startsWith(prefix[0])
                    && Character.isUpperCase(dict.charAt(prefix[0].length()))) {
                String material = dict.substring(prefix[0].length())
                        .replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.US);
                return TagKey.create(Registries.ITEM, Ids.of("c", prefix[1] + "/" + material));
            }
        }
        return TagKey.create(Registries.ITEM, Ids.of("c", dict.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.US)));
    }

    public static AStack readAStack(JsonArray array) {
        try {
            String type = array.get(0).getAsString();
            int stacksize = array.size() > 2 ? array.get(2).getAsInt() : 1;
            if ("item".equals(type) || "nbt".equals(type)) {
                Item item = item(array.get(1).getAsString());
                if (item != Items.AIR) return new ComparableStack(item, stacksize);
            }
            if ("dict".equals(type)) {
                return new TagStack(dictTag(array.get(1).getAsString()), stacksize);
            }
        } catch (Exception ex) { }
        Constants.LOG.error("Error reading stack array " + array);
        return new ComparableStack(Items.AIR);
    }

    public static AStack[] readAStackArray(JsonArray array) {
        try {
            AStack[] items = new AStack[array.size()];
            for (int i = 0; i < items.length; i++) items[i] = readAStack(array.get(i).getAsJsonArray());
            return items;
        } catch (Exception ex) { }
        Constants.LOG.error("Error reading stack array " + array);
        return new AStack[0];
    }

    public static ChanceStack readItemStackChance(JsonArray array) {
        try {
            Item item = item(array.get(0).getAsString());
            int stacksize = array.size() > 2 ? array.get(1).getAsInt() : 1;
            if (item != Items.AIR) {
                float chance = array.get(array.size() - 1).getAsFloat();
                return new ChanceStack(new ItemStack(item, stacksize), chance);
            }
        } catch (Exception ex) { }
        Constants.LOG.error("Error reading stack array " + array + " - defaulting to NOTHING item!");
        return new ChanceStack(ItemStack.EMPTY, 1F);
    }

    public static ChanceStack[] readItemStackArrayChance(JsonArray array) {
        try {
            ChanceStack[] items = new ChanceStack[array.size()];
            for (int i = 0; i < items.length; i++) items[i] = readItemStackChance(array.get(i).getAsJsonArray());
            return items;
        } catch (Exception ex) { }
        Constants.LOG.error("Error reading stack array " + array);
        return new ChanceStack[0];
    }

    public static FluidStack readFluidStack(JsonArray array) {
        try {
            FluidType type = Fluids.fromName(array.get(0).getAsString());
            int fill = array.get(1).getAsInt();
            int pressure = array.size() < 3 ? 0 : array.get(2).getAsInt();
            return new FluidStack(type, fill, pressure);
        } catch (Exception ex) { }
        Constants.LOG.error("Error reading fluid array " + array);
        return new FluidStack(Fluids.NONE, 0);
    }

    public static FluidStack[] readFluidArray(JsonArray array) {
        try {
            FluidStack[] fluids = new FluidStack[array.size()];
            for (int i = 0; i < fluids.length; i++) fluids[i] = readFluidStack(array.get(i).getAsJsonArray());
            return fluids;
        } catch (Exception ex) { }
        Constants.LOG.error("Error reading fluid array " + array);
        return new FluidStack[0];
    }

    public static void writeItem(String id, int stacksize, JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.setIndent("");
        writer.value("item");
        writer.value(id);
        if (stacksize != 1) writer.value(stacksize);
        writer.endArray();
        writer.setIndent("  ");
    }

    public static void writeDict(String name, int stacksize, JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.setIndent("");
        writer.value("dict");
        writer.value(name);
        if (stacksize != 1) writer.value(stacksize);
        writer.endArray();
        writer.setIndent("  ");
    }

    public static void writeAStack(AStack astack, JsonWriter writer) throws IOException {
        if (astack instanceof ComparableStack comp) {
            writeItem(BuiltInRegistries.ITEM.getKey(comp.item).toString(), comp.stacksize, writer);
        } else if (astack instanceof TagStack tag) {
            writeDict("#" + tag.tag.location(), tag.stacksize, writer);
        }
    }

    public static void writeItemStackChance(ChanceStack stack, JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.setIndent("");
        writer.value(BuiltInRegistries.ITEM.getKey(stack.stack().getItem()).toString());
        if (stack.stack().getCount() != 1) writer.value(stack.stack().getCount());
        writer.value(stack.chance());
        writer.endArray();
        writer.setIndent("  ");
    }

    public static void writeFluidStack(FluidStack stack, JsonWriter writer) throws IOException {
        writer.beginArray();
        writer.setIndent("");
        writer.value(stack.type.getName());
        writer.value(stack.fill);
        if (stack.pressure != 0) writer.value(stack.pressure);
        writer.endArray();
        writer.setIndent("  ");
    }

    /** 1.7 {@code Pair<ItemStack, Float>}: an output and the chance it is produced. */
    public record ChanceStack(ItemStack stack, float chance) {}

    public static class CustomMachineRecipe {

        public FluidStack[] inputFluids;
        public AStack[] inputItems;
        public FluidStack[] outputFluids;
        public ChanceStack[] outputItems;

        public int duration;
        public int consumptionPerTick;

        public String pollutionType;
        public float pollutionAmount;
        public float radiationAmount;
        public int flux;
        public int heat;
    }
}
