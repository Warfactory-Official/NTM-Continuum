package com.hbm.api.content.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.SILEXRecipes.FluidIconStack;
import com.hbm.inventory.recipes.SILEXRecipes.SILEXRecipe;
import com.hbm.inventory.recipes.SILEXRecipes.WeightedOutput;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * <pre>
 * e.silex.add('hbm:ingot_uranium', r => r.produced(900).consumed(100).laser('visible')
 *       .output('hbm:nugget_u235', 1).output('hbm:nugget_u238', 11))
 * e.silex.addFluid('uf6', r => r.produced(900).consumed(100).laser('visible').output('hbm:nugget_u235', 1))
 * e.silex.removeByInput('hbm:ingot_uranium')
 * e.silex.removeByFluid('uf6')
 * e.silex.removeByOutput('hbm:nugget_u235')
 * e.silex.removeAll()
 * </pre>
 * produced: mB of solution per input item; consumed: mB per output roll; laser: minimum wavelength
 * (ir, visible, uv, gamma, drx); defaults 900/100/visible.
 * Ids: {@code hbm:silex/<input item or fluid>}, e.g. {@code hbm:silex/hbm.ingot_uranium}, {@code hbm:silex/uf6}.
 */
public final class SILEXHandler extends TableHandler {

    @Override
    protected Object registry() {
        return SILEXRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        SILEXRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(SILEXRecipes.recipes),
                recipe -> RecipeIds.bare(input(recipe.input)),
                recipe -> List.of(input(recipe.input)),
                recipe -> {
                    List<String> out = new ArrayList<>();
                    for (WeightedOutput o : recipe.outputs) out.add(RecipeIds.spec(o.stack()));
                    return out;
                });
    }

    private static String input(AStack input) {
        return input instanceof FluidIconStack icon ? RecipeIds.spec(icon.fluid) : RecipeIds.spec(input);
    }

    @Override
    protected void delete(RecipeView view) {
        SILEXRecipe target = (SILEXRecipe) view.recipe();
        SILEXRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        SILEXRecipes.reset();
    }

    public void add(Spec.Ingredient input, Consumer<Builder> spec) {
        enqueue("add " + input, () -> ScriptSpecs.ingredient(input), spec);
    }

    public void addFluid(Spec.Fluid fluid, Consumer<Builder> spec) {
        enqueue("addFluid " + fluid, () -> new FluidIconStack(ScriptSpecs.fluidType(fluid)), spec);
    }

    private void enqueue(String description, Supplier<AStack> input, Consumer<Builder> spec) {
        Builder builder = new Builder();
        spec.accept(builder);
        edit(description, () -> {
            List<WeightedOutput> outputs = new ArrayList<>();
            for (int i = 0; i < builder.outputs.size(); i++) {
                outputs.add(new WeightedOutput(ScriptSpecs.stack(builder.outputs.get(i)), builder.weights.get(i)));
            }
            SILEXRecipes.addRecipe(input.get(), builder.produced, builder.consumed,
                    ScriptSpecs.constant(EnumWavelengths.class, builder.laser), outputs);
        });
    }

    public void removeByInput(Spec.Ingredient input) {
        edit("removeByInput " + input, () -> {
            List<Item> items = ScriptSpecs.items(input);
            SILEXRecipes.removeIf(recipe -> {
                for (Item item : items) if (recipe.input.matchesRecipe(new ItemStack(item), true)) return true;
                return false;
            });
        });
    }

    public void removeByFluid(Spec.Fluid fluid) {
        edit("removeByFluid " + fluid, () -> {
            FluidType type = ScriptSpecs.fluidType(fluid);
            SILEXRecipes.removeIf(recipe -> recipe.input instanceof FluidIconStack icon && icon.fluid == type);
        });
    }

    public void removeByOutput(Spec.Ingredient output) {
        edit("removeByOutput " + output, () -> {
            List<Item> items = ScriptSpecs.items(output);
            SILEXRecipes.removeIf(recipe -> {
                for (WeightedOutput out : recipe.outputs) {
                    for (Item item : items) if (out.stack().is(item)) return true;
                }
                return false;
            });
        });
    }

    public void removeAll() {
        edit("removeAll", () -> SILEXRecipes.removeIf(recipe -> true));
    }

    public static final class Builder {

        private int produced = 900;
        private int consumed = 100;
        private String laser = "visible";
        private final List<Spec.Item> outputs = new ArrayList<>();
        private final List<Integer> weights = new ArrayList<>();

        public Builder produced(int mB) {
            this.produced = mB;
            return this;
        }

        public Builder consumed(int mB) {
            this.consumed = mB;
            return this;
        }

        public Builder laser(String wavelength) {
            this.laser = wavelength;
            return this;
        }

        public Builder output(Spec.Item spec, int weight) {
            outputs.add(spec);
            weights.add(weight);
            return this;
        }
    }
}
