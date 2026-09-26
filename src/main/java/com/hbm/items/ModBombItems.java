package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.ItemCustomLore;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/**
 * Hand-registered bomb crafting components (igniters, cores, explosive lenses) used to arm HBM bombs.
 * Kept separate from the generated {@link ModItems} so codegen never clobbers them. The "used in: ..."
 * tooltips come from {@code item.hbm.<id>.desc.N} lang lines, read by {@link ItemCustomLore}.
 */
public final class ModBombItems {

    private ModBombItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Fat Man / Gadget explosive lens. Keeps the original id {@code gadget_explosive8}. */
    public static final RegistryObject<ItemCustomLore> early_explosive_lenses =
            ITEMS.registerItem("gadget_explosive8", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Fat Man / Mike / Tsar fissile core. */
    public static final RegistryObject<ItemCustomLore> man_core =
            ITEMS.registerItem("man_core", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Fat Man igniter assembly. */
    public static final RegistryObject<ItemCustomLore> man_igniter =
            ITEMS.registerItem("man_igniter", ItemCustomLore::new, p -> p.stacksTo(1));

    /** The Gadget's four lens wiring assembly. */
    public static final RegistryObject<ItemCustomLore> gadget_wireing =
            ITEMS.registerItem("gadget_wireing", ItemCustomLore::new, p -> p.stacksTo(1));

    /** The Gadget's large plutonium core. */
    public static final RegistryObject<ItemCustomLore> gadget_core =
            ITEMS.registerItem("gadget_core", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Little Boy's ordered five-part firing assembly. */
    public static final RegistryObject<ItemCustomLore> boy_shielding =
            ITEMS.registerItem("boy_shielding", ItemCustomLore::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemCustomLore> boy_target =
            ITEMS.registerItem("boy_target", ItemCustomLore::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemCustomLore> boy_bullet =
            ITEMS.registerItem("boy_bullet", ItemCustomLore::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemCustomLore> boy_propellant =
            ITEMS.registerItem("boy_propellant", ItemCustomLore::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemCustomLore> boy_igniter =
            ITEMS.registerItem("boy_igniter", ItemCustomLore::new, p -> p.stacksTo(1));

    /** The conventional explosive lenses used by Ivy Mike/Tsar. */
    public static final RegistryObject<ItemCustomLore> explosive_lenses =
            ITEMS.registerItem("man_explosive8", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Ivy Mike's uranium-coated primary vessel. */
    public static final RegistryObject<ItemCustomLore> mike_core =
            ITEMS.registerItem("mike_core", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Ivy Mike's deuterium secondary vessel. */
    public static final RegistryObject<ItemCustomLore> mike_deut =
            ITEMS.registerItem("mike_deut", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Ivy Mike's deuterium cooling assembly. */
    public static final RegistryObject<ItemCustomLore> mike_cooling_unit =
            ITEMS.registerItem("mike_cooling_unit", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Optional sixth-stage Tsar core. */
    public static final RegistryObject<ItemCustomLore> tsar_core =
            ITEMS.registerItem("tsar_core", ItemCustomLore::new, p -> p.stacksTo(1));

    /** One charge occupies one of N²'s twelve slots. */
    public static final RegistryObject<ItemCustomLore> n2_charge =
            ITEMS.registerItem("n2_charge", ItemCustomLore::new, p -> p.stacksTo(12));

    static {
        ModTabs.addTo(HbmTab.BOMBS, early_explosive_lenses);
        ModTabs.addTo(HbmTab.BOMBS, man_core);
        ModTabs.addTo(HbmTab.BOMBS, man_igniter);
        ModTabs.addTo(HbmTab.BOMBS, gadget_wireing);
        ModTabs.addTo(HbmTab.BOMBS, gadget_core);
        ModTabs.addTo(HbmTab.BOMBS, boy_shielding);
        ModTabs.addTo(HbmTab.BOMBS, boy_target);
        ModTabs.addTo(HbmTab.BOMBS, boy_bullet);
        ModTabs.addTo(HbmTab.BOMBS, boy_propellant);
        ModTabs.addTo(HbmTab.BOMBS, boy_igniter);
        ModTabs.addTo(HbmTab.BOMBS, explosive_lenses);
        ModTabs.addTo(HbmTab.BOMBS, mike_core);
        ModTabs.addTo(HbmTab.BOMBS, mike_deut);
        ModTabs.addTo(HbmTab.BOMBS, mike_cooling_unit);
        ModTabs.addTo(HbmTab.BOMBS, tsar_core);
        ModTabs.addTo(HbmTab.BOMBS, n2_charge);
    }

    public static void init() {}
}
