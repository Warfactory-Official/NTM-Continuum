package com.hbm.client.render;

//? if <26.1 {

/*import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.items.ModDataComponents;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.render.CompatRenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

 /^ 
 * The pre-26.1 half of the OBJ item renderer: reads the same `assets/<ns>/items/<name>.json` item
 * definitions 26.1 uses, and builds a {@link MachineItemModel} for every one that names the
 * `hbm:machine_obj` special renderer.
 *
 * <p>Below 1.21.4 vanilla has no item-definition layer at all — it never opens these files — so parsing
 * them here is not duplicating vanilla's work, it is supplying the step vanilla is missing. Doing it
 * from the {@link ResourceManager} rather than from the jar keeps the data resource-pack-overridable,
 * exactly as it is on 26.1, and keeps ONE copy of every machine's framing values in the tree.
 *
 * <p>Fields and defaults are kept in lockstep with {@code MachineItemModelRenderer.Unbaked}'s codec.
 * A definition that is malformed is skipped with a warning rather than failing the reload: a bad icon
 * must not cost the player their whole resource pack.
 ^/
public final class MachineItemModels {

    private MachineItemModels() {}

    private static final Identifier DEFAULT_TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/models/machines/lava_gray.png");

    /^ Item id -> its OBJ model. Rebuilt on every resource reload; empty until the first one. ^/
private static Map<Identifier, MachineItemModel> models = Map.of();
private static Map<Identifier, Map<Integer, MachineItemModel>> variantModels = Map.of();
    private static Map<Identifier, RBMKColumnItemModel> rbmkColumns = Map.of();
    private static Map<Identifier, RBMKBlockColumnItemModel> blockColumns = Map.of();

    /^ The model for an item, or null if that item is not an OBJ machine. ^/
public static MachineItemModel get(Identifier item) {
    return models.get(item);
}

 /^ The selected OBJ model for an item-definition variant, falling back to its default branch. ^/
public static MachineItemModel get(Identifier item, ItemStack stack) {
    Map<Integer, MachineItemModel> choices = variantModels.get(item);
    if (choices == null) return models.get(item);
    int variant = ModDataComponents.ITEM_VARIANT.getOr(stack, 0);
    return choices.getOrDefault(variant, choices.get(-1));
}

    /^ Whether any OBJ item models are loaded (used to skip work when a pack removes them all). ^/
    public static boolean isEmpty() {
        return models.isEmpty() && rbmkColumns.isEmpty() && blockColumns.isEmpty();
    }

    /^ The dedicated four-section fuel-channel model, or null for every other item. ^/
    public static RBMKColumnItemModel rbmkColumn(Identifier item) { return rbmkColumns.get(item); }

    /^ The plain/piped/lidded column preview, or null for every other item. ^/
    public static RBMKBlockColumnItemModel blockColumn(Identifier item) { return blockColumns.get(item); }

    /^ Reparses every item definition. Call from a client resource-reload listener. ^/
    public static void reload(ResourceManager resources) {
        // One reload listener drives both parsers: guns read the same item definitions for the weapon
        // renderer they name.
        com.hbm.client.render.weapon.GunLegacyModels.reload(resources);
        Map<Identifier, MachineItemModel> built = new HashMap<>();
        Map<Identifier, Map<Integer, MachineItemModel>> builtVariants = new HashMap<>();
        Map<Identifier, RBMKColumnItemModel> columns = new HashMap<>();
        Map<Identifier, RBMKBlockColumnItemModel> blocks = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry :
                resources.listResources("items", path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                JsonObject machine = machineObjOf(root);
                // "items/<name>.json" -> "<name>", the item's registry path.
                String path = file.getPath();
                String name = path.substring("items/".length(), path.length() - ".json".length());
                Identifier item = dev.golem.core.Ids.of(file.getNamespace(), name);
                if (machine != null) built.put(item, parse(machine));
                Map<Integer, JsonObject> variantJson = machineObjVariantsOf(root);
                if (!variantJson.isEmpty()) {
                    Map<Integer, MachineItemModel> parsed = new HashMap<>();
                    variantJson.forEach((variant, value) -> parsed.put(variant, parse(value)));
                    builtVariants.put(item, Map.copyOf(parsed));
                    MachineItemModel fallback = parsed.get(-1);
                    if (fallback != null) built.put(item, fallback);
                }
                JsonObject column = rbmkColumnOf(root);
                if (column != null) columns.put(item, parseColumn(column));
                JsonObject block = blockColumnOf(root);
                if (block != null) blocks.put(item, parseBlockColumn(block));
            } catch (Exception e) {
                Constants.LOG.warn("Skipping OBJ item model {}: {}", file, e.toString());
            }
        }
        models = Map.copyOf(built);
        variantModels = Map.copyOf(builtVariants);
        rbmkColumns = Map.copyOf(columns);
        blockColumns = Map.copyOf(blocks);
        // Logged at info, not debug: a silently-empty map renders every machine as an invisible item and
        // produces no warning anywhere, so the count is the only positive signal that this worked.
        Constants.LOG.info("Loaded {} OBJ item model(s) and {} RBMK fuel-column item model(s)", models.size(), rbmkColumns.size());
    }

     /^ 
     * The `hbm:machine_obj` object inside an item definition, or null if this definition is not one.
     * Shape: `{"model": {"type": "minecraft:special", "base": ..., "model": {"type": "hbm:machine_obj", ...}}}`.
     ^/
    private static JsonObject machineObjOf(JsonElement root) {
        if (!root.isJsonObject()) return null;
        JsonElement model = root.getAsJsonObject().get("model");
        if (model == null || !model.isJsonObject()) return null;
        JsonObject special = model.getAsJsonObject();
        if (!"minecraft:special".equals(string(special, "type", ""))) return null;
        JsonElement inner = special.get("model");
        if (inner == null || !inner.isJsonObject()) return null;
        JsonObject machine = inner.getAsJsonObject();
        // Same id as MachineItemModelRenderer.ID on 26.1; spelled out because that class does not exist here.
        return "hbm:machine_obj".equals(string(machine, "type", "")) ? machine : null;
    }

     /^ Reads both direct special models and the component-select form used by metadata-backed items. ^/
    private static Map<Integer, JsonObject> machineObjVariantsOf(JsonElement root) {
        Map<Integer, JsonObject> out = new LinkedHashMap<>();
        if (!root.isJsonObject()) return out;
        JsonElement model = root.getAsJsonObject().get("model");
        if (model == null || !model.isJsonObject()) return out;
        JsonObject modelObject = model.getAsJsonObject();
        if ("minecraft:special".equals(string(modelObject, "type", ""))) {
            JsonObject machine = machineObjFromSpecial(modelObject);
            if (machine != null) out.put(-1, machine);
            return out;
        }
        if (!"minecraft:select".equals(string(modelObject, "type", ""))
                || !"hbm:item_variant".equals(string(modelObject, "property", ""))) return out;
        JsonElement fallback = modelObject.get("fallback");
        if (fallback != null && fallback.isJsonObject()) {
            JsonObject machine = machineObjFromSpecial(fallback.getAsJsonObject());
            if (machine != null) out.put(-1, machine);
        }
        JsonElement cases = modelObject.get("cases");
        if (cases != null && cases.isJsonArray()) {
            for (JsonElement entry : cases.getAsJsonArray()) {
                if (!entry.isJsonObject()) continue;
                JsonObject caseObject = entry.getAsJsonObject();
                JsonElement when = caseObject.get("when");
                JsonElement selected = caseObject.get("model");
                if (when == null || selected == null || !selected.isJsonObject()) continue;
                try {
                    JsonObject machine = machineObjFromSpecial(selected.getAsJsonObject());
                    if (machine != null) out.put(when.getAsInt(), machine);
                } catch (RuntimeException ignored) {
                    // A malformed optional branch should not make every legacy OBJ item disappear.
                }
            }
        }
        return out;
    }

    private static JsonObject machineObjFromSpecial(JsonObject special) {
        if (!"minecraft:special".equals(string(special, "type", ""))) return null;
        JsonElement inner = special.get("model");
        if (inner == null || !inner.isJsonObject()) return null;
        JsonObject machine = inner.getAsJsonObject();
        return "hbm:machine_obj".equals(string(machine, "type", "")) ? machine : null;
    }

    private static JsonObject rbmkColumnOf(JsonElement root) {
        if (!root.isJsonObject()) return null;
        JsonElement model = root.getAsJsonObject().get("model");
        if (model == null || !model.isJsonObject()) return null;
        JsonObject special = model.getAsJsonObject();
        if (!"minecraft:special".equals(string(special, "type", ""))) return null;
        JsonElement inner = special.get("model");
        if (inner == null || !inner.isJsonObject()) return null;
        JsonObject column = inner.getAsJsonObject();
        return "hbm:rbmk_fuel_column".equals(string(column, "type", "")) ? column : null;
    }

     /^ 
     * The `hbm:rbmk_block_column` object inside an item definition, or null if this definition is not
     * one. Same shape as {@link #rbmkColumnOf} — only the type id differs.
     ^/
    private static JsonObject blockColumnOf(JsonElement root) {
        if (!root.isJsonObject()) return null;
        JsonElement model = root.getAsJsonObject().get("model");
        if (model == null || !model.isJsonObject()) return null;
        JsonObject special = model.getAsJsonObject();
        if (!"minecraft:special".equals(string(special, "type", ""))) return null;
        JsonElement inner = special.get("model");
        if (inner == null || !inner.isJsonObject()) return null;
        JsonObject column = inner.getAsJsonObject();
        return "hbm:rbmk_block_column".equals(string(column, "type", "")) ? column : null;
    }

    private static RBMKColumnItemModel parseColumn(JsonObject json) {
        return new RBMKColumnItemModel(id(json, "side", null), id(json, "top", null), id(json, "inner", null),
                f(json, "scale", .35F), f(json, "yaw", 0F), f(json, "lift", -.675F));
    }

    private static RBMKBlockColumnItemModel parseBlockColumn(JsonObject json) {
        return new RBMKBlockColumnItemModel(id(json, "side", null), id(json, "top", null), id(json, "bottom", null),
                id(json, "pipe_side", null), id(json, "pipe_top", null), id(json, "lid_texture", null),
                f(json, "scale", 1F), f(json, "yaw", 0F), f(json, "lift", -0.5F));
    }

    private static MachineItemModel parse(JsonObject json) {
        WavefrontObj obj = WavefrontObj.load(id(json, "obj", null));
        Identifier texture = id(json, "texture", DEFAULT_TEXTURE);
        Identifier overlayTexture = id(json, "overlayTexture", null);
        List<Float> pivot = floats(json, "rotatePivot");

        Map<String, RenderType> materialTypes = Map.of();
        if (bool(json, "mtl", false)) {
            // Multi-texture OBJ: resolve per-material textures from the .mtl next to the model.
            Identifier objId = id(json, "obj", null);
            Map<String, Identifier> lib = WavefrontObj.loadMtl(dev.golem.core.Ids.of(
                    objId.getNamespace(), objId.getPath().replace(".obj", ".mtl")));
            Map<String, RenderType> resolved = new LinkedHashMap<>();
            for (String mat : obj.materialNames()) {
                Identifier tex = lib.get(mat);
                if (tex != null) resolved.put(mat, CompatRenderTypes.entityCutout(tex));
            }
            materialTypes = resolved;
        }

        return new MachineItemModel(
                obj,
                CompatRenderTypes.entityCutout(texture),
                f(json, "scale", 0.28F),
                f(json, "yaw", 90.0F),
                f(json, "pitch", 0.0F),
                f(json, "ty", 0.0F),
                Set.copyOf(strings(json, "exclude")),
                overlayTexture == null ? null : CompatRenderTypes.entityCutout(overlayTexture),
                strings(json, "overlayParts"),
                strings(json, "rotateParts"),
                f(json, "rotateX", 0.0F),
                pivot.size() == 3 ? new Vector3f(pivot.get(0), pivot.get(1), pivot.get(2)) : new Vector3f(),
                materialTypes);
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private static Identifier id(JsonObject json, String key, Identifier fallback) {
        String value = string(json, key, null);
        return value == null ? fallback : dev.golem.core.Ids.parse(value);
    }

    private static float f(JsonObject json, String key, float fallback) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsFloat() : fallback;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsBoolean() : fallback;
    }

    private static List<String> strings(JsonObject json, String key) {
        JsonElement e = json.get(key);
        if (e == null || !e.isJsonArray()) return List.of();
        List<String> out = new ArrayList<>();
        e.getAsJsonArray().forEach(v -> out.add(v.getAsString()));
        return List.copyOf(out);
    }

    private static List<Float> floats(JsonObject json, String key) {
        JsonElement e = json.get(key);
        if (e == null || !e.isJsonArray()) return List.of();
        List<Float> out = new ArrayList<>();
        e.getAsJsonArray().forEach(v -> out.add(v.getAsFloat()));
        return List.copyOf(out);
    }
}
*///?}
