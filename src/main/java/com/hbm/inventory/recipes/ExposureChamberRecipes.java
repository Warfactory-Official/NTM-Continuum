package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.RecipeIngredients;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * The exposure chamber (1.7 {@code ExposureChamberRecipes}): the datapack table {@code hbm:exposure_chamber}, one file
 * per row in {@code data/hbm/recipe/exposure_chamber/}, converted once from the former Java table and the source now.
 * A row is one cycle as 1.7 stated it: input 0 the particle, input 1 the ingredient, one output (ntm-next's
 * {@code ExposureChamberRecipe}); the schrabidate row's {@code input_items_expensive} is 1.7's expensive-mode branch.
 *
 * <p>The machine reads {@link #recipes}; the recipe lists show each row as 1.7's NEI handler did, with eight
 * ingredients and outputs ({@code getRecipes}: one capsule is good for {@link TileEntityMachineExposureChamber#MAX_PARTICLES}).
 */
public final class ExposureChamberRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final ExposureChamberRecipes INSTANCE = new ExposureChamberRecipes();
    /** One entry per row, one cycle each; rebuilt with the table. */
    public static final List<ExposureChamberRecipe> recipes = new ArrayList<>();

    private ExposureChamberRecipes() {
        super(Constants.MOD_ID, "exposure_chamber");
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

    @Override
    protected void arrange(List<GenericRecipe> rows) {
        List<ExposureChamberRecipe> cycles = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            GenericRecipe row = rows.get(index);
            if (row.inputItem == null || row.inputItem.length < 2 || row.outputItem == null || row.outputItem.length == 0) continue;
            AStack particle = row.inputItem[0];
            AStack ingredient = row.inputItem[1];
            ItemStack output = row.outputItem[0];
            cycles.add(new ExposureChamberRecipe(particle, ingredient, output.copy()));
            int shown = TileEntityMachineExposureChamber.MAX_PARTICLES;
            rows.set(index, new GenericRecipe(row.getInternalName()).setup(row.duration, row.power)
                    .inputItems(particle, RecipeIngredients.withCount(ingredient, ingredient.stacksize * shown))
                    .outputItems(ItemStacks.copyWithCount(output, output.getCount() * shown)));
        }
        synchronized (recipes) {
            recipes.clear();
            recipes.addAll(cycles);
        }
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
        public final AStack particle;
        public final AStack ingredient;
        public final ItemStack output;

        private ExposureChamberRecipe(AStack particle, AStack ingredient, ItemStack output) {
            this.particle = particle;
            this.ingredient = ingredient;
            this.output = output;
        }
    }
}
