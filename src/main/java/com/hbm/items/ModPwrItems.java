package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatItem;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Map;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPWRPrinter;

/**
 * Feature-local items of the PWR: the fifteen fuel rods, their hot and depleted states, and the printer. 1.7
 * {@code ModItems}: all four on the control tab.
 */
public final class ModPwrItems {
    private ModPwrItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static final Map<EnumPWRFuel, RegistryObject<ItemPWRFuel>> FUEL = new EnumMap<>(EnumPWRFuel.class);
    private static final Map<EnumPWRFuel, RegistryObject<ItemPWRFuelSpent>> HOT = new EnumMap<>(EnumPWRFuel.class);
    private static final Map<EnumPWRFuel, RegistryObject<ItemPWRFuelSpent>> DEPLETED = new EnumMap<>(EnumPWRFuel.class);

    public static final RegistryObject<ItemPWRPrinter> PWR_PRINTER = ITEMS.registerItem("pwr_printer", ItemPWRPrinter::new);

    static {
        for (EnumPWRFuel fuel : EnumPWRFuel.VALUES) {
            RegistryObject<ItemPWRFuel> fresh = ITEMS.registerItem("pwr_fuel_" + fuel.id,
                    p -> new ItemPWRFuel(p, fuel));
            FUEL.put(fuel, fresh);
            ModTabs.addTo(HbmTab.CONTROL, fresh);
        }
        for (EnumPWRFuel fuel : EnumPWRFuel.VALUES) {
            RegistryObject<ItemPWRFuelSpent> hot = ITEMS.registerItem("pwr_fuel_hot_" + fuel.id,
                    p -> new ItemPWRFuelSpent(p, fuel));
            HOT.put(fuel, hot);
            ModTabs.addTo(HbmTab.CONTROL, hot);
        }
        for (EnumPWRFuel fuel : EnumPWRFuel.VALUES) {
            RegistryObject<ItemPWRFuelSpent> depleted = ITEMS.registerItem("pwr_fuel_depleted_" + fuel.id,
                    p -> new ItemPWRFuelSpent(p, fuel));
            DEPLETED.put(fuel, depleted);
            ModTabs.addTo(HbmTab.CONTROL, depleted);
        }
        ModTabs.addTo(HbmTab.CONTROL, PWR_PRINTER);
    }

    public static ItemPWRFuel fuel(EnumPWRFuel fuel) { return FUEL.get(fuel).get(); }
    public static ItemPWRFuelSpent hot(EnumPWRFuel fuel) { return HOT.get(fuel).get(); }
    public static ItemPWRFuelSpent depleted(EnumPWRFuel fuel) { return DEPLETED.get(fuel).get(); }

    /** The fresh-rod constant an item stands for, or null when the item is not a PWR fuel rod. */
    public static EnumPWRFuel fuelOf(Item item) {
        return item instanceof ItemPWRFuel rod ? rod.fuel : null;
    }

    /** A hot or depleted rod: nothing but the constant it was made from. */
    public static class ItemPWRFuelSpent extends CompatItem {
        public final EnumPWRFuel fuel;

        public ItemPWRFuelSpent(Properties properties, EnumPWRFuel fuel) {
            super(properties);
            this.fuel = fuel;
        }
    }

    public static void init() {}
}
