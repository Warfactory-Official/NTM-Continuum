package com.hbm.scripting.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.liquefactor.add('minecraft:sugar', '100x ethanol')      one item consumed per operation
 * e.liquefactor.add('#c:dusts/coal', '100x coaloil')
 * e.liquefactor.remove('minecraft:ice')
 * e.liquefactor.remove('#minecraft:logs')
 * e.liquefactor.removeById('hbm:liquefactor/tag.minecraft.logs')   id = input item or tag
 * </pre>
 * Item entries beat tag entries; a new tag ranks after the built-in ones.
 */
public final class LiquefactionHandler extends TableHandler {

    @Override
    protected Object registry() {
        return LiquefactionRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        List<Map.Entry<?, ?>> rows = new ArrayList<>(LiquefactionRecipes.getItemRecipes().entrySet());
        rows.addAll(LiquefactionRecipes.getTagRecipes().entrySet());
        return RecipeIds.views(machine(), rows, e -> input(e.getKey()), e -> List.of(input(e.getKey())),
                e -> List.of(RecipeIds.spec(e.getValue())));
    }

    private static String input(Object key) {
        return key instanceof TagKey<?> tag ? "#" + tag.location() : RecipeIds.spec(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        Object key = ((Map.Entry<?, ?>) view.recipe()).getKey();
        if (key instanceof Item item) LiquefactionRecipes.remove(item);
        else LiquefactionRecipes.remove((TagKey<Item>) key);
    }

    @Override
    public void reset() {
        LiquefactionRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Fluid output) {
        edit("add " + input, () -> {
            AStack in = ScriptSpecs.ingredient(input);
            if (in instanceof TagStack tag) LiquefactionRecipes.put(tag.tag, ScriptSpecs.fluid(output));
            else LiquefactionRecipes.put(item(in), ScriptSpecs.fluid(output));
        });
    }

    public void remove(Spec.Ingredient input) {
        edit("remove " + input, () -> {
            AStack in = ScriptSpecs.ingredient(input);
            if (in instanceof TagStack tag) LiquefactionRecipes.remove(tag.tag);
            else LiquefactionRecipes.remove(item(in));
        });
    }

    private static Item item(AStack stack) {
        if (stack instanceof ComparableStack comparable) return comparable.item;
        throw new IllegalArgumentException("liquefaction takes an item or a tag, not " + stack);
    }
}
