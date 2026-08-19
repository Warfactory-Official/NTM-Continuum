package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Emits the <b>small</b> textured cubes that stand in for not-yet-placed blocks — the 26.1 replacement
 * for 1.12.2's {@code render/util/SmallBlockPronter} ({@code renderSimpleBlockAt} /
 * {@code renderPilarBlockAt}). Used by {@link StructPreviewRenderer} to draw the ghost of a multiblock
 * that has not been built yet.
 *
 * <p>The size is the whole point of the name: every face is inset by {@link #INSET} of a block on each
 * side, so the ghost is a 5-pixel cube floating in the middle of its cell rather than a full block. A
 * full-size ghost reads as "there is already a block here" and buries the structure in opaque mass;
 * the small one reads as a marker.
 *
 * <p>Upstream sampled the block atlas; here each face samples a standalone texture over the full
 * {@code 0..1} UV range, which is what {@code CompatRenderTypes} works with and avoids depending on
 * atlas stitching from a block-entity renderer. Top/bottom and side faces are emitted separately so a
 * pillar-textured block can send them to two different render types.
 */
public final class SmallBlockPronter {

    /** Upstream's {@code 11 * pixel / 2} — each face sits this far in from the cell edge. */
    public static final float INSET = 11F / 32F;

    private SmallBlockPronter() {}

    /** The four vertical faces of the small cube centred in the cell whose corner is {@code (x, y, z)}. */
    public static void sideFaces(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                                 float r, float g, float b, float a, int light) {
        float x1 = x + 1 - INSET, y1 = y + 1 - INSET, z1 = z + 1 - INSET;
        x += INSET; y += INSET; z += INSET;
        // north (-Z)
        quad(pose, buffer, r, g, b, a, light, 0, 0, -1,
                x1, y, z, x, y, z, x, y1, z, x1, y1, z);
        // south (+Z)
        quad(pose, buffer, r, g, b, a, light, 0, 0, 1,
                x, y, z1, x1, y, z1, x1, y1, z1, x, y1, z1);
        // west (-X)
        quad(pose, buffer, r, g, b, a, light, -1, 0, 0,
                x, y, z, x, y, z1, x, y1, z1, x, y1, z);
        // east (+X)
        quad(pose, buffer, r, g, b, a, light, 1, 0, 0,
                x1, y, z1, x1, y, z, x1, y1, z, x1, y1, z1);
    }

    /** The top and bottom faces of the small cube centred in the cell whose corner is {@code (x, y, z)}. */
    public static void endFaces(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                                float r, float g, float b, float a, int light) {
        float x1 = x + 1 - INSET, y1 = y + 1 - INSET, z1 = z + 1 - INSET;
        x += INSET; y += INSET; z += INSET;
        // down (-Y)
        quad(pose, buffer, r, g, b, a, light, 0, -1, 0,
                x, y, z, x1, y, z, x1, y, z1, x, y, z1);
        // up (+Y)
        quad(pose, buffer, r, g, b, a, light, 0, 1, 0,
                x, y1, z1, x1, y1, z1, x1, y1, z, x, y1, z);
    }

    /** The whole small cube with one texture on every face. */
    public static void cube(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z,
                            float r, float g, float b, float a, int light) {
        sideFaces(pose, buffer, x, y, z, r, g, b, a, light);
        endFaces(pose, buffer, x, y, z, r, g, b, a, light);
    }

    /** Vertices are given counter-clockwise as seen from outside, starting at the lower-left corner. */
    private static void quad(PoseStack.Pose pose, VertexConsumer buffer,
                             float r, float g, float b, float a, int light,
                             float nx, float ny, float nz,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        Vertexes.vertex(buffer, pose, x0, y0, z0, r, g, b, a, 0F, 1F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        Vertexes.vertex(buffer, pose, x1, y1, z1, r, g, b, a, 1F, 1F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        Vertexes.vertex(buffer, pose, x2, y2, z2, r, g, b, a, 1F, 0F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        Vertexes.vertex(buffer, pose, x3, y3, z3, r, g, b, a, 0F, 0F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
    }
}
