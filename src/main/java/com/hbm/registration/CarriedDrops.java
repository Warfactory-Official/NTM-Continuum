package com.hbm.registration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.Drops;
import dev.golem.core.registry.Loot;

/**
 * Loot for a multiblock machine whose block entity carries state in its item ({@code collectCarriedData}). Only the
 * core rolls it; a cell has no block entity, so the block forwards the core's roll when a player breaks a cell.
 */
public final class CarriedDrops {

    /** Drops itself even when blown up. */
    public static final Loot ALWAYS = (namespace, block) -> table(namespace, block, false);

    /** Drops itself, with vanilla's chance of losing it to an explosion. */
    public static final Loot SURVIVING = (namespace, block) -> table(namespace, block, true);

    private CarriedDrops() {}

    private static void table(String namespace, String block, boolean survivesExplosion) {
        JsonObject loot = Drops.container(namespace, block, "minecraft:custom_data", "minecraft:custom_name");
        JsonObject cellProperty = new JsonObject();
        cellProperty.addProperty("cell", "false");
        JsonObject coreOnly = new JsonObject();
        coreOnly.addProperty("condition", "minecraft:block_state_property");
        coreOnly.addProperty("block", namespace + ":" + block);
        coreOnly.add("properties", cellProperty);
        JsonArray conditions = new JsonArray();
        conditions.add(coreOnly);
        if (survivesExplosion) {
            JsonObject survives = new JsonObject();
            survives.addProperty("condition", "minecraft:survives_explosion");
            conditions.add(survives);
        }
        loot.getAsJsonArray("pools").get(0).getAsJsonObject().add("conditions", conditions);
    }
}
