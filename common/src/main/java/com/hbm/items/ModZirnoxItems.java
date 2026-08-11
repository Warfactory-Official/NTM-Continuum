package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemZirnoxWaste;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Hand-registered functional ZIRNOX fuel and spent-fuel items. */
public final class ModZirnoxItems {

    private ModZirnoxItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Map<ZirnoxFuel, RegistryObject<ItemZirnoxRod>> ACTIVE = new EnumMap<>(ZirnoxFuel.class);
    private static final Map<ZirnoxFuel, RegistryObject<Item>> DEPLETED = new EnumMap<>(ZirnoxFuel.class);
    private static final Map<ZirnoxFuel, RegistryObject<ItemZirnoxWaste>> WASTE = new EnumMap<>(ZirnoxFuel.class);

    static {
        for (ZirnoxFuel fuel : ZirnoxFuel.values()) {
            RegistryObject<ItemZirnoxRod> active = ITEMS.registerItem("rod_zirnox_" + fuel.id,
                    p -> new ItemZirnoxRod(p.stacksTo(1), fuel));
            ACTIVE.put(fuel, active);
            ModTabs.addTo(HbmTab.CONTROL, active);

            if (fuel.wasteId != null) {
                RegistryObject<Item> depleted = ITEMS.registerSimpleItem("rod_zirnox_depleted_" + fuel.id,
                        p -> p.craftRemainder(ModItems.rod_zirnox_empty.get()));
                DEPLETED.put(fuel, depleted);
                ModTabs.addTo(HbmTab.CONTROL, depleted);

                RegistryObject<ItemZirnoxWaste> waste = ITEMS.registerItem("waste_" + fuel.wasteId,
                        ItemZirnoxWaste::new);
                WASTE.put(fuel, waste);
                ModTabs.addTo(HbmTab.CONSUMABLES, waste);
            }
        }
    }

    public static ItemZirnoxRod active(ZirnoxFuel fuel) { return ACTIVE.get(fuel).get(); }
    public static Item depleted(ZirnoxFuel fuel) { return DEPLETED.get(fuel).get(); }
    public static ItemZirnoxWaste waste(ZirnoxFuel fuel) { return WASTE.get(fuel).get(); }
    public static boolean hasDepleted(ZirnoxFuel fuel) { return DEPLETED.containsKey(fuel); }
    public static ItemStack hotWaste(ZirnoxFuel fuel, int count) { return ItemZirnoxWaste.makeHot(waste(fuel), count); }

    public static void init() {}
}
