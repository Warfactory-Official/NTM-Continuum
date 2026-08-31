package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.ItemWasteStorage;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The four old metadata item registrations consumed by {@code machine_storage_drum}. */
public final class ModStorageDrumItems {

    private ModStorageDrumItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemWasteStorage> nuclear_waste_long = ITEMS.registerItem(
            "nuclear_waste_long", p -> new ItemWasteStorage(p.stacksTo(64), ItemWasteStorage.Family.LONG));
    public static final RegistryObject<ItemWasteStorage> nuclear_waste_long_tiny = ITEMS.registerItem(
            "nuclear_waste_long_tiny", p -> new ItemWasteStorage(p.stacksTo(64), ItemWasteStorage.Family.LONG));
    public static final RegistryObject<ItemWasteStorage> nuclear_waste_short = ITEMS.registerItem(
            "nuclear_waste_short", p -> new ItemWasteStorage(p.stacksTo(64), ItemWasteStorage.Family.SHORT));
    public static final RegistryObject<ItemWasteStorage> nuclear_waste_short_tiny = ITEMS.registerItem(
            "nuclear_waste_short_tiny", p -> new ItemWasteStorage(p.stacksTo(64), ItemWasteStorage.Family.SHORT));
    public static final RegistryObject<ItemWasteStorage> nuclear_waste_long_depleted = ITEMS.registerItem(
            "nuclear_waste_long_depleted", p -> new ItemWasteStorage(p.stacksTo(64), ItemWasteStorage.Family.LONG));
    public static final RegistryObject<ItemWasteStorage> nuclear_waste_long_depleted_tiny = ITEMS.registerItem(
            "nuclear_waste_long_depleted_tiny", p -> new ItemWasteStorage(p.stacksTo(64), ItemWasteStorage.Family.LONG));
    public static final RegistryObject<ItemWasteStorage> nuclear_waste_short_depleted = ITEMS.registerItem(
            "nuclear_waste_short_depleted", p -> new ItemWasteStorage(p.stacksTo(64), ItemWasteStorage.Family.SHORT));
    public static final RegistryObject<ItemWasteStorage> nuclear_waste_short_depleted_tiny = ITEMS.registerItem(
            "nuclear_waste_short_depleted_tiny", p -> new ItemWasteStorage(p.stacksTo(64), ItemWasteStorage.Family.SHORT));

    static {
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(nuclear_waste_long, ItemWasteStorage.LongWasteClass.values().length));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(nuclear_waste_long_tiny, ItemWasteStorage.LongWasteClass.values().length));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(nuclear_waste_short, ItemWasteStorage.ShortWasteClass.values().length));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(nuclear_waste_short_tiny, ItemWasteStorage.ShortWasteClass.values().length));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(nuclear_waste_long_depleted, ItemWasteStorage.LongWasteClass.values().length));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(nuclear_waste_long_depleted_tiny, ItemWasteStorage.LongWasteClass.values().length));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(nuclear_waste_short_depleted, ItemWasteStorage.ShortWasteClass.values().length));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(nuclear_waste_short_depleted_tiny, ItemWasteStorage.ShortWasteClass.values().length));
    }

    private static List<ItemStack> variants(RegistryObject<ItemWasteStorage> item, int count) {
        List<ItemStack> stacks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) stacks.add(ItemWasteStorage.make(item.get(), i));
        return stacks;
    }

    public static ItemStack make(Item item, int variant, int count) {
        ItemStack stack = ItemWasteStorage.make(item, variant);
        stack.setCount(count);
        return stack;
    }

    public static void init() {}
}
