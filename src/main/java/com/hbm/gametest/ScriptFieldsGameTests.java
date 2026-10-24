package com.hbm.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.RecipeShapes;
import com.mojang.serialization.JsonOps;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.platform.Platform;
import dev.golem.core.world.ItemPatch;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.RecipeFields;
import dev.golem.core.world.RecipeFields.FluidAmount;
import dev.golem.core.world.RecipeFields.ItemIn;
import dev.golem.core.world.RecipeFields.ItemOut;
import dev.golem.core.world.RecipeFields.Kind;
import dev.golem.core.world.RecipeFields.Shape;
import dev.golem.core.world.RecipeTables;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The recipe tables' script fields ({@link RecipeFields}, read by Golem Core's KubeJS adapter): every stated field is a
 * key of its table's codec, every HBM table states all of its keys, each HBM shape gives back the value it wrote, and
 * rows written through the shapes are read by the tables' own codecs (and their typed fields read back from the
 * re-encoded row). Registered on {@link RecipeTableGameTests#TESTS}.
 */
public final class ScriptFieldsGameTests {

    private ScriptFieldsGameTests() {}

    public static final GameTests.Function MATCH_CODECS = RecipeTableGameTests.TESTS.function(
            "script_fields_match_codecs", 20, () -> ScriptFieldsGameTests::matchCodecs);

    public static final GameTests.Function ROWS_PARSE = RecipeTableGameTests.TESTS.function(
            "script_fields_rows_parse", 20, () -> ScriptFieldsGameTests::rowsParse);

    public static final GameTests.Function OUTPUT_DATA = RecipeTableGameTests.TESTS.function(
            "script_fields_output_item_data", 20, () -> ScriptFieldsGameTests::outputData);

    public static void init() {}

    private static final String IRON = "minecraft:iron_ingot";
    private static final String GOLD = "minecraft:gold_ingot";
    private static final String STEEL_TAG = "c:ingots/steel";

    /** One representative neutral value per HBM shape (a new shape needs one here). */
    private static Map<Shape, Object> samples() {
        Map<Shape, Object> samples = new IdentityHashMap<>();
        samples.put(RecipeShapes.INGREDIENT, new ItemIn(STEEL_TAG, true, 3));
        samples.put(RecipeShapes.INGREDIENT_ID, new ItemIn(STEEL_TAG, true, 1));
        samples.put(RecipeShapes.ITEM_ID_IN, new ItemIn(IRON, false, 1));
        samples.put(RecipeShapes.TAG_ID_IN, new ItemIn(STEEL_TAG, true, 1));
        samples.put(RecipeShapes.ITEM_STACK_IN, new ItemIn(IRON, false, 4));
        samples.put(RecipeShapes.SLOT, new ItemIn(IRON, false, 2));
        samples.put(RecipeShapes.PEDESTAL_SLOT, new ItemIn(STEEL_TAG, true, 4));
        samples.put(RecipeShapes.STACK_OUT, new ItemOut(GOLD, 3, null));
        samples.put(RecipeShapes.STACK_OUT_OBJECT, new ItemOut(GOLD, 1, null));
        samples.put(RecipeShapes.ITEM_ID_OUT, new ItemOut(GOLD, 1, null));
        samples.put(RecipeShapes.OUTPUT, new ItemOut(GOLD, 2, 0.5F));
        samples.put(RecipeShapes.CHANCE_OUTPUT_OBJECT, new ItemOut(GOLD, 1, 0.25F));
        samples.put(RecipeShapes.FLUID_STACK, new FluidAmount("hbm:oil", 500));
        samples.put(RecipeShapes.FLUID_ID, new FluidAmount("hbm:oil", 1000));
        return samples;
    }

    private static Set<String> keys(RecipeTables.Table<?> table) {
        Set<String> keys = new TreeSet<>();
        table.codec().keys(JsonOps.INSTANCE).forEach(key -> keys.add(key.getAsString()));
        return keys;
    }

    /** A field {@link RecipeTables.Table#fields()} added for an unstated codec key (raw JSON, nothing else set). */
    private static boolean unstated(RecipeFields.Field field) {
        return field.kind() == Kind.JSON && field.isOptional() && !field.isList() && field.shape() == null
                && field.fallback() == null && field.aliases().isEmpty();
    }

    public static void matchCodecs(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        Map<Shape, Object> samples = samples();
        int tables = 0;
        int hbm = 0;
        for (RecipeTables.Table<?> table : RecipeTables.all()) {
            tables++;
            Set<String> keys = keys(table);
            boolean own = table.id().startsWith("hbm:");
            if (own) hbm++;
            Set<String> named = new TreeSet<>();
            for (RecipeFields.Field field : table.fields().fields()) {
                named.add(field.name());
                if (!keys.contains(field.name())) problems.add(table.id() + ": field " + field.name() + " is not a codec key " + keys);
                if (own && unstated(field)) problems.add(table.id() + ": key " + field.name() + " is not stated");
                if (field.shape() != null && !samples.containsKey(field.shape())) {
                    problems.add(table.id() + ": field " + field.name() + " uses a shape without a test sample");
                }
            }
            if (!named.equals(keys)) problems.add(table.id() + ": fields " + named + " differ from the codec keys " + keys);
        }
        // every shape gives back what it wrote
        for (Map.Entry<Shape, Object> sample : samples.entrySet()) {
            JsonElement written = sample.getKey().write(sample.getValue());
            Object back = sample.getKey().read(JsonParser.parseString(written.toString()));
            if (!Objects.equals(back, sample.getValue())) problems.add("shape " + written + " reads back " + back + ", not " + sample.getValue());
        }
        // HBM's special forms stay raw JSON
        expectNull(problems, RecipeShapes.INGREDIENT, "{\"material\": \"iron\", \"shape\": \"ingot\"}");
        expectNull(problems, RecipeShapes.INGREDIENT, "{\"type\": \"hbm:fluid_container\", \"fluid\": \"hbm:oil\", \"amount\": 1000}");
        expectNull(problems, RecipeShapes.INGREDIENT, "{\"ingredient\": \"minecraft:paper\", \"exact\": true}");
        expectNull(problems, RecipeShapes.OUTPUT, "{\"tag\": \"c:ingots/steel\", \"count\": 2}");
        expectNull(problems, RecipeShapes.OUTPUT, "{\"id\": \"hbm:blueprints\", \"item_data\": {\"blueprint_pool\": \"x\"}}");
        expectNull(problems, RecipeShapes.OUTPUT, "[{\"data\": \"minecraft:stone\", \"weight\": 1}]");
        expectNull(problems, RecipeShapes.FLUID_STACK, "{\"type\": \"hbm:steam\", \"amount\": 100, \"pressure\": 1}");
        expectNull(problems, RecipeShapes.STACK_OUT, "{\"scraps\": {\"material\": \"iron\", \"amount\": 72}}");
        // a value the form cannot hold fails instead of being dropped
        expectFailure(problems, RecipeShapes.STACK_OUT, new ItemOut(GOLD, 1, 0.5F));
        expectFailure(problems, RecipeShapes.INGREDIENT_ID, new ItemIn(IRON, false, 2));
        expectFailure(problems, RecipeShapes.ITEM_ID_IN, new ItemIn(STEEL_TAG, true, 1));
        helper.assertTrue(hbm >= 53, "expected HBM's 53 recipe tables, found " + hbm + " of " + tables);
        helper.assertTrue(problems.isEmpty(), String.join("\n", problems));
        helper.succeed();
    }

    private static void expectNull(List<String> problems, Shape shape, String json) {
        Object read = shape.read(JsonParser.parseString(json));
        if (read != null) problems.add("special form " + json + " read as " + read);
    }

    private static void expectFailure(List<String> problems, Shape shape, Object value) {
        try {
            problems.add("shape wrote " + shape.write(value) + " for " + value + " instead of failing");
        } catch (IllegalArgumentException expected) {
            // the form cannot state it
        }
    }

    // Rows

    /** A row written field by field through the stated shapes, as a script would. */
    private static final class Row {

        final RecipeTables.Table<?> table;
        final JsonObject json = new JsonObject();
        final Map<String, List<Object>> typed = new LinkedHashMap<>();
        final List<String> problems;

        Row(String id, List<String> problems) {
            this.problems = problems;
            RecipeTables.Table<?> found = null;
            for (RecipeTables.Table<?> table : RecipeTables.all()) if (table.id().equals(id)) found = table;
            if (found == null) problems.add("no table " + id);
            this.table = found;
        }

        private RecipeFields.Field field(String name) {
            RecipeFields.Field field = table == null ? null : table.fields().get(name);
            if (field == null) problems.add((table == null ? "?" : table.id()) + ": no field " + name);
            return field;
        }

        /** Typed values through the field's shape (one value, or the elements of a list field). */
        Row put(String name, Object... values) {
            RecipeFields.Field field = field(name);
            if (field == null) return this;
            if (field.shape() == null) {
                problems.add(table.id() + ": field " + name + " has no shape");
                return this;
            }
            List<Object> list = new ArrayList<>();
            for (Object value : values) list.add(value);
            if (field.isList()) {
                JsonArray array = new JsonArray();
                for (Object value : values) array.add(field.shape().write(value));
                json.add(name, array);
            } else {
                json.add(name, field.shape().write(values[0]));
            }
            typed.put(name, list);
            return this;
        }

        /** A plain value or raw JSON, as a script passes it. */
        Row raw(String name, Object value) {
            if (field(name) == null) return this;
            JsonElement element = value instanceof JsonElement json ? json
                    : value instanceof String text ? (text.startsWith("{") || text.startsWith("[") ? JsonParser.parseString(text) : new JsonPrimitive(text))
                    : value instanceof Boolean flag ? new JsonPrimitive(flag)
                    : new JsonPrimitive((Number) value);
            json.add(name, element);
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        void check(HolderLookup.Provider registries) {
            if (table == null) return;
            JsonElement back;
            try {
                Object value = table.fromJson(json, registries);
                back = ((RecipeTables.Table) table).toJson(value, registries);
            } catch (RuntimeException e) {
                problems.add(table.id() + ": " + json + " does not parse: " + e.getMessage());
                return;
            }
            for (Map.Entry<String, List<Object>> entry : typed.entrySet()) {
                RecipeFields.Field field = table.fields().get(entry.getKey());
                JsonElement encoded = back.getAsJsonObject().get(entry.getKey());
                List<Object> read = new ArrayList<>();
                if (encoded != null && field.isList()) {
                    for (JsonElement element : encoded.getAsJsonArray()) read.add(field.shape().read(element));
                } else if (encoded != null) {
                    read.add(field.shape().read(encoded));
                }
                List<Object> expected = entry.getValue();
                if (!read.equals(expected)) {
                    problems.add(table.id() + ": " + entry.getKey() + " reads back " + read + " from " + encoded + ", wrote " + expected);
                }
            }
        }
    }

    public static void rowsParse(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ItemIn steel3 = new ItemIn(STEEL_TAG, true, 3);
        ItemIn iron = new ItemIn(IRON, false, 1);
        ItemIn iron2 = new ItemIn(IRON, false, 2);
        ItemOut gold = new ItemOut(GOLD, 1, null);
        ItemOut gold4 = new ItemOut(GOLD, 4, null);
        FluidAmount oil = new FluidAmount("hbm:oil", 1000);
        FluidAmount water = new FluidAmount("minecraft:water", 250);

        // the generic schema, and a subclass with typed extras
        new Row("hbm:chemical_plant", problems).raw("name", "chem.script_test")
                .put("output_items", new ItemOut(GOLD, 2, 0.5F), new ItemOut(GOLD, 1, 1F)) // a row with a chance states every chance.put("input_items", steel3, iron)
                .put("output_fluids", new FluidAmount("hbm:heavyoil", 500)).put("input_fluids", water, oil)
                .raw("duration", 100).raw("power", 400L).put("icon", gold4).raw("requires", "[\"!enable_528\"]")
                .put("input_items_expensive", iron2).check(registries);
        new Row("hbm:fusion", problems).raw("name", "fus.script_test").put("input_fluids", oil)
                .put("output_fluids", water).raw("duration", 20).raw("ignition_temp", 1000L).raw("red", 0.5D).check(registries);
        // record tables
        new Row("hbm:press", problems).put("output", gold4).put("input", steel3).raw("stamp", "flat").check(registries);
        new Row("hbm:refinery", problems).put("outputs", new FluidAmount("hbm:heavyoil", 500), new FluidAmount("hbm:naphtha", 250),
                        new FluidAmount("hbm:lightoil", 150), new FluidAmount("hbm:petroleum", 100))
                .put("byproduct", gold).put("input", oil).check(registries);
        new Row("hbm:cracking", problems).put("left", new FluidAmount("hbm:petroleum", 50)).put("input", oil).check(registries);
        new Row("hbm:coker", problems).put("output", gold).put("input", oil).raw("amount", 100)
                .put("byproduct", new FluidAmount("hbm:heavyoil", 10)).check(registries);
        new Row("hbm:compressor", problems).put("output", new FluidAmount("hbm:heavyoil", 100)).put("input", oil)
                .raw("amount", 1000).raw("duration", 20).raw("pressure", 1).check(registries);
        new Row("hbm:liquefaction", problems).put("output", new FluidAmount("hbm:oil", 100))
                .put("input", new ItemIn(STEEL_TAG, true, 1)).check(registries);
        new Row("hbm:shredder", problems).put("output", gold4).put("input", iron).check(registries);
        new Row("hbm:storage_drum", problems).put("output", gold).put("input", iron2).raw("decay", "short").check(registries);
        new Row("hbm:crystallizer", problems).put("output", gold4).put("input", iron).put("acid", water)
                .raw("duration", 200).raw("productivity", 0.1D).check(registries);
        new Row("hbm:cyclotron", problems).put("output", gold).put("input", steel3).put("particle", iron)
                .raw("antimatter", 10).check(registries);
        new Row("hbm:ammo_press", problems).put("output", gold4)
                .put("grid", null, iron2, null, null, steel3, null, null, iron, null).raw("order", 3).check(registries);
        new Row("hbm:pedestal", problems).put("output", gold)
                .put("grid", new ItemIn(STEEL_TAG, true, 1), null, iron, null, new ItemIn(STEEL_TAG, true, 4), null, iron, null, iron)
                .raw("count", 2).check(registries);
        new Row("hbm:anvil_construction", problems).put("outputs", new ItemOut(GOLD, 2, 0.25F), gold)
                .put("inputs", steel3, iron).raw("tier", 2).check(registries);
        new Row("hbm:annihilator", problems).raw("milestones", "[{\"amount\": 64, \"payout\": \"minecraft:diamond\"}]")
                .put("tag", new ItemIn(STEEL_TAG, true, 1)).check(registries);
        new Row("hbm:magic", problems).put("result", gold).put("ingredients", iron, new ItemIn(STEEL_TAG, true, 1)).check(registries);
        new Row("hbm:arc_welder", problems).put("output", gold).put("ingredients", steel3, iron).raw("duration", 100)
                .raw("consumption", 1000L).put("fluid", water).check(registries);
        new Row("hbm:sawmill", problems).put("output", gold4).put("input", iron).raw("sawdust_chance", 0.5D).check(registries);
        helper.assertTrue(problems.isEmpty(), String.join("\n", problems));
        helper.succeed();
    }

    /** A script's output stack with data (a vehicle crate's block entity) reaches the machine's output unchanged. */
    public static void outputData(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ItemStack named = new ItemStack(Items.GOLD_INGOT, 2);
        ItemStacks.setCustomName(named, Component.literal("script output"));
        JsonElement data = ItemPatch.of(named).json(RegistryOps.create(JsonOps.INSTANCE, registries));
        Row row = new Row("hbm:assembly_machine", problems).raw("name", "ass.script_data")
                .put("output_items", new ItemOut(GOLD, 2, null, data)).put("input_items", new ItemIn(IRON, false, 1))
                .raw("duration", 100).raw("power", 10L);
        row.check(registries);
        expectFailure(problems, RecipeShapes.STACK_OUT, new ItemOut(GOLD, 1, null, data));
        if (row.table != null && problems.isEmpty()) {
            GenericRecipe recipe = (GenericRecipe) row.table.fromJson(row.json, registries);
            recipe.bindStacks();
            ItemStack out = recipe.outputItem[0];
            if (out.getCount() != 2 || !ItemStacks.sameItemAndData(out, named)) {
                problems.add("bound output " + out + " lost the data of " + named);
            }
        }
        // with KubeJS, the row of harness/kubejs/server_scripts/golem_kubejs_functional.js
        if (Platform.isModLoaded("kubejs")) {
            GenericRecipe scripted = AssemblyMachineRecipes.INSTANCE.recipeNameMap.get("kjs.item_data");
            ItemStack out = scripted == null ? ItemStack.EMPTY : scripted.outputItem[0];
            if (!out.is(Items.IRON_PICKAXE) || out.getDamageValue() != 3) problems.add("scripted kjs.item_data output: " + out);
        }
        helper.assertTrue(problems.isEmpty(), String.join("\n", problems));
        helper.succeed();
    }
}
