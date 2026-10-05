package com.hbm.inventory.gui;

/**
 * Shared grey-RTG screen rectangles (1.7 {@code GUIMachineRTG}) so JEI cannot claim the heat tooltip's area.
 * The gauges are 16x51 with their top at y=10; 1.7's hover rectangles start at y=9 with its one-pixel-lower
 * hit test, which covers the same rows.
 */
public final class RtgGuiLayout {
    public static final int HEAT_X = 124;
    public static final int POWER_X = 146;
    public static final int GAUGE_Y = 10;
    public static final int GAUGE_WIDTH = 16;
    public static final int GAUGE_HEIGHT = 51;

    // The gap between the last fuel column (x 88..104) and the heat gauge (x 124) is unused by the GUI.
    public static final int RECIPE_CLICK_X = 106;
    public static final int RECIPE_CLICK_Y = 18;
    public static final int RECIPE_CLICK_WIDTH = 16;
    public static final int RECIPE_CLICK_HEIGHT = 52;

    private RtgGuiLayout() {}

    public static boolean overlaps(int x, int y, int width, int height,
                                   int otherX, int otherY, int otherWidth, int otherHeight) {
        return x < otherX + otherWidth && x + width > otherX
                && y < otherY + otherHeight && y + height > otherY;
    }
}
