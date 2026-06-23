package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ModFluidItems {

    private ModFluidItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemFluidBucket> fluid_bucket =
            ITEMS.registerItem("fluid_bucket", ItemFluidBucket::new, p -> p.stacksTo(1));

    public static final RegistryObject<ItemFluidIcon> fluid_icon =
            ITEMS.registerItem("fluid_icon", ItemFluidIcon::new, p -> p.stacksTo(1));

    public static final RegistryObject<ItemFluidTank> fluid_tank =
            ITEMS.registerItem("fluid_tank", p -> new ItemFluidTank(p, 1000, "fluid_tank"), p -> p.stacksTo(64));

    public static final RegistryObject<ItemFluidTank> fluid_barrel =
            ITEMS.registerItem("fluid_barrel", p -> new ItemFluidTank(p, 16000, "fluid_barrel"), p -> p.stacksTo(64));

    static {

        ModTabs.addStacksTo(HbmTab.TEMPLATES, () -> {
            List<ItemStack> list = new ArrayList<>();
            for (FluidType fluid : Fluids.getAll()) {
                if (fluid == Fluids.NONE || fluid == Fluids.WATER || fluid == Fluids.LAVA) continue;
                list.add(ItemFluidBucket.make(fluid));
            }
            return list;
        });

        ModTabs.addStacksTo(HbmTab.CONTROL, () -> fluidContainerVariants(fluid_tank.get()));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> fluidContainerVariants(fluid_barrel.get()));
    }

    private static List<ItemStack> fluidContainerVariants(ItemFluidTank item) {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(item));
        for (FluidType fluid : Fluids.getAll()) {
            if (fluid == Fluids.NONE) continue;
            list.add(item.make(fluid, item.capacity));
        }
        return list;
    }

    public static void init() {}
}
