package com.hbm.module;

import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The old seven-segment {@code NumberDisplay} GUI module (integer part only: the research reactor and
 * its controller never used the float or padding modes). Coordinates are GUI-relative; the screen
 * passes its own origin when drawing.
 */
public final class NumberDisplay {

    private final int displayX;
    private final int displayY;
    private final int color;
    /** The amount of padding between digits, default 3 */
    private final int padding = 3;
    private boolean blink = false;
    /** Max number of digits the display has, default 3 */
    private int digitLength = 3;
    private char[] toDisp = {'0', '0', '0'};
    private int dispOffset = 0;
    /** Length and thickness of segments. */
    private final int verticalLength = 5;
    private final int horizontalLength = 4;
    private final int thickness = 1;

    private GuiGraphicsExtractor graphics;
    private int guiLeft;
    private int guiTop;

    public NumberDisplay(int x, int y, int color) {
        this.displayX = x;
        this.displayY = y;
        this.color = 0xFF000000 | (color & 0xFFFFFF);
    }

    public NumberDisplay setDigitLength(int length) {
        this.digitLength = length;
        return this;
    }

    public NumberDisplay setBlinks(boolean doesBlink) {
        this.blink = doesBlink;
        return this;
    }

    public static boolean getBlink() {
        return System.currentTimeMillis() % 1000 < 500;
    }

    public void drawNumber(GuiGraphicsExtractor graphics, int guiLeft, int guiTop, Number num) {
        this.graphics = graphics;
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        toDisp = Long.toString(Math.round(num.doubleValue())).toCharArray();
        drawNumber(toDisp);
    }

    private void drawNumber(char[] num) {
        if (blink && !getBlink()) return;

        int gap = digitLength - num.length;
        for (int i = 0; i < num.length; i++) {
            if (num[i] == '.') gap--;
            dispOffset = (padding + horizontalLength + 2 * thickness) * (i + gap);
            drawChar(num[i]);
        }
    }

    private void drawChar(char num) {
        switch (num) {
            case '1' -> { drawVertical(1, 0); drawVertical(1, 1); }
            case '2' -> { drawHorizontal(0); drawVertical(1, 0); drawHorizontal(1); drawVertical(0, 1); drawHorizontal(2); }
            case '3' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(1, 0); drawVertical(1, 1); }
            case '4' -> { drawVertical(0, 0); drawVertical(1, 0); drawVertical(1, 1); drawHorizontal(1); }
            case '5' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(1, 1); }
            case '6' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(0, 1); drawVertical(1, 1); }
            case '7' -> { drawHorizontal(0); drawVertical(1, 0); drawVertical(1, 1); }
            case '8' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(1, 0); drawVertical(0, 1); drawVertical(1, 1); }
            case '9' -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(1, 0); drawVertical(1, 1); }
            case '0' -> { drawHorizontal(0); drawHorizontal(2); drawVertical(0, 0); drawVertical(0, 1); drawVertical(1, 0); drawVertical(1, 1); }
            case '-' -> drawHorizontal(1);
            case '.' -> drawPeriod();
            default -> { drawHorizontal(0); drawHorizontal(1); drawHorizontal(2); drawVertical(0, 0); drawVertical(0, 1); }
        }
    }

    private void drawHorizontal(int pos) {
        int offset = pos * (verticalLength + thickness);
        renderSegment(guiLeft + displayX + dispOffset + thickness, guiTop + displayY + offset, horizontalLength, thickness);
    }

    private void drawPeriod() {
        // 1.7 used guiLeft for the y term here too; kept as it was
        renderSegment(guiLeft + displayX + dispOffset + padding - (int) Math.ceil(padding / 2) + (horizontalLength + thickness),
                guiLeft + displayY + 2 * (verticalLength + thickness), thickness, thickness);
    }

    private void drawVertical(int posX, int posY) {
        int offsetX = posX * (horizontalLength + thickness);
        int offsetY = posY * (verticalLength + thickness);
        renderSegment(guiLeft + displayX + offsetX + dispOffset, guiTop + displayY + offsetY + thickness, thickness, verticalLength);
    }

    /** drawTexturedModalRect() for cool kids */
    private void renderSegment(int renX, int renY, int width, int height) {
        GuiDraw.fill(graphics, renX, renY, renX + width, renY + height, color);
    }
}
