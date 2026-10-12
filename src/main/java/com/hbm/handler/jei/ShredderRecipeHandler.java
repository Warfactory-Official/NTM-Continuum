package com.hbm.handler.jei;

import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModMachineItems;
import dev.golem.core.viewer.ViewerType;
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

    private final Drawable powerBar;
    private final Drawable progressBar;

    public ShredderRecipeHandler(ViewerType<ShredderPair> type, Component title, ItemStack icon) {
        super(type, title, icon, "gui_nei_shredder.png");
        Drawable.Texture power = Drawable.texture(tex("gui_nei_shredder.png"), 36, 86, 16, 52);
        this.powerBar = Drawable.animated(power, 480, Drawable.Direction.TOP, true);
        Drawable.Texture progress = Drawable.texture(tex("gui_nei_shredder.png"), 100, 118, 24, 16);
        this.progressBar = Drawable.animated(progress, 48, Drawable.Direction.LEFT, false);
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
    public void setRecipe(SlotLayout builder, ShredderPair recipe) {
        builder.addSlot(Role.INPUT, 39, 24).addItemStacks(List.of(recipe.input()));   // old init 38,23
        builder.addSlot(Role.OUTPUT, 129, 24).addItemStacks(List.of(recipe.output())); // old init 128,23
        List<ItemStack> blades = blades();
        builder.addSlot(Role.CRAFTING_STATION, 84, 6).addItemStacks(blades);           // old init 83,5
        builder.addSlot(Role.CRAFTING_STATION, 84, 42).addItemStacks(blades);          // old init 83,41
    }

    @Override
    public void draw(ShredderPair recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, g, mouseX, mouseY);
        powerBar.draw(g, 3, 6);
        progressBar.draw(g, 80, 23);
    }
}
