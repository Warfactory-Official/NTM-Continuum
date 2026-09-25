package com.hbm.inventory.recipes;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.PU;
import static com.hbm.inventory.OreDictManager.SBD;
import static com.hbm.inventory.OreDictManager.U;
import static com.hbm.inventory.OreDictManager.U238;

/** The eight-cycle particle conversion table from the 1.12.2 ExposureChamberRecipes. */
public final class ExposureChamberRecipes extends GenericRecipes {

    public static final ExposureChamberRecipes INSTANCE = new ExposureChamberRecipes();
    public static final List<ExposureChamberRecipe> recipes = new ArrayList<>();

    private ExposureChamberRecipes() {}

    @Override
    protected void populate() {
        add("exposure.uranium", new ComparableStack(ModItems.particle_higgs.get()), U.ingot(),
                new ItemStack(ModFuelItems.ingot_schraranium.get()));
        add("exposure.uranium238", new ComparableStack(ModItems.particle_higgs.get()), U238.ingot(),
                new ItemStack(ModItems.ingot_schrabidium.get()));
        add("exposure.plutonium", new ComparableStack(ModItems.particle_dark.get()), PU.ingot(),
                new ItemStack(ModItems.ingot_euphemium.get()));

        AStack finalIngredient = GeneralConfig.enableExpensiveMode
                ? new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.DEGENERATE_MATTER))
                : SBD.ingot();
        add("exposure.schrabidate", new ComparableStack(ModItems.particle_sparkticle.get()), finalIngredient,
                new ItemStack(ModItems.ingot_dineutronium.get()));
    }

    private void add(String name, AStack particle, AStack ingredient, ItemStack output) {
        ExposureChamberRecipe recipe = new ExposureChamberRecipe(name, particle, ingredient, output.copy());
        recipes.add(recipe);

        // The old JEI adapter displayed one catalyst and eight ingredient/output units, while the
        // machine consumes and produces one unit per cycle.
        AStack displayIngredient = GeneralConfig.enableExpensiveMode
                && name.equals("exposure.schrabidate")
                ? new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.DEGENERATE_MATTER), 8)
                : ingredient instanceof com.hbm.inventory.RecipesCommon.TagStack tag
                ? new com.hbm.inventory.RecipesCommon.TagStack(tag.tag, 8)
                : new ComparableStack(((ComparableStack) ingredient).item, 8);
        register(new GenericRecipe(name).setup(TileEntityMachineExposureChamber.PROCESS_TIME_BASE,
                        TileEntityMachineExposureChamber.CONSUMPTION_BASE)
                .inputItems(particle, displayIngredient)
                .outputItems(new ItemStack(output.getItem(), TileEntityMachineExposureChamber.MAX_PARTICLES)));
    }

    public static void put(String name, AStack particle, AStack ingredient, ItemStack output) {
        INSTANCE.registerRecipes();
        INSTANCE.add(name, particle, ingredient, output);
    }

    @Override
    public boolean remove(String name) {
        recipes.removeIf(recipe -> recipe.name.equals(name));
        return super.remove(name);
    }

    @Override
    protected void clear() {
        super.clear();
        recipes.clear();
    }

    public static ExposureChamberRecipe getRecipe(ItemStack particle, ItemStack ingredient) {
        INSTANCE.registerRecipes();
        for (ExposureChamberRecipe recipe : recipes) {
            if (recipe.particle.matchesRecipe(particle, true) && recipe.ingredient.matchesRecipe(ingredient, true)) {
                return recipe;
            }
        }
        return null;
    }

    public static final class ExposureChamberRecipe {
        public final String name;
        public final AStack particle;
        public final AStack ingredient;
        public final ItemStack output;

        private ExposureChamberRecipe(String name, AStack particle, AStack ingredient, ItemStack output) {
            this.name = name;
            this.particle = particle;
            this.ingredient = ingredient;
            this.output = output;
        }
    }
}
