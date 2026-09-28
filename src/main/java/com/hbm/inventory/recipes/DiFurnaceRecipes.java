package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The legacy, fuel-fired DiFurnace recipe set. This is deliberately separate from the modern airblast
 * {@link BlastFurnaceRecipesNT}: the old DiFurnace consumed one of two legacy alloy inputs and emitted
 * only its recipe result plus crafting remainders.
 */
public final class DiFurnaceRecipes extends GenericRecipes {

    public static final DiFurnaceRecipes INSTANCE = new DiFurnaceRecipes();

    private static final TagKey<Item> ANY_COKE = TagKey.create(Registries.ITEM,
            dev.golem.core.Ids.of("c", "cokes"));

    private static List<FuelEntry> fuelEntries;

    private DiFurnaceRecipes() {}

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        // These entries are the old BlastFurnaceRecipes.registerDefaults() entries whose item
        // dependencies exist in the current port. Missing old dependencies are listed in the
        // integration report rather than being replaced with a plausible modern item.
        register(recipe("difurnace.steelFromIron", shapes(Mats.MAT_IRON), shapes(Mats.MAT_COAL),
                new ItemStack(ModItems.ingot_steel.get())));
        register(recipe("difurnace.steelFromCoke", shapes(Mats.MAT_IRON), new TagStack(ANY_COKE),
                new ItemStack(ModItems.ingot_steel.get())));
        register(recipe("difurnace.steelFromOre", Mats.MAT_IRON.ore(), shapes(Mats.MAT_COAL),
                new ItemStack(ModItems.ingot_steel.get(), 2)));
        register(recipe("difurnace.steelFromOreCoke", Mats.MAT_IRON.ore(), new TagStack(ANY_COKE),
                new ItemStack(ModItems.ingot_steel.get(), 3)));
        register(recipe("difurnace.steelWithFlux", Mats.MAT_IRON.ore(),
                new ComparableStack(ModItems.powder_flux.get()), new ItemStack(ModItems.ingot_steel.get(), 3)));

        register(recipe("difurnace.redCopper", shapes(Mats.MAT_COPPER), shapes(Mats.MAT_REDSTONE),
                new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        register(recipe("difurnace.neutronReflectorCoal", shapes(Mats.MAT_TUNGSTEN), shapes(Mats.MAT_COAL),
                new ItemStack(ModItems.neutron_reflector.get(), 2)));
        register(recipe("difurnace.neutronReflectorCoke", shapes(Mats.MAT_TUNGSTEN), new TagStack(ANY_COKE),
                new ItemStack(ModItems.neutron_reflector.get(), 2)));
        register(recipe("difurnace.magnetizedTungsten", shapes(Mats.MAT_TUNGSTEN), Mats.MAT_SCHRABIDIUM.nugget(),
                new ItemStack(ModItems.ingot_magnetized_tungsten.get())));
        register(recipe("difurnace.tcalloy", shapes(Mats.MAT_STEEL), Mats.MAT_TECHNETIUM.nugget(),
                new ItemStack(ModItems.ingot_tcalloy.get())));
        register(recipe("difurnace.paa", Mats.MAT_GOLD.plate(), new ComparableStack(ModItems.plate_mixed.get()),
                new ItemStack(ModItems.plate_paa.get(), 2)));
    }

    private static GenericRecipe recipe(String id, AStack first, AStack second, ItemStack output) {
        return new GenericRecipe(id).setup(TileEntityDiFurnaceConstants.PROCESSING_SPEED, 0)
                .inputItems(first, second).outputItems(output);
    }

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

    /** Fuel entry used by both the machine and the feature-local JEI category. */
    public record FuelEntry(AStack ingredient, int power) {}

    public static int getItemPower(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        for (FuelEntry fuel : fuels()) {
            if (fuel.ingredient().matchesRecipe(stack, true)) return fuel.power();
        }
        return 0;
    }

    public static List<ItemStack> getAlloyFuels() {
        List<ItemStack> result = new ArrayList<>();
        for (FuelEntry fuel : fuels()) result.addAll(fuel.ingredient().getDisplayStacks());
        return result;
    }

    public static List<FuelEntry> getFuelEntries() {
        return List.copyOf(fuels());
    }

    private static List<FuelEntry> fuels() {
        if (fuelEntries != null) return fuelEntries;

        List<FuelEntry> entries = new ArrayList<>();
        entries.add(new FuelEntry(Mats.MAT_COAL.gem(), 200));
        entries.add(new FuelEntry(Mats.MAT_COAL.dust(), 220));
        entries.add(new FuelEntry(Mats.MAT_COAL.block(), 2_000));
        entries.add(new FuelEntry(Mats.MAT_LIGNITE.gem(), 150));
        entries.add(new FuelEntry(Mats.MAT_LIGNITE.dust(), 150));
        entries.add(new FuelEntry(Mats.MAT_LIGNITE.block(), 1_500));
        entries.add(new FuelEntry(new ComparableStack(Items.CHARCOAL), 200));
        entries.add(new FuelEntry(new TagStack(ANY_COKE), 400));
        entries.add(new FuelEntry(new ComparableStack(Items.LAVA_BUCKET), 12_800));
        entries.add(new FuelEntry(new ComparableStack(Items.BLAZE_ROD), 1_000));
        entries.add(new FuelEntry(new ComparableStack(Items.BLAZE_POWDER), 300));
        entries.add(new FuelEntry(new ComparableStack(ModFuelItems.coal_infernal.get()), 300));
        entries.add(new FuelEntry(new ComparableStack(ModFuelItems.solid_fuel.get()), 400));
        entries.add(new FuelEntry(new ComparableStack(ModFuelItems.solid_fuel_presto.get()), 800));
        entries.add(new FuelEntry(new ComparableStack(ModFuelItems.solid_fuel_presto_triplet.get()), 2_400));
        FinalConsumableMachineRecipes.diFurnaceFuels(entries); // briquettes, 200 each
        fuelEntries = entries;
        return entries;
    }

    /** Old DictFrame matching: ingot, plate, gem and dust are all equivalent inputs. */
    private static AStack shapes(com.hbm.inventory.material.NTMMaterial material) {
        return new AStack() {
            private final List<AStack> choices = List.of(
                    material.ingot(), material.plate(), material.gem(), material.dust());

            @Override
            public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
                for (AStack choice : choices) if (choice.matchesRecipe(stack, ignoreSize)) return true;
                return false;
            }

            @Override
            public List<ItemStack> getDisplayStacks() {
                List<ItemStack> display = new ArrayList<>();
                for (AStack choice : choices) display.addAll(choice.getDisplayStacks());
                return display;
            }
        };
    }

    /** Keeps the fixed processing constant local without making the recipe class depend on the TE. */
    private static final class TileEntityDiFurnaceConstants {
        private static final int PROCESSING_SPEED = 400;
    }
}
