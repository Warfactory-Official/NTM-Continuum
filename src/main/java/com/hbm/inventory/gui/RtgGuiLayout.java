package com.hbm.inventory.gui;

/** Shared grey-RTG screen rectangles so JEI cannot claim the heat tooltip's area. */
public final class RtgGuiLayout {
    public static final int HEAT_X = 134;
    public static final int POWER_X = 152;
    public static final int GAUGE_Y = 17;
    public static final int GAUGE_WIDTH = 16;
    public static final int GAUGE_HEIGHT = 52;

    /* The 16px separator between the last machine slot and the gauges is intentionally unused by the GUI. */
    public static final int RECIPE_CLICK_X = 116;
    public static final int RECIPE_CLICK_Y = 22;
    public static final int RECIPE_CLICK_WIDTH = 16;
    public static final int RECIPE_CLICK_HEIGHT = 52;

    private RtgGuiLayout() {}

    public static boolean overlaps(int x, int y, int width, int height,
                                   int otherX, int otherY, int otherWidth, int otherHeight) {
        return x < otherX + otherWidth && x + width > otherX
                && y < otherY + otherHeight && y + height > otherY;
    }
}
