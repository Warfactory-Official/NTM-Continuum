package com.hbm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

public interface ILookOverlay {

    void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos);

    static void printGeneric(GuiGraphicsExtractor graphics, String title, int titleCol, int bgCol, List<String> text) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int pX = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int pZ = mc.getWindow().getGuiScaledHeight() / 2;

        graphics.text(font, title, pX + 1, pZ - 9, opaque(bgCol), false);
        graphics.text(font, title, pX, pZ - 10, opaque(titleCol), false);

        for (String line : text) {
            int color = 0xFFFFFF;
            if (line.startsWith("&[")) {
                int end = line.lastIndexOf("&]");
                if (end > 2) {
                    try {
                        color = Integer.parseInt(line.substring(2, end));
                    } catch (NumberFormatException ignored) {}
                    line = line.substring(end + 2);
                }
            }
            graphics.text(font, line, pX, pZ, opaque(color), true);
            pZ += 10;
        }
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }
}
