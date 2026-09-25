package com.hbm.scripting.recipes;

import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.shredder.add('minecraft:stone', '2x minecraft:gravel')     input may be a #tag; replaces any entry
 * e.shredder.remove('minecraft:stone')                         the item then shreds to scrap
 * </pre>
 * Ids: {@code hbm:shredder/<input item>}, e.g. {@code hbm:shredder/minecraft.stone}.
 */
public final class ShredderHandler extends TableHandler {

    @Override
    protected Object registry() {
        return ShredderRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        List<Map.Entry<Item, ItemStack>> entries = ShredderRecipes.getRecipes().entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(entry -> RecipeIds.spec(entry.getKey())))
                .toList();
        return RecipeIds.views(machine(), entries,
                entry -> RecipeIds.spec(entry.getKey()),
                entry -> List.of(RecipeIds.spec(entry.getKey())),
                entry -> List.of(RecipeIds.spec(entry.getValue())));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        ShredderRecipes.remove(((Map.Entry<Item, ItemStack>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        ShredderRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Item output) {
        edit("add " + input, () -> {
            ItemStack out = ScriptSpecs.stack(output);
            for (Item item : ScriptSpecs.items(input)) ShredderRecipes.put(item, out.copy());
        });
    }

    public void remove(Spec.Ingredient input) {
        edit("remove " + input, () -> {
            for (Item item : ScriptSpecs.items(input)) ShredderRecipes.remove(item);
        });
    }
}
