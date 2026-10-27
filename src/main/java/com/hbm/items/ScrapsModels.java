package com.hbm.items;

import com.google.gson.JsonObject;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.ItemModel;
import dev.golem.core.dejson.ItemModels;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Textures;
import dev.golem.core.dejson.Tint;
import java.util.List;

/** Material-keyed scraps models and the same two-endpoint palettes as ItemAutogen. */
public final class ScrapsModels {
    private ScrapsModels() {}

    /** DESH keeps the name of its inherited model; material persistence still uses its native name. */
    public static String key(NTMMaterial material) {
        return material == Mats.MAT_DESH ? "workers_alloy" : material.name;
    }

    public static final String FALLBACK_MODEL_ID = "hbm:item/scraps";
    public static final Shape FALLBACK_MODEL = Models.generated(FALLBACK_MODEL_ID);

    public static final Tint COLOR = () -> {
        var json = new JsonObject();
        json.addProperty("type", "hbm:scraps_color");
        return json;
    };

    /** The host emits inherited models. An addon emits only its own models and extends the selector. */
    public static ItemModel definition(String addonNamespace) {
        var base = addonNamespace == null;
        var select = ItemModels.select("hbm:scrap_material");
        for (var material : Mats.orderedList) {
            if (!material.namespace.equals("hbm")) continue;
            palette(select, material, "hbm", base);
        }
        if (!base) {
            for (var material : Mats.orderedList) {
                if (material.namespace.equals(addonNamespace)) palette(select, material, addonNamespace, true);
            }
        }
        for (var molten : List.of("liquid", "additive")) {
            var model = "hbm:item/scraps_" + molten;
            if (base) {
                Textures.copy(model, "hbm:items/scraps_" + molten);
                Dejson.model(model, Models.generated(model));
            }
            select.when(molten, ItemModels.model(model, COLOR));
        }
        if (base) {
            Textures.copy(FALLBACK_MODEL_ID, "hbm:items/scraps");
            Dejson.model(FALLBACK_MODEL_ID, FALLBACK_MODEL);
        }
        return select.fallback(ItemModels.model(FALLBACK_MODEL_ID, COLOR));
    }

    private static void palette(ItemModels.Select select, NTMMaterial material, String namespace, boolean generate) {
        var authored = material == Mats.MAT_BISMUTH;
        if (!authored && material.solidColorLight == material.solidColorDark) return;
        var key = key(material);
        var model = namespace + ":item/scraps_" + key;
        if (generate) {
            if (authored) Textures.copy(model, "hbm:items/scraps_bismuth");
            else Textures.remap(model, "hbm:items/scraps", material.solidColorLight, material.solidColorDark);
            Dejson.model(model, Models.generated(model));
        }
        select.when(key, ItemModels.model(model));
        if (material.smeltable == NTMMaterial.SmeltingBehavior.NOT_SMELTABLE) {
            select.when(key + "_liquid", ItemModels.model(model, COLOR));
        }
    }
}
