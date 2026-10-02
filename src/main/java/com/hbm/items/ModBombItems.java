package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
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
    private static final Reg REG = Reg.of(ITEMS);

    /** Fat Man / Gadget explosive lens. Keeps the original id {@code gadget_explosive8}. */
    public static final RegistryObject<ItemCustomLore> early_explosive_lenses =
            REG.item("gadget_explosive8", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** Fat Man / Mike / Tsar fissile core. */
    public static final RegistryObject<ItemCustomLore> man_core =
            REG.item("man_core", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** Fat Man igniter assembly. */
    public static final RegistryObject<ItemCustomLore> man_igniter =
            REG.item("man_igniter", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** The Gadget's four lens wiring assembly. */
    public static final RegistryObject<ItemCustomLore> gadget_wireing =
            REG.item("gadget_wireing", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** The Gadget's large plutonium core. */
    public static final RegistryObject<ItemCustomLore> gadget_core =
            REG.item("gadget_core", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** Little Boy's ordered five-part firing assembly. */
    public static final RegistryObject<ItemCustomLore> boy_shielding =
            REG.item("boy_shielding", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemCustomLore> boy_target =
            REG.item("boy_target", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemCustomLore> boy_bullet =
            REG.item("boy_bullet", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemCustomLore> boy_propellant =
            REG.item("boy_propellant", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);
    public static final RegistryObject<ItemCustomLore> boy_igniter =
            REG.item("boy_igniter", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** The conventional explosive lenses used by Ivy Mike/Tsar. */
    public static final RegistryObject<ItemCustomLore> explosive_lenses =
            REG.item("man_explosive8", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** Ivy Mike's uranium-coated primary vessel. */
    public static final RegistryObject<ItemCustomLore> mike_core =
            REG.item("mike_core", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** Ivy Mike's deuterium secondary vessel. */
    public static final RegistryObject<ItemCustomLore> mike_deut =
            REG.item("mike_deut", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** Ivy Mike's deuterium cooling assembly. */
    public static final RegistryObject<ItemCustomLore> mike_cooling_unit =
            REG.item("mike_cooling_unit", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** Optional sixth-stage Tsar core. */
    public static final RegistryObject<ItemCustomLore> tsar_core =
            REG.item("tsar_core", ItemCustomLore::new, p -> p.stacksTo(1))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);

    /** One charge occupies one of N²'s twelve slots. */
    public static final RegistryObject<ItemCustomLore> n2_charge =
            REG.item("n2_charge", ItemCustomLore::new, p -> p.stacksTo(12))
                    .transform(Presets.sprite()).tab(HbmTab.BOMBS);


    public static void init() {}
}
