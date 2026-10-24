package com.hbm.recipes.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.itempool.ItemPool.PoolFile;
import com.hbm.itempool.ItemPool.Row;
import com.hbm.itempool.PoolFill;
import dev.golem.core.Ids;
import dev.golem.core.world.DataSource;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * The structure loot table of each item pool, {@code data/hbm/loot_table/chests/<pool>.json}: one roll of the pool's rows
 * with their weights. A vanilla loot pool picks by weight as 1.7's {@code WeightedRandom} does, and {@code set_count}
 * with a uniform count is 1.7's {@code min + rand(max - min + 1)}.
 */
public final class ChestLootSource extends DataSource<JsonObject> {

    private static final String PREFIX = "pool_";

    public ChestLootSource() {
        super("loot_table/chests");
    }

    @Override
    public JsonElement encode(JsonObject row, HolderLookup.Provider registries) {
        return row;
    }

    @Override
    public JsonObject decode(JsonElement json, HolderLookup.Provider registries) {
        return json.getAsJsonObject();
    }

    @Override
    protected void registerDefaults() {
        for (Map.Entry<String, PoolFile> pool : new ItemPoolSource().rows().entrySet()) {
            String id = pool.getKey().startsWith(PREFIX) ? pool.getKey().substring(PREFIX.length()) : pool.getKey();
            register(id, table(pool.getValue()));
        }
    }

    private static JsonObject table(PoolFile pool) {
        JsonArray entries = new JsonArray();
        for (Row row : pool.entries()) {
            if (row.item() != null && !kept(row)) continue;
            entries.add(entry(row));
        }
        JsonObject lootPool = new JsonObject();
        lootPool.addProperty("rolls", 1);
        lootPool.addProperty("bonus_rolls", 0);
        lootPool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(lootPool);
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:chest");
        table.add("pools", pools);
        return table;
    }

    /** False where {@code ItemPool} skips the row: an item this port has not registered, or a fluid it cannot hold. */
    private static boolean kept(Row row) {
        Identifier id = Ids.parse(row.item());
        if (!BuiltInRegistries.ITEM.containsKey(id)) return false;
        return row.fluid() == null || PoolFill.fills(BuiltInRegistries.ITEM.getValue(id), row.fluid());
    }

    /** In {@code ItemPool.Row}'s order: the fluid fill (it may change the item), then the components, then the count. */
    private static JsonObject entry(Row row) {
        JsonObject entry = new JsonObject();
        if (row.item() == null) {
            entry.addProperty("type", "minecraft:empty");
            entry.addProperty("weight", row.weight());
            return entry;
        }
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", row.item());
        entry.addProperty("weight", row.weight());
        JsonArray functions = new JsonArray();
        if (row.fluid() != null) {
            JsonObject fill = new JsonObject();
            fill.addProperty("function", PoolFill.ID);
            fill.addProperty("fluid", row.fluid());
            functions.add(fill);
        }
        if (row.components() != null) {
            JsonObject components = new JsonObject();
            components.addProperty("function", "minecraft:set_components");
            components.add("components", row.components().deepCopy());
            functions.add(components);
        }
        if (row.min() != 1 || row.max() != 1) {
            JsonObject count = new JsonObject();
            count.addProperty("function", "minecraft:set_count");
            if (row.min() == row.max()) {
                count.addProperty("count", row.min());
            } else {
                JsonObject uniform = new JsonObject();
                uniform.addProperty("type", "minecraft:uniform");
                uniform.addProperty("min", row.min());
                uniform.addProperty("max", row.max());
                count.add("count", uniform);
            }
            functions.add(count);
        }
        if (!functions.isEmpty()) entry.add("functions", functions);
        return entry;
    }
}
