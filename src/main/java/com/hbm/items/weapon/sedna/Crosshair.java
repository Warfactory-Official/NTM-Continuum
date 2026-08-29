package com.hbm.items.weapon.sedna;

/**
 * Which crosshair a gun draws, and where it sits on {@code textures/misc/overlay_misc.png}.
 *
 * <p>Ported from the enum nested in 1.12.2's {@code RenderScreenOverlay}. It lives in the common
 * weapon package rather than a client class because a {@link GunConfig} names one, and gun configs are
 * built on the server as well — the drawing itself stays client-side.
 *
 * <p>The atlas coordinates are carried verbatim so the original texture lines up unchanged.
 */
public enum Crosshair {
    NONE(0, 0, 0),
    CROSS(1, 55, 16),
    CIRCLE(19, 55, 16),
    SEMI(37, 55, 16),
    KRUCK(55, 55, 16),
    DUAL(1, 73, 16),
    SPLIT(19, 73, 16),
    CLASSIC(37, 73, 16),
    BOX(55, 73, 16),

    L_CROSS(0, 90, 32),
    L_KRUCK(32, 90, 32),
    L_CLASSIC(64, 90, 32),
    L_CIRCLE(96, 90, 32),
    L_SPLIT(0, 122, 32),
    L_ARROWS(32, 122, 32),
    L_BOX(64, 122, 32),
    L_CIRCUMFLEX(96, 122, 32),
    L_RAD(0, 154, 32),
    L_MODERN(32, 154, 32),
    L_BOX_OUTLINE(64, 154, 32);

    public final int x;
    public final int y;
    public final int size;

    Crosshair(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
    }
}
