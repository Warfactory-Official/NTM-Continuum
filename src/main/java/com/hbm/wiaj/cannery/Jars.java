package com.hbm.wiaj.cannery;

import dev.golem.core.Ids;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;

/**
 * Which item opens which presentation. 1.7 keyed this on {@code ComparableStack}s so that a meta item
 * (the willow leaf, the CD0 willow flower) could have one; those are items of their own in the port, so
 * the item alone is the key.
 */
public class Jars {

    public static HashMap<Item, CanneryBase> canneries = new HashMap<>();

    /** Presentations addons registered, by item id, applied after HBM's on every (re)build. */
    private static final Map<String, CanneryBase> ADDED = new LinkedHashMap<>();
    private static boolean built;

    /**
     * Gives the item {@code itemId} a presentation (an addon's machine, or a replacement for one of HBM's). Client
     * side, at start-up; it survives {@link #initJars} rebuilding the table.
     */
    public static synchronized void register(String itemId, CanneryBase cannery) {
        ADDED.put(itemId, cannery);
        built = false;
    }

    public static synchronized void initJars() {
        built = true;
        canneries.clear();
        put("hbm:heater_firebox", new CanneryFirebox());
        put("hbm:machine_stirling", new CanneryStirling());
        put("hbm:machine_stirling_steel", new CanneryStirling());
        put("hbm:machine_gascent", new CanneryCentrifuge());
        put("hbm:machine_fel", new CannerySILEX());
        put("hbm:machine_silex", new CannerySILEX());
        put("hbm:foundry_channel", new CanneryFoundryChannel());
        put("hbm:machine_crucible", new CanneryCrucible());

        put("hbm:plant_item_mustardwillow", new CanneryWillow());
        put("hbm:plant_flower_cd0", new CanneryWillow());

        ADDED.forEach(Jars::put);
    }

    private static void put(String id, CanneryBase cannery) {
        Item item = BuiltInRegistries.ITEM.getValue(Ids.parse(id));
        if (item != null && item != Items.AIR) canneries.put(item, cannery);
    }

    /** The presentation {@code stack} opens, if any. */
    public static @Nullable CanneryBase get(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (!built) initJars();
        return canneries.get(stack.getItem());
    }
}
