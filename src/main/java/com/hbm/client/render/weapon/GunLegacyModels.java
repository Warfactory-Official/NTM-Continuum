package com.hbm.client.render.weapon;

//? if <26.1 {

/*import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.RecordingSubmitSink;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
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
 * <p>Mirrors {@code MachineItemModels}, which does the same job for OBJ machines.
 ^/
public final class GunLegacyModels {

    private static Map<Identifier, WeaponRenderer> models = Map.of();

    private GunLegacyModels() {}

    public static boolean isEmpty() {
        return models.isEmpty();
    }

    public static WeaponRenderer get(Identifier item) {
        return models.get(item);
    }

    /^* Re-reads every {@code assets/hbm/items/^.json} and records the guns among them. ^/
    public static void reload(ResourceManager resources) {
        Map<Identifier, WeaponRenderer> parsed = new HashMap<>();
        String prefix = "items/";

        for (Map.Entry<Identifier, Resource> entry : resources.listResources(
                "items", id -> id.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            if (!Constants.MOD_ID.equals(file.getNamespace())) continue;

            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                String weapon = weaponOf(root);
                if (weapon == null) continue;
                String path = file.getPath();
                String name = path.substring(prefix.length(), path.length() - ".json".length());
                parsed.put(dev.golem.core.Ids.of(file.getNamespace(), name), WeaponRenderers.byName(weapon));
            } catch (Exception e) {
                Constants.LOG.warn("Failed to read gun item definition {}: {}", file, e.toString());
            }
        }

        models = Map.copyOf(parsed);
        Constants.LOG.info("Loaded {} gun item model(s)", models.size());
    }

    /^* The weapon named by {@code {"model": {"type": "hbm:gun", "weapon": ...}}}, or null. ^/
    private static String weaponOf(JsonElement root) {
        if (!root.isJsonObject()) return null;
        JsonElement model = root.getAsJsonObject().get("model");
        if (model == null || !model.isJsonObject()) return null;
        JsonObject gun = model.getAsJsonObject();
        JsonElement type = gun.get("type");
        if (type == null || !"hbm:gun".equals(type.getAsString())) return null;
        JsonElement weapon = gun.get("weapon");
        return weapon == null ? null : weapon.getAsString();
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
        WeaponRenderer renderer = models.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (renderer == null) return false;

        RecordingSubmitSink sink = new RecordingSubmitSink();
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
