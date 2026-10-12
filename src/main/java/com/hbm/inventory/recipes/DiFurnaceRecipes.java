package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.api.data.DiFurnaceDataMaps;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import dev.golem.core.Ids;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

/**
 * The legacy, fuel-fired DiFurnace (1.7 {@code BlastFurnaceRecipes}, the two-input alloy furnace). This is deliberately
 * separate from the modern airblast {@link BlastFurnaceRecipesNT}: the old DiFurnace consumed one of two legacy alloy
 * inputs and emitted only its recipe result plus crafting remainders.
 *
 * <p>The recipes are the datapack table {@code hbm:difurnace}, one file per row in {@code data/hbm/recipe/difurnace/},
 * converted once from the former Java table and the source now. 1.7's {@code DictFrame} of a material's ingot, plate,
 * gem and dust is an {@code hbm:any_of} ingredient, its gasoline canister an {@code hbm:fluid_item}. The fuel values are
 * the item data map {@code hbm:difurnace_fuel} ({@link DiFurnaceDataMaps}).
 */
public final class DiFurnaceRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final DiFurnaceRecipes INSTANCE = new DiFurnaceRecipes();

    private DiFurnaceRecipes() {
        super(Constants.MOD_ID, "difurnace");
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

    /** 1.7 hid this one from NEI ({@code hiddenRecipes}); the JEI handler skips it. */
    public static final String HIDDEN_METEOR_SWORD = "difurnace.meteorSword";

    /** Matches both input orders and returns the exact count required from each physical slot. */
    public Match getMatch(ItemStack first, ItemStack second) {
        registerRecipes();
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe.inputItem == null || recipe.inputItem.length != 2) continue;
            AStack a = recipe.inputItem[0];
            AStack b = recipe.inputItem[1];
            if (a.matchesRecipe(first, false) && b.matchesRecipe(second, false)) {
                return new Match(recipe, a.stacksize, b.stacksize);
            }
            if (b.matchesRecipe(first, false) && a.matchesRecipe(second, false)) {
                return new Match(recipe, b.stacksize, a.stacksize);
            }
        }
        return null;
    }

    /** The legacy getRequiredCounts result, with counts aligned to slot 0 and slot 1. */
    public record Match(GenericRecipe recipe, int firstCount, int secondCount) {}

    /** Fuel entry used by the feature-local JEI category. */
    public record FuelEntry(AStack ingredient, int power) {}

    /** 1.7 {@code TileEntityDiFurnace.getItemPower}: the fuel value of {@code stack} (the data map), 0 for none. */
    public static int getItemPower(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        Integer power = DiFurnaceDataMaps.DIFURNACE_FUEL.get(stack.getItem());
        return power == null ? 0 : power;
    }

    public static List<ItemStack> getAlloyFuels() {
        List<ItemStack> result = new ArrayList<>();
        for (FuelEntry fuel : getFuelEntries()) result.addAll(fuel.ingredient().getDisplayStacks());
        return result;
    }

    /** Every fuel of the data map: the items it names, then its tags. */
    public static List<FuelEntry> getFuelEntries() {
        List<FuelEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : DiFurnaceDataMaps.DIFURNACE_FUEL.byId().entrySet()) {
            BuiltInRegistries.ITEM.getOptional(Ids.parse(entry.getKey()))
                    .ifPresent(item -> entries.add(new FuelEntry(new ComparableStack(item), entry.getValue())));
        }
        for (Map.Entry<String, Integer> entry : DiFurnaceDataMaps.DIFURNACE_FUEL.byTag()) {
            entries.add(new FuelEntry(new TagStack(TagKey.create(Registries.ITEM, Ids.parse(entry.getKey()))), entry.getValue()));
        }
        return entries;
    }
}
