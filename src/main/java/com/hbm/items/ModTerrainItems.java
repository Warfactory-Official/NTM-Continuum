package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/** 1.7 {@code plant_item} ({@code ItemEnumMulti} over {@code EnumPlantType}), flattened to three items in the parts tab. */
public final class ModTerrainItems {

    private ModTerrainItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<Item> plant_item_tobacco = item("plant_item_tobacco");
    public static final RegistryObject<Item> plant_item_rope = item("plant_item_rope");
    public static final RegistryObject<Item> plant_item_mustardwillow = item("plant_item_mustardwillow");

    private static RegistryObject<Item> item(String id) {
        RegistryObject<Item> item = ITEMS.registerSimpleItem(id);
        ModTabs.addTo(HbmTab.MATERIALS, item);
        return item;
    }

    public static void init() {}
}
