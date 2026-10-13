package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemCatalyst;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/** Hand-registered AMS reaction catalysts formerly constructed by {@code ItemCatalyst}. */
public final class ModCatalystItems {

    private ModCatalystItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    private static Reg.Handle<ItemCatalyst> catalyst(String id, int color, long powerAbs, float powerMod,
                                                         float heatMod, float fuelMod) {
        Reg.Handle<ItemCatalyst> item = REG.item(id,
                p -> new ItemCatalyst(p, color, powerAbs, powerMod, heatMod, fuelMod), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONTROL, item);
        return item;
    }

    /** The blank catalyst remains the generated {@link ModItems} entry and is not duplicated. */
    public static final RegistryObject<Item> blank = ModItems.ams_catalyst_blank;
    public static final RegistryObject<ItemCatalyst> iron = catalyst("ams_catalyst_iron", 0xFF7E22, 10, 0.50F, 1.50F, 1.50F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> copper = catalyst("ams_catalyst_copper", 0xAADE29, 100, 0.60F, 1.20F, 0.60F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> aluminium = catalyst("ams_catalyst_aluminium", 0xCCCCCC, 250, 0.70F, 0.85F, 0.85F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> lithium = catalyst("ams_catalyst_lithium", 0xFF2727, 500, 0.80F, 0.75F, 1.15F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> beryllium = catalyst("ams_catalyst_beryllium", 0x97978B, 1000, 0.90F, 1.15F, 0.75F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> tungsten = catalyst("ams_catalyst_tungsten", 0xF5FF48, 5000, 1.00F, 1.00F, 0.95F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> cobalt = catalyst("ams_catalyst_cobalt", 0x789BBE, 10000, 1.02F, 0.95F, 1.00F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> niobium = catalyst("ams_catalyst_niobium", 0x3BF1B6, 25000, 1.05F, 1.15F, 1.00F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> cerium = catalyst("ams_catalyst_cerium", 0x1D3FFF, 50000, 1.05F, 1.00F, 1.15F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> thorium = catalyst("ams_catalyst_thorium", 0x653B22, 100000, 1.10F, 0.95F, 1.20F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> strontium = catalyst("ams_catalyst_strontium", 0xDD0D35, 200000, 1.15F, 0.90F, 1.30F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> caesium = catalyst("ams_catalyst_caesium", 0x6400FF, 400000, 1.20F, 0.85F, 1.40F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> schrabidium = catalyst("ams_catalyst_schrabidium", 0x32FFFF, 600000, 1.30F, 0.70F, 1.25F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> euphemium = catalyst("ams_catalyst_euphemium", 0xFF9CD2, 800000, 1.50F, 1.25F, 0.70F)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemCatalyst> dineutronium = catalyst("ams_catalyst_dineutronium", 0x334077, 1000000, 2.00F, 1.50F, 2.00F)
            .transform(Presets.sprite());

    public static void init() {}
}
