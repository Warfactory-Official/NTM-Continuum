package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.items.machine.ItemBlades;
import com.hbm.items.machine.ItemBlueprints;
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

/**
 * Hand-registered machine items (templates, etc.) that aren't part of the generated {@link ModItems}.
 * Kept separate so codegen never clobbers them.
 */
public final class ModMachineItems {

    private ModMachineItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Assembly machine "plates" template — unlocks the metal-plate recipes. */
    public static final RegistryObject<ItemBlueprints> blueprint_plate = ITEMS.registerItem(
            "blueprint_plate", p -> new ItemBlueprints(p, AssemblyMachineRecipes.POOL_PLATES), p -> p.stacksTo(1));

    /** Fluid identifier — a dedicated one per fluid; set a tank/duct's type with it. */
    public static final RegistryObject<ItemFluidId> fluid_identifier = ITEMS.registerItem(
            "fluid_identifier", ItemFluidId::new, p -> p.stacksTo(1));

    // --- shredder blades (wear parts; two per shredder). Durabilities are the 1.12.2 values; desh has
    // none, which means unbreakable (the original's maxDamage == 0). ---

    public static final RegistryObject<ItemBlades> blades_steel = ITEMS.registerItem(
            "blades_steel", ItemBlades::new, p -> p.stacksTo(1).durability(400));

    public static final RegistryObject<ItemBlades> blades_titanium = ITEMS.registerItem(
            "blades_titanium", ItemBlades::new, p -> p.stacksTo(1).durability(500));

    public static final RegistryObject<ItemBlades> blades_desh = ITEMS.registerItem(
            "blades_desh", ItemBlades::new, p -> p.stacksTo(1));

    /** Foundry scraps — solidified molten material (material + quanta amount in data components). */
    public static final RegistryObject<ItemScraps> scraps = ITEMS.registerItem(
            "scraps", ItemScraps::new, p -> p.stacksTo(64));

    // --- FEL laser crystals: the wavelength "lens" a Free Electron Laser is loaded with. Each one
    // unlocks the SILEX recipes at or below its wavelength and multiplies the speed of the weaker
    // ones (see TileEntitySILEX.process). ---

    public static final RegistryObject<ItemFELCrystal> laser_crystal_co2 = ITEMS.registerItem(
            "laser_crystal_co2", p -> new ItemFELCrystal(p, EnumWavelengths.IR, false));
    public static final RegistryObject<ItemFELCrystal> laser_crystal_bismuth = ITEMS.registerItem(
            "laser_crystal_bismuth", p -> new ItemFELCrystal(p, EnumWavelengths.VISIBLE, false));
    public static final RegistryObject<ItemFELCrystal> laser_crystal_cmb = ITEMS.registerItem(
            "laser_crystal_cmb", p -> new ItemFELCrystal(p, EnumWavelengths.UV, false));
    public static final RegistryObject<ItemFELCrystal> laser_crystal_bale = ITEMS.registerItem(
            "laser_crystal_bale", p -> new ItemFELCrystal(p, EnumWavelengths.GAMMA, false));
    /** The digamma crystal shows scrambled text instead of a description — upstream's easter egg. */
    public static final RegistryObject<ItemFELCrystal> laser_crystal_digamma = ITEMS.registerItem(
            "laser_crystal_digamma", p -> new ItemFELCrystal(p, EnumWavelengths.DRX, true));

    static {
        ModTabs.addTo(HbmTab.TEMPLATES, blueprint_plate);
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
