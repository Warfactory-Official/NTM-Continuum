package com.hbm.client.render.skinlayer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.Vertexes;
import java.util.Arrays;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 1.12.2 {@code SkinBoxVoxelizer} (mlbv, "inspired by 3D Skin Layers by tr7zw"): one box of a 64x64 skin turned
 * into geometry. Each face's opaque texels are merged into rectangles drawn flush on the face, and every opaque texel
 * on the face's border with a clear neighbour gets the side walls of a one-texel-deep voxel, so an overlay layer
 * reads as solid pixels. Units are texels, the box standing on y = 0 and centred on x and z.
 *
 * <p>1.12.2 uploaded the quads into a VBO and drew them with fixed-function client arrays; here the same quads are
 * kept on the CPU (positions, texture coordinates and one normal per quad, in 1.12.2's order and winding) and emitted
 * through the submit sink each frame.
 */
public final class SkinBoxVoxelizer {

    private static final int TEX_SIZE = 64;
    private static final float INV_TEX = 1f / TEX_SIZE;
    private static final int NORM_XP = 0, NORM_XN = 1, NORM_YP = 2, NORM_YN = 3, NORM_ZP = 4, NORM_ZN = 5;
    private static final float[][] NORMALS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** Per quad: four vertices of x, y, z, u, v. */
    private final float[] verts;
    private final int[] norms;
    private final int quadCount;

    private SkinBoxVoxelizer(float[] verts, int[] norms, int quadCount) {
        this.verts = verts;
        this.norms = norms;
        this.quadCount = quadCount;
    }

    /** {@code skin}: 64x64 {@code 0xAARRGGBB}, row-major. */
    public static SkinBoxVoxelizer create(int[] skin, int w, int h, int d, int texU, int texV) {
        return create(skin, w, h, d, texU, texV, 0f);
    }

    public static SkinBoxVoxelizer create(int[] skin, int w, int h, int d, int texU, int texV, float grow) {
        int maxQuads = 2 * (w * h + w * d + h * d) * 5;
        float[] tmpVerts = new float[maxQuads * 20];
        int[] tmpNorms = new int[maxQuads];
        int count = 0;

        float xOff = -w / 2f;
        float zOff = -d / 2f;
        boolean[] frontMask = new boolean[w * h];
        for (int c = 0; c < w; c++) {
            for (int r = 0; r < h; r++) {
                frontMask[c + r * w] = isOpaque(skin, texU + d + c, texV + d + r);
            }
        }
        count = emitFrontSurface(tmpVerts, tmpNorms, count, frontMask, w, h, d, texU, texV, xOff, zOff, grow);
        for (int c = 0; c < w; c++) {
            for (int r = 0; r < h; r++) {
                int maskIdx = c + r * w;
                if (!frontMask[maskIdx]) continue;
                int tu = texU + d + c;
                int tv = texV + d + r;
                float px = xOff + c, py = h - 1 - r, pz = zOff;
                float x0 = px - grow, x1 = px + 1 + grow;
                float y0 = py - grow, y1 = py + 1 + grow;
                float z0 = pz - grow, z1 = pz + 1 + grow;
                float[] uv = pixelUV(tu, tv);
                if (c == 0 || !frontMask[maskIdx - 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_XN, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
                if (c == w - 1 || !frontMask[maskIdx + 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_XP, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
                if (r == 0 || !frontMask[c + (r - 1) * w])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_YP, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
                if (r == h - 1 || !frontMask[c + (r + 1) * w])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_YN, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
            }
        }

        boolean[] backMask = new boolean[w * h];
        for (int c = 0; c < w; c++) {
            for (int r = 0; r < h; r++) {
                backMask[c + r * w] = isOpaque(skin, texU + 2 * d + w + c, texV + d + r);
            }
        }
        count = emitBackSurface(tmpVerts, tmpNorms, count, backMask, w, h, d, texU, texV, xOff, zOff, grow);
        for (int c = 0; c < w; c++) {
            for (int r = 0; r < h; r++) {
                int maskIdx = c + r * w;
                if (!backMask[maskIdx]) continue;
                int tu = texU + 2 * d + w + c;
                int tv = texV + d + r;
                float px = xOff + (w - 1 - c), py = h - 1 - r, pz = zOff + d - 1;
                float x0 = px - grow, x1 = px + 1 + grow;
                float y0 = py - grow, y1 = py + 1 + grow;
                float z0 = pz - grow, z1 = pz + 1 + grow;
                float[] uv = pixelUV(tu, tv);
                if (c == 0 || !backMask[maskIdx - 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_XP, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
                if (c == w - 1 || !backMask[maskIdx + 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_XN, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
                if (r == 0 || !backMask[c + (r - 1) * w])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_YP, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
                if (r == h - 1 || !backMask[c + (r + 1) * w])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_YN, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
            }
        }

        boolean[] leftMask = new boolean[d * h];
        for (int c = 0; c < d; c++) {
            for (int r = 0; r < h; r++) {
                leftMask[c + r * d] = isOpaque(skin, texU + d - 1 - c, texV + d + r);
            }
        }
        count = emitLeftSurface(tmpVerts, tmpNorms, count, leftMask, h, d, texU, texV, xOff, zOff, grow);
        for (int c = 0; c < d; c++) {
            for (int r = 0; r < h; r++) {
                int maskIdx = c + r * d;
                if (!leftMask[maskIdx]) continue;
                int tu = texU + d - 1 - c;
                int tv = texV + d + r;
                float px = xOff, py = h - 1 - r, pz = zOff + c;
                float x0 = px - grow, x1 = px + 1 + grow;
                float y0 = py - grow, y1 = py + 1 + grow;
                float z0 = pz - grow, z1 = pz + 1 + grow;
                float[] uv = pixelUV(tu, tv);
                if (c == 0 || !leftMask[maskIdx - 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_ZN, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
                if (c == d - 1 || !leftMask[maskIdx + 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_ZP, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
                if (r == 0 || !leftMask[c + (r - 1) * d])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_YP, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
                if (r == h - 1 || !leftMask[c + (r + 1) * d])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_YN, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
            }
        }

        boolean[] rightMask = new boolean[d * h];
        for (int c = 0; c < d; c++) {
            for (int r = 0; r < h; r++) {
                rightMask[c + r * d] = isOpaque(skin, texU + d + w + c, texV + d + r);
            }
        }
        count = emitRightSurface(tmpVerts, tmpNorms, count, rightMask, h, d, texU, texV, xOff, zOff, w, grow);
        for (int c = 0; c < d; c++) {
            for (int r = 0; r < h; r++) {
                int maskIdx = c + r * d;
                if (!rightMask[maskIdx]) continue;
                int tu = texU + d + w + c;
                int tv = texV + d + r;
                float px = xOff + w - 1, py = h - 1 - r, pz = zOff + c;
                float x0 = px - grow, x1 = px + 1 + grow;
                float y0 = py - grow, y1 = py + 1 + grow;
                float z0 = pz - grow, z1 = pz + 1 + grow;
                float[] uv = pixelUV(tu, tv);
                if (c == 0 || !rightMask[maskIdx - 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_ZN, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
                if (c == d - 1 || !rightMask[maskIdx + 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_ZP, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
                if (r == 0 || !rightMask[c + (r - 1) * d])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_YP, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
                if (r == h - 1 || !rightMask[c + (r + 1) * d])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_YN, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
            }
        }

        boolean[] topMask = new boolean[w * d];
        for (int c = 0; c < w; c++) {
            for (int r = 0; r < d; r++) {
                topMask[c + r * w] = isOpaque(skin, texU + d + c, texV + d - 1 - r);
            }
        }
        count = emitTopSurface(tmpVerts, tmpNorms, count, topMask, w, d, texU, texV, xOff, zOff, h, grow);
        for (int c = 0; c < w; c++) {
            for (int r = 0; r < d; r++) {
                int maskIdx = c + r * w;
                if (!topMask[maskIdx]) continue;
                int tu = texU + d + c;
                int tv = texV + d - 1 - r;
                float px = xOff + c, py = h - 1, pz = zOff + r;
                float x0 = px - grow, x1 = px + 1 + grow;
                float y0 = py - grow, y1 = py + 1 + grow;
                float z0 = pz - grow, z1 = pz + 1 + grow;
                float[] uv = pixelUV(tu, tv);
                if (c == 0 || !topMask[maskIdx - 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_XN, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
                if (c == w - 1 || !topMask[maskIdx + 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_XP, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
                if (r == 0 || !topMask[c + (r - 1) * w])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_ZN, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
                if (r == d - 1 || !topMask[c + (r + 1) * w])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_ZP, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
            }
        }

        boolean[] bottomMask = new boolean[w * d];
        for (int c = 0; c < w; c++) {
            for (int r = 0; r < d; r++) {
                bottomMask[c + r * w] = isOpaque(skin, texU + d + w + c, texV + d - 1 - r);
            }
        }
        count = emitBottomSurface(tmpVerts, tmpNorms, count, bottomMask, w, d, texU, texV, xOff, zOff, grow);
        for (int c = 0; c < w; c++) {
            for (int r = 0; r < d; r++) {
                int maskIdx = c + r * w;
                if (!bottomMask[maskIdx]) continue;
                int tu = texU + d + w + c;
                int tv = texV + d - 1 - r;
                float px = xOff + c, py = 0, pz = zOff + r;
                float x0 = px - grow, x1 = px + 1 + grow;
                float y0 = py - grow, y1 = py + 1 + grow;
                float z0 = pz - grow, z1 = pz + 1 + grow;
                float[] uv = pixelUV(tu, tv);
                if (c == 0 || !bottomMask[maskIdx - 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_XN, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1);
                if (c == w - 1 || !bottomMask[maskIdx + 1])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_XP, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
                if (r == 0 || !bottomMask[c + (r - 1) * w])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_ZN, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
                if (r == d - 1 || !bottomMask[c + (r + 1) * w])
                    count = addQuad(tmpVerts, tmpNorms, count, uv, NORM_ZP, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1);
            }
        }

        return new SkinBoxVoxelizer(Arrays.copyOf(tmpVerts, count * 20), Arrays.copyOf(tmpNorms, count), count);
    }

    private static int emitFrontSurface(float[] verts, int[] norms, int idx, boolean[] mask, int w, int h, int d, int texU,
                                        int texV, float xOff, float zOff, float grow) {
        int[] rows = new int[h];
        buildRows(mask, w, h, rows);
        int[] rects = new int[mask.length * 4];
        int rectCount = collectRectangles(rows, w, h, rects);
        int baseU = texU + d;
        int baseV = texV + d;
        float z = zOff - grow;
        for (int i = 0; i < rectCount; i++) {
            int off = i * 4;
            int c0 = rects[off], r0 = rects[off + 1], c1 = rects[off + 2], r1 = rects[off + 3];
            float x0 = xOff + c0 - grow;
            float x1 = xOff + c1 + grow;
            float y0 = h - r1 - grow;
            float y1 = h - r0 + grow;
            float u0 = texCoord(baseU + c0), u1 = texCoord(baseU + c1);
            float v0 = texCoord(baseV + r0), v1 = texCoord(baseV + r1);
            idx = addQuadUV(verts, norms, idx, NORM_ZN, x0, y0, z, u0, v1, x1, y0, z, u1, v1, x1, y1, z, u1, v0,
                    x0, y1, z, u0, v0);
        }
        return idx;
    }

    private static int emitBackSurface(float[] verts, int[] norms, int idx, boolean[] mask, int w, int h, int d, int texU,
                                       int texV, float xOff, float zOff, float grow) {
        int[] rows = new int[h];
        buildRows(mask, w, h, rows);
        int[] rects = new int[mask.length * 4];
        int rectCount = collectRectangles(rows, w, h, rects);
        int baseU = texU + 2 * d + w;
        int baseV = texV + d;
        float z = zOff + d + grow;
        for (int i = 0; i < rectCount; i++) {
            int off = i * 4;
            int c0 = rects[off], r0 = rects[off + 1], c1 = rects[off + 2], r1 = rects[off + 3];
            float x0 = xOff + (w - c1) - grow;
            float x1 = xOff + (w - c0) + grow;
            float y0 = h - r1 - grow;
            float y1 = h - r0 + grow;
            float u0 = texCoord(baseU + c0), u1 = texCoord(baseU + c1);
            float v0 = texCoord(baseV + r0), v1 = texCoord(baseV + r1);
            idx = addQuadUV(verts, norms, idx, NORM_ZP, x0, y0, z, u1, v1, x1, y0, z, u0, v1, x1, y1, z, u0, v0,
                    x0, y1, z, u1, v0);
        }
        return idx;
    }

    private static int emitLeftSurface(float[] verts, int[] norms, int idx, boolean[] mask, int h, int d, int texU,
                                       int texV, float xOff, float zOff, float grow) {
        int[] rows = new int[h];
        buildRows(mask, d, h, rows);
        int[] rects = new int[mask.length * 4];
        int rectCount = collectRectangles(rows, d, h, rects);
        int baseU = texU + d;
        int baseV = texV + d;
        float x = xOff - grow;
        for (int i = 0; i < rectCount; i++) {
            int off = i * 4;
            int c0 = rects[off], r0 = rects[off + 1], c1 = rects[off + 2], r1 = rects[off + 3];
            float z0 = zOff + c0 - grow;
            float z1 = zOff + c1 + grow;
            float y0 = h - r1 - grow;
            float y1 = h - r0 + grow;
            float u0 = texCoord(baseU - c1), u1 = texCoord(baseU - c0);
            float v0 = texCoord(baseV + r0), v1 = texCoord(baseV + r1);
            idx = addQuadUV(verts, norms, idx, NORM_XN, x, y0, z0, u1, v1, x, y0, z1, u0, v1, x, y1, z1, u0, v0,
                    x, y1, z0, u1, v0);
        }
        return idx;
    }

    private static int emitRightSurface(float[] verts, int[] norms, int idx, boolean[] mask, int h, int d, int texU,
                                        int texV, float xOff, float zOff, int w, float grow) {
        int[] rows = new int[h];
        buildRows(mask, d, h, rows);
        int[] rects = new int[mask.length * 4];
        int rectCount = collectRectangles(rows, d, h, rects);
        int baseU = texU + d + w;
        int baseV = texV + d;
        float x = xOff + w + grow;
        for (int i = 0; i < rectCount; i++) {
            int off = i * 4;
            int c0 = rects[off], r0 = rects[off + 1], c1 = rects[off + 2], r1 = rects[off + 3];
            float z0 = zOff + c0 - grow;
            float z1 = zOff + c1 + grow;
            float y0 = h - r1 - grow;
            float y1 = h - r0 + grow;
            float u0 = texCoord(baseU + c0), u1 = texCoord(baseU + c1);
            float v0 = texCoord(baseV + r0), v1 = texCoord(baseV + r1);
            idx = addQuadUV(verts, norms, idx, NORM_XP, x, y0, z0, u0, v1, x, y0, z1, u1, v1, x, y1, z1, u1, v0,
                    x, y1, z0, u0, v0);
        }
        return idx;
    }

    private static int emitTopSurface(float[] verts, int[] norms, int idx, boolean[] mask, int w, int d, int texU,
                                      int texV, float xOff, float zOff, int h, float grow) {
        int[] rows = new int[d];
        buildRows(mask, w, d, rows);
        int[] rects = new int[mask.length * 4];
        int rectCount = collectRectangles(rows, w, d, rects);
        int baseU = texU + d;
        int baseV = texV + d;
        float y = h + grow;
        for (int i = 0; i < rectCount; i++) {
            int off = i * 4;
            int c0 = rects[off], r0 = rects[off + 1], c1 = rects[off + 2], r1 = rects[off + 3];
            float x0 = xOff + c0 - grow;
            float x1 = xOff + c1 + grow;
            float z0 = zOff + r0 - grow;
            float z1 = zOff + r1 + grow;
            float u0 = texCoord(baseU + c0), u1 = texCoord(baseU + c1);
            float v0 = texCoord(baseV - r1), v1 = texCoord(baseV - r0);
            idx = addQuadUV(verts, norms, idx, NORM_YP, x0, y, z0, u0, v1, x1, y, z0, u1, v1, x1, y, z1, u1, v0,
                    x0, y, z1, u0, v0);
        }
        return idx;
    }

    private static int emitBottomSurface(float[] verts, int[] norms, int idx, boolean[] mask, int w, int d, int texU,
                                         int texV, float xOff, float zOff, float grow) {
        int[] rows = new int[d];
        buildRows(mask, w, d, rows);
        int[] rects = new int[mask.length * 4];
        int rectCount = collectRectangles(rows, w, d, rects);
        int baseU = texU + d + w;
        int baseV = texV + d;
        float y = -grow;
        for (int i = 0; i < rectCount; i++) {
            int off = i * 4;
            int c0 = rects[off], r0 = rects[off + 1], c1 = rects[off + 2], r1 = rects[off + 3];
            float x0 = xOff + c0 - grow;
            float x1 = xOff + c1 + grow;
            float z0 = zOff + r0 - grow;
            float z1 = zOff + r1 + grow;
            float u0 = texCoord(baseU + c0), u1 = texCoord(baseU + c1);
            float v0 = texCoord(baseV - r1), v1 = texCoord(baseV - r0);
            idx = addQuadUV(verts, norms, idx, NORM_YN, x0, y, z1, u0, v0, x1, y, z1, u1, v0, x1, y, z0, u1, v1,
                    x0, y, z0, u0, v1);
        }
        return idx;
    }

    private static void buildRows(boolean[] mask, int width, int height, int[] rowsOut) {
        Arrays.fill(rowsOut, 0);
        for (int r = 0; r < height; r++) {
            int rowMask = 0;
            int rowBase = r * width;
            for (int c = 0; c < width; c++) {
                if (mask[rowBase + c]) rowMask |= (1 << c);
            }
            rowsOut[r] = rowMask;
        }
    }

    /** Greedy rectangles: the widest run of each row, extended down while the rows below hold it whole. */
    private static int collectRectangles(int[] rows, int width, int height, int[] rects) {
        int rectCount = 0;
        int widthMask = (1 << width) - 1;
        for (int v0 = 0; v0 < height; v0++) {
            while (true) {
                int rowMask = rows[v0] & widthMask;
                if (rowMask == 0) break;

                int u0 = Integer.numberOfTrailingZeros(rowMask);
                int shifted = (rowMask >>> u0) & widthMask;
                int inv = (~shifted) & widthMask;
                int rectWidth = Integer.numberOfTrailingZeros(inv);
                if (rectWidth == 32) rectWidth = width - u0;
                if (rectWidth <= 0) break;

                int rectMask = ((1 << rectWidth) - 1) << u0;
                int rectHeight = 1;
                while (v0 + rectHeight < height) {
                    int m = rows[v0 + rectHeight] & widthMask;
                    if ((m & rectMask) != rectMask) break;
                    rectHeight++;
                }

                for (int vv = 0; vv < rectHeight; vv++) {
                    rows[v0 + vv] &= ~rectMask;
                }

                int rectOff = rectCount * 4;
                rects[rectOff] = u0;
                rects[rectOff + 1] = v0;
                rects[rectOff + 2] = u0 + rectWidth;
                rects[rectOff + 3] = v0 + rectHeight;
                rectCount++;
            }
        }
        return rectCount;
    }

    private static boolean isOpaque(int[] img, int u, int v) {
        if (u < 0 || u >= TEX_SIZE || v < 0 || v >= TEX_SIZE) return false;
        return ((img[v * TEX_SIZE + u] >> 24) & 0xFF) != 0;
    }

    private static float[] pixelUV(int u, int v) {
        return new float[]{u * INV_TEX, v * INV_TEX, (u + 1) * INV_TEX, (v + 1) * INV_TEX};
    }

    private static float texCoord(int tex) {
        return tex * INV_TEX;
    }

    private static int addQuad(float[] verts, int[] norms, int idx, float[] uv, int normal, float x0, float y0,
                               float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3,
                               float z3) {
        return addQuadUV(verts, norms, idx, normal, x0, y0, z0, uv[2], uv[1], x1, y1, z1, uv[0], uv[1],
                x2, y2, z2, uv[0], uv[3], x3, y3, z3, uv[2], uv[3]);
    }

    private static int addQuadUV(float[] verts, int[] norms, int idx, int normal, float x0, float y0, float z0, float u0,
                                 float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2,
                                 float u2, float v2, float x3, float y3, float z3, float u3, float v3) {
        int off = idx * 20;
        verts[off] = x0; verts[off + 1] = y0; verts[off + 2] = z0; verts[off + 3] = u0; verts[off + 4] = v0;
        verts[off + 5] = x1; verts[off + 6] = y1; verts[off + 7] = z1; verts[off + 8] = u1; verts[off + 9] = v1;
        verts[off + 10] = x2; verts[off + 11] = y2; verts[off + 12] = z2; verts[off + 13] = u2; verts[off + 14] = v2;
        verts[off + 15] = x3; verts[off + 16] = y3; verts[off + 17] = z3; verts[off + 18] = u3; verts[off + 19] = v3;
        norms[idx] = normal;
        return idx + 1;
    }

    public boolean isEmpty() {
        return quadCount == 0;
    }

    public int quadCount() {
        return quadCount;
    }

    /** {@code render()}: the quads under the current pose, white, at {@code light}. */
    public void emit(PoseStack.Pose pose, VertexConsumer buffer, int light) {
        for (int q = 0; q < quadCount; q++) {
            float[] n = NORMALS[norms[q]];
            int base = q * 20;
            for (int v = 0; v < 4; v++) {
                int off = base + v * 5;
                Vertexes.vertex(buffer, pose, verts[off], verts[off + 1], verts[off + 2], 0xFFFFFFFF,
                        verts[off + 3], verts[off + 4], OverlayTexture.NO_OVERLAY, light, n[0], n[1], n[2]);
            }
        }
    }
}
