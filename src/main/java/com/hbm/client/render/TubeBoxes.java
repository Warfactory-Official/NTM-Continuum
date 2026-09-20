package com.hbm.client.render;

import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;
import net.minecraft.core.Direction;

/**
 * The boxes 1.7 {@code RenderPneumoTube} drew through {@code RenderBlocks.setRenderBounds} +
 * {@code renderStandardBlock}, stated as code-built quads: every face of a box maps the sub-rectangle of the 16x16
 * sprite its bounds cover, exactly as {@code RenderBlocks} interpolated its icon UVs, and a face may be left out
 * (1.7 {@code renderSides}) or have its UVs turned a quarter (1.7 {@code uvRotate*}).
 */
public final class TubeBoxes {

    private TubeBoxes() {}

    /** A box with the named faces, {@code faceMask} being {@code 1 << Direction.ordinal()} bits. */
    static Geometry box(String name, double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                        int faceMask, int rotateMask) {
        Quads.Builder builder = Quads.named(name);
        float x0 = (float) minX, y0 = (float) minY, z0 = (float) minZ;
        float x1 = (float) maxX, y1 = (float) maxY, z1 = (float) maxZ;
        for (Direction face : Direction.values()) {
            if ((faceMask & (1 << face.ordinal())) == 0) continue;
            boolean rotate = (rotateMask & (1 << face.ordinal())) != 0;
            switch (face) {
                case DOWN -> quad(builder, 0, -1, 0, rotate,
                        x0, y0, z1, x0, z1,
                        x0, y0, z0, x0, z0,
                        x1, y0, z0, x1, z0,
                        x1, y0, z1, x1, z1);
                case UP -> quad(builder, 0, 1, 0, rotate,
                        x1, y1, z1, x1, z1,
                        x1, y1, z0, x1, z0,
                        x0, y1, z0, x0, z0,
                        x0, y1, z1, x0, z1);
                case NORTH -> quad(builder, 0, 0, -1, rotate,
                        x0, y1, z0, 1 - x0, 1 - y1,
                        x1, y1, z0, 1 - x1, 1 - y1,
                        x1, y0, z0, 1 - x1, 1 - y0,
                        x0, y0, z0, 1 - x0, 1 - y0);
                case SOUTH -> quad(builder, 0, 0, 1, rotate,
                        x0, y1, z1, x0, 1 - y1,
                        x0, y0, z1, x0, 1 - y0,
                        x1, y0, z1, x1, 1 - y0,
                        x1, y1, z1, x1, 1 - y1);
                case WEST -> quad(builder, -1, 0, 0, rotate,
                        x0, y1, z1, z1, 1 - y1,
                        x0, y1, z0, z0, 1 - y1,
                        x0, y0, z0, z0, 1 - y0,
                        x0, y0, z1, z1, 1 - y0);
                case EAST -> quad(builder, 1, 0, 0, rotate,
                        x1, y0, z1, 1 - z1, 1 - y0,
                        x1, y0, z0, 1 - z0, 1 - y0,
                        x1, y1, z0, 1 - z0, 1 - y1,
                        x1, y1, z1, 1 - z1, 1 - y1);
            }
        }
        return builder.build();
    }

    /** One face; {@code rotate} swaps each corner's u and v, the quarter turn 1.7's uvRotate applied. */
    private static void quad(Quads.Builder builder, float nx, float ny, float nz, boolean rotate,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv) {
        builder.normal(nx, ny, nz);
        if (rotate) {
            builder.vertex(ax, ay, az, av, au).vertex(bx, by, bz, bv, bu)
                   .vertex(cx, cy, cz, cv, cu).vertex(dx, dy, dz, dv, du);
        } else {
            builder.vertex(ax, ay, az, au, av).vertex(bx, by, bz, bu, bv)
                   .vertex(cx, cy, cz, cu, cv).vertex(dx, dy, dz, du, dv);
        }
    }

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
