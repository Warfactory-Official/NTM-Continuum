package com.hbm.client.render;

import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Quads;

/**
 * Box faces for the RBMK column pieces, which have no OBJ of their own. Stated as {@link Quads} so a
 * column's icon and the placed column are the same geometry; the height is a dial, but the boxes are
 * not. Sides and caps stay apart because they wear different textures.
 */
final class RBMKColumnGeometry {

    private RBMKColumnGeometry() {}

    /** The lid, a quarter-block cap sitting on the column's top cell. */
    static final float LID_HEIGHT = .25F;
    static final Geometry LID_SIDES =
            blockSides(Quads.named("hbm:rbmk_lid_side"), 0F, 0F, 0F, 1F, LID_HEIGHT, 1F).build();
    static final Geometry LID_CAPS =
            blockCaps(Quads.named("hbm:rbmk_lid_cap"), 0F, 0F, 0F, 1F, LID_HEIGHT, 1F).build();

    /**
     * Sides textured like 1.7 RenderBlocks: UVs from the position in the block (v = 1 - y), so a quarter-high box
     * shows the texture's bottom four rows, where the lid and pipe textures draw.
     */
    static Quads.Builder blockSides(Quads.Builder faces, float x0, float y0, float z0,
                                    float x1, float y1, float z1) {
        blockFace(faces, 0F, 0F, -1F, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        blockFace(faces, 0F, 0F, 1F, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        blockFace(faces, -1F, 0F, 0F, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        blockFace(faces, 1F, 0F, 0F, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        return faces;
    }

    /** Caps with UVs from the position in the block, wound like {@link #caps}. */
    static Quads.Builder blockCaps(Quads.Builder faces, float x0, float y0, float z0,
                                   float x1, float y1, float z1) {
        faces.normal(0F, 1F, 0F)
                .vertex(x0, y1, z0, x0, z0).vertex(x0, y1, z1, x0, z1)
                .vertex(x1, y1, z1, x1, z1).vertex(x1, y1, z0, x1, z0);
        faces.normal(0F, -1F, 0F)
                .vertex(x0, y0, z1, x0, z1).vertex(x0, y0, z0, x0, z0)
                .vertex(x1, y0, z0, x1, z0).vertex(x1, y0, z1, x1, z1);
        return faces;
    }

    static Quads.Builder sides(Quads.Builder faces, float x0, float y0, float z0,
                               float x1, float y1, float z1) {
        face(faces, 0F, 0F, -1F, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        face(faces, 0F, 0F, 1F, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(faces, -1F, 0F, 0F, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        face(faces, 1F, 0F, 0F, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
        return faces;
    }

    static Quads.Builder caps(Quads.Builder faces, float x0, float y0, float z0,
                              float x1, float y1, float z1) {
        // Wound counter-clockwise from outside like vanilla's UP/DOWN faces. The other winding makes a culled
        // surface drop the top and draw the bottom from above: a lid's underside, shaded as a down face and
        // coplanar with the column's cap, shows as a dark, z-fighting top, and the block-column items get no top.
        faces.normal(0F, 1F, 0F)
                .vertex(x0, y1, z0, 0F, 0F).vertex(x0, y1, z1, 0F, 1F)
                .vertex(x1, y1, z1, 1F, 1F).vertex(x1, y1, z0, 1F, 0F);
        faces.normal(0F, -1F, 0F)
                .vertex(x0, y0, z1, 0F, 1F).vertex(x0, y0, z0, 1F, 1F)
                .vertex(x1, y0, z0, 1F, 0F).vertex(x1, y0, z1, 0F, 0F);
        return faces;
    }

    /** The model-space box those faces enclose, for an icon's framing. */
    static float[][] bounds(float x0, float y0, float z0, float x1, float y1, float z1) {
        return new float[][] {{x0, y0, z0}, {x1, y1, z1}};
    }

    private static void face(Quads.Builder faces, float nx, float ny, float nz,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz) {
        faces.normal(nx, ny, nz)
                .vertex(ax, ay, az, 0F, 0F)
                .vertex(bx, by, bz, 1F, 0F)
                .vertex(cx, cy, cz, 1F, 1F)
                .vertex(dx, dy, dz, 0F, 1F);
    }

    private static void blockFace(Quads.Builder faces, float nx, float ny, float nz,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz) {
        boolean alongZ = nx != 0F;
        faces.normal(nx, ny, nz)
                .vertex(ax, ay, az, alongZ ? az : ax, 1F - ay)
                .vertex(bx, by, bz, alongZ ? bz : bx, 1F - by)
                .vertex(cx, cy, cz, alongZ ? cz : cx, 1F - cy)
                .vertex(dx, dy, dz, alongZ ? dz : dx, 1F - dy);
    }

}
