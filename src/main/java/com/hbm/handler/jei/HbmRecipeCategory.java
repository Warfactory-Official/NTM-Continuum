package com.hbm.handler.jei;

import com.hbm.lib.Library;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.ViewerCategory;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Base for the mod's recipe-viewer categories, faithful to the 1.12.2 {@code handler/jei} handlers: every category
 * is backed by its own {@code textures/gui/jei/gui_nei*.png} texture (copied from the old mod), cropped
 * {@code (5,11,166,65)} exactly as the originals did, and drawn as the recipe background. Subclasses draw
 * their machine-specific extras (slot frames, the arrow/machine pill, progress bars, stat text) on top,
 * mirroring the old {@code drawInfo}/{@code drawExtras} bodies.
 *
 * <p>Written once for JEI and REI (Golem Core's {@link ViewerCategory}): the JEI
 * plugin and the REI plugin both wrap the same category objects.
 */
public abstract class HbmRecipeCategory<T> extends ViewerCategory<T> {

    public static final int WIDTH = 166;
    public static final int HEIGHT = 65;

    protected final Drawable background;

    protected HbmRecipeCategory(ViewerType<T> type, Component title, ItemStack icon, String texture) {
        this(type, title, icon, texture, WIDTH, HEIGHT, 5, 11);
    }

    protected HbmRecipeCategory(ViewerType<T> type, Component title, ItemStack icon,
                                String texture, int width, int height, int textureX, int textureY) {
        this(type, title, icon, tex(texture), width, height, textureX, textureY);
    }

    /** A background from any texture, cropped like the others. */
    protected HbmRecipeCategory(ViewerType<T> type, Component title, ItemStack icon,
                                Identifier texture, int width, int height, int textureX, int textureY) {
        super(type, title, icon, width, height);
        this.background = Drawable.texture(texture, textureX, textureY, width, height);
    }

    /** {@code <ns>:textures/gui/jei/<name>}; a bare name is HBM's, an addon category names its own as {@code "ns:name"}. */
    protected static Identifier tex(String name) {
        return Library.parseId(name, "textures/gui/jei/", "");
    }

    @Override
    public void draw(T recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        background.draw(g, 0, 0);
    }

    /** Draws a dark stat string exactly like the old handlers' {@code fontRenderer.drawString(s, x, y, 0x404040)}. */
    protected static void text(GuiGraphicsExtractor g, String s, int x, int y) {
        GuiDraw.text(g, Minecraft.getInstance().font, s, x, y, 0xFF404040, false);
    }

    protected static int textWidth(String s) {
        return Minecraft.getInstance().font.width(s);
    }
}
