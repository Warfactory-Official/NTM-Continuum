package com.hbm.inventory.recipes.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.golem.core.world.RecipeFields;
import dev.golem.core.world.RecipeFields.FluidAmount;
import dev.golem.core.world.RecipeFields.ItemIn;
import dev.golem.core.world.RecipeFields.ItemOut;
import dev.golem.core.world.RecipeFields.Shape;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * HBM's recipe JSON forms as {@link RecipeFields.Shape}s: how a script's typed value (an item, a tag, a counted
 * ingredient, a fluid amount) becomes the JSON a table's codec reads, and back. A shape is applied to one value (each
 * element of a list field). {@code read} answers only the plain forms; HBM's special forms (autogen
 * {@code {"material", "shape"}}, the typed {@code hbm:*} ingredients, exact item data, tag outputs, weighted choices, a
 * fluid with a non-zero {@code pressure}) read as null, so a script sees them as raw JSON and they are never rewritten
 * into something else. A script writes such a form by passing a plain object, which the adapter keeps unchanged.
 *
 * <p>Writing is lenient about the value's type: besides the neutral values ({@link ItemIn}, {@link ItemOut},
 * {@link FluidAmount}) a shape accepts a string id ({@code "#c:ingots/steel"} for a tag). A value the form cannot state
 * (a count on a field without one, a chance on an output without one) fails rather than losing data.
 *
 * <p>Fluid pressure (the {@code "pressure"} key of {@link GenericRecipeCodec#FLUID_STACK}) is not part of the neutral
 * {@link FluidAmount}: {@link #FLUID_STACK} writes pressure 0 (the codec's default), and a script that needs a pressurized
 * fluid passes the object itself ({@code {type: 'hbm:steam', amount: 1000, pressure: 1}}).
 */
public final class RecipeShapes {

    private RecipeShapes() {}

    // Item inputs

    /**
     * {@link GenericRecipeCodec#STACK} / {@link TableCodecs#INGREDIENT}: {@code {"ingredient": id or "#tag", "count"}}
     * ({@code count} only when not 1). Reads back only that plain form.
     */
    public static final Shape INGREDIENT = shape(value -> {
        ItemIn in = itemIn(value);
        JsonObject json = new JsonObject();
        json.addProperty("ingredient", hashed(in));
        if (in.count() != 1) json.addProperty("count", in.count());
        return json;
    }, json -> {
        JsonObject object = object(json, Set.of("ingredient", "count"));
        if (object == null || !isString(object.get("ingredient"))) return null;
        String id = object.get("ingredient").getAsString();
        Integer count = count(object);
        return count == null ? null : unhashed(id, count);
    });

    /** {@link GenericRecipeCodec#INGREDIENT}: a bare {@code "id"} or {@code "#tag"}, one item. */
    public static final Shape INGREDIENT_ID = shape(value -> {
        ItemIn in = itemIn(value);
        single(in.count(), "this input takes no count");
        return new JsonPrimitive(hashed(in));
    }, json -> isString(json) ? unhashed(json.getAsString(), 1) : null);

    /** A bare item id ({@code ItemStacks.itemCodec()}), no tag and no count. */
    public static final Shape ITEM_ID_IN = shape(value -> {
        ItemIn in = itemIn(value);
        if (in.tag()) throw new IllegalArgumentException("this input takes an item, not the tag #" + in.id());
        single(in.count(), "this input takes no count");
        return new JsonPrimitive(in.id());
    }, json -> isString(json) && !json.getAsString().startsWith("#") ? new ItemIn(json.getAsString(), false, 1) : null);

    /** A tag id without {@code #} (the annihilator's {@code "tag"}). */
    public static final Shape TAG_ID_IN = shape(value -> {
        ItemIn in = itemIn(value);
        if (!in.tag()) throw new IllegalArgumentException("this input takes a tag, not the item " + in.id());
        single(in.count(), "this input takes no count");
        return new JsonPrimitive(in.id());
    }, json -> isString(json) ? new ItemIn(strip(json.getAsString()), true, 1) : null);

    /** An item input in the item-stack form ({@link GenericRecipeCodec#ITEM_OUT}): {@code "id"} or {@code {"id", "count"}}. */
    public static final Shape ITEM_STACK_IN = shape(value -> {
        ItemIn in = itemIn(value);
        if (in.tag()) throw new IllegalArgumentException("this input takes an item, not the tag #" + in.id());
        return stack(in.id(), in.count(), false);
    }, json -> {
        ItemOut out = readStack(json, false);
        return out == null ? null : new ItemIn(out.id(), false, out.count());
    });

    /** A grid position ({@link TableCodecs#SLOT}): {@link #INGREDIENT}, or {@code ""} (null) for a slot that stays empty. */
    public static final Shape SLOT = shape(value -> empty(value) ? new JsonPrimitive("") : INGREDIENT.write(value),
            json -> isString(json) ? null : INGREDIENT.read(json));

    /** A pedestal: {@code "id"} / {@code "#tag"}, {@code {"id", "count"}} when more than one, {@code ""} (null) when empty. */
    public static final Shape PEDESTAL_SLOT = shape(value -> {
        if (empty(value)) return new JsonPrimitive("");
        ItemIn in = itemIn(value);
        if (in.count() == 1) return new JsonPrimitive(hashed(in));
        JsonObject json = new JsonObject();
        json.addProperty("id", hashed(in));
        json.addProperty("count", in.count());
        return json;
    }, json -> {
        if (isString(json)) return json.getAsString().isEmpty() ? null : unhashed(json.getAsString(), 1);
        JsonObject object = object(json, Set.of("id", "count"));
        if (object == null || !isString(object.get("id"))) return null;
        Integer count = count(object);
        return count == null ? null : unhashed(object.get("id").getAsString(), count);
    });

    // Item outputs

    /** {@link GenericRecipeCodec#ITEM_OUT} / {@link ItemSpec#CODEC}: {@code "id"}, or {@code {"id", "count"}}; no chance. */
    public static final Shape STACK_OUT = shape(value -> {
        ItemOut out = itemOut(value);
        noChance(out);
        return stack(out.id(), out.count(), false);
    }, json -> readStack(json, false));

    /** {@link ItemSpec#MAP_CODEC} as an object: always {@code {"id", "count"}} (the generic schema's {@code icon}). */
    public static final Shape STACK_OUT_OBJECT = shape(value -> {
        ItemOut out = itemOut(value);
        noChance(out);
        return stack(out.id(), out.count(), true);
    }, json -> json.isJsonObject() ? readStack(json, false) : null);

    /** A bare item id, one item (lemegeton, magic, pedestal outputs). */
    public static final Shape ITEM_ID_OUT = shape(value -> {
        ItemOut out = itemOut(value);
        noChance(out);
        single(out.count(), "this output takes no count");
        return new JsonPrimitive(out.id());
    }, json -> isString(json) && !json.getAsString().startsWith("#") ? new ItemOut(json.getAsString(), 1, null) : null);

    /**
     * The generic schema's output ({@code output_items}): {@code "id"} for one plain item, else {@code {"id", "count",
     * "chance"}}. Tag outputs, item data and weighted choices read as null (raw JSON).
     */
    public static final Shape OUTPUT = shape(value -> {
        ItemOut out = itemOut(value);
        if (out.count() == 1 && out.chance() == null) return new JsonPrimitive(out.id());
        return chanceObject(out);
    }, json -> readStack(json, true));

    /** An output that is always an object, with an optional chance (the anvil's construction outputs). */
    public static final Shape CHANCE_OUTPUT_OBJECT = shape(value -> chanceObject(itemOut(value)),
            json -> json.isJsonObject() ? readStack(json, true) : null);

    // Fluids

    /** {@link GenericRecipeCodec#FLUID_STACK}: {@code {"type", "amount"}}; one with a non-zero pressure reads as null. */
    public static final Shape FLUID_STACK = shape(value -> {
        FluidAmount fluid = fluid(value);
        JsonObject json = new JsonObject();
        json.addProperty("type", fluid.id());
        json.addProperty("amount", fluid.amount());
        return json;
    }, json -> {
        JsonObject object = object(json, Set.of("type", "amount", "pressure"));
        if (object == null || !isString(object.get("type")) || !isInt(object.get("amount"))) return null;
        if (object.has("pressure") && (!isInt(object.get("pressure")) || object.get("pressure").getAsInt() != 0)) return null;
        return new FluidAmount(object.get("type").getAsString(), object.get("amount").getAsInt());
    });

    /**
     * {@link GenericRecipeCodec#FLUID}: a bare fluid id (a table keyed by its input fluid, whose amounts are other
     * fields). Writing ignores the amount; reading answers one bucket, so a script's fluid filter sees a non-empty fluid.
     */
    public static final Shape FLUID_ID = shape(value -> new JsonPrimitive(fluid(value).id()),
            json -> isString(json) ? new FluidAmount(json.getAsString(), 1000) : null);

    // Helpers

    private static Shape shape(Function<Object, JsonElement> write, Function<JsonElement, Object> read) {
        return new Shape() {
            @Override
            public JsonElement write(Object value) {
                return write.apply(value);
            }

            @Override
            @Nullable
            public Object read(JsonElement json) {
                return json == null ? null : read.apply(json);
            }
        };
    }

    private static ItemIn itemIn(Object value) {
        if (value instanceof ItemIn in) return in;
        if (value instanceof ItemOut out && out.chance() == null) return new ItemIn(out.id(), false, out.count());
        if (value instanceof String text && !text.isEmpty()) return unhashed(text, 1);
        throw new IllegalArgumentException("not an item input: " + value);
    }

    private static ItemOut itemOut(Object value) {
        if (value instanceof ItemOut out) return out;
        if (value instanceof ItemIn in && !in.tag()) return new ItemOut(in.id(), in.count(), null);
        if (value instanceof String text && !text.isEmpty() && !text.startsWith("#")) return new ItemOut(text, 1, null);
        throw new IllegalArgumentException("not an item output: " + value);
    }

    private static FluidAmount fluid(Object value) {
        if (value instanceof FluidAmount fluid) return fluid;
        if (value instanceof String text && !text.isEmpty()) return new FluidAmount(text, 1000);
        throw new IllegalArgumentException("not a fluid: " + value);
    }

    private static boolean empty(Object value) {
        return value == null || (value instanceof String text && text.isEmpty())
                || (value instanceof ItemIn in && in.id().isEmpty());
    }

    private static String hashed(ItemIn in) {
        return in.tag() ? "#" + in.id() : in.id();
    }

    private static ItemIn unhashed(String id, int count) {
        return id.startsWith("#") ? new ItemIn(id.substring(1), true, count) : new ItemIn(id, false, count);
    }

    private static String strip(String id) {
        return id.startsWith("#") ? id.substring(1) : id;
    }

    private static void single(int count, String message) {
        if (count != 1) throw new IllegalArgumentException(message + " (got " + count + ")");
    }

    private static void noChance(ItemOut out) {
        if (out.chance() != null) throw new IllegalArgumentException("this output takes no chance: " + out.id());
    }

    private static JsonElement stack(String id, int count, boolean object) {
        if (count == 1 && !object) return new JsonPrimitive(id);
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        if (count != 1) json.addProperty("count", count);
        return json;
    }

    private static JsonObject chanceObject(ItemOut out) {
        JsonObject json = new JsonObject();
        json.addProperty("id", out.id());
        if (out.count() != 1) json.addProperty("count", out.count());
        if (out.chance() != null) json.addProperty("chance", out.chance());
        return json;
    }

    /** {@code "id"} or {@code {"id", "count"(, "chance")}} with no other key; anything else is null. */
    @Nullable
    private static ItemOut readStack(JsonElement json, boolean chance) {
        if (isString(json)) return json.getAsString().startsWith("#") ? null : new ItemOut(json.getAsString(), 1, null);
        JsonObject object = object(json, chance ? Set.of("id", "count", "chance") : Set.of("id", "count"));
        if (object == null || !isString(object.get("id"))) return null;
        Integer count = count(object);
        if (count == null) return null;
        Float value = null;
        if (object.has("chance")) {
            if (!object.get("chance").isJsonPrimitive() || !object.get("chance").getAsJsonPrimitive().isNumber()) return null;
            value = object.get("chance").getAsFloat();
        }
        return new ItemOut(object.get("id").getAsString(), count, value);
    }

    /** {@code json} as an object whose keys are all in {@code keys}, else null. */
    @Nullable
    private static JsonObject object(JsonElement json, Set<String> keys) {
        if (!json.isJsonObject()) return null;
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            if (!keys.contains(entry.getKey())) return null;
        }
        return json.getAsJsonObject();
    }

    /** The object's {@code count} (1 when absent), or null when it is not an integer. */
    @Nullable
    private static Integer count(JsonObject object) {
        if (!object.has("count")) return 1;
        return isInt(object.get("count")) ? object.get("count").getAsInt() : null;
    }

    private static boolean isString(@Nullable JsonElement json) {
        return json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isString();
    }

    private static boolean isInt(@Nullable JsonElement json) {
        if (json == null || !json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) return false;
        double value = json.getAsDouble();
        return value == Math.rint(value) && Math.abs(value) <= Integer.MAX_VALUE;
    }
}
