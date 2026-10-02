package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.tool.ItemWand;
import com.hbm.items.tool.ItemWandD;
import com.hbm.items.tool.ItemWandS;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/**
 * 1.7's three creative wands, declared as {@code ModItems} declares them: {@code setMaxStackSize(1)},
 * {@code consumableTab}, {@code setFull3D} (handheld models). The construction wand's
 * field is {@code wand} but its registry name is {@code wand_k}.
 */
public final class ModWandItems {

    private ModWandItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemWand> wand =
            REG.item("wand_k", ItemWand::new, p -> p.stacksTo(1))
                    .transform(Presets.handheld("hbm:items/wand"))
                    .tab(HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemWandS> wand_s =
            REG.item("wand_s", ItemWandS::new, p -> p.stacksTo(1))
                    .transform(Presets.handheld())
                    .tab(HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemWandD> wand_d =
            REG.item("wand_d", ItemWandD::new, p -> p.stacksTo(1))
                    .transform(Presets.handheld())
                    .tab(HbmTab.CONSUMABLES);

    public static void init() {}
}
