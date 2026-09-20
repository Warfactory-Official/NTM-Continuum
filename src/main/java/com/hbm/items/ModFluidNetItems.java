package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.inventory.FluidContainers;
import com.hbm.items.machine.ItemFluidContainerNT;
import com.hbm.items.machine.ItemFluidSiphon;
import com.hbm.items.machine.ItemInfiniteFluid;
import com.hbm.items.tool.ItemModBucket;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.blocks.ModHazardBlocks;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import com.hbm.blocks.ModFluidNetBlocks;

/**
 * The fluid-container items of the 1.7 {@code items/machine} and {@code items/tool} packages that the
 * port did not have yet: the five fluid-block buckets, the siphon, the infinite sources, and the four
 * metadata containers ({@code canister_full}, {@code gas_full}, {@code fluid_pack_full},
 * {@code fluid_tank_lead_full}) in the component-backed form of {@code ModFluidItems.fluid_tank}.
 */
public final class ModFluidNetItems {

    private ModFluidNetItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    // --- buckets (1.7 blockTab) ---

    public static final RegistryObject<ItemModBucket> bucket_mud = ITEMS.registerItem("bucket_mud",
            p -> new ItemModBucket(p, ModHazardBlocks.mud_block), p -> p.stacksTo(1).craftRemainder(Items.BUCKET));
    public static final RegistryObject<ItemModBucket> bucket_acid = ITEMS.registerItem("bucket_acid",
            p -> new ItemModBucket(p, ModFluidNetBlocks.acid_block), p -> p.stacksTo(1).craftRemainder(Items.BUCKET));
    public static final RegistryObject<ItemModBucket> bucket_toxic = ITEMS.registerItem("bucket_toxic",
            p -> new ItemModBucket(p, ModFluidNetBlocks.toxic_block), p -> p.stacksTo(1).craftRemainder(Items.BUCKET));
    public static final RegistryObject<ItemModBucket> bucket_schrabidic_acid = ITEMS.registerItem("bucket_schrabidic_acid",
            p -> new ItemModBucket(p, ModFluidNetBlocks.schrabidic_block), p -> p.stacksTo(1).craftRemainder(Items.BUCKET));
    public static final RegistryObject<ItemModBucket> bucket_sulfuric_acid = ITEMS.registerItem("bucket_sulfuric_acid",
            p -> new ItemModBucket(p, ModFluidNetBlocks.sulfuric_acid_block), p -> p.stacksTo(1).craftRemainder(Items.BUCKET));

    // --- tools ---

    public static final RegistryObject<ItemFluidSiphon> siphon = ITEMS.registerItem("siphon", ItemFluidSiphon::new, p -> p.stacksTo(1));

    // --- infinite sources ---

    public static final RegistryObject<ItemInfiniteFluid> inf_water = ITEMS.registerItem("inf_water",
            p -> new ItemInfiniteFluid(p, () -> Fluids.WATER, 50, 1, false), p -> p.stacksTo(1));
    public static final RegistryObject<ItemInfiniteFluid> inf_water_mk2 = ITEMS.registerItem("inf_water_mk2",
            p -> new ItemInfiniteFluid(p, () -> Fluids.WATER, 500, 1, false), p -> p.stacksTo(1));
    public static final RegistryObject<ItemInfiniteFluid> chlorine_pinwheel = ITEMS.registerItem("chlorine_pinwheel",
            p -> new ItemInfiniteFluid(p, () -> Fluids.CHLORINE, 1, 2, false));
    public static final RegistryObject<ItemInfiniteFluid> fluid_barrel_infinite = ITEMS.registerItem("fluid_barrel_infinite",
            p -> new ItemInfiniteFluid(p, null, 1_000_000_000, 1, true), p -> p.stacksTo(1));

    // --- containers ---

    public static final RegistryObject<ItemFluidContainerNT> canister_full = ITEMS.registerItem("canister_full",
            p -> new ItemFluidContainerNT(p, "canister_full", FluidContainers::hasCanister, FluidContainers::canisterColor),
            p -> p.stacksTo(64).craftRemainder(ModItems.canister_empty.get()));
    public static final RegistryObject<ItemFluidContainerNT> gas_full = ITEMS.registerItem("gas_full",
            p -> new ItemFluidContainerNT(p, "gas_full", FluidContainers::hasGasTank, FluidContainers::gasBottleColor),
            p -> p.stacksTo(64).craftRemainder(ModItems.gas_empty.get()));
    /**
     * 1.7's pack: 32,000 mB, packed and unpacked only by the assembler ({@code ass.package<fluid>} /
     * {@code ass.unpackage<fluid>}, every fluid without FT_NoContainer), never by a machine's fill slot
     * (it was absent from {@code FluidContainerRegistry}).
     */
    public static final int FLUID_PACK_CAPACITY = 32_000;
    public static final RegistryObject<ItemFluidContainerNT> fluid_pack_full = ITEMS.registerItem("fluid_pack_full",
            p -> new ItemFluidContainerNT(p, "fluid_pack_full", FLUID_PACK_CAPACITY, false, anyContainer(), FluidType::getColor),
            p -> p.stacksTo(64).craftRemainder(ModItems.fluid_pack_empty.get()));
    public static final RegistryObject<ItemFluidContainerNT> fluid_tank_lead_full = ITEMS.registerItem("fluid_tank_lead_full",
            // 1.7 FluidContainerRegistry: the lead tank is registered for every fluid without FT_NoContainer
            // (the lead-only fluids and all the ordinary ones); only the plain containers skip the lead-only set.
            p -> new ItemFluidContainerNT(p, "fluid_tank_lead_full", anyContainer(), FluidType::getColor),
            p -> p.stacksTo(64).craftRemainder(ModItems.fluid_tank_lead_empty.get()));

    /** 1.7 ItemFluidTank.getSubItems: every fluid without FT_NoContainer that does not need lead. */
    private static Predicate<FluidType> plainContainer() {
        return type -> !FluidContainers.hasNoContainer(type) && !FluidContainers.needsLeadContainer(type);
    }

    /** 1.7 ItemFluidTank.getSubItems for the lead tank: every fluid without FT_NoContainer. */
    private static Predicate<FluidType> anyContainer() {
        return type -> !FluidContainers.hasNoContainer(type);
    }

    private static List<ItemStack> variants(ItemFluidContainerNT item) {
        List<ItemStack> list = new ArrayList<>();
        for (FluidType fluid : Fluids.getAll()) {
            if (fluid == Fluids.NONE || !item.allows(fluid)) continue;
            list.add(item.make(fluid, item.capacity));
        }
        return list;
    }

    static {
        ModTabs.addTo(HbmTab.BLOCKS, bucket_mud);
        ModTabs.addTo(HbmTab.BLOCKS, bucket_acid);
        ModTabs.addTo(HbmTab.BLOCKS, bucket_toxic);
        ModTabs.addTo(HbmTab.BLOCKS, bucket_schrabidic_acid);
        ModTabs.addTo(HbmTab.BLOCKS, bucket_sulfuric_acid);
        ModTabs.addTo(HbmTab.CONTROL, siphon);
        ModTabs.addTo(HbmTab.CONTROL, inf_water);
        ModTabs.addTo(HbmTab.CONTROL, inf_water_mk2);
        ModTabs.addTo(HbmTab.MATERIALS, chlorine_pinwheel);
        ModTabs.addTo(HbmTab.CONTROL, fluid_barrel_infinite);
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(canister_full.get()));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(gas_full.get()));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(fluid_pack_full.get()));
        ModTabs.addStacksTo(HbmTab.CONTROL, () -> variants(fluid_tank_lead_full.get()));
    }

    public static void init() {}
}
