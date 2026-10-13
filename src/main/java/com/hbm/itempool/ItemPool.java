package com.hbm.itempool;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.special.ItemCell;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.data.DataFiles;
import dev.golem.core.world.ItemStacks;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The 1.7 weighted item pools (supply crates, Bobmazon, satellites, meteor safes), copied from ntm-next, as a datapack
 * folder: {@code data/<namespace>/item_pool/<pool>.json}, {@code {"replace": false, "entries": [{"item", "min", "max",
 * "weight", "fluid"?, "components"?}, ...]}}. The pool's name is the file name upper-cased ({@code pool_ammo.json} is
 * {@code POOL_AMMO}, the names code and saves use), in any namespace: an addon or a datapack adds rows to one of HBM's
 * pools with a file of the same name in its own namespace (HBM's rows first, then other namespaces in id order, each
 * pack's copy lowest priority first), or replaces the pool with {@code "replace": true}; a new name is a new pool.
 * Golem Core {@code DataFiles} reads the folder on the server and sends it to clients (JEI's satellite page reads the
 * same pools). ntm-next baked the 1.7 tables at datagen; the baked file was split into these files once.
 *
 * <p>Two deliberate deviations from ntm-next: an entry naming an item this port has not registered
 * yet is dropped with a debug line rather than failing the whole load (ntm-next's registry is a
 * superset of this one), and a template's components ride through Golem's cross-era stack codec, so
 * on the pre-component nodes the entry degrades to the bare item instead of not decoding.
 */
public class ItemPool {

    /** The datapack folder. */
    public static final String FOLDER = "item_pool";

    public static final Map<String, ItemPool> POOLS = new HashMap<>();

    /** In a holder: the class is loaded at mod construction ({@link #declare}), before stacks can be built. */
    private static final class Backup {
        static final List<Entry> POOL = List.of(
                Entry.of(Items.BREAD, 1, 3, 10),
                Entry.of(Items.STICK, 2, 5, 10)
        );
    }

    /** The merged rows of every pool, by pool name, as the datapacks gave them. */
    private static volatile Map<String, JsonArray> raw = Map.of();
    /** The registries the server decodes item components with; null until a server started. */
    private static HolderLookup.@Nullable Provider decodeWith;
    private static boolean decoded;

    public final String name;
    public List<Entry> pool = List.of();

    public ItemPool(String name) {
        this.name = name;
        POOLS.put(name, this);
    }

    /** Declares the datapack folder at mod construction. */
    public static void declare() {
        DataFiles.stackedFolder(FOLDER, ItemPool::apply);
    }

    private static void apply(Map<String, List<JsonElement>> stacks) {
        List<String> ids = new ArrayList<>(stacks.keySet());
        // HBM's own rows first (1.7's order), then every other namespace in id order
        ids.sort(Comparator.comparing((String id) -> !id.startsWith(Constants.MOD_ID + ":")).thenComparing(id -> id));
        Map<String, JsonArray> merged = new LinkedHashMap<>();
        for (String id : ids) {
            String pool = id.substring(id.indexOf(':') + 1).toUpperCase(Locale.ROOT);
            for (JsonElement file : stacks.get(id)) {
                if (!file.isJsonObject()) continue;
                PoolFile parsed = PoolFile.read(file.getAsJsonObject());
                if (parsed.replace()) merged.remove(pool);
                JsonArray rows = merged.computeIfAbsent(pool, p -> new JsonArray());
                for (Row row : parsed.entries()) rows.add(row.write());
            }
        }
        synchronized (ItemPool.class) {
            raw = Collections.unmodifiableMap(merged);
            decoded = false;
        }
    }

    /** Decodes the pools with the server's registries (server start, {@code /ntmreload}). */
    public static synchronized void load(HolderLookup.Provider registries) {
        decodeWith = registries;
        decoded = false;
        decode();
    }

    private static synchronized void decode() {
        if (decoded || decodeWith == null) return;
        POOLS.clear();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, decodeWith);
        for (Map.Entry<String, JsonArray> pool : raw.entrySet()) {
            new ItemPool(pool.getKey()).pool = body(pool.getValue(), ops);
        }
        decoded = true;
    }

    /** One pool decoded with {@code registries}, bypassing {@link #POOLS}: a remote client never runs {@link #load}. */
    public static List<Entry> read(HolderLookup.Provider registries, String name) {
        JsonArray pool = raw.get(name);
        if (pool == null) throw new IllegalArgumentException("no item pool " + name + " in data/*/" + FOLDER);
        return body(pool, RegistryOps.create(JsonOps.INSTANCE, registries));
    }

    /** Every pool's merged rows as loaded, by pool name. */
    public static Map<String, JsonArray> rawPools() {
        return raw;
    }

    private static List<Entry> body(JsonElement pool, RegistryOps<JsonElement> ops) {
        List<Entry> body = new ArrayList<>();
        for (JsonElement element : pool.getAsJsonArray()) {
            Entry entry = entry(element.getAsJsonObject(), ops);
            if (entry != null) body.add(entry);
        }
        return List.copyOf(body);
    }

    private static @Nullable Entry entry(JsonObject json, RegistryOps<JsonElement> ops) {
        Row row = Row.read(json);
        int min = row.min();
        int max = row.max();
        int weight = row.weight();
        if (row.item() == null) return Entry.empty(min, max, weight);
        Identifier id = Ids.parse(row.item());
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            Constants.LOG.debug("{} names {}, which this port has not registered; entry skipped", FOLDER, id);
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        ItemStack template = new ItemStack(item);
        if (row.fluid() != null) {
            // 1.7 metadata fluid containers (canister_full meta = fluid id, cell_deuterium ...): the port keeps the
            // fluid in components, which only the item itself knows how to write (fill, cell variant, tint).
            template = filled(template, Fluids.fromName(row.fluid()));
            if (template == null) {
                Constants.LOG.warn("{} fills {} with {}, which it cannot hold; entry skipped", FOLDER, id, row.fluid());
                return null;
            }
        }
        if (row.components() != null) {
            ItemStack base = template;
            template = ItemStacks.withComponents(template, row.components(), ops).orElseGet(() -> {
                Constants.LOG.warn("{}: the components of {} do not decode; using the bare item", FOLDER, id);
                return base;
            });
        }
        return new Entry(template, min, max, weight);
    }

    private static @Nullable ItemStack filled(ItemStack stack, com.hbm.inventory.fluid.@Nullable FluidType fluid) {
        if (fluid == null || fluid == Fluids.NONE) return null;
        if (stack.getItem() instanceof ItemCell) return ItemCell.getFullCell(fluid);
        if (stack.getItem() instanceof ItemFluidTank tank) return tank.make(fluid, tank.capacity);
        return null;
    }

    /**
     * Grabs the named pool, or the backup pool if it was never registered.
     */
    public static List<Entry> getPool(String name) {
        ItemPool p;
        synchronized (ItemPool.class) {
            decode();
            p = POOLS.get(name);
        }
        return p == null ? Backup.POOL : p.pool;
    }

    public static ItemStack getStack(String name, RandomSource rand) {
        return getStack(getPool(name), rand);
    }

    public static ItemStack getStack(List<Entry> pool, RandomSource rand) {
        Entry entry = select(pool, rand);
        return entry == null ? ItemStack.EMPTY : entry.roll(rand);
    }

    public static ItemStack getUnit(String name, RandomSource rand) {
        // 1.7: BlockKeyhole copies the selected template without ItemPool.getStack's count draw.
        Entry entry = select(getPool(name), rand);
        return entry == null || entry.isEmpty() ? ItemStack.EMPTY : ItemStacks.copyWithCount(entry.template(), 1);
    }

    private static @Nullable Entry select(List<Entry> pool, RandomSource rand) {
        int total = 0;
        for (Entry e : pool) total += e.weight();
        if (total <= 0) return null;

        int roll = rand.nextInt(total);
        int acc = 0;
        for (Entry e : pool) {
            acc += e.weight();
            if (roll < acc) return e;
        }
        throw new AssertionError();
    }

    /**
     * One pool file as written: {@code {"replace"?, "entries": [...]}}. {@link #read} is what {@link #apply} parses and
     * {@link #write} its exact inverse ({@code replace} is written only when true, the reader's default being false).
     */
    public record PoolFile(boolean replace, List<Row> entries) {

        public static PoolFile read(JsonObject file) {
            boolean replace = file.has("replace") && file.get("replace").getAsBoolean();
            List<Row> entries = new ArrayList<>();
            if (file.has("entries")) {
                for (JsonElement row : file.getAsJsonArray("entries")) entries.add(Row.read(row.getAsJsonObject()));
            }
            return new PoolFile(replace, List.copyOf(entries));
        }

        public JsonObject write() {
            JsonObject file = new JsonObject();
            if (replace) file.addProperty("replace", true);
            JsonArray rows = new JsonArray();
            for (Row row : entries) rows.add(row.write());
            file.add("entries", rows);
            return file;
        }
    }

    /**
     * One row of a pool file as written: {@code {"item"?, "min", "max", "weight", "fluid"?, "components"?}}. A missing
     * item is the EMPTY marker; {@code fluid} is a {@code Fluids} name the item is filled with; {@code components} is
     * decoded with the server's registries. {@link #read} and {@link #write} are each other's inverse.
     */
    public record Row(@Nullable String item, int min, int max, int weight, @Nullable String fluid,
                      @Nullable JsonObject components) {

        public static Row read(JsonObject row) {
            return new Row(
                    row.has("item") ? row.get("item").getAsString() : null,
                    row.get("min").getAsInt(),
                    row.get("max").getAsInt(),
                    row.get("weight").getAsInt(),
                    row.has("fluid") ? row.get("fluid").getAsString() : null,
                    row.has("components") ? row.getAsJsonObject("components") : null);
        }

        public JsonObject write() {
            JsonObject row = new JsonObject();
            if (item != null) row.addProperty("item", item);
            row.addProperty("min", min);
            row.addProperty("max", max);
            row.addProperty("weight", weight);
            if (fluid != null) row.addProperty("fluid", fluid);
            if (components != null) row.add("components", components.deepCopy());
            return row;
        }
    }

    /**
     * One weighted entry: a template stack (1.7 packed subtypes into metadata), rolled count range,
     * selection weight; a null template is the EMPTY marker keeping an unported line's weight.
     */
    public record Entry(@Nullable ItemStack template, int min, int max, int weight) {

        /** An entry for an item that carries no 1.7 metadata subtype. */
        public static Entry of(Item item, int min, int max, int weight) {
            return new Entry(new ItemStack(item), min, max, weight);
        }

        /** The EMPTY marker for a 1.7 entry whose item is unported - keeps 1.7's weight and counts. */
        public static Entry empty(int min, int max, int weight) {
            return new Entry(null, min, max, weight);
        }

        public boolean isEmpty() {
            return template == null;
        }

        public ItemStack roll(RandomSource rand) {
            // 1.7: skipping desyncs the seed.
            int count = min + rand.nextInt(max - min + 1);
            return template == null ? ItemStack.EMPTY : ItemStacks.copyWithCount(template, count);
        }
    }
}
