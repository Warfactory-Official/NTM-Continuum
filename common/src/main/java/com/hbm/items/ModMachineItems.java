package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemFluidIdentifier;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ModMachineItems {

    private ModMachineItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemBlueprints> blueprint_plate = ITEMS.registerItem(
            "blueprint_plate", p -> new ItemBlueprints(p, AssemblyMachineRecipes.POOL_PLATES), p -> p.stacksTo(1));

    public static final RegistryObject<ItemFluidIdentifier> fluid_identifier = ITEMS.registerItem(
            "fluid_identifier", ItemFluidIdentifier::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.TEMPLATES, blueprint_plate);

        ModTabs.addStacksTo(HbmTab.TEMPLATES, () -> {
            List<ItemStack> list = new ArrayList<>();
            for (FluidType fluid : Fluids.getAll()) {
                if (fluid == Fluids.NONE) continue;
                list.add(ItemFluidIdentifier.make(fluid));
            }
            return list;
        });
    }

    public static void init() {}
}
