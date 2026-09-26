package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.ItemHot;
import com.hbm.items.special.ItemSteelDusted;
import com.hbm.items.weapon.ItemMissilePart;
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

    /** Old ItemMissile.makeThruster(XENON, 1F, 1.5F, SIZE_10).setHealth(5F). */
    public static final RegistryObject<ItemMissilePart> mp_thruster_10_xenon = ITEMS.registerItem(
            "mp_thruster_10_xenon",
            p -> new ItemMissilePart(p, Fluids.XENON, 1F, 1.5F,
                    ItemMissilePart.PartSize.SIZE_10, 5F),
            p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.MATERIALS, ingot_steel_dusted);
        ModTabs.addTo(HbmTab.MATERIALS, ingot_chainsteel);
        ModTabs.addTo(HbmTab.MISSILES, mp_thruster_10_xenon);
    }

    public static void init() {}
}
