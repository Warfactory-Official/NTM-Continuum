package com.hbm.inventory.recipes;

import net.minecraft.core.registries.BuiltInRegistries;
import com.hbm.api.data.ReactorDataMaps;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.ModPwrItems;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.ItemZirnoxWaste;
import com.hbm.items.machine.ZirnoxFuel;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
    public record Entry(ItemStack input, ItemStack output) {
        /** Stable catalog identity, named for the registered input item as in FuelPoolRecipes. */
        public String id() {
            var itemId = BuiltInRegistries.ITEM.getKey(input.getItem());
            return itemId.getNamespace() + ":fuel_pool/" + itemId.getPath();
        }
    }

    /** Every registered craftable rod, preserving HBM's order and then addon registration order. */
    public static List<ItemRBMKRod> craftableRods() {
        List<ItemRBMKRod> rods = new ArrayList<>();
        for (var handle : ModRBMKItems.PELLETS.keySet()) rods.add(handle.get());
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ItemRBMKRod rod && rod.pelletItem() != null && !rods.contains(rod)) rods.add(rod);
        }
        return List.copyOf(rods);
    }

    /**
     * 1.7 {@code FuelPoolRecipes.recipes} for the recipe browser (1.7 {@code FuelPoolHandler}): the hot ZIRNOX wastes, the
     * hot plate wastes and the hot PWR rods to their cooled forms, then every craftable RBMK rod shown hot (core 100, hull
     * 50, 1.7 {@code ComparableStackHeat}) to the same rod cold, both at 20% of its yield.
     */
    public static List<Entry> displayRecipes() {
        List<Entry> out = new ArrayList<>();
        for (ZirnoxFuel fuel : ZirnoxFuel.values()) {
            if (fuel.wasteId == null) continue;
            ItemStack hot = ModZirnoxItems.hotWaste(fuel, 1);
            out.add(new Entry(hot, cool(hot)));
        }
        for (var plate : List.of(ModNuclearMiscItems.WASTE_PLATE_U233, ModNuclearMiscItems.WASTE_PLATE_U235,
                ModNuclearMiscItems.WASTE_PLATE_MOX, ModNuclearMiscItems.WASTE_PLATE_PU239,
                ModNuclearMiscItems.WASTE_PLATE_SA326, ModNuclearMiscItems.WASTE_PLATE_RA226BE,
                ModNuclearMiscItems.WASTE_PLATE_PU238BE)) {
            ItemStack hot = ItemDepletedFuel.makeHot(plate.get(), 1);
            out.add(new Entry(hot, cool(hot)));
        }
        for (Item fresh : pwrFuels()) {
            ItemStack hot = new ItemStack(ItemPWRFuel.stats(fresh).hotItem());
            out.add(new Entry(hot, cool(hot)));
        }
        for (ItemRBMKRod rod : craftableRods()) {
            ItemStack hot = new ItemStack(rod);
            ItemRBMKRod.setYield(hot, 0.2 * rod.stats().yield());
            ItemRBMKRod.setCoreHeat(hot, 100);
            ItemRBMKRod.setHullHeat(hot, 50);
            ItemStack cold = new ItemStack(rod);
            ItemRBMKRod.setYield(cold, 0.2 * rod.stats().yield());
            out.add(new Entry(hot, cold));
        }
        return out;
    }

    /** The depleted rod a hot PWR rod cools into (the {@code hbm:pwr_fuel} entries' hot/depleted pairs), or null. */
    private static Item hotPwr(ItemStack stack) {
        for (Item fresh : pwrFuels()) {
            ReactorDataMaps.PWRFuel fuel = ItemPWRFuel.stats(fresh);
            if (stack.getItem() == fuel.hotItem()) return fuel.depletedItem();
        }
        return null;
    }

    /** Every fresh PWR rod: HBM's in 1.7 order, then any other item the data map names. */
    private static List<Item> pwrFuels() {
        List<Item> fuels = new ArrayList<>();
        for (EnumPWRFuel fuel : EnumPWRFuel.values()) {
            Item item = ModPwrItems.fuel(fuel);
            if (ItemPWRFuel.stats(item) != null) fuels.add(item);
        }
        for (String id : new TreeSet<>(ReactorDataMaps.PWR_FUEL.byId().keySet())) {
            Item item = ReactorDataMaps.itemOf(id);
            if (!fuels.contains(item) && item != Items.AIR) fuels.add(item);
        }
        return fuels;
    }
}
