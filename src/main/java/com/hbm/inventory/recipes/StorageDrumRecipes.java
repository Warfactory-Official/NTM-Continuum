package com.hbm.inventory.recipes;

import com.hbm.items.ModItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.special.ItemWasteStorage;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The old storage-drum waste transformation table, including its liquid/gas byproducts. */
public final class StorageDrumRecipes {

    public record Recipe(ItemStack input, ItemStack output, int chance, int liquid, int gas) {
        public Recipe {
            input = input.copy();
            output = output.copy();
        }
    }

    private static final List<Recipe> RECIPES = new ArrayList<>();
    private static boolean initialized;

    private StorageDrumRecipes() {}

    public static void registerRecipes() {
        if (initialized) return;
        initialized = true;

        // Old default: VersatileConfig returns 3 hours when 528 and LBSM are both disabled. The target
        // has no config bridge for those switches, so this preserves the shipped default exactly.
        int decayChance = 3 * 60 * 60 * 20;
        addWasteFamily(ModStorageDrumItems.nuclear_waste_long.get(),
                ModStorageDrumItems.nuclear_waste_long_depleted.get(),
                ItemWasteStorage.LongWasteClass.values(), decayChance, 1.0);
        addWasteFamily(ModStorageDrumItems.nuclear_waste_long_tiny.get(),
                ModStorageDrumItems.nuclear_waste_long_depleted_tiny.get(),
                ItemWasteStorage.LongWasteClass.values(), (int) (decayChance * 0.1), 0.1);
        addWasteFamily(ModStorageDrumItems.nuclear_waste_short.get(),
                ModStorageDrumItems.nuclear_waste_short_depleted.get(),
                ItemWasteStorage.ShortWasteClass.values(), decayChance, 1.0);
        addWasteFamily(ModStorageDrumItems.nuclear_waste_short_tiny.get(),
                ModStorageDrumItems.nuclear_waste_short_depleted_tiny.get(),
                ItemWasteStorage.ShortWasteClass.values(), (int) (decayChance * 0.1), 0.1);

        add(new ItemStack(ModItems.ingot_au198.get()), new ItemStack(ModItems.bottle_mercury.get()),
                (int) (decayChance * 0.01), 500, 500);
        // Deferred faithfully: the old nugget_au198 -> ingot_mercury recipe needs the unported item.
    }

    private static void addWasteFamily(Item input, Item output, ItemWasteStorage.LongWasteClass[] values,
                                       int chance, double wasteScale) {
        for (int i = 0; i < values.length; i++) {
            ItemWasteStorage.LongWasteClass waste = values[i];
            add(ModStorageDrumItems.make(input, i, 1), ModStorageDrumItems.make(output, i, 1),
                    chance, (int) (waste.liquid * wasteScale), (int) (waste.gas * wasteScale));
        }
    }

    private static void addWasteFamily(Item input, Item output, ItemWasteStorage.ShortWasteClass[] values,
                                       int chance, double wasteScale) {
        for (int i = 0; i < values.length; i++) {
            ItemWasteStorage.ShortWasteClass waste = values[i];
            add(ModStorageDrumItems.make(input, i, 1), ModStorageDrumItems.make(output, i, 1),
                    chance, (int) (waste.liquid * wasteScale), (int) (waste.gas * wasteScale));
        }
    }

    private static void add(ItemStack input, ItemStack output, int chance, int liquid, int gas) {
        RECIPES.add(new Recipe(input, output, chance, liquid, gas));
    }

    public static ItemStack getOutput(ItemStack stack) {
        for (Recipe recipe : getAll()) {
            if (same(recipe.input(), stack)) return recipe.output();
        }
        return null;
    }

    public static int[] getWaste(ItemStack stack) {
        for (Recipe recipe : getAll()) {
            if (same(recipe.input(), stack)) return new int[]{recipe.chance(), recipe.liquid(), recipe.gas()};
        }
        return null;
    }

    public static List<Recipe> getAll() {
        registerRecipes();
        return Collections.unmodifiableList(RECIPES);
    }

    private static boolean same(ItemStack expected, ItemStack actual) {
        return !actual.isEmpty() && expected.getItem() == actual.getItem()
                && dev.golem.core.world.ItemStacks.sameItemAndData(expected, actual);
    }
}
