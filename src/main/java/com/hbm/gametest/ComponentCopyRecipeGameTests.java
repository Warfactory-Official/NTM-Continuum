package com.hbm.gametest;

import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.inventory.recipes.ComponentCopyShapelessRecipe;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** Runtime contracts for the state-copying ZIRNOX loading recipe. */
public final class ComponentCopyRecipeGameTests {

    private ComponentCopyRecipeGameTests() {}

    private static ComponentCopyShapelessRecipe moxRecipe() {
        Ingredient empty = Ingredient.of(ModItems.rod_zirnox_empty.get());
        Ingredient billet = Ingredient.of(ModItems.billet_mox_fuel.get());
        return ComponentCopyShapelessRecipe.of(List.of(empty, billet, billet), empty,
                new ItemStack(ModZirnoxItems.active(ZirnoxFuel.MOX)));
    }

    public static void exactMultiset(GameTestHelper helper) {
        ComponentCopyShapelessRecipe recipe = moxRecipe();
        List<ItemStack> input = List.of(new ItemStack(ModItems.billet_mox_fuel.get()),
                new ItemStack(ModItems.rod_zirnox_empty.get()), new ItemStack(ModItems.billet_mox_fuel.get()),
                ItemStack.EMPTY);
        helper.assertTrue(recipe.matchesItemStacks(input, helper.getLevel()),
                "ZIRNOX loading must match the unordered empty-rod plus two-billet multiset");
        ItemStack result = recipe.assembleItemStacks(input);
        helper.assertTrue(result.is(ModZirnoxItems.active(ZirnoxFuel.MOX)) && result.getCount() == 1,
                "ZIRNOX loading must create one active MOX rod");
        helper.succeed();
    }

    public static void sourceStateAndFreshOutput(GameTestHelper helper) {
        ComponentCopyShapelessRecipe recipe = moxRecipe();
        ItemStack empty = new ItemStack(ModItems.rod_zirnox_empty.get());
        ModDataComponents.MATERIAL.set(empty, "carrier-state");
        ModDataComponents.zirnoxLife(empty, 12_345);
        ModDataComponents.wasteHot(empty, true);

        ItemStack result = recipe.assembleItemStacks(List.of(empty,
                new ItemStack(ModItems.billet_mox_fuel.get()), new ItemStack(ModItems.billet_mox_fuel.get())));
        helper.assertTrue("carrier-state".equals(ModDataComponents.MATERIAL.getOr(result, null)),
                "ZIRNOX loading must preserve unrelated source custom state");
        helper.assertTrue(!ModDataComponents.ZIRNOX_LIFE.has(result)
                        && !ModDataComponents.FUEL_WASTE_HOT.has(result)
                        && ((ItemZirnoxRod) result.getItem()).getLife(result) == 0,
                "ZIRNOX loading must clear stale life and hot-fuel state from the fresh rod");
        helper.succeed();
    }

    public static void rejectsWrongExtraAndAmbiguousInput(GameTestHelper helper) {
        ComponentCopyShapelessRecipe recipe = moxRecipe();
        ItemStack empty = new ItemStack(ModItems.rod_zirnox_empty.get());
        ItemStack mox = new ItemStack(ModItems.billet_mox_fuel.get());
        helper.assertFalse(recipe.matchesItemStacks(List.of(empty, mox,
                        new ItemStack(ModItems.billet_uranium.get())), helper.getLevel()),
                "A different billet must not satisfy the duplicate MOX ingredients");
        helper.assertFalse(recipe.matchesItemStacks(List.of(empty, mox, mox,
                        new ItemStack(Items.STICK)), helper.getLevel()),
                "An extra non-empty stack must reject the shapeless recipe");
        helper.assertFalse(recipe.matchesItemStacks(List.of(empty, new ItemStack(ModItems.rod_zirnox_empty.get()), mox),
                helper.getLevel()),
                "Two source stacks must reject an ambiguous copy source");
        helper.succeed();
    }
}
