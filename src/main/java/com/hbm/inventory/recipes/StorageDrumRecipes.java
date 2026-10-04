package com.hbm.inventory.recipes;

import com.hbm.items.ModItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.special.ItemWasteStorage;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The 1.7 storage-drum decay table ({@code TileEntityStorageDrum.updateEntity}), with its liquid/gas byproducts. */
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

    /** 1.7 {@code VersatileConfig.getLongDecayChance}: 3 h, 15 h in 528 mode, 15 min with LBSM_shortDecay. */
    public static int longDecayChance() {
        return com.hbm.config.GeneralConfig.enable528 ? 15 * HOUR
                : (com.hbm.config.GeneralConfig.enableLBSM && com.hbm.config.GeneralConfig.enableLBSMShorterDecay) ? 15 * MINUTE : 3 * HOUR;
    }

    /** 1.7 {@code VersatileConfig.getShortDecayChance}: 15 min, 3 h in 528 mode, 3 min with LBSM_shortDecay. */
    public static int shortDecayChance() {
        return com.hbm.config.GeneralConfig.enable528 ? 3 * HOUR
                : (com.hbm.config.GeneralConfig.enableLBSM && com.hbm.config.GeneralConfig.enableLBSMShorterDecay) ? 3 * MINUTE : 15 * MINUTE;
    }

    private static final int MINUTE = 60 * 20;
    private static final int HOUR = 60 * MINUTE;

    /**
     * 1.7 {@code TileEntityStorageDrum.updateEntity}: every tick each slot rolls {@code rand.nextInt(chance) == 0}.
     * Tiny waste rolls a tenth of the chance and releases a tenth of the liquid/gas (integer division, as 1.7).
     * 1.7's {@code ingot_mercury} field is the droplet {@code nugget_mercury}, its {@code nugget_mercury} field
     * {@code nugget_mercury_tiny}.
     */
    public static void registerRecipes() {
        if (initialized) return;
        initialized = true;

        int longChance = longDecayChance();
        int shortChance = shortDecayChance();
        addWasteFamily(ModStorageDrumItems.nuclear_waste_long.get(),
                ModStorageDrumItems.nuclear_waste_long_depleted.get(),
                ItemWasteStorage.LongWasteClass.values(), longChance, 1);
        addWasteFamily(ModStorageDrumItems.nuclear_waste_long_tiny.get(),
                ModStorageDrumItems.nuclear_waste_long_depleted_tiny.get(),
                ItemWasteStorage.LongWasteClass.values(), longChance / 10, 10);
        addWasteFamily(ModStorageDrumItems.nuclear_waste_short.get(),
                ModStorageDrumItems.nuclear_waste_short_depleted.get(),
                ItemWasteStorage.ShortWasteClass.values(), shortChance, 1);
        addWasteFamily(ModStorageDrumItems.nuclear_waste_short_tiny.get(),
                ModStorageDrumItems.nuclear_waste_short_depleted_tiny.get(),
                ItemWasteStorage.ShortWasteClass.values(), shortChance / 10, 10);

        add(new ItemStack(ModItems.ingot_au198.get()), new ItemStack(ModItems.nugget_mercury.get()), shortChance / 20, 0, 0);
        add(new ItemStack(ModItems.nugget_au198.get()), new ItemStack(ModItems.nugget_mercury_tiny.get()), shortChance / 100, 0, 0);
        add(new ItemStack(ModItems.ingot_pb209.get()), new ItemStack(ModItems.ingot_bismuth.get()), shortChance / 10, 0, 0);
        add(new ItemStack(ModItems.nugget_pb209.get()), new ItemStack(ModItems.nugget_bismuth.get()), shortChance / 50, 0, 0);
        add(new ItemStack(ModItems.powder_sr90.get()), new ItemStack(ModItems.powder_zirconium.get()), shortChance / 10, 0, 0);
        add(new ItemStack(ModItems.nugget_sr90.get()), new ItemStack(ModItems.nugget_zirconium.get()), shortChance / 50, 0, 0);
    }

    private static void addWasteFamily(Item input, Item output, ItemWasteStorage.LongWasteClass[] values,
                                       int chance, int divisor) {
        for (int i = 0; i < values.length; i++) {
            ItemWasteStorage.LongWasteClass waste = values[i];
            add(ModStorageDrumItems.make(input, i, 1), ModStorageDrumItems.make(output, i, 1),
                    chance, waste.liquid / divisor, waste.gas / divisor);
        }
    }

    private static void addWasteFamily(Item input, Item output, ItemWasteStorage.ShortWasteClass[] values,
                                       int chance, int divisor) {
        for (int i = 0; i < values.length; i++) {
            ItemWasteStorage.ShortWasteClass waste = values[i];
            add(ModStorageDrumItems.make(input, i, 1), ModStorageDrumItems.make(output, i, 1),
                    chance, waste.liquid / divisor, waste.gas / divisor);
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
