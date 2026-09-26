package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.food.ItemCrayon;
import com.hbm.items.machine.ItemChemicalDye;
import com.hbm.items.machine.ItemChemicalDye.EnumChemDye;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatConsumables;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Flattened hand-owned registrations for the old chemical dye and crayon metadata families. */
public final class ModChemicalDyeItems {

    private ModChemicalDyeItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Map<EnumChemDye, RegistryObject<ItemChemicalDye>> CHEMICAL =
            new EnumMap<>(EnumChemDye.class);
    private static final Map<EnumChemDye, RegistryObject<ItemCrayon>> CRAYON =
            new EnumMap<>(EnumChemDye.class);

    static {
        for (EnumChemDye dye : EnumChemDye.VALUES) {
            String suffix = dye.name().toLowerCase(Locale.ROOT);
            RegistryObject<ItemChemicalDye> chemical = ITEMS.registerItem(
                    "chemical_dye_" + suffix, p -> new ItemChemicalDye(p, dye));
            RegistryObject<ItemCrayon> crayon = ITEMS.registerItem(
                    "crayon_" + suffix, p -> new ItemCrayon(p, dye),
                    p -> CompatConsumables.foodAlwaysEdible(p, 3, 0.6F, List.of()));
            CHEMICAL.put(dye, chemical);
            CRAYON.put(dye, crayon);
            ModTabs.addTo(HbmTab.MATERIALS, chemical);
            ModTabs.addTo(HbmTab.MATERIALS, crayon);
        }
    }

    public static ItemChemicalDye chemicalDye(EnumChemDye dye) {
        return CHEMICAL.get(dye).get();
    }

    public static ItemCrayon crayon(EnumChemDye dye) {
        return CRAYON.get(dye).get();
    }

    public static ItemStack chemicalDyeStack(EnumChemDye dye, int count) {
        return chemicalDye(dye).stack(count);
    }

    public static int registeredCount() {
        return CHEMICAL.size() + CRAYON.size();
    }

    public static void init() {}
}
