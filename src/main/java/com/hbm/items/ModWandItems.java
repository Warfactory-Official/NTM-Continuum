package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemWand;
import com.hbm.items.tool.ItemWandD;
import com.hbm.items.tool.ItemWandS;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/**
 * 1.7's three creative wands, declared as {@code ModItems} declares them: {@code setMaxStackSize(1)},
 * {@code consumableTab}, {@code setFull3D} (handheld models, in {@link WandDeclarations}). The construction wand's
 * field is {@code wand} but its registry name is {@code wand_k}.
 */
public final class ModWandItems {

    private ModWandItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemWand> wand =
            ITEMS.registerItem("wand_k", ItemWand::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemWandS> wand_s =
            ITEMS.registerItem("wand_s", ItemWandS::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemWandD> wand_d =
            ITEMS.registerItem("wand_d", ItemWandD::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, wand);
        ModTabs.addTo(HbmTab.CONSUMABLES, wand_s);
        ModTabs.addTo(HbmTab.CONSUMABLES, wand_d);
    }

    public static void init() {}
}
