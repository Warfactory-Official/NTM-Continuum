package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModMachineItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import dev.golem.core.jei.JeiRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI category for the shredder — a faithful port of the 1.12.2 {@code ShredderRecipeHandler}: its own
 * {@code gui_nei_shredder.png} background, input at {@code (39,24)} → output at {@code (129,24)}, the two
 * blade slots above/below the centre at {@code (84,6)}/{@code (84,42)} cycling the blade items, the
 * animated power bar {@code (36,86,16,52)} at {@code (3,6)} and grind progress bar {@code (100,118,24,16)}
 * at {@code (80,23)}. The universal scrap fallback is not enumerated (it would list every item).
 */
public class ShredderRecipeHandler extends HbmRecipeCategory<ShredderRecipeHandler.ShredderPair> {

    /** One shredder recipe row (input item → output stack). */
    public record ShredderPair(ItemStack input, ItemStack output) {}

    private final IDrawableAnimated powerBar;
    private final IDrawableAnimated progressBar;

    public ShredderRecipeHandler(IGuiHelper helper, JeiRecipeType<ShredderPair> type, Component title, ItemStack icon) {
        super(helper, type, title, icon, "gui_nei_shredder.png");
        IDrawableStatic power = helper.createDrawable(tex("gui_nei_shredder.png"), 36, 86, 16, 52);
        this.powerBar = helper.createAnimatedDrawable(power, 480, IDrawableAnimated.StartDirection.TOP, true);
        IDrawableStatic progress = helper.createDrawable(tex("gui_nei_shredder.png"), 100, 118, 24, 16);
        this.progressBar = helper.createAnimatedDrawable(progress, 48, IDrawableAnimated.StartDirection.LEFT, false);
    }

    public static List<ShredderPair> buildRecipes() {
        List<ShredderPair> out = new ArrayList<>();
        for (Map.Entry<Item, ItemStack> e : ShredderRecipes.getRecipes().entrySet()) {
            ItemStack in = new ItemStack(e.getKey());
            if (!in.isEmpty() && e.getValue() != null && !e.getValue().isEmpty()) {
                out.add(new ShredderPair(in, e.getValue().copy()));
            }
        }
        return out;
    }

    private static List<ItemStack> blades() {
        return List.of(
                new ItemStack(ModMachineItems.blades_steel.get()),
                new ItemStack(ModMachineItems.blades_titanium.get()),
                new ItemStack(ModMachineItems.blades_desh.get()));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ShredderPair recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 39, 24).addItemStacks(List.of(recipe.input()));   // old init 38,23
        builder.addSlot(RecipeIngredientRole.OUTPUT, 129, 24).addItemStacks(List.of(recipe.output())); // old init 128,23
        List<ItemStack> blades = blades();
        builder.addSlot(dev.golem.core.jei.Jei.CRAFTING_STATION, 84, 6).addItemStacks(blades);           // old init 83,5
        builder.addSlot(dev.golem.core.jei.Jei.CRAFTING_STATION, 84, 42).addItemStacks(blades);          // old init 83,41
    }

    @Override
    public void draw(ShredderPair recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        powerBar.draw(g, 3, 6);
        progressBar.draw(g, 80, 23);
    }
}
//?}
