package com.hbm.scripting.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.OverlayType;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <pre>
 * e.anvil.addSmithing(1, 'hbm:ingot_gunmetal', '#c:ingots/copper', '#c:ingots/aluminum')
 * e.anvil.addSmithing(1, 'hbm:ingot_gunmetal', '#c:ingots/copper', '#c:ingots/aluminum', true)   shapeless
 * e.anvil.removeSmithingByOutput('hbm:ingot_gunmetal')
 * e.anvil.removeSmithingAll()
 * e.anvil.addConstruction(r => r.inputs('4x #c:plates/steel', 'hbm:motor').outputs('hbm:machine_press', 'hbm:scrap')
 *       .chances(1, 0.25).tier(2).overlay('construction'))                     tierRange(lower, upper), upper -1 = open
 * e.anvil.removeConstructionByOutput('hbm:plate_iron')
 * e.anvil.removeConstructionByInput('minecraft:iron_ingot')
 * e.anvil.removeConstructionAll()
 * ids: hbm:anvil/smithing/&lt;left input&gt;, hbm:anvil/construction/&lt;first input&gt;
 * </pre>
 * Overlay defaults by shape: 1:1 smithing, n:1 construction, 1:n recycling, else none.
 */
public final class AnvilHandler extends TableHandler {

    @Override
    protected Object registry() {
        return AnvilRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        AnvilRecipes.register();
        List<Object> rows = new ArrayList<>(AnvilRecipes.getSmithing());
        rows.addAll(AnvilRecipes.getConstruction());
        return RecipeIds.views(machine(), rows, row -> {
            if (row instanceof AnvilSmithingRecipe smithing) return "smithing/" + RecipeIds.bare(RecipeIds.spec(AnvilRecipes.smithingInputs(smithing).get(0)));
            return "construction/" + RecipeIds.bare(RecipeIds.spec(((AnvilConstructionRecipe) row).input.get(0)));
        }, row -> {
            if (row instanceof AnvilSmithingRecipe smithing) return AnvilRecipes.smithingInputs(smithing).stream().map(RecipeIds::spec).toList();
            return ((AnvilConstructionRecipe) row).input.stream().map(RecipeIds::spec).toList();
        }, row -> {
            if (row instanceof AnvilSmithingRecipe smithing) return List.of(RecipeIds.spec(smithing.getSimpleOutput()));
            return ((AnvilConstructionRecipe) row).output.stream().map(out -> RecipeIds.spec(out.stack)).toList();
        });
    }

    @Override
    protected void delete(RecipeView view) {
        Object target = view.recipe();
        if (target instanceof AnvilSmithingRecipe) AnvilRecipes.removeSmithingIf(recipe -> recipe == target);
        else AnvilRecipes.removeConstructionIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        AnvilRecipes.reset();
    }

    public void addSmithing(int tier, Spec.Item output, Spec.Ingredient left, Spec.Ingredient right) {
        addSmithing(tier, output, left, right, false);
    }

    public void addSmithing(int tier, Spec.Item output, Spec.Ingredient left, Spec.Ingredient right, boolean shapeless) {
        edit("addSmithing " + output, () -> {
            AnvilSmithingRecipe recipe = new AnvilSmithingRecipe(tier, ScriptSpecs.stack(output),
                    ScriptSpecs.ingredient(left), ScriptSpecs.ingredient(right));
            if (shapeless) recipe.makeShapeless();
            AnvilRecipes.addSmithing(recipe);
        });
    }

    public void removeSmithingByOutput(Spec.Item item) {
        edit("removeSmithingByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            AnvilRecipes.removeSmithingIf(recipe -> recipe.getSimpleOutput().is(target));
        });
    }

    public void removeSmithingAll() {
        edit("removeSmithingAll", () -> AnvilRecipes.removeSmithingIf(recipe -> true));
    }

    public void addConstruction(Consumer<ConstructionBuilder> spec) {
        ConstructionBuilder builder = new ConstructionBuilder();
        spec.accept(builder);
        edit("addConstruction", () -> AnvilRecipes.addConstruction(builder.build()));
    }

    public void removeConstructionByOutput(Spec.Item item) {
        edit("removeConstructionByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            AnvilRecipes.removeConstructionIf(recipe -> {
                for (AnvilOutput out : recipe.output) if (out.stack.is(target)) return true;
                return false;
            });
        });
    }

    public void removeConstructionByInput(Spec.Item item) {
        edit("removeConstructionByInput " + item, () -> {
            ItemStack probe = new ItemStack(ScriptSpecs.item(item));
            AnvilRecipes.removeConstructionIf(recipe -> {
                for (AStack in : recipe.input) if (in.matchesRecipe(probe, true)) return true;
                return false;
            });
        });
    }

    public void removeConstructionAll() {
        edit("removeConstructionAll", () -> AnvilRecipes.removeConstructionIf(recipe -> true));
    }

    public static final class ConstructionBuilder {

        private Spec.Ingredient[] inputs = new Spec.Ingredient[0];
        private Spec.Item[] outputs = new Spec.Item[0];
        private float[] chances;
        private int tierLower;
        private int tierUpper = -1;
        private String overlay;

        public ConstructionBuilder inputs(Spec.Ingredient... specs) {
            this.inputs = specs;
            return this;
        }

        public ConstructionBuilder outputs(Spec.Item... specs) {
            this.outputs = specs;
            return this;
        }

        public ConstructionBuilder chances(float... chances) {
            this.chances = chances;
            return this;
        }

        public ConstructionBuilder tier(int tier) {
            this.tierLower = tier;
            return this;
        }

        public ConstructionBuilder tierRange(int lower, int upper) {
            this.tierLower = lower;
            this.tierUpper = upper;
            return this;
        }

        public ConstructionBuilder overlay(String type) {
            this.overlay = type;
            return this;
        }

        private AnvilConstructionRecipe build() {
            if (inputs.length == 0 || outputs.length == 0) throw new IllegalArgumentException("construction needs inputs and outputs");
            AStack[] in = ScriptSpecs.ingredients(inputs);
            AnvilOutput[] out = new AnvilOutput[outputs.length];
            for (int i = 0; i < outputs.length; i++) {
                ItemStack stack = ScriptSpecs.stack(outputs[i]);
                out[i] = chances != null && i < chances.length ? new AnvilOutput(stack, chances[i]) : new AnvilOutput(stack);
            }
            AnvilConstructionRecipe recipe;
            if (in.length == 1 && out.length == 1) recipe = new AnvilConstructionRecipe(in[0], out[0]);
            else if (out.length == 1) recipe = new AnvilConstructionRecipe(in, out[0]);
            else if (in.length == 1) recipe = new AnvilConstructionRecipe(in[0], out);
            else recipe = new AnvilConstructionRecipe(in, out);
            recipe.setTierRange(tierLower, tierUpper);
            if (overlay != null) recipe.setOverlay(ScriptSpecs.constant(OverlayType.class, overlay));
            return recipe;
        }
    }
}
