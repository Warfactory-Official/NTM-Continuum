package com.hbm.itempool;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.JsonOps;
import dev.golem.core.Ids;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The 1.7 weighted item pools (supply crates, Bobmazon, satellites, meteor safes), copied from ntm-next.
 * ntm-next bakes the 1.7 tables to {@code /hbm/item_pools.json} at datagen; the baked file ships here
 * as a plain resource.
 *
 * <p>Two deliberate deviations from ntm-next: an entry naming an item this port has not registered
 * yet is dropped with a debug line rather than failing the whole load (ntm-next's registry is a
 * superset of this one), and a template's components ride through Golem's cross-era stack codec, so
 * on the pre-component nodes the entry degrades to the bare item instead of not decoding.
 */
public class ItemPool {

    public static final String RESOURCE = "/hbm/item_pools.json";

    public static final Map<String, ItemPool> POOLS = new HashMap<>();

    private static final List<Entry> BACKUP_POOL = List.of(
            Entry.of(Items.BREAD, 1, 3, 10),
            Entry.of(Items.STICK, 2, 5, 10)
    );
    public final String name;
    public List<Entry> pool = List.of();

    public ItemPool(String name) {
        this.name = name;
        POOLS.put(name, this);
    }

    public static void load(HolderLookup.Provider registries) {
        POOLS.clear();
        RegistryOps<JsonElement> ops = net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE, registries);
        JsonObject root = root();
        if (root == null) return;
        for (Map.Entry<String, JsonElement> pool : root.entrySet()) {
            new ItemPool(pool.getKey()).pool = body(pool.getValue(), ops);
        }
    }

    /** One baked pool, bypassing {@link #POOLS}: a remote client never runs {@link #load}. */
    public static List<Entry> read(HolderLookup.Provider registries, String name) {
        JsonObject root = root();
        JsonElement pool = root == null ? null : root.get(name);
        if (pool == null) throw new IllegalArgumentException(RESOURCE + " holds no pool " + name);
        return body(pool, net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE, registries));
    }

    private static @Nullable JsonObject root() {
        try (InputStream in = ItemPool.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                Constants.LOG.warn("missing the baked pools at {} - every draw answers the backup pool", RESOURCE);
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException("reading " + RESOURCE, e);
        }
    }

    private static List<Entry> body(JsonElement pool, RegistryOps<JsonElement> ops) {
        List<Entry> body = new ArrayList<>();
        for (JsonElement element : pool.getAsJsonArray()) {
            Entry entry = entry(element.getAsJsonObject(), ops);
            if (entry != null) body.add(entry);
        }
        return List.copyOf(body);
    }

    private static @Nullable Entry entry(JsonObject row, RegistryOps<JsonElement> ops) {
        int min = row.get("min").getAsInt();
        int max = row.get("max").getAsInt();
        int weight = row.get("weight").getAsInt();
        if (!row.has("item")) return Entry.empty(min, max, weight);
        Identifier id = Ids.parse(row.get("item").getAsString());
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            Constants.LOG.debug("{} names {}, which this port has not registered; entry skipped", RESOURCE, id);
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        ItemStack template = new ItemStack(item);
        if (row.has("fluid")) {
            // 1.7 metadata fluid containers (canister_full meta = fluid id, cell_deuterium ...): the port keeps the
            // fluid in components, which only the item itself knows how to write (fill, cell variant, tint).
            template = filled(template, Fluids.fromName(row.get("fluid").getAsString()));
            if (template == null) {
                Constants.LOG.warn("{} fills {} with {}, which it cannot hold; entry skipped", RESOURCE, id, row.get("fluid"));
                return null;
            }
        }
        if (row.has("components")) {
            ItemStack base = template;
            template = ItemStacks.withComponents(template, row.getAsJsonObject("components"), ops).orElseGet(() -> {
                Constants.LOG.warn("{}: the components of {} do not decode; using the bare item", RESOURCE, id);
                return base;
            });
        }
        return new Entry(template, min, max, weight);
    }

    private static @Nullable ItemStack filled(ItemStack stack, com.hbm.inventory.fluid.@Nullable FluidType fluid) {
        if (fluid == null || fluid == Fluids.NONE) return null;
        if (stack.getItem() instanceof com.hbm.items.special.ItemCell) return com.hbm.items.special.ItemCell.getFullCell(fluid);
        if (stack.getItem() instanceof com.hbm.items.machine.ItemFluidTank tank) return tank.make(fluid, tank.capacity);
        return null;
    }

    /**
     * Grabs the named pool, or the backup pool if it was never registered.
     */
    public static List<Entry> getPool(String name) {
        ItemPool p = POOLS.get(name);
        return p == null ? BACKUP_POOL : p.pool;
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
