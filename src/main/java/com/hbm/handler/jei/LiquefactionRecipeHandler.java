package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the liquefactor — input item(s) in, output droplet out. */
public class LiquefactionRecipeHandler extends JEIUniversalHandler<LiquefactionRecipeHandler.Entry> {

    public record Entry(List<ItemStack> input, FluidStack output) {}

    public LiquefactionRecipeHandler(IGuiHelper helper, IRecipeType<Entry> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : LiquefactionRecipes.getItemRecipes().entrySet()) {
            list.add(new Entry(List.of(new ItemStack(e.getKey())), e.getValue()));
        }
        for (var e : LiquefactionRecipes.getTagRecipes().entrySet()) {
            List<ItemStack> stacks = itemsOfTag(e.getKey());
            if (!stacks.isEmpty()) list.add(new Entry(stacks, e.getValue()));
        }
        return list;
    }

    private static List<ItemStack> itemsOfTag(TagKey<Item> tag) {
        List<ItemStack> stacks = new ArrayList<>();
        BuiltInRegistries.ITEM.get(tag).ifPresent(named -> named.forEach(h -> stacks.add(new ItemStack(h))));
        return stacks;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(recipe.input);
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(recipe.output.type)));
    }
}
