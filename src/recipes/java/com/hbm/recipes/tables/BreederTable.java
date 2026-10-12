package com.hbm.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.inventory.recipes.BreederRecipes.BreederRecipe;
import com.hbm.inventory.recipes.BreederRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code BreederRecipes.registerDefaults}. */
public final class BreederTable extends RecipeSource<Row> {

    public BreederTable() {
        super(BreederRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        setRecipe(BreedingRodType.LITHIUM, BreedingRodType.TRITIUM, 200);
        setRecipe(BreedingRodType.CO, BreedingRodType.CO60, 100);
        setRecipe(BreedingRodType.RA226, BreedingRodType.AC227, 300);
        setRecipe(BreedingRodType.TH232, BreedingRodType.THF, 500);
        setRecipe(BreedingRodType.U235, BreedingRodType.NP237, 300);
        setRecipe(BreedingRodType.NP237, BreedingRodType.PU238, 200);
        setRecipe(BreedingRodType.PU238, BreedingRodType.PU239, 1000);
        setRecipe(BreedingRodType.U238, BreedingRodType.RGP, 300);
        setRecipe(BreedingRodType.URANIUM, BreedingRodType.RGP, 200);
        setRecipe(BreedingRodType.RGP, BreedingRodType.WASTE, 200);

        put(new ComparableStack(ModAbilityToolItems.meteorite_sword_etched.get()), new BreederRecipe(new ItemStack(ModAbilityToolItems.meteorite_sword_bred.get()), 1000));
    }

    /** Sets recipes for single, dual, and quad rods **/
    private void setRecipe(BreedingRodType inputType, BreedingRodType outputType, int flux) {
        put(new ComparableStack(ModNuclearMiscItems.rod(RodSize.SINGLE, inputType)), new BreederRecipe(new ItemStack(ModNuclearMiscItems.rod(RodSize.SINGLE, outputType)), flux));
        put(new ComparableStack(ModNuclearMiscItems.rod(RodSize.DUAL, inputType)), new BreederRecipe(new ItemStack(ModNuclearMiscItems.rod(RodSize.DUAL, outputType)), flux * 2));
        put(new ComparableStack(ModNuclearMiscItems.rod(RodSize.QUAD, inputType)), new BreederRecipe(new ItemStack(ModNuclearMiscItems.rod(RodSize.QUAD, outputType)), flux * 3));
    }

    /** 1.7 {@code recipes.put(input, recipe)}; the id is the input's item name. */
    private void put(ComparableStack input, BreederRecipe recipe) {
        register(BuiltInRegistries.ITEM.getKey(input.item).getPath(),
                new Row(input.item, ItemSpec.of(recipe.output()).getOrThrow(), recipe.flux(), nextOrder()));
    }
}
