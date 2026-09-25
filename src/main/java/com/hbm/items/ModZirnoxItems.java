package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemZirnoxWaste;
import com.hbm.items.machine.ZirnoxFuel;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Hand-registered functional ZIRNOX fuel and spent-fuel items. */
public final class ModZirnoxItems {

    private ModZirnoxItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Map<ZirnoxFuel, RegistryObject<ItemZirnoxRod>> ACTIVE = new IdentityHashMap<>();
    private static final Map<ZirnoxFuel, RegistryObject<Item>> DEPLETED = new IdentityHashMap<>();
    private static final Map<ZirnoxFuel, RegistryObject<ItemZirnoxWaste>> WASTE = new IdentityHashMap<>();
    private static final Map<ZirnoxFuel, Supplier<? extends Item>> PRODUCT = new IdentityHashMap<>();

    static {
        for (ZirnoxFuel fuel : ZirnoxFuel.values()) register(ITEMS, fuel);
        product(ZirnoxFuel.TH232, () -> active(ZirnoxFuel.THORIUM));
        product(ZirnoxFuel.LITHIUM, () -> ModItems.rod_zirnox_tritium.get());
    }

    public static void register(DeferredRegister.Items register, ZirnoxFuel fuel) {
        RegistryObject<ItemZirnoxRod> active = register.registerItem("rod_zirnox_" + fuel.id,
                p -> new ItemZirnoxRod(p.stacksTo(1), fuel));
        ACTIVE.put(fuel, active);
        ModTabs.addTo(HbmTab.CONTROL, active);

        if (fuel.wasteId != null) {
            RegistryObject<Item> depleted = register.registerSimpleItem("rod_zirnox_depleted_" + fuel.id,
                    p -> p.craftRemainder(ModItems.rod_zirnox_empty.get()));
            DEPLETED.put(fuel, depleted);
            ModTabs.addTo(HbmTab.CONTROL, depleted);

            RegistryObject<ItemZirnoxWaste> waste = register.registerItem("waste_" + fuel.wasteId,
                    ItemZirnoxWaste::new);
            WASTE.put(fuel, waste);
            ModTabs.addTo(HbmTab.CONSUMABLES, waste);
        }
    }

    public static void product(ZirnoxFuel fuel, Supplier<? extends Item> item) {
        PRODUCT.put(fuel, item);
    }

    public static Item spent(ZirnoxFuel fuel) {
        Supplier<? extends Item> product = PRODUCT.get(fuel);
        return product != null ? product.get() : depleted(fuel);
    }

    public static ItemZirnoxRod active(ZirnoxFuel fuel) { return ACTIVE.get(fuel).get(); }
    public static Item depleted(ZirnoxFuel fuel) { return DEPLETED.get(fuel).get(); }
    public static ItemZirnoxWaste waste(ZirnoxFuel fuel) { return WASTE.get(fuel).get(); }
    public static boolean hasDepleted(ZirnoxFuel fuel) { return DEPLETED.containsKey(fuel); }
    public static ItemStack hotWaste(ZirnoxFuel fuel, int count) { return ItemZirnoxWaste.makeHot(waste(fuel), count); }

    public static void init() {}
}
