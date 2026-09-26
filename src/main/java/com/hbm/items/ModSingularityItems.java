package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.ItemSingularity;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.ItemAntimatterPellet;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/** The five legacy drop capsules needed by the Spark Singularity progression. */
public final class ModSingularityItems {
    private ModSingularityItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** The metadata cell required by the old antimatter-to-singularity production chain. */
    public static final RegistryObject<ItemCell> cell = ITEMS.registerItem("cell", ItemCell::new);
    public static final RegistryObject<ItemAntimatterPellet> pellet_antimatter = ITEMS.registerItem(
            "pellet_antimatter", p -> new ItemAntimatterPellet(ItemAntimatterPellet.properties(p)));

    public static final RegistryObject<ItemSingularity> singularity = register(
            "singularity", ItemSingularity.Kind.SINGULARITY);
    public static final RegistryObject<ItemSingularity> singularity_counter_resonant = register(
            "singularity_counter_resonant", ItemSingularity.Kind.COUNTER_RESONANT);
    public static final RegistryObject<ItemSingularity> singularity_super_heated = register(
            "singularity_super_heated", ItemSingularity.Kind.SUPER_HEATED);
    public static final RegistryObject<ItemSingularity> black_hole = register(
            "black_hole", ItemSingularity.Kind.BLACK_HOLE);
    public static final RegistryObject<ItemSingularity> singularity_spark = register(
            "singularity_spark", ItemSingularity.Kind.SPARK);

    static {
        ModTabs.addTo(HbmTab.CONTROL, cell);
        ModTabs.addTo(HbmTab.CONTROL, pellet_antimatter);
    }

    private static RegistryObject<ItemSingularity> register(String name, ItemSingularity.Kind kind) {
        RegistryObject<ItemSingularity> item = ITEMS.registerItem(name,
                p -> new ItemSingularity(ItemSingularity.properties(p), kind));
        ModTabs.addTo(HbmTab.CONTROL, item);
        return item;
    }

    public static void init() {}
}
