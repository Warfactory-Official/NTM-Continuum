package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A machine whose recipes are named {@code GenericRecipe}s, e.g. {@code ass.plateiron}.
 *
 * <pre>
 * e.assembly.add('kubejs.gear', r => r.duration(60).power(100).inputs('2x #c:ingots/iron').outputs('hbm:gear'))
 * e.assembly.remove('ass.plateiron')
 * e.assembly.removeByOutput('hbm:plate_iron')
 * e.assembly.removeAll()
 * </pre>
 * Ids: {@code hbm:<machine>/<name>}, e.g. {@code hbm:assembly/ass.plateiron}.
 */
public class GenericHandler<B extends GenericBuilder> extends TableHandler {

    protected final GenericRecipes set;
    private final Supplier<B> builders;

    public GenericHandler(GenericRecipes set, Supplier<B> builders) {
        this.set = set;
        this.builders = builders;
    }

    public static GenericHandler<GenericBuilder> of(GenericRecipes set) {
        return new GenericHandler<>(set, GenericBuilder::new);
    }

    @Override
    protected Object registry() {
        return set;
    }

    @Override
    public List<RecipeView> views() {
        set.registerRecipes();
        return RecipeIds.views(machine(), List.copyOf(set.recipeOrderedList), GenericRecipe::getInternalName,
                GenericHandler::inputs, GenericHandler::outputs);
    }

    @Override
    protected void delete(RecipeView view) {
        set.remove(((GenericRecipe) view.recipe()).getInternalName());
    }

    @Override
    public void reset() {
        set.reset();
    }

    static List<String> inputs(GenericRecipe recipe) {
        List<String> out = new ArrayList<>();
        if (recipe.inputItem != null) for (var in : recipe.inputItem) out.add(RecipeIds.spec(in));
        if (recipe.inputFluid != null) for (var in : recipe.inputFluid) out.add(RecipeIds.spec(in));
        return out;
    }

    static List<String> outputs(GenericRecipe recipe) {
        List<String> out = new ArrayList<>();
        if (recipe.outputItem != null) for (var o : recipe.outputItem) out.add(RecipeIds.spec(o));
        if (recipe.outputFluid != null) for (var o : recipe.outputFluid) out.add(RecipeIds.spec(o));
        return out;
    }

    public void add(String name, Consumer<B> spec) {
        B builder = builders.get();
        spec.accept(builder);
        edit("add " + name, () -> set.registerGenerated(builder.build(name, set)));
    }

    public void remove(String name) {
        edit("remove " + name, () -> {
            if (!set.remove(name)) throw new IllegalArgumentException("no recipe named " + name);
        });
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            for (GenericRecipe recipe : List.copyOf(set.recipeOrderedList)) {
                if (recipe.outputItem == null) continue;
                for (ItemStack out : recipe.outputItem) {
                    if (out.is(target)) {
                        set.remove(recipe.getInternalName());
                        break;
                    }
                }
            }
        });
    }

    public void removeAll() {
        edit("removeAll", () ->
                List.copyOf(set.recipeOrderedList).forEach(recipe -> set.remove(recipe.getInternalName())));
    }
}
