package com.hbm.items;

import com.google.gson.JsonObject;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;

/**
 * Item declarations for the items 1.7 gave an {@code IItemRenderer} ({@code ClientProxy.registerItemRenderer}),
 * as {@code hbm:custom_item} models naming the port of that renderer ({@code CustomItemRenderers}). Touches no
 * Minecraft class: the family declarations call it from {@code dejson.gradle.kts}'s plain JVM.
 *
 * <p>Two shapes, because 1.7's renderers came in two. One declined {@code INVENTORY} and so showed the item's
 * sprite in a slot and the model everywhere else ({@link #heldModel}); the other drew every slot itself
 * ({@link #model}).
 */
public final class ItemRendererDeclarations {

    private static final String NS = "hbm";

    private ItemRendererDeclarations() {}

    /**
     * A sprite in the inventory, the renderer everywhere else. The sprite is the item's own {@code item/handheld}
     * model (26.1 falls back to it for a declined slot) and is also named as {@code sprite}, which is what the
     * pre-26.1 path draws, since there the item's model is the renderer's {@code builtin/entity} marker.
     */
    public static void heldModel(String id, String sprite, String renderer) {
        Dejson.item(NS, id).models(Models.handheld("hbm:items/" + sprite)).register();
        JsonObject model = custom(renderer, "weapon");
        model.addProperty("flat", NS + ":item/" + id);
        model.addProperty("sprite", NS + ":items/" + sprite);
        Dejson.itemDefinition(NS + ":item/" + id).add("model", model);
    }

    /**
     * The renderer in every slot, the inventory included. {@code frame} is the engine pose it drew in:
     * {@code weapon} for an {@code IItemRenderer} of its own, {@code machine} for an {@code ItemRenderBase}.
     * 1.7 never clipped an icon to its slot, so neither does this.
     */
    public static void model(String id, String renderer, String frame) {
        Dejson.item(NS, id).shares(NS + ":item/" + frame + "_obj_context_base").register();
        Dejson.itemDefinition(NS + ":item/" + id).add("model", custom(renderer, frame));
        Dejson.itemDefinition(NS + ":item/" + id).addProperty("oversized_in_gui", true);
    }

    /**
     * A renderer that only adds to an already declared sprite item ({@code frame} {@code generated} or
     * {@code handheld}, the sprite's own model): the sprite in every slot, the renderer's extras over it where 1.7's
     * renderer handled the slot. Rewrites only the item definition; the sprite model stays as declared.
     */
    public static void overSprite(String id, String sprite, String renderer, String frame) {
        JsonObject model = custom(renderer, frame);
        model.addProperty("flat", NS + ":item/" + id);
        model.addProperty("sprite", NS + ":items/" + sprite);
        Dejson.itemDefinition(NS + ":item/" + id).add("model", model);
    }

    private static JsonObject custom(String renderer, String frame) {
        JsonObject model = new JsonObject();
        model.addProperty("type", NS + ":custom_item");
        model.addProperty("renderer", renderer);
        model.addProperty("frame", frame);
        return model;
    }
}
