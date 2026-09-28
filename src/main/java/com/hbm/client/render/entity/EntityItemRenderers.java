package com.hbm.client.render.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.client.render.item.CustomItemRenderer;
import com.hbm.client.render.item.CustomItemRenderers;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which items had a 1.7 Forge {@code IItemRenderer} for {@code ENTITY}: the items whose framed stack Forge's
 * {@code ForgeHooksClient.renderEntityItem} drew with the renderer's own entity chain instead of
 * {@code RenderItem}'s block or sprite arm (see {@link FramedItem}).
 *
 * <p>The port kept each such renderer as a special item model, so the item definitions say it: {@code hbm:machine_obj}
 * (1.7 {@code ItemRenderBase}/{@code IItemRendererProvider} machines and {@code ItemRenderMissileGeneric} missiles,
 * all of which handled {@code ENTITY}) and {@code hbm:custom_item} (a ported {@code IItemRenderer}, asked for
 * {@code ENTITY} per stack, as Forge asked {@code handleRenderType}). The Sedna guns ({@code hbm:gun}) are
 * {@code WeaponRenderers}' own. Read from {@code assets/<ns>/items/*.json} on every client resource reload, the same
 * files both eras ship, so a resource pack that replaces a definition moves the item with it.
 */
public final class EntityItemRenderers {

    private static Set<Identifier> machines = Set.of();
    private static Map<Identifier, String> custom = Map.of();

    private EntityItemRenderers() {}

    /** Whether 1.7 drew a framed {@code stack} through its own {@code IItemRenderer}'s {@code ENTITY} chain. */
    public static boolean handles(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (machines.contains(id)) return true;
        String renderer = custom.get(id);
        return renderer != null && CustomItemRenderers.byName(renderer).handles(stack, CustomItemRenderer.Type.ENTITY);
    }

    /** How many items route through their renderer; the count to read when checking this loaded. */
    public static int count() {
        return machines.size() + custom.size();
    }

    public static void reload() {
        Set<Identifier> foundMachines = new HashSet<>();
        Map<Identifier, String> foundCustom = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : Minecraft.getInstance().getResourceManager()
                .listResources("items", path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath();
            Identifier item = dev.golem.core.Ids.of(file.getNamespace(),
                    path.substring("items/".length(), path.length() - ".json".length()));
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement root = JsonParser.parseReader(reader);
                String[] renderer = {null};
                boolean[] machine = {false};
                scan(root, machine, renderer);
                if (machine[0]) foundMachines.add(item);
                else if (renderer[0] != null && CustomItemRenderers.exists(renderer[0])) foundCustom.put(item, renderer[0]);
            } catch (Exception e) {
                Constants.LOG.warn("Skipping item definition {} for framed-item routing: {}", file, e.toString());
            }
        }
        machines = Set.copyOf(foundMachines);
        custom = Map.copyOf(foundCustom);
        Constants.LOG.info("Framed-item routing: {} OBJ machine/missile items, {} ported item renderers",
                machines.size(), custom.size());
    }

    /** Finds the special model anywhere in the definition (selects and conditions nest it). */
    private static void scan(JsonElement element, boolean[] machine, String[] renderer) {
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) scan(child, machine, renderer);
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement type = object.get("type");
            if (type != null && type.isJsonPrimitive()) {
                String name = type.getAsString();
                if (name.equals("hbm:machine_obj")) machine[0] = true;
                if (name.equals("hbm:custom_item") && object.has("renderer") && renderer[0] == null) {
                    renderer[0] = object.get("renderer").getAsString();
                }
            }
            for (Map.Entry<String, JsonElement> child : object.entrySet()) scan(child.getValue(), machine, renderer);
        }
    }
}
