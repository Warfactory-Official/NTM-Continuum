package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.blocks.ModNuclearMiscBlocks;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the radiolysis chamber: {input 100} -> {left, right (unless NONE)}, the old NEI strip. */
public final class RadiolysisRecipeHandler extends JEIUniversalHandler<RadiolysisRecipeHandler.Entry> {
    public static final JeiRecipeType<Entry> TYPE = JeiRecipeType.create("hbm", "radiolysis", Entry.class);

    public record Entry(FluidType input, RadiolysisRecipes.RadiolysisRecipe recipe) {}

    public RadiolysisRecipeHandler(IGuiHelper helper) {
        super(helper, TYPE, Component.translatable("hbm.radiolysis.name"), new ItemStack(ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get()));
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : RadiolysisRecipes.getAll().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(new FluidStack(recipe.input, 100))));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        List<List<ItemStack>> out = new ArrayList<>();
        out.add(List.of(ItemFluidIcon.make(recipe.recipe.left())));
        if (recipe.recipe.right().type != Fluids.NONE) out.add(List.of(ItemFluidIcon.make(recipe.recipe.right())));
        return out;
    }
}
//?}
