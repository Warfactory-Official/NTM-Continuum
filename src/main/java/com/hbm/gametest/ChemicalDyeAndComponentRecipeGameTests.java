package com.hbm.gametest;

import com.hbm.blocks.material.MaterialCompletionRegistrations;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.items.ModChemicalDyeItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.food.ItemCrayon;
import com.hbm.items.machine.ItemChemicalDye;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Contracts for the flattened dye families and component-sensitive shaped recipes. */
public final class ChemicalDyeAndComponentRecipeGameTests {

    private ChemicalDyeAndComponentRecipeGameTests() {}

    private static final int[] COLORS = {
            1973019, 11743532, 3887386, 5320730, 2437522, 8073150, 2651799, 11250603,
            4408131, 14188952, 4312372, 14602026, 6719955, 12801229, 15435844, 15790320,
    };

    public static void dyeFamilies(GameTestHelper helper) {
        helper.assertTrue(ItemChemicalDye.EnumChemDye.VALUES.length == 16,
                "chemical dye enum no longer has 16 legacy variants");
        helper.assertTrue(ModChemicalDyeItems.registeredCount() == 32,
                "chemical dye and crayon registry count changed");
        for (int i = 0; i < COLORS.length; i++) {
            ItemChemicalDye.EnumChemDye dye = ItemChemicalDye.EnumChemDye.VALUES[i];
            helper.assertTrue(dye.color == COLORS[i], "chemical dye tint changed at " + i);
            ItemChemicalDye chemical = ModChemicalDyeItems.chemicalDye(dye);
            ItemCrayon crayon = ModChemicalDyeItems.crayon(dye);
            helper.assertTrue(chemical.getDescriptionId().equals("item.hbm.chemical_dye_" + dye.translationName())
                            && crayon.getDescriptionId().equals("item.hbm.crayon_" + dye.translationName()),
                    "flattened dye registry id changed at " + i);
            ItemStack food = new ItemStack(crayon);
            helper.assertTrue(ItemStacks.foodProperties(food) != null
                            && ItemStacks.foodNutrition(ItemStacks.foodProperties(food)) == 3,
                    "crayon food properties changed at " + i);
        }
        helper.succeed();
    }

    public static void dyeCrystallizerRecipes(GameTestHelper helper) {
        CrystallizerRecipes.register();
        int matches = 0;
        for (CrystallizerRecipes.CrystallizerRecipe recipe : CrystallizerRecipes.recipes) {
            if (!(recipe.output.getItem() instanceof ItemChemicalDye)) continue;
            matches++;
            helper.assertTrue(recipe.output.getCount() == 4 && recipe.duration == 20
                            && recipe.acidAmount == 100 && recipe.productivity == 0.15F,
                    "chemical dye crystallizer constants changed");
        }
        helper.assertTrue(matches == 18, "expected 18 chemical dye crystallizer recipes, found " + matches);
        helper.succeed();
    }

    public static void componentShapedRecipes(GameTestHelper helper) {
        List<ItemStack> white = gridWithConcrete(0);
        ItemStack whiteStairs = Recipes.craftingResult(helper.getLevel(), 3, 3, white).orElse(ItemStack.EMPTY);
        helper.assertTrue(whiteStairs.is(MaterialCompletionRegistrations.CONCRETE_COLORED_STAIRS.get(0).get().asItem())
                        && whiteStairs.getCount() == 8,
                "component-shaped white concrete stair recipe did not assemble");

        List<ItemStack> orange = gridWithConcrete(1);
        ItemStack orangeStairs = Recipes.craftingResult(helper.getLevel(), 3, 3, orange).orElse(ItemStack.EMPTY);
        helper.assertTrue(orangeStairs.is(MaterialCompletionRegistrations.CONCRETE_COLORED_STAIRS.get(1).get().asItem())
                        && orangeStairs.getCount() == 8,
                "component-shaped recipe did not select the exact orange component");

        List<ItemStack> mixed = gridWithConcrete(0);
        mixed.set(8, MaterialCompletionRegistrations.concreteColoredItem(1));
        helper.assertTrue(Recipes.craftingResult(helper.getLevel(), 3, 3, mixed).isEmpty(),
                "component-shaped recipe accepted a mixed component input");

        List<ItemStack> lightstone = List.of(
                MaterialCompletionRegistrations.lightstoneItem(0),
                MaterialCompletionRegistrations.lightstoneItem(0),
                MaterialCompletionRegistrations.lightstoneItem(0),
                MaterialCompletionRegistrations.lightstoneItem(0));
        ItemStack tile = Recipes.craftingResult(helper.getLevel(), 2, 2, lightstone).orElse(ItemStack.EMPTY);
        helper.assertTrue(tile.is(MaterialCompletionRegistrations.LIGHTSTONE_ITEM.get().asItem())
                        && ModDataComponents.ITEM_VARIANT.getOr(tile, -1) == 1 && tile.getCount() == 4,
                "component-shaped lightstone tile recipe did not preserve its result component");
        helper.succeed();
    }

    private static List<ItemStack> gridWithConcrete(int variant) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
        int[] occupied = {0, 3, 4, 6, 7, 8};
        for (int slot : occupied) grid.set(slot, MaterialCompletionRegistrations.concreteColoredItem(variant));
        return grid;
    }
}
