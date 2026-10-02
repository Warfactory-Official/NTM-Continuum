package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemZirnoxWaste;
import com.hbm.items.machine.ZirnoxFuel;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Hand-registered functional ZIRNOX fuel and spent-fuel items. */
public final class ModZirnoxItems {

    private ModZirnoxItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);
    private static final Map<ZirnoxFuel, RegistryObject<ItemZirnoxRod>> ACTIVE = new EnumMap<>(ZirnoxFuel.class);
    private static final Map<ZirnoxFuel, RegistryObject<Item>> DEPLETED = new EnumMap<>(ZirnoxFuel.class);
    private static final Map<ZirnoxFuel, RegistryObject<ItemZirnoxWaste>> WASTE = new EnumMap<>(ZirnoxFuel.class);

    static {
        for (ZirnoxFuel fuel : ZirnoxFuel.values()) {
            RegistryObject<ItemZirnoxRod> active = REG.item("rod_zirnox_" + fuel.id,
                    (Item.Properties p) -> new ItemZirnoxRod(p.stacksTo(1), fuel))
                    .transform(Presets.sprite())
                    .tab(HbmTab.CONTROL);
            ACTIVE.put(fuel, active);

            if (fuel.wasteId != null) {
                RegistryObject<Item> depleted = REG.simpleItem("rod_zirnox_depleted_" + fuel.id,
                        (Item.Properties p) -> p.craftRemainder(ModItems.rod_zirnox_empty.get()))
                        .transform(Presets.sprite())
                        .tab(HbmTab.CONTROL);
                DEPLETED.put(fuel, depleted);

                // every spent-fuel waste shows the one zirnox_waste model
                RegistryObject<ItemZirnoxWaste> waste = REG.item("waste_" + fuel.wasteId,
                        ItemZirnoxWaste::new)
                        .transform(Presets.shares("hbm:item/zirnox_waste"))
                        .tab(HbmTab.CONSUMABLES);
                WASTE.put(fuel, waste);
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
