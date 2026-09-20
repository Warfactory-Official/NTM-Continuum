package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Geometry;

/**
 * One OBJ part drawn through user clip planes, which is what the old door renderers did with
 * {@code GL_CLIP_PLANE0/1} to stop a sliding leaf poking out of its frame. Modern rendering has no
 * fixed-function clip planes, so each triangle is clipped on the CPU against the same half-spaces
 * (Sutherland–Hodgman, keeping {@code a·x + b·y + c·z + d >= 0} exactly as GL did) and re-emitted.
 *
 * <p>A plane is given in the space the part is drawn in. A leaf translated by {@code t} inside a frame
 * whose plane is fixed is clipped by the plane shifted by {@code -t}, which is what {@link #draw} takes
 * as {@code offset}: the door's translation, applied to the plane rather than the triangles.
 */
public final class ClippedObjPart {

    private static final int STRIDE = 8;

    private final Geometry geometry;
    private float[] vertices;
    private int[] indices;

    public ClippedObjPart(WavefrontObj model, String part) {
        this.geometry = model.part(part);
    }

    /**
     * Emits the part clipped by {@code planes} (rows of {@code a, b, c, d} in frame space), with the
     * part itself translated by {@code offset} relative to that frame. The pose already carries the
     * translation; the planes are moved the other way here so the two agree.
     */
    public void draw(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, double[][] planes,
                     double offsetX, double offsetY, double offsetZ) {
        if (vertices == null) {
            vertices = geometry.vertices();
            indices = geometry.indices();
        }
        float[][] work = new float[9][STRIDE];
        float[][] swap = new float[9][STRIDE];
        for (int tri = 0; tri + 2 < indices.length; tri += 3) {
            int n = 3;
            for (int c = 0; c < 3; c++) {
                System.arraycopy(vertices, indices[tri + c] * STRIDE, work[c], 0, STRIDE);
            }
            for (double[] plane : planes) {
                double d = plane[3] + plane[0] * offsetX + plane[1] * offsetY + plane[2] * offsetZ;
                n = clip(work, n, swap, (float) plane[0], (float) plane[1], (float) plane[2], (float) d);
                float[][] t = work; work = swap; swap = t;
                if (n == 0) break;
            }
            if (n < 3) continue;
            // Fan the polygon out as triangles, each padded to a quad the way WavefrontObj does.
            for (int i = 1; i + 1 < n; i++) {
                emit(buffer, pose, work[0], light, overlay);
                emit(buffer, pose, work[i], light, overlay);
                emit(buffer, pose, work[i + 1], light, overlay);
                emit(buffer, pose, work[i + 1], light, overlay);
            }
        }
    }

    private static int clip(float[][] in, int n, float[][] out, float a, float b, float c, float d) {
        int count = 0;
        for (int i = 0; i < n; i++) {
            float[] current = in[i];
            float[] next = in[(i + 1) % n];
            float dc = a * current[0] + b * current[1] + c * current[2] + d;
            float dn = a * next[0] + b * next[1] + c * next[2] + d;
            if (dc >= 0) {
                System.arraycopy(current, 0, out[count++], 0, STRIDE);
            }
            if ((dc >= 0) != (dn >= 0)) {
                float t = dc / (dc - dn);
                float[] target = out[count++];
                for (int k = 0; k < STRIDE; k++) target[k] = current[k] + (next[k] - current[k]) * t;
            }
        }
        return count;
    }

    private static void emit(VertexConsumer buffer, PoseStack.Pose pose, float[] v, int light, int overlay) {
        Vertexes.vertex(buffer, pose, v[0], v[1], v[2], -1, v[3], v[4], overlay, light, v[5], v[6], v[7]);
    }
}
