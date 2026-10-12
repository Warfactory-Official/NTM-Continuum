package com.hbm.inventory.recipes;

import com.hbm.items.ModPwrItems;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemZirnoxWaste;
import dev.golem.core.world.ItemStacks;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code FuelPoolRecipes}: what the spent-fuel pool drum ({@code TileEntityWasteDrum}) cools, hot stack to cold stack.
 * The 1.7 table holds the 9 hot ZIRNOX wastes, the 7 hot research-reactor plate wastes and every hot PWR rod. In the port
 * the first two keep their item and carry a hot flag ({@code fuel_waste_hot}); hot PWR rods are separate items. 1.7's
 * RBMK entries are display-only: the drum cools any RBMK rod through its own heat branch, not through this table.
 */
public final class FuelPoolRecipes {

    private FuelPoolRecipes() {}

    /** True for a 1.7 FuelPool key: an item the drum turns into its cooled form. */
    public static boolean isHot(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemZirnoxWaste) return ItemZirnoxWaste.isHot(stack);
        if (stack.getItem() instanceof ItemDepletedFuel) return ItemDepletedFuel.isHot(stack);
        return hotPwr(stack) != null;
    }

    /** The cooled form of a hot stack (count kept), or {@link ItemStack#EMPTY} if the stack is not a key. */
    public static ItemStack cool(ItemStack stack) {
        if (!isHot(stack)) return ItemStack.EMPTY;
        if (stack.getItem() instanceof ItemZirnoxWaste) {
            ItemStack out = stack.copy();
            ItemZirnoxWaste.setHot(out, false);
            return out;
        }
        if (stack.getItem() instanceof ItemDepletedFuel) {
            ItemStack out = stack.copy();
            ItemDepletedFuel.setHot(out, false);
            return out;
        }
        return ItemStacks.copyWithCount(new ItemStack(hotPwr(stack)), stack.getCount());
    }

    /** One 1.7 FuelPool entry as the recipe browser shows it. */
    public record Entry(ItemStack input, ItemStack output) {}

    /**
     * 1.7 {@code FuelPoolRecipes.recipes} for the recipe browser (1.7 {@code FuelPoolHandler}): the hot ZIRNOX wastes, the
     * hot plate wastes and the hot PWR rods to their cooled forms, then every craftable RBMK rod shown hot (core 100, hull
     * 50, 1.7 {@code ComparableStackHeat}) to the same rod cold, both at 20% of its yield.
     */
    public static java.util.List<Entry> displayRecipes() {
        java.util.List<Entry> out = new java.util.ArrayList<>();
        for (com.hbm.items.machine.ZirnoxFuel fuel : com.hbm.items.machine.ZirnoxFuel.values()) {
            if (fuel.wasteId == null) continue;
            ItemStack hot = com.hbm.items.ModZirnoxItems.hotWaste(fuel, 1);
            out.add(new Entry(hot, cool(hot)));
        }
        for (var plate : java.util.List.of(com.hbm.items.ModNuclearMiscItems.WASTE_PLATE_U233, com.hbm.items.ModNuclearMiscItems.WASTE_PLATE_U235,
                com.hbm.items.ModNuclearMiscItems.WASTE_PLATE_MOX, com.hbm.items.ModNuclearMiscItems.WASTE_PLATE_PU239,
                com.hbm.items.ModNuclearMiscItems.WASTE_PLATE_SA326, com.hbm.items.ModNuclearMiscItems.WASTE_PLATE_RA226BE,
                com.hbm.items.ModNuclearMiscItems.WASTE_PLATE_PU238BE)) {
            ItemStack hot = ItemDepletedFuel.makeHot(plate.get(), 1);
            out.add(new Entry(hot, cool(hot)));
        }
        for (net.minecraft.world.item.Item fresh : pwrFuels()) {
            ItemStack hot = new ItemStack(com.hbm.items.machine.ItemPWRFuel.stats(fresh).hotItem());
            out.add(new Entry(hot, cool(hot)));
        }
        for (var rodHandle : com.hbm.items.ModRBMKItems.PELLETS.keySet()) {
            com.hbm.items.machine.ItemRBMKRod rod = rodHandle.get();
            ItemStack hot = new ItemStack(rod);
            com.hbm.items.machine.ItemRBMKRod.setYield(hot, 0.2 * rod.stats().yield());
            com.hbm.items.machine.ItemRBMKRod.setCoreHeat(hot, 100);
            com.hbm.items.machine.ItemRBMKRod.setHullHeat(hot, 50);
            ItemStack cold = new ItemStack(rod);
            com.hbm.items.machine.ItemRBMKRod.setYield(cold, 0.2 * rod.stats().yield());
            out.add(new Entry(hot, cold));
        }
        return out;
    }

    /** The depleted rod a hot PWR rod cools into (the {@code hbm:pwr_fuel} entries' hot/depleted pairs), or null. */
    private static net.minecraft.world.item.Item hotPwr(ItemStack stack) {
        for (net.minecraft.world.item.Item fresh : pwrFuels()) {
            com.hbm.api.data.ReactorDataMaps.PWRFuel fuel = com.hbm.items.machine.ItemPWRFuel.stats(fresh);
            if (stack.getItem() == fuel.hotItem()) return fuel.depletedItem();
        }
        return null;
    }

    /** Every fresh PWR rod: HBM's in 1.7 order, then any other item the data map names. */
    private static java.util.List<net.minecraft.world.item.Item> pwrFuels() {
        java.util.List<net.minecraft.world.item.Item> fuels = new java.util.ArrayList<>();
        for (EnumPWRFuel fuel : EnumPWRFuel.values()) {
            net.minecraft.world.item.Item item = ModPwrItems.fuel(fuel);
            if (com.hbm.items.machine.ItemPWRFuel.stats(item) != null) fuels.add(item);
        }
        for (String id : new java.util.TreeSet<>(com.hbm.api.data.ReactorDataMaps.PWR_FUEL.byId().keySet())) {
            net.minecraft.world.item.Item item = com.hbm.api.data.ReactorDataMaps.itemOf(id);
            if (!fuels.contains(item) && item != net.minecraft.world.item.Items.AIR) fuels.add(item);
        }
        return fuels;
    }
}
