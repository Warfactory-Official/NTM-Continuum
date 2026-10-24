package com.hbm.recipes.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.core.world.DataSource;
import net.minecraft.core.HolderLookup;

/**
 * The death drops of HBM's mobs that scale with looting, {@code data/hbm/loot_table/entities/<mob>.json}, each the
 * mob's 1.7 {@code dropFewItems}. {@code rand(n)} is a uniform count {@code 0..n-1}; 1.7's looting bonus
 * {@code rand(looting + 1)} is a second, added uniform count up to {@code golemcore:looting}.
 */
public final class EntityLootSource extends DataSource<JsonObject> {

    public EntityLootSource() {
        super("loot_table/entities");
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
        // 1.7 EntityCreeper: a player kill drops 5 + rand(6 + 2 looting) gold crystals, any other death 3
        creeper("entity_mob_gold_creeper",
                pool(item("hbm:crystal_gold", setCount(uniform(constant(5), looting(2, 10, false)))), killedByPlayer()),
                pool(item("hbm:crystal_gold", setCount(constant(3))), not(killedByPlayer())));
        creeper("entity_mob_nuclear_creeper",
                pool(item("minecraft:tnt", setCount(uniform(constant(0), constant(2))), addLooting(false))),
                pool(item("hbm:coin_creeper"), randomChance(1F / 3F)));
        creeper("entity_mob_phosgene_creeper",
                pool(item("minecraft:gunpowder", setCount(uniform(constant(0), constant(2))), addLooting(false))));
        creeper("entity_mob_tainted_creeper",
                pool(item("minecraft:tnt", setCount(uniform(constant(0), constant(2))), addLooting(false))));
        creeper("entity_mob_volatile_creeper",
                pool(item("hbm:sulfur", setCount(uniform(constant(2), constant(4))))),
                pool(item("hbm:stick_tnt", setCount(uniform(constant(1), constant(2))))));

        register("entity_fucc_a_ducc", table(
                feathers(),
                pool(alternatives(
                        item("minecraft:cooked_chicken"), onFire(),
                        item("minecraft:chicken")))));
        // the chicken stays in EntityPigeon: a fat pigeon drops three, and fatness is not saved where loot can see it
        register("entity_pigeon", table(feathers()));
        // 1.7 EntityRADBeast: a player kill drops one polonium nugget per looting level; the rods stay in the class
        register("entity_radiation_blaze", table(
                pool(item("hbm:nugget_polonium", setCount(looting(1, 0, false))), killedByPlayer())));

        // 1.7 EntityLivingBase.onDeath reads looting off a player killer only; a rare drop is nextInt(200) - looting < 5
        register("entity_taint_crab", table(
                pool(item("hbm:coil_copper", setCount(uniform(constant(0), constant(2))), addLooting(true))),
                pool(item("hbm:coil_magnetized_tungsten"), killedByPlayer(), rareDrop())));
        register("entity_tesla_crab", table(
                pool(item("hbm:coil_copper"), killedByPlayer(), rareDrop())));

        // 1.7 EntityGlyphid: half the time (int) scale * 2 + looting meat, grilled when the glyphid burns
        glyphid("entity_glyphid", 1.0);
        glyphid("entity_glyphid_scout", 0.75);
        glyphid("entity_glyphid_brawler", 1.25);
        glyphid("entity_glyphid_digger", 1.3);
        glyphid("entity_glyphid_bombardier", 1.0);
        glyphid("entity_glyphid_blaster", 1.25);
        glyphid("entity_glyphid_behemoth", 1.5);
        glyphid("entity_glyphid_brenda", 2.0);
        glyphid("entity_glyphid_nuclear", 2.0);
    }

    /** Every 1.7 creeper also drops a music disc when a skeleton kills it ({@code EntityCreeper.onDeath}). */
    private void creeper(String id, JsonObject... pools) {
        JsonObject disc = new JsonObject();
        disc.addProperty("type", "minecraft:tag");
        disc.addProperty("name", "minecraft:creeper_drop_music_discs");
        disc.addProperty("expand", true);
        JsonObject skeleton = new JsonObject();
        skeleton.addProperty("type", "#minecraft:skeletons");
        JsonObject killer = new JsonObject();
        killer.addProperty("condition", "minecraft:entity_properties");
        killer.addProperty("entity", "attacker");
        killer.add("predicate", skeleton);
        JsonObject[] all = new JsonObject[pools.length + 1];
        System.arraycopy(pools, 0, all, 0, pools.length);
        all[pools.length] = pool(disc, killer);
        register(id, table(all));
    }

    private void glyphid(String id, double scale) {
        JsonObject count = setCount(looting(1, (int) scale * 2, false));
        register(id, table(pool(alternatives(
                item("hbm:glyphid_meat_grilled", count.deepCopy()), onFire(),
                item("hbm:glyphid_meat", count)), randomChance(0.5F))));
    }

    /** {@code rand(3) + rand(1 + looting)} feathers (EntityDuck, EntityPigeon). */
    private static JsonObject feathers() {
        return pool(item("minecraft:feather", setCount(uniform(constant(0), constant(2))), addLooting(false)));
    }

    private static JsonObject table(JsonObject... pools) {
        JsonArray list = new JsonArray();
        for (JsonObject pool : pools) list.add(pool);
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:entity");
        table.add("pools", list);
        return table;
    }

    private static JsonObject pool(JsonObject entry, JsonObject... conditions) {
        JsonArray entries = new JsonArray();
        entries.add(entry);
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        pool.add("entries", entries);
        if (conditions.length > 0) pool.add("conditions", array(conditions));
        return pool;
    }

    private static JsonObject item(String name, JsonObject... functions) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", name);
        if (functions.length > 0) entry.add("functions", array(functions));
        return entry;
    }

    /** The first child when {@code condition} holds, else the second. */
    private static JsonObject alternatives(JsonObject first, JsonObject condition, JsonObject second) {
        first.add("conditions", array(condition));
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:alternatives");
        entry.add("children", array(first, second));
        return entry;
    }

    private static JsonObject setCount(JsonObject count) {
        JsonObject function = new JsonObject();
        function.addProperty("function", "minecraft:set_count");
        function.add("count", count);
        return function;
    }

    /** 1.7's {@code rand(looting + 1)}, added to the count. */
    private static JsonObject addLooting(boolean playerOnly) {
        JsonObject function = setCount(uniform(constant(0), looting(1, 0, playerOnly)));
        function.addProperty("add", true);
        return function;
    }

    private static JsonObject constant(int value) {
        JsonObject number = new JsonObject();
        number.addProperty("type", "minecraft:constant");
        number.addProperty("value", value);
        return number;
    }

    private static JsonObject uniform(JsonObject min, JsonObject max) {
        JsonObject number = new JsonObject();
        number.addProperty("type", "minecraft:uniform");
        number.add("min", min);
        number.add("max", max);
        return number;
    }

    private static JsonObject looting(int factor, int add, boolean playerOnly) {
        JsonObject number = new JsonObject();
        number.addProperty("type", "golemcore:looting");
        if (factor != 1) number.addProperty("factor", factor);
        if (add != 0) number.addProperty("add", add);
        if (playerOnly) number.addProperty("player_only", true);
        return number;
    }

    private static JsonObject killedByPlayer() {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:killed_by_player");
        return condition;
    }

    private static JsonObject not(JsonObject term) {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:inverted");
        condition.add("term", term);
        return condition;
    }

    private static JsonObject onFire() {
        JsonObject flags = new JsonObject();
        flags.addProperty("is_on_fire", true);
        JsonObject predicate = new JsonObject();
        predicate.add("flags", flags);
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:entity_properties");
        condition.addProperty("entity", "this");
        condition.add("predicate", predicate);
        return condition;
    }

    /** 1.7 {@code rand.nextInt(n) == 0} as {@code random_chance}: within a float's rounding of {@code 1 / n}. */
    private static JsonObject randomChance(float chance) {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:random_chance");
        condition.addProperty("chance", chance);
        return condition;
    }

    /** {@code nextInt(200) - looting < 5}: a uniform 0..199 at most {@code 4 + looting}, looting from a player killer. */
    private static JsonObject rareDrop() {
        JsonObject range = new JsonObject();
        range.add("max", looting(1, 4, true));
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:value_check");
        condition.add("value", uniform(constant(0), constant(199)));
        condition.add("range", range);
        return condition;
    }

    private static JsonArray array(JsonObject... values) {
        JsonArray array = new JsonArray();
        for (JsonObject value : values) array.add(value);
        return array;
    }
}
