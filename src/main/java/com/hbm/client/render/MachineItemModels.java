package com.hbm.client.render;

//? if <26.1 {

/*import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.items.ModDataComponents;
import com.hbm.client.model.WavefrontObj;
import dev.golem.core.render.gem.Surface;
import dev.golem.core.world.ItemModels;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

 /^ 
 * The pre-26.1 half of the OBJ item renderer: reads the same `assets/<ns>/items/<name>.json` item
 * definitions 26.1 uses, and builds a {@link MachineItemModel} for every one that names the
 * `hbm:machine_obj` special renderer.
 *
 * <p>Below 1.21.4 vanilla has no item-definition layer and never opens these files, so parsing them
 * here supplies the missing step. Reading through the {@link ResourceManager} rather than the jar keeps
 * the data resource-pack-overridable, as on 26.1, and keeps one copy of every machine's framing values.
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
    private static Map<Identifier, com.hbm.client.jetpack.JetpackGliderItemModel> gliders = Map.of();
    private static Map<Identifier, RBMKColumnItemModel> rbmkColumns = Map.of();
    private static Map<Identifier, RBMKBlockColumnItemModel> blockColumns = Map.of();
    private static Set<Identifier> oversized = Set.of();

    /^ The model for an item, or null if that item is not an OBJ machine. ^/
public static MachineItemModel get(Identifier item) {
    return models.get(item);
}

 /^ The selected OBJ model for an item-definition variant, falling back to its default branch. ^/
public static MachineItemModel get(Identifier item, ItemStack stack) {
    Map<Integer, MachineItemModel> choices = variantModels.get(item);
    if (choices == null) return models.get(item);
    int variant = com.hbm.items.ModelState.of(stack);
    return choices.getOrDefault(variant, choices.get(-1));
}

    /^ Whether any OBJ item models are loaded (used to skip work when a pack removes them all). ^/
    public static boolean isEmpty() {
        return models.isEmpty() && gliders.isEmpty() && rbmkColumns.isEmpty() && blockColumns.isEmpty();
    }

    public static boolean isGlider(Identifier item) { return gliders.containsKey(item); }

    /^ Whether the definition opted out of slot clipping, the way 26.1's item properties do. ^/
    public static boolean isOversizedInGui(Identifier item) { return oversized.contains(item); }

    /^ The dedicated four-section fuel-channel model, or null for every other item. ^/
    public static RBMKColumnItemModel rbmkColumn(Identifier item) { return rbmkColumns.get(item); }

    /^ The plain/piped/lidded column preview, or null for every other item. ^/
    public static RBMKBlockColumnItemModel blockColumn(Identifier item) { return blockColumns.get(item); }

    /^ Reparses every item definition. Call from a client resource-reload listener. ^/
    public static void reload(ResourceManager resources) {
        // One reload listener drives both parsers: guns read the same item definitions for the weapon
        // renderer they name.
        com.hbm.client.render.weapon.GunLegacyModels.reload(resources);
        reloadCustom(resources);
        Map<Identifier, MachineItemModel> built = new HashMap<>();
        Map<Identifier, Map<Integer, MachineItemModel>> builtVariants = new HashMap<>();
        Map<Identifier, RBMKColumnItemModel> columns = new HashMap<>();
        Map<Identifier, RBMKBlockColumnItemModel> blocks = new HashMap<>();
        Map<Identifier, com.hbm.client.jetpack.JetpackGliderItemModel> gliderItems = new HashMap<>();
        Set<Identifier> oversizedItems = new HashSet<>();
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
                if (jetpackGliderOf(root) != null) gliderItems.put(item,
                        new com.hbm.client.jetpack.JetpackGliderItemModel());
                if (machine != null) built.put(item, parse(machine, isBlockFramed(root)));
                if (root.isJsonObject() && bool(root.getAsJsonObject(), "oversized_in_gui", false)) {
                    oversizedItems.add(item);
                }
                Map<Integer, JsonObject> variantJson = machineObjVariantsOf(root);
                if (!variantJson.isEmpty()) {
                    Map<Integer, MachineItemModel> parsed = new HashMap<>();
                    boolean blockFramed = isBlockFramed(root);
                    variantJson.forEach((variant, value) -> parsed.put(variant, parse(value, blockFramed)));
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
        gliders = Map.copyOf(gliderItems);
        oversized = Set.copyOf(oversizedItems);
        // Logged at info, not debug: a silently-empty map renders every machine as an invisible item and
        // produces no warning anywhere, so the count is the only positive signal that this worked.
        Constants.LOG.info("Loaded {} OBJ item model(s) and {} RBMK fuel-column item model(s)", models.size(), rbmkColumns.size());
    }

    private static JsonObject jetpackGliderOf(JsonElement root) {
        if (!root.isJsonObject()) return null;
        JsonElement model = root.getAsJsonObject().get("model");
        if (model == null || !model.isJsonObject()
                ) return null;
        if ("hbm:jetpack_glider".equals(string(model.getAsJsonObject(), "type", "")))
            return model.getAsJsonObject();
        if (!"minecraft:special".equals(string(model.getAsJsonObject(), "type", ""))) return null;
        JsonElement inner = model.getAsJsonObject().get("model");
        return inner != null && inner.isJsonObject()
                && "hbm:jetpack_glider".equals(string(inner.getAsJsonObject(), "type", ""))
                ? inner.getAsJsonObject() : null;
    }

     /^ 
     * The `hbm:machine_obj` object inside an item definition, or null if this definition is not one.
      * Shape: `{"model": {"type": "minecraft:special", "base": ..., "model": {"type": "hbm:machine_obj", ...}}}`
      * or the context-aware direct form `{"model": {"type": "hbm:machine_obj", ...}}`.
     ^/
    private static JsonObject machineObjOf(JsonElement root) {
        if (!root.isJsonObject()) return null;
        JsonElement model = root.getAsJsonObject().get("model");
        if (model == null || !model.isJsonObject()) return null;
        JsonObject special = model.getAsJsonObject();
        if ("hbm:machine_obj".equals(string(special, "type", ""))) return special;
        if (!"minecraft:special".equals(string(special, "type", ""))) return null;
        JsonElement inner = special.get("model");
        if (inner == null || !inner.isJsonObject()) return null;
        JsonObject machine = inner.getAsJsonObject();
        // Same id as MachineItemModelRenderer.ID on 26.1; spelled out because that class does not exist here.
        return "hbm:machine_obj".equals(string(machine, "type", "")) ? machine : null;
    }

    private static boolean isBlockFramed(JsonElement root) {
        if (!root.isJsonObject()) return true;
        JsonElement model = root.getAsJsonObject().get("model");
        return model == null || !model.isJsonObject()
                || !"hbm:machine_obj".equals(string(model.getAsJsonObject(), "type", ""));
    }

     /^ Reads both direct special models and the component-select form used by metadata-backed items. ^/
    private static Map<Integer, JsonObject> machineObjVariantsOf(JsonElement root) {
        Map<Integer, JsonObject> out = new LinkedHashMap<>();
        if (!root.isJsonObject()) return out;
        JsonElement model = root.getAsJsonObject().get("model");
        if (model == null || !model.isJsonObject()) return out;
        JsonObject modelObject = model.getAsJsonObject();
        if ("hbm:machine_obj".equals(string(modelObject, "type", ""))) {
            out.put(-1, modelObject);
            return out;
        }
        if ("minecraft:special".equals(string(modelObject, "type", ""))) {
            JsonObject machine = machineObjFromSpecial(modelObject);
            if (machine != null) out.put(-1, machine);
            return out;
        }
        if (!"minecraft:select".equals(string(modelObject, "type", ""))
                || !"hbm:model_state".equals(string(modelObject, "property", ""))) return out;
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
        if ("hbm:machine_obj".equals(string(special, "type", ""))) return special;
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
     * one. Same shape as {@link #rbmkColumnOf}, only the type id differs.
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
                id(json, "fuel", null), f(json, "scale", .35F), f(json, "yaw", 0F), f(json, "lift", -.675F));
    }

    private static RBMKBlockColumnItemModel parseBlockColumn(JsonObject json) {
        return new RBMKBlockColumnItemModel(id(json, "side", null), id(json, "top", null), id(json, "bottom", null),
                id(json, "pipe_side", null), id(json, "pipe_top", null), id(json, "lid_texture", null),
                f(json, "scale", 1F), f(json, "yaw", 0F), f(json, "lift", -0.5F));
    }

    private static MachineItemModel parse(JsonObject json, boolean blockFramed) {
         Identifier objId = id(json, "obj", null);
         WavefrontObj obj = WavefrontObj.load(objId);
        Identifier texture = id(json, "texture", DEFAULT_TEXTURE);
        boolean noCull = bool(json, "noCull", false);
        Identifier overlayTexture = id(json, "overlayTexture", null);
        List<Float> pivot = floats(json, "rotatePivot");
        List<Float> spinPivot = floats(json, "spinPivot");
         List<Float> itemTranslate = floats(json, "itemTranslate");
         List<Float> itemPreTranslate = floats(json, "itemPreTranslate");
         List<Float> guiTranslate = floats(json, "guiTranslate");
         List<Float> primaryTranslate = floats(json, "translate");
         List<MachineItemModel.ModelPass> modelPasses = new ArrayList<>();
         JsonElement passesValue = json.get("passes");
         if (passesValue != null && passesValue.isJsonArray()) {
             for (JsonElement passValue : passesValue.getAsJsonArray()) {
                 if (!passValue.isJsonObject()) continue;
                 JsonObject pass = passValue.getAsJsonObject();
                 Identifier passObj = id(pass, "obj", objId);
                 Identifier passTexture = id(pass, "texture", texture);
                 List<Float> translate = floats(pass, "translate");
                 WavefrontObj passModel = objId.equals(passObj) ? obj : WavefrontObj.load(passObj);
                 modelPasses.add(new MachineItemModel.ModelPass(passModel,
                         cutout(passTexture, noCull), strings(pass, "parts"), vector(translate),
                         passSteps(pass), pass.has("tint") ? pass.get("tint").getAsInt()
                                 : MachineItemModel.ModelPass.UNTINTED, f(pass, "scale", 1F)));
             }
         }

         Map<String, Surface> materialTypes = Map.of();
          if (bool(json, "mtl", false)) {
            // Multi-texture OBJ: resolve per-material textures from the .mtl next to the model.
             Map<String, Identifier> lib = WavefrontObj.loadMtl(dev.golem.core.Ids.of(
                    objId.getNamespace(), objId.getPath().replace(".obj", ".mtl")));
            Map<String, Surface> resolved = new LinkedHashMap<>();
            for (String mat : obj.materialNames()) {
                Identifier tex = lib.get(mat);
                if (tex != null) resolved.put(mat, cutout(tex, noCull));
            }
                   materialTypes = resolved;
         }
         Map<String, Surface> partTypes = Map.of();
         JsonElement partTextureValue = json.get("partTextures");
         if (partTextureValue != null && partTextureValue.isJsonObject()) {
             Map<String, Surface> resolved = new LinkedHashMap<>();
             resolved.put("Body", cutout(texture, noCull));
             for (Map.Entry<String, JsonElement> entry : partTextureValue.getAsJsonObject().entrySet()) {
                 if (entry.getValue().isJsonPrimitive()) {
                     resolved.put(entry.getKey(), cutout(dev.golem.core.Ids.parse(entry.getValue().getAsString()), noCull));
                 }
             }
             partTypes = resolved;
         }
         List<String> blackParts = strings(json, "blackParts");
         Map<String, MachineItemModel.ContextTransform> contextTransforms = contextTransforms(json);

          return new MachineItemModel(
                 obj,
                 cutout(texture, noCull),
                 f(json, "scale", 0.28F),
                 bool(json, "rawLegacyTransform", false),
                 blockFramed,
                 f(json, "yaw", 90.0F),
                f(json, "pitch", 0.0F),
                 f(json, "ty", 0.0F),
                 itemTranslate.size() == 3 ? new Vector3f(itemTranslate.get(0), itemTranslate.get(1), itemTranslate.get(2)) : new Vector3f(),
                 itemPreTranslate.size() == 3 ? new Vector3f(itemPreTranslate.get(0), itemPreTranslate.get(1), itemPreTranslate.get(2)) : new Vector3f(),
                  guiTranslate.size() == 3 ? new Vector3f(guiTranslate.get(0), guiTranslate.get(1), guiTranslate.get(2)) : new Vector3f(),
                  f(json, "guiScale", 1.0F), f(json, "guiYaw", 0.0F),
                 Set.copyOf(strings(json, "exclude")),
                 overlayTexture == null ? null : cutout(overlayTexture, noCull),
                 strings(json, "overlayParts"),
                 blackParts.isEmpty() ? null : Surface.solid("hbm:textures/models/misc/white.png"),
                 blackParts,
                 strings(json, "rotateParts"),
                f(json, "rotateX", 0.0F),
                pivot.size() == 3 ? new Vector3f(pivot.get(0), pivot.get(1), pivot.get(2)) : new Vector3f(),
                 strings(json, "spinParts"),
                 spinPivot.size() == 3 ? new Vector3f(spinPivot.get(0), spinPivot.get(1), spinPivot.get(2)) : new Vector3f(),
                    materialTypes, partTypes,
                    bool(json, "spin", false), contextTransforms, string(json, "animation", ""),
                     strings(json, "parts"), vector(primaryTranslate), modelPasses, f(json, "guiSpinSpeed", 0F))
                    .withGuiSteps(guiSteps(json));
     }

    private static Surface cutout(Identifier texture, boolean noCull) {
        return noCull ? Surface.cutoutNoCull(texture.toString()) : Surface.cutout(texture.toString());
    }

    private static Map<String, MachineItemModel.ContextTransform> contextTransforms(JsonObject json) {
        JsonElement value = json.get("contextTransforms");
        if (value == null || !value.isJsonObject()) return Map.of();
        Map<String, MachineItemModel.ContextTransform> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject transform = entry.getValue().getAsJsonObject();
            List<Float> rotation = floats(transform, "rotation");
            List<Float> translation = floats(transform, "translation");
            out.put(entry.getKey(), new MachineItemModel.ContextTransform(
                    vector(rotation), vector(translation), f(transform, "scale", 1F), bool(transform, "raw_mesh", false)));
        }
        return Map.copyOf(out);
    }

    // The GUI-only renderInventory tail: [{"rotate": [..]}, {"translate": [..]}, {"scale": [..]}, {"spin": [axis]}].
    private static List<MachineItemModel.GuiStep> guiSteps(JsonObject json) {
        JsonElement value = json.get("guiSteps");
        if (value == null || !value.isJsonArray()) return List.of();
        List<MachineItemModel.GuiStep> steps = new ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject step = element.getAsJsonObject();
            for (String kind : List.of("spin", "scale", "translate", "rotate")) {
                if (step.has(kind)) {
                    steps.add(new MachineItemModel.GuiStep(kind, vector(floats(step, kind))));
                    break;
                }
            }
        }
        return List.copyOf(steps);
    }

    // A pass's own ordered transform: [{"rotate": [..]}, {"translate": [..]}, ...].
    private static List<MachineItemModel.PassStep> passSteps(JsonObject pass) {
        JsonElement value = pass.get("steps");
        if (value == null || !value.isJsonArray()) return List.of();
        List<MachineItemModel.PassStep> steps = new java.util.ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject step = element.getAsJsonObject();
            if (step.has("rotate")) steps.add(new MachineItemModel.PassStep(true, vector(floats(step, "rotate"))));
            else if (step.has("translate")) {
                steps.add(new MachineItemModel.PassStep(false, vector(floats(step, "translate"))));
            }
        }
        return List.copyOf(steps);
    }

    private static Vector3f vector(List<Float> values) {
        return values.size() == 3
                ? new Vector3f(values.get(0), values.get(1), values.get(2))
                : new Vector3f();
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

    // hbm:custom_item: the ported 1.7 item renderers (MachineItemModelRenderer.CustomItem on 26.1)
    // The same definitions, read here because nothing below 1.21.4 reads them. The item's own model is generated as
    // a builtin/entity marker wearing the frame's display{} (<frame>_obj_context_legacy, <frame>_obj_flat_legacy
    // when the inventory is the sprite, <generated|handheld>_sprite_legacy for a sprite the renderer draws on), so
    // vanilla applies the frame exactly as 26.1 does; a declined slot is drawn from the definition's sprite.

    /^* {@code spriteFrame}: a declined slot is the extruded sprite rather than the inventory square. ^/
    private record CustomEntry(com.hbm.client.render.item.CustomItemRenderer renderer, Identifier sprite, boolean oversized,
                         boolean spriteFrame) {}

    private static Map<Identifier, CustomEntry> customModels = Map.of();

    
    /^* Re-reads every {@code assets/<ns>/items/<name>.json} and records the custom-rendered items among them. ^/
    static void reloadCustom(ResourceManager resources) {
        Map<Identifier, CustomEntry> parsed = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : resources.listResources(
                "items", id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject()) continue;
                JsonElement model = root.getAsJsonObject().get("model");
                if (model == null || !model.isJsonObject()) continue;
                JsonObject custom = model.getAsJsonObject();
                if (!"hbm:custom_item".equals(customString(custom, "type"))) continue;
                String renderer = customString(custom, "renderer");
                if (renderer == null || !com.hbm.client.render.item.CustomItemRenderers.exists(renderer)) {
                    Constants.LOG.warn("Unknown hbm:custom_item renderer '{}' in {}", renderer, file);
                    continue;
                }
                String sprite = customString(custom, "sprite");
                String frame = customString(custom, "frame");
                boolean spriteFrame = sprite != null && ("generated".equals(frame) || "handheld".equals(frame));
                Identifier texture = sprite == null ? null : customTexture(dev.golem.core.Ids.parse(sprite));
                JsonElement oversized = root.getAsJsonObject().get("oversized_in_gui");
                String path = file.getPath();
                String name = path.substring("items/".length(), path.length() - ".json".length());
                parsed.put(dev.golem.core.Ids.of(file.getNamespace(), name),
                        new CustomEntry(com.hbm.client.render.item.CustomItemRenderers.byName(renderer), texture,
                                oversized != null && oversized.getAsBoolean(), spriteFrame));
            } catch (Exception e) {
                Constants.LOG.warn("Skipping custom item model {}: {}", file, e.toString());
            }
        }
        customModels = Map.copyOf(parsed);
        dev.golem.core.render.ItemSprites.clear();
        // The count is the positive signal: an empty map leaves every one of these items invisible, silently.
        Constants.LOG.info("Loaded {} custom item renderer model(s)", customModels.size());
    }

    private static Identifier customTexture(Identifier sprite) {
        return dev.golem.core.Ids.of(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
    }

    private static String customString(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    /^* Draws {@code stack} if its definition names a custom renderer; returns whether it did. ^/
    public static boolean renderCustom(ItemStack stack, net.minecraft.world.item.ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                 net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay) {
        if (customModels.isEmpty()) return false;
        CustomEntry entry = customModels.get(ItemModels.id(stack.getItem()));
        if (entry == null) return false;

        // No slot clip, unlike the machines: every renderer here that draws the inventory is oversized (1.7 clipped
        // nothing), and a declined inventory slot is the sprite's own square. The machines' clip also assumes a
        // block-centred model space, which the weapon frame's pixel slot is not.
        com.hbm.client.render.item.CustomItemRenderer.Type type = com.hbm.client.render.item.CustomItemRenderer.Type.of(context);
        dev.golem.core.render.RecordingSubmitSink sink = new dev.golem.core.render.RecordingSubmitSink();
        if (entry.renderer().handles(stack, type)) {
            entry.renderer().render(stack, context, poseStack, sink, light, overlay, dev.golem.core.render.ItemRenderContext.owner());
        } else if (entry.sprite() != null && entry.spriteFrame()) {
            dev.golem.core.render.ItemSprites.submit(poseStack, sink, entry.sprite(), light, overlay);
        } else if (entry.sprite() != null) {
            customSprite(poseStack, sink, entry.sprite(), light, overlay);
        }
        // The bobblehead and plushie hand props are item sub-draws, which need the level.
        sink.flush(buffers, net.minecraft.client.Minecraft.getInstance().level);
        return true;
    }

    /^*
     * The sprite over the slot's sixteen pixels, facing the viewer. The weapon frame's inventory space runs y
     * down from the slot's corner, so pixel {@code (0, 0)} takes the texture's top left.
     ^/
    public static void customSprite(com.mojang.blaze3d.vertex.PoseStack pose, dev.golem.core.render.SubmitSink sink, Identifier texture, int light, int overlay) {
        sink.submitCustomGeometry(pose, dev.golem.core.render.CompatRenderTypes.entityCutoutNoCull(texture), (p, buffer) -> {
            dev.golem.core.render.Vertexes.vertex(buffer, p, 0F, 0F, 0F, 0xFFFFFFFF, 0F, 0F, overlay, light, 0F, 0F, 1F);
            dev.golem.core.render.Vertexes.vertex(buffer, p, 0F, 16F, 0F, 0xFFFFFFFF, 0F, 1F, overlay, light, 0F, 0F, 1F);
            dev.golem.core.render.Vertexes.vertex(buffer, p, 16F, 16F, 0F, 0xFFFFFFFF, 1F, 1F, overlay, light, 0F, 0F, 1F);
            dev.golem.core.render.Vertexes.vertex(buffer, p, 16F, 0F, 0F, 0xFFFFFFFF, 1F, 0F, overlay, light, 0F, 0F, 1F);
        });
    }
}
*///?}
