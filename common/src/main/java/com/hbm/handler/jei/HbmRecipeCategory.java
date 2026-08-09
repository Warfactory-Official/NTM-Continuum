package com.hbm.handler.jei;

import com.hbm.Constants;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Base for the mod's JEI categories, faithful to the 1.12.2 {@code handler/jei} handlers: every category
 * is backed by its own {@code textures/gui/jei/gui_nei*.png} texture (copied from the old mod), cropped
 * {@code (5,11,166,65)} exactly as the originals did, and drawn as the recipe background. Subclasses draw
 * their machine-specific extras (slot frames, the arrow/machine pill, progress bars, stat text) on top,
 * mirroring the old {@code drawInfo}/{@code drawExtras} bodies.
 */
public abstract class HbmRecipeCategory<T> extends AbstractRecipeCategory<T> {

    public static final int WIDTH = 166;
    public static final int HEIGHT = 65;

    protected final IGuiHelper helper;
    protected final IDrawable background;

    protected HbmRecipeCategory(IGuiHelper helper, IRecipeType<T> type, Component title, ItemStack icon, String texture) {
        super(type, title, helper.createDrawableItemStack(icon), WIDTH, HEIGHT);
        this.helper = helper;
        this.background = helper.createDrawable(tex(texture), 5, 11, WIDTH, HEIGHT);
    }

    protected static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/jei/" + name);
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        background.draw(g, 0, 0);
    }

    /** Draws a dark stat string exactly like the old handlers' {@code fontRenderer.drawString(s, x, y, 0x404040)}. */
    protected static void text(GuiGraphicsExtractor g, String s, int x, int y) {
        g.text(Minecraft.getInstance().font, s, x, y, 0xFF404040, false);
    }

    protected static int textWidth(String s) {
        return Minecraft.getInstance().font.width(s);
    }
}
