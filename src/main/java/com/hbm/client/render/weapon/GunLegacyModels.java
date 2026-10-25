package com.hbm.client.render.weapon;

//? if <26.1 {

/*import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.client.render.MachineItemModels;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.RecordingSubmitSink;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/^*
 * The gun item models below 26.1, where there is no item-model system to declare one in.
 *
 * <p>26.1 resolves {@code {"type": "hbm:gun", "weapon": "atlas"}} in an item definition to a
 * {@link GunItemModel}. Older versions never read those files, so they are parsed here on resource
 * reload for the one thing that matters, which weapon renderer an item wants, and the draw goes
 * through the loader's "render this item yourself" hook, which <em>does</em> receive the display
 * context. Same renderers, same animations; only the plumbing differs.
 *
 * <p>Mirrors {@code MachineItemModels}, which does the same job for OBJ machines. A gun behind a
 * {@code display_context} select draws its sprite case's {@code layer0} in those contexts, as 1.7's B92 does in the inventory.
 ^/
public final class GunLegacyModels {

    private record GunModel(WeaponRenderer renderer, Set<String> spriteContexts, Identifier sprite) {}

    private static Map<Identifier, GunModel> models = Map.of();

    private GunLegacyModels() {}

    public static boolean isEmpty() {
        return models.isEmpty();
    }

    public static WeaponRenderer get(Identifier item) {
        GunModel model = models.get(item);
        return model == null ? null : model.renderer();
    }

    /^* Re-reads every {@code assets/hbm/items/^.json} and records the guns among them. ^/
    public static void reload(ResourceManager resources) {
        Map<Identifier, GunModel> parsed = new HashMap<>();
        String prefix = "items/";

        for (Map.Entry<Identifier, Resource> entry : resources.listResources(
                "items", id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            if (!Constants.MOD_ID.equals(file.getNamespace())) continue;

            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonObject() || !root.getAsJsonObject().has("model")) continue;
                JsonElement model = root.getAsJsonObject().get("model");
                Set<String> spriteContexts = new HashSet<>();
                Identifier sprite = null;
                if (model.isJsonObject() && "minecraft:select".equals(string(model.getAsJsonObject(), "type"))
                        && "minecraft:display_context".equals(string(model.getAsJsonObject(), "property"))) {
                    JsonObject select = model.getAsJsonObject();
                    for (JsonElement element : select.getAsJsonArray("cases")) {
                        JsonObject branch = element.getAsJsonObject();
                        String spriteModel = string(branch.getAsJsonObject("model"), "model");
                        if (spriteModel == null) continue;
                        sprite = layer0(resources, dev.golem.core.Ids.parse(spriteModel));
                        JsonElement when = branch.get("when");
                        if (when.isJsonArray()) when.getAsJsonArray().forEach(v -> spriteContexts.add(v.getAsString()));
                        else spriteContexts.add(when.getAsString());
                    }
                    model = select.get("fallback");
                }
                String weapon = weaponOf(model);
                if (weapon == null) continue;
                String path = file.getPath();
                String name = path.substring(prefix.length(), path.length() - ".json".length());
                parsed.put(dev.golem.core.Ids.of(file.getNamespace(), name),
                        new GunModel(WeaponRenderers.byName(weapon), Set.copyOf(spriteContexts), sprite));
            } catch (Exception e) {
                Constants.LOG.warn("Failed to read gun item definition {}: {}", file, e.toString());
            }
        }

        models = Map.copyOf(parsed);
        Constants.LOG.info("Loaded {} gun item model(s)", models.size());
    }

    /^* The weapon named by {@code {"type": "hbm:gun", "weapon": ...}}, or null. ^/
    private static String weaponOf(JsonElement model) {
        if (model == null || !model.isJsonObject()) return null;
        JsonObject gun = model.getAsJsonObject();
        return "hbm:gun".equals(string(gun, "type")) ? string(gun, "weapon") : null;
    }

    private static String string(JsonObject json, String key) {
        JsonElement e = json.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    /^* The texture file of a sprite model's {@code layer0}. ^/
    private static Identifier layer0(ResourceManager resources, Identifier model) throws IOException {
        Identifier file = dev.golem.core.Ids.of(model.getNamespace(), "models/" + model.getPath() + ".json");
        try (BufferedReader reader = resources.getResourceOrThrow(file).openAsReader()) {
            Identifier sprite = dev.golem.core.Ids.parse(JsonParser.parseReader(reader).getAsJsonObject()
                    .getAsJsonObject("textures").get("layer0").getAsString());
            return dev.golem.core.Ids.of(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
        }
    }

    /^*
     * Draws {@code stack} if it is a gun; returns whether it did.
     *
     * <p>The first-person body rebuilds its own pose from the camera, so the transform vanilla has
     * already applied for the hand is discarded inside, exactly as on 26.1.
     ^/
    public static boolean render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffers, int light, int overlay) {
        if (models.isEmpty()) return false;
        GunModel model = models.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (model == null) return false;
        WeaponRenderer renderer = model.renderer();

        RecordingSubmitSink sink = new RecordingSubmitSink();
        if (model.spriteContexts().contains(context.getSerializedName())) {
            MachineItemModels.customSprite(poseStack, sink, model.sprite(), light, overlay);
            sink.flush(buffers, null);
            return true;
        }
        boolean firstPerson = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        if (firstPerson) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            boolean mainHand = minecraft.player != null
                    && (minecraft.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT)
                    == (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
            renderer.submitFirstPerson(stack, poseStack, sink, light, overlay, !mainHand);
        } else {
            poseStack.pushPose();
            renderer.setupStatic(poseStack, stack, context);
            renderer.renderStatic(stack, poseStack, sink, light, overlay,
                    dev.golem.core.render.ItemRenderContext.owner());
            poseStack.popPose();
        }

        // No level: these draws are in hand or inventory space, and a gun makes no item sub-draws.
        sink.flush(buffers, null);
        return true;
    }
}
*///?}
