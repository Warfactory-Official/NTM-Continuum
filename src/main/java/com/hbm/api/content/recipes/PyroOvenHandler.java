package com.hbm.api.content.recipes;

import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <pre>
 * e.pyroOven.add(r => r.duration(100).fluidInput('500x steam').input('#c:gems/coal').fluidOutput('1000x syngas'))
 * e.pyroOven.add(r => r.duration(60).fluidInput('250x diesel').output('hbm:solid_fuel'))
 * e.pyroOven.removeByInput('#c:gems/coal')
 * e.pyroOven.removeByFluidInput('diesel')
 * e.pyroOven.removeByOutput('hbm:solid_fuel')
 * e.pyroOven.removeByFluidOutput('syngas')
 * e.pyroOven.removeAll()
 * </pre>
 * Every input and output is optional; duration defaults to 100 ticks.
 * Ids: {@code hbm:pyro_oven/<item input, else fluid input>}, e.g. {@code hbm:pyro_oven/diesel}.
 */
public final class PyroOvenHandler extends TableHandler {

    @Override
    protected Object registry() {
        return PyroOvenRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(PyroOvenRecipes.getAll()),
                recipe -> RecipeIds.bare(RecipeIds.spec(recipe.inputItem != null ? recipe.inputItem : recipe.inputFluid)),
                recipe -> {
                    List<String> in = new ArrayList<>();
                    if (recipe.inputItem != null) in.add(RecipeIds.spec(recipe.inputItem));
                    if (recipe.inputFluid != null) in.add(RecipeIds.spec(recipe.inputFluid));
                    return in;
                },
                recipe -> {
                    List<String> out = new ArrayList<>();
                    if (recipe.outputItem != null) out.add(RecipeIds.spec(recipe.outputItem));
                    if (recipe.outputFluid != null) out.add(RecipeIds.spec(recipe.outputFluid));
                    return out;
                });
    }

    @Override
    protected void delete(RecipeView view) {
        PyroOvenRecipe target = (PyroOvenRecipe) view.recipe();
        PyroOvenRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        PyroOvenRecipes.reset();
    }

    public void add(Consumer<Builder> spec) {
        Builder builder = new Builder();
        spec.accept(builder);
        edit("add", () -> PyroOvenRecipes.addRecipe(builder.build()));
    }

    public void removeByInput(Spec.Ingredient input) {
        edit("removeByInput " + input, () -> {
            List<Item> items = ScriptSpecs.items(input);
            PyroOvenRecipes.removeIf(recipe -> {
                if (recipe.inputItem == null) return false;
                for (Item item : items) if (recipe.inputItem.matchesRecipe(new ItemStack(item), true)) return true;
                return false;
            });
        });
    }

    public void removeByFluidInput(Spec.Fluid fluid) {
        edit("removeByFluidInput " + fluid, () -> {
            FluidType type = ScriptSpecs.fluidType(fluid);
            PyroOvenRecipes.removeIf(recipe -> recipe.inputFluid != null && recipe.inputFluid.type == type);
        });
    }

    public void removeByOutput(Spec.Ingredient output) {
        edit("removeByOutput " + output, () -> {
            List<Item> items = ScriptSpecs.items(output);
            PyroOvenRecipes.removeIf(recipe -> {
                if (recipe.outputItem == null) return false;
                for (Item item : items) if (recipe.outputItem.is(item)) return true;
                return false;
            });
        });
    }

    public void removeByFluidOutput(Spec.Fluid fluid) {
        edit("removeByFluidOutput " + fluid, () -> {
            FluidType type = ScriptSpecs.fluidType(fluid);
            PyroOvenRecipes.removeIf(recipe -> recipe.outputFluid != null && recipe.outputFluid.type == type);
        });
    }

    public void removeAll() {
        edit("removeAll", () -> PyroOvenRecipes.removeIf(recipe -> true));
    }

    public static final class Builder {

        private int duration = 100;
        private Spec.Ingredient input;
        private Spec.Fluid fluidInput;
        private Spec.Item output;
        private Spec.Fluid fluidOutput;

        public Builder duration(int ticks) {
            this.duration = ticks;
            return this;
        }

        public Builder input(Spec.Ingredient spec) {
            this.input = spec;
            return this;
        }

        public Builder fluidInput(Spec.Fluid spec) {
            this.fluidInput = spec;
            return this;
        }

        public Builder output(Spec.Item spec) {
            this.output = spec;
            return this;
        }

        public Builder fluidOutput(Spec.Fluid spec) {
            this.fluidOutput = spec;
            return this;
        }

        PyroOvenRecipe build() {
            PyroOvenRecipe recipe = new PyroOvenRecipe(duration);
            if (input != null) recipe.in(ScriptSpecs.ingredient(input));
            if (fluidInput != null) recipe.in(ScriptSpecs.fluid(fluidInput));
            if (output != null) recipe.out(ScriptSpecs.stack(output));
            if (fluidOutput != null) recipe.out(ScriptSpecs.fluid(fluidOutput));
            return recipe;
        }
    }
}
