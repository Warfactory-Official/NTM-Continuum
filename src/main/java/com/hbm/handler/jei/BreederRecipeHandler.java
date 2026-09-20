package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.blocks.ModNuclearMiscBlocks;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the breeding reactor: rod in, bred rod out, and the flux the recipe needs. */
public final class BreederRecipeHandler extends JEIUniversalHandler<BreederRecipeHandler.Entry> {
    public static final JeiRecipeType<Entry> TYPE = JeiRecipeType.create("hbm", "breeder", Entry.class);

    public record Entry(ItemStack input, ItemStack output, int flux) {}

    public BreederRecipeHandler(IGuiHelper helper) {
        super(helper, TYPE, Component.translatable("hbm.breeder.name"), new ItemStack(ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get()));
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : BreederRecipes.getAllRecipes().entrySet()) {
            Item input = e.getKey();
            list.add(new Entry(new ItemStack(input), e.getValue().output().copy(), e.getValue().flux()));
        }
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(recipe.input()));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(List.of(recipe.output()));
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, slots, graphics, mouseX, mouseY);
        String flux = "Flux: " + recipe.flux();
        text(graphics, flux, 160 - textWidth(flux), 55);
    }
}
//?}
