package com.hbm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A block that draws an info overlay next to the crosshair while the player looks at it — the 1.12.2
 * {@code ILookOverlay} (battery charge, fluid-duct type, etc.). The central dispatcher
 * ({@link HbmLookOverlay}) raytraces the looked-at block each frame and calls {@link #printHook} on any
 * block that implements this; each loader registers the dispatcher on its HUD-render hook.
 *
 * <p>Client-only in practice (it touches {@code net.minecraft.client.*}); it lives in {@code common} like
 * the GUIs/renderers and is only loaded on the client.
 */
public interface ILookOverlay {

    /** Draw this block's overlay. Called every frame while the player is looking at {@code pos}. */
    void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos);

    /**
     * Draws a {@code title} + a list of info lines just right of the crosshair, ported from the 1.12.2
     * {@code ILookOverlay.printGeneric}. A line may begin with {@code &[<rgb>&]} to set its colour (decimal
     * RGB int); otherwise it is white. Legacy {@code §} formatting codes in a line are honoured.
     */
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

    /** Forces a fully-opaque alpha channel — a colour with alpha 0 (e.g. {@code 0xffff00}) renders
     * invisibly in the 26.1 text pipeline. */
    private static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }
}
