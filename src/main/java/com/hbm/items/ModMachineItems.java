package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.items.machine.ItemBlades;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemBlueprintFolder;
import com.hbm.items.machine.ItemBroken;
import com.hbm.items.machine.ItemFELCrystal;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.items.machine.ItemScraps;
import com.hbm.inventory.material.MaterialShapes;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.ItemModels;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tints;
import net.minecraft.world.item.Item;

/**
 * Hand-registered machine items (templates, etc.) that aren't part of the generated {@link ModItems}.
 * Kept separate so codegen never clobbers them.
 */
public final class ModMachineItems {

    private ModMachineItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** Turret targeting chip — the biometry card a turret accepts in its chip slot. */
    public static final RegistryObject<com.hbm.items.machine.ItemTurretChip> turret_chip = REG.<com.hbm.items.machine.ItemTurretChip>item(
            "turret_chip", com.hbm.items.machine.ItemTurretChip::new, p -> p.stacksTo(1))
            .transform(Presets.sprite());

    /** Handheld biometric scanner — writes the holder's name onto a turret whitelist. */
    public static final RegistryObject<com.hbm.items.machine.ItemTurretBiometry> turret_biometry = REG.<com.hbm.items.machine.ItemTurretBiometry>item(
            "turret_biometry", com.hbm.items.machine.ItemTurretBiometry::new, p -> p.stacksTo(1))
            .icon(d -> d.models(Models.generated("hbm:items/rei_scanner")));

    /** Assembly machine "plates" template — unlocks the metal-plate recipes. */
    public static final RegistryObject<ItemBlueprints> blueprint_plate = REG.<ItemBlueprints>item(
            "blueprint_plate", p -> new ItemBlueprints(p, AssemblyMachineRecipes.POOL_PLATES), p -> p.stacksTo(1))
            .icon(d -> d.models(Models.generated("hbm:items/blueprints")));

    /** Dynamic blueprint selected from a blueprint folder's pool. */
    public static final RegistryObject<ItemBlueprints> blueprints = REG.<ItemBlueprints>item(
            "blueprints", (Item.Properties p) -> new ItemBlueprints(p, null))
            .icon(d -> d.models(Models.generated("hbm:items/blueprints"))
                    .definition(ItemModels.select("hbm:item_variant").when("1", ItemModels.model("hbm:item/blueprints_discover")).when("2", ItemModels.model("hbm:item/blueprints_secret")).when("3", ItemModels.model("hbm:item/blueprints_528")).fallback(ItemModels.model("hbm:item/blueprints"))));
    /** The 1.12.2 booklet which turns into a randomly selected blueprint on right-click. */
    public static final RegistryObject<ItemBlueprintFolder> blueprint_folder = REG.<ItemBlueprintFolder>item(
            "blueprint_folder", ItemBlueprintFolder::new, p -> p.stacksTo(1))
            .icon(d -> d.models(Models.generated("hbm:items/blueprint_folder"))
                    .definition(ItemModels.select("hbm:item_variant").when("1", ItemModels.model("hbm:item/blueprint_folder_discover")).fallback(ItemModels.model("hbm:item/blueprint_folder"))));
    /** Faulty output from precision-assembler recipes; the original item id is carried as data. */
    public static final RegistryObject<ItemBroken> broken_item = REG.<ItemBroken>item(
            "broken_item", ItemBroken::new)
            .transform(Presets.sprite());

    /** Fluid identifier — a dedicated one per fluid; set a tank/duct's type with it. */
    public static final RegistryObject<ItemFluidId> fluid_identifier = REG.<ItemFluidId>item(
            "fluid_identifier", ItemFluidId::new, p -> p.stacksTo(1))
            .transform(Presets.sprite("hbm:items/fluid_identifier_multi", "hbm:items/fluid_identifier_overlay").tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF)));

    // --- shredder blades (wear parts; two per shredder). Durabilities are the 1.12.2 values; desh has
    // none, which means unbreakable (the original's maxDamage == 0). ---

    public static final RegistryObject<ItemBlades> blades_steel = REG.<ItemBlades>item(
            "blades_steel", ItemBlades::new, p -> p.stacksTo(1).durability(400))
            .transform(Presets.sprite());

    public static final RegistryObject<ItemBlades> blades_titanium = REG.<ItemBlades>item(
            "blades_titanium", ItemBlades::new, p -> p.stacksTo(1).durability(500))
            .transform(Presets.sprite());

    public static final RegistryObject<ItemBlades> blades_desh = REG.<ItemBlades>item(
            "blades_desh", ItemBlades::new, p -> p.stacksTo(1))
            .transform(Presets.sprite());

    /** Foundry scraps — solidified molten material (material + quanta amount in data components). */
    public static final RegistryObject<ItemScraps> scraps = REG.<ItemScraps>item(
            "scraps", ItemScraps::new, p -> p.stacksTo(64))
            .icon(d -> d.models(Models.generated("hbm:items/scraps"))
                    .definition(ItemModels.customModelDataFlag(0, ItemModels.model("hbm:item/scraps_liquid", Tints.customModelData(0, 16777215)), ItemModels.model("hbm:item/scraps", Tints.customModelData(0, 16777215)))));

    // --- FEL laser crystals: the wavelength "lens" a Free Electron Laser is loaded with. Each one
    // unlocks the SILEX recipes at or below its wavelength and multiplies the speed of the weaker
    // ones (see TileEntitySILEX.process). ---

    public static final RegistryObject<ItemFELCrystal> laser_crystal_co2 = REG.<ItemFELCrystal>item(
            "laser_crystal_co2", (Item.Properties p) -> new ItemFELCrystal(p, EnumWavelengths.IR, false))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemFELCrystal> laser_crystal_bismuth = REG.<ItemFELCrystal>item(
            "laser_crystal_bismuth", (Item.Properties p) -> new ItemFELCrystal(p, EnumWavelengths.VISIBLE, false))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemFELCrystal> laser_crystal_cmb = REG.<ItemFELCrystal>item(
            "laser_crystal_cmb", (Item.Properties p) -> new ItemFELCrystal(p, EnumWavelengths.UV, false))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemFELCrystal> laser_crystal_bale = REG.<ItemFELCrystal>item(
            "laser_crystal_bale", (Item.Properties p) -> new ItemFELCrystal(p, EnumWavelengths.GAMMA, false))
            .transform(Presets.sprite());
    /** The digamma crystal shows scrambled text instead of a description — upstream's easter egg. */
    public static final RegistryObject<ItemFELCrystal> laser_crystal_digamma = REG.<ItemFELCrystal>item(
            "laser_crystal_digamma", (Item.Properties p) -> new ItemFELCrystal(p, EnumWavelengths.DRX, true))
            .transform(Presets.sprite());

    static {
        ModTabs.addTo(HbmTab.TEMPLATES, blueprint_plate);
        ModTabs.addTo(HbmTab.TEMPLATES, blueprints);
        ModTabs.addTo(HbmTab.TEMPLATES, blueprint_folder);
        ModTabs.addTo(HbmTab.CONTROL, laser_crystal_co2);
        ModTabs.addTo(HbmTab.CONTROL, laser_crystal_bismuth);
        ModTabs.addTo(HbmTab.CONTROL, laser_crystal_cmb);
        ModTabs.addTo(HbmTab.CONTROL, laser_crystal_bale);
        ModTabs.addTo(HbmTab.CONTROL, laser_crystal_digamma);
        ModTabs.addTo(HbmTab.CONTROL, blades_steel);
        ModTabs.addTo(HbmTab.CONTROL, blades_titanium);
        ModTabs.addTo(HbmTab.CONTROL, blades_desh);
        // One identifier per fluid in the creative menu (skip NONE), each pre-set to its fluid — the 26.1
        // equivalent of the 1.12.2 ItemFluidIDMulti's one-metadata-per-fluid subtypes. Resolved lazily at
        // tab-build time, after Fluids.init() has populated.
        ModTabs.addStacksTo(HbmTab.TEMPLATES, () -> {
            List<ItemStack> list = new ArrayList<>();
            AssemblyMachineRecipes.INSTANCE.registerRecipes();
            PrecAssRecipes.INSTANCE.registerRecipes();
            for (String pool : GenericRecipes.blueprintPools.keySet()) {
                if (!pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) list.add(ItemBlueprints.make(pool));
            }
            for (FluidType fluid : Fluids.getAll()) {
                if (fluid == Fluids.NONE) continue;
                list.add(ItemFluidId.make(fluid));
            }
            return list;
        });
        // One ingot's worth of scraps per meltable material (the old ItemScraps creative subtypes).
        ModTabs.addStacksTo(HbmTab.MATERIALS, () -> {
            List<ItemStack> list = new ArrayList<>();
            for (NTMMaterial mat : Mats.orderedList) {
                if (mat.smeltable == SmeltingBehavior.SMELTABLE || mat.smeltable == SmeltingBehavior.ADDITIVE) {
                    list.add(ItemScraps.create(new Mats.MaterialStack(mat, MaterialShapes.INGOT.quantity)));
                }
            }
            return list;
        });
    }

    public static void init() {}
}
