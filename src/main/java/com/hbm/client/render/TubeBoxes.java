package com.hbm.client.render;

import net.minecraft.core.Direction;

/**
 * Face masks for {@code Quads.box}, the boxes 1.7 {@code RenderPneumoTube} drew: faces left out = 1.7
 * {@code renderSides}, quarter-turned UVs = 1.7 {@code uvRotate*}.
 */
public final class TubeBoxes {

    private TubeBoxes() {}

    static int mask(Direction... faces) {
        int mask = 0;
        for (Direction face : faces) mask |= 1 << face.ordinal();
        return mask;
    }

    static final int ALL = 0x3F;

    /** Every face but the two on the given axis: what an arm or collar shows. */
    static int lateral(Direction.Axis axis) {
        int mask = ALL;
        for (Direction face : Direction.values()) if (face.getAxis() == axis) mask &= ~(1 << face.ordinal());
        return mask;
    }
}
