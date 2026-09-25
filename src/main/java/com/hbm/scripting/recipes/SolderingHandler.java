package com.hbm.scripting.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.SolderingRecipes.SolderingRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <pre>
 * e.soldering.add('hbm:upgrade_speed_1', r => r.duration(200).power(1000)
 *       .toppings('4x hbm:circuit_vacuum_tube', 'hbm:circuit_capacitor').pcb('hbm:upgrade_template', '4x #c:dusts/mingrade')
 *       .solder('4x #c:wires/lead').fluid('1000x sulfuric_acid'))
 * e.soldering.removeByInput('hbm:upgrade_template')
 * e.soldering.removeByOutput('hbm:upgrade_speed_1')
 * e.soldering.removeAll()
 * </pre>
 * toppings at most 3, pcb at most 2, solder at most 1; power in HE per tick; duration defaults to 100 ticks.
 * Ids: {@code hbm:soldering/<output>}, e.g. {@code hbm:soldering/hbm.upgrade_speed_1}.
 */
public final class SolderingHandler extends TableHandler {

    @Override
    protected Object registry() {
        return SolderingRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        SolderingRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(SolderingRecipes.recipes),
                recipe -> RecipeIds.bare(RecipeIds.spec(recipe.output)),
                recipe -> {
                    List<String> in = new ArrayList<>();
                    for (AStack stack : recipe.toppings) in.add(RecipeIds.spec(stack));
                    for (AStack stack : recipe.pcb) in.add(RecipeIds.spec(stack));
                    for (AStack stack : recipe.solder) in.add(RecipeIds.spec(stack));
                    if (recipe.fluid != null) in.add(RecipeIds.spec(recipe.fluid));
                    return in;
                },
                recipe -> List.of(RecipeIds.spec(recipe.output)));
    }

    @Override
    protected void delete(RecipeView view) {
        SolderingRecipe target = (SolderingRecipe) view.recipe();
        SolderingRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        SolderingRecipes.reset();
    }

    public void add(Spec.Item output, Consumer<Builder> spec) {
        Builder builder = new Builder();
        spec.accept(builder);
        if (builder.toppings.length > 3 || builder.pcb.length > 2 || builder.solder.length > 1) {
            throw new IllegalArgumentException("soldering takes at most 3 toppings, 2 pcb and 1 solder");
        }
        edit("add " + output, () ->
                SolderingRecipes.addRecipe(ScriptSpecs.stack(output), builder.duration, builder.power,
                        builder.fluid == null ? null : ScriptSpecs.fluid(builder.fluid),
                        ScriptSpecs.ingredients(builder.toppings), ScriptSpecs.ingredients(builder.pcb),
                        ScriptSpecs.ingredients(builder.solder)));
    }

    public void removeByInput(Spec.Ingredient input) {
        edit("removeByInput " + input, () -> {
            List<Item> items = ScriptSpecs.items(input);
            SolderingRecipes.removeIf(recipe ->
                    matches(recipe.toppings, items) || matches(recipe.pcb, items) || matches(recipe.solder, items));
        });
    }

    public void removeByOutput(Spec.Ingredient output) {
        edit("removeByOutput " + output, () -> {
            List<Item> items = ScriptSpecs.items(output);
            SolderingRecipes.removeIf(recipe -> items.contains(recipe.output.getItem()));
        });
    }

    public void removeAll() {
        edit("removeAll", () -> SolderingRecipes.removeIf(recipe -> true));
    }

    private static boolean matches(AStack[] stacks, List<Item> items) {
        for (AStack stack : stacks) {
            for (Item item : items) if (stack.matchesRecipe(new ItemStack(item), true)) return true;
        }
        return false;
    }

    public static final class Builder {

        private int duration = 100;
        private long power;
        private Spec.Fluid fluid;
        private Spec.Ingredient[] toppings = new Spec.Ingredient[0];
        private Spec.Ingredient[] pcb = new Spec.Ingredient[0];
        private Spec.Ingredient[] solder = new Spec.Ingredient[0];

        public Builder duration(int ticks) {
            this.duration = ticks;
            return this;
        }

        public Builder power(long perTick) {
            this.power = perTick;
            return this;
        }

        public Builder fluid(Spec.Fluid spec) {
            this.fluid = spec;
            return this;
        }

        public Builder toppings(Spec.Ingredient... specs) {
            this.toppings = specs;
            return this;
        }

        public Builder pcb(Spec.Ingredient... specs) {
            this.pcb = specs;
            return this;
        }

        public Builder solder(Spec.Ingredient... specs) {
            this.solder = specs;
            return this;
        }
    }
}
