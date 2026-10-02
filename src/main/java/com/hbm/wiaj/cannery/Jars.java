package com.hbm.wiaj.cannery;

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

    public static void initJars() {
        canneries.clear();
        put("hbm:heater_firebox", new CanneryFirebox());
        put("hbm:machine_stirling", new CanneryStirling());
        put("hbm:machine_stirling_steel", new CanneryStirling());
        put("hbm:machine_gascent", new CanneryCentrifuge());
        put("hbm:machine_fensu", new CanneryFEnSU());
        put("hbm:machine_fel", new CannerySILEX());
        put("hbm:machine_silex", new CannerySILEX());
        put("hbm:foundry_channel", new CanneryFoundryChannel());
        put("hbm:machine_crucible", new CanneryCrucible());

        put("hbm:plant_item_mustardwillow", new CanneryWillow());
        put("hbm:plant_flower_cd0", new CanneryWillow());
    }

    private static void put(String id, CanneryBase cannery) {
        Item item = BuiltInRegistries.ITEM.getValue(dev.golem.core.Ids.parse(id));
        if (item != null && item != Items.AIR) canneries.put(item, cannery);
    }

    /** The presentation {@code stack} opens, if any. */
    public static @Nullable CanneryBase get(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (canneries.isEmpty()) initJars();
        return canneries.get(stack.getItem());
    }
}
