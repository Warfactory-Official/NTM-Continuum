package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Tints;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemFluidTank;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluid-container items. Currently the {@code fluid_bucket} — a single item that carries any fluid (1000
 * mB) via its data components, with one creative-menu variant per registered fluid. Kept out of the
 * generated {@link ModItems} so codegen never clobbers it.
 */
public final class ModFluidItems {

    private ModFluidItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemFluidBucket> fluid_bucket =
            REG.item("fluid_bucket", ItemFluidBucket::new, p -> p.stacksTo(1))
            .transform(Presets.sprite("minecraft:item/bucket", "hbm:items/fluid_bucket_fill")
                    .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF)));

    /** The fluid "droplet" icon — a per-fluid visual representation used by recipes (not a real item; no
     * creative tab, like the 1.12.2 {@code setCreativeTab(null)}). */
    public static final RegistryObject<ItemFluidIcon> fluid_icon =
            REG.item("fluid_icon", ItemFluidIcon::new, p -> p.stacksTo(1))
            .transform(Presets.sprite().tints(Tints.customModelData(0, 0xFFFFFF)));

    /** Universal fluid tank — a portable 1000&nbsp;mB container for any fluid (1.12.2 {@code fluid_tank_v2}).
     * Stacks to 64 (containers holding the same fluid+amount merge; different contents don't). */
    public static final RegistryObject<ItemFluidTank> fluid_tank =
            REG.item("fluid_tank", p -> new ItemFluidTank(p, 1000, "fluid_tank"), p -> p.stacksTo(64))
            .transform(Presets.sprite("hbm:items/fluid_tank", "hbm:items/fluid_tank_overlay")
                    .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF)));

    /** Fluid barrel — a portable 16000&nbsp;mB container for any fluid (1.12.2 {@code fluid_barrel_v2}). */
    public static final RegistryObject<ItemFluidTank> fluid_barrel =
            REG.item("fluid_barrel", p -> new ItemFluidTank(p, 16000, "fluid_barrel"), p -> p.stacksTo(64))
            .transform(Presets.sprite("hbm:items/fluid_barrel", "hbm:items/fluid_barrel_overlay")
                    .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF)));

    static {
        // One full bucket per fluid in the creative menu, so any fluid can be grabbed and poured into a
        // tank. Skip NONE, and WATER/LAVA (vanilla already provides those buckets). Resolved lazily at
        // tab-build time, after Fluids.init() has populated.
        ModTabs.addStacksTo(HbmTab.TEMPLATES, () -> {
            List<ItemStack> list = new ArrayList<>();
            for (FluidType fluid : Fluids.getAll()) {
                if (fluid == Fluids.NONE || fluid == Fluids.WATER || fluid == Fluids.LAVA) continue;
                list.add(ItemFluidBucket.make(fluid));
            }
            return list;
        });
        // The universal tank + barrel: an empty one plus one full of each fluid (1.12.2 controlTab).
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
