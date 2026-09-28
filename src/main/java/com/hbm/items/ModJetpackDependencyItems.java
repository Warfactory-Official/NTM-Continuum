package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.ItemHot;
import com.hbm.items.special.ItemSteelDusted;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/** Dedicated registrations for non-plain dependencies of the jetpack progression. */
public final class ModJetpackDependencyItems {

    private ModJetpackDependencyItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Old ItemHotDusted: ten component-backed metadata stages, cooled by the heat countdown. */
    public static final RegistryObject<ItemSteelDusted> ingot_steel_dusted = ITEMS.registerItem(
            "ingot_steel_dusted", ItemSteelDusted::new);

    /** Old ItemHot(100); the effective legacy global scale is preserved by ItemHot. */
    public static final RegistryObject<ItemHot> ingot_chainsteel = ITEMS.registerItem(
            "ingot_chainsteel", p -> new ItemHot(p, 200));

    // mp_thruster_10_xenon moved to ModCustomMissileItems with the rest of the custom missile parts.

    static {
        ModTabs.addTo(HbmTab.MATERIALS, ingot_steel_dusted);
        ModTabs.addTo(HbmTab.MATERIALS, ingot_chainsteel);
    }

    public static void init() {}
}
