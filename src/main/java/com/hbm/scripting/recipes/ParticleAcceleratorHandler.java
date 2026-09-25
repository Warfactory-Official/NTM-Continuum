package com.hbm.scripting.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes.ParticleAcceleratorRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * e.particleAccelerator.add('hbm:particle_hydrogen', 'hbm:particle_lead', 6500, 'hbm:particle_higgs')
 * e.particleAccelerator.add('hbm:particle_strange', 'hbm:powder_magic', 12500, 'hbm:particle_sparkticle', 'hbm:dust')
 * e.particleAccelerator.removeByInput('hbm:particle_amat')          either particle
 * e.particleAccelerator.removeByOutput('hbm:particle_dark')         either output
 * e.particleAccelerator.removeAll()
 * </pre>
 * Ids: {@code hbm:particle_accelerator/<input1>/<input2>}, e.g. {@code hbm:particle_accelerator/hbm.particle_amat/hbm.particle_amat}.
 */
public final class ParticleAcceleratorHandler extends TableHandler {

    @Override
    protected Object registry() {
        return ParticleAcceleratorRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        ParticleAcceleratorRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(ParticleAcceleratorRecipes.recipes),
                recipe -> RecipeIds.bare(RecipeIds.spec(recipe.input1)) + "/" + RecipeIds.bare(RecipeIds.spec(recipe.input2)),
                recipe -> List.of(RecipeIds.spec(recipe.input1), RecipeIds.spec(recipe.input2)),
                recipe -> {
                    List<String> out = new ArrayList<>();
                    out.add(RecipeIds.spec(recipe.output1));
                    if (recipe.output2 != null) out.add(RecipeIds.spec(recipe.output2));
                    return out;
                });
    }

    @Override
    protected void delete(RecipeView view) {
        ParticleAcceleratorRecipe target = (ParticleAcceleratorRecipe) view.recipe();
        ParticleAcceleratorRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        ParticleAcceleratorRecipes.reset();
    }

    public void add(Spec.Ingredient input1, Spec.Ingredient input2, int momentum, Spec.Item output) {
        add(input1, input2, momentum, output, null);
    }

    public void add(Spec.Ingredient input1, Spec.Ingredient input2, int momentum, Spec.Item output1, Spec.Item output2) {
        edit("add " + output1, () ->
                ParticleAcceleratorRecipes.addRecipe(ScriptSpecs.ingredient(input1), ScriptSpecs.ingredient(input2),
                        momentum, ScriptSpecs.stack(output1), output2 == null ? null : ScriptSpecs.stack(output2)));
    }

    public void removeByInput(Spec.Ingredient input) {
        edit("removeByInput " + input, () -> {
            List<Item> items = ScriptSpecs.items(input);
            ParticleAcceleratorRecipes.removeIf(recipe -> matches(recipe.input1, items) || matches(recipe.input2, items));
        });
    }

    public void removeByOutput(Spec.Ingredient output) {
        edit("removeByOutput " + output, () -> {
            List<Item> items = ScriptSpecs.items(output);
            ParticleAcceleratorRecipes.removeIf(recipe -> is(recipe.output1, items) || is(recipe.output2, items));
        });
    }

    public void removeAll() {
        edit("removeAll", () -> ParticleAcceleratorRecipes.removeIf(recipe -> true));
    }

    private static boolean matches(AStack stack, List<Item> items) {
        for (Item item : items) if (stack.matchesRecipe(new ItemStack(item), true)) return true;
        return false;
    }

    private static boolean is(ItemStack stack, List<Item> items) {
        if (stack == null) return false;
        for (Item item : items) if (stack.is(item)) return true;
        return false;
    }
}
