package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * The plain 1.7 {@code GuiButton} of the structure-wand screens (a labelled box with a hover tint), drawn the way
 * the port's other plain screens draw theirs ({@code GUIDiode}).
 */
final class GUIStructureWandButton {

    final int x, y, width, height;
    String label;

    GUIStructureWandButton(int x, int y, int width, int height, String label) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
    }

    boolean isOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    void draw(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        boolean hover = isOver(mouseX, mouseY);
        GuiDraw.fill(g, x, y, x + width, y + height, 0xFF000000);
        GuiDraw.fill(g, x + 1, y + 1, x + width - 1, y + height - 1, hover ? 0xFF7A7AA0 : 0xFF6F6F6F);
        GuiDraw.text(g, font, label, x + width / 2 - font.width(label) / 2, y + (height - 8) / 2, hover ? 0xFFFFFFA0 : 0xFFE0E0E0, true);
    }

    static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
