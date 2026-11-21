package com.hbm.client.render.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Geometry;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * One side of a mesh cut by a horizontal plane: an armour arm cut at the elbow so its forearm can follow a bent arm.
 */
final class SplitGeometry implements Geometry {
    private final float[] vertices;
    private final int[] indices;
    private final String name;

    private SplitGeometry(float[] vertices, String name) {
        this.vertices = vertices;
        this.name = name;
        this.indices = new int[vertices.length / FLOATS_PER_VERTEX];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
    }

    /**
     * The part of {@code whole} above ({@code above}, smaller y: model space points down) or below the plane {@code y}; null when nothing is there.
     * Triangles crossing the plane are cut on it (position, texture and normal interpolated), so each half ends at the plane and a long face down the
     * whole arm is shared between them.
     */
    static @Nullable SplitGeometry of(Geometry whole, float y, boolean above) {
        float[] src = whole.vertices();
        int[] idx = whole.indices();
        float[] out = new float[idx.length * FLOATS_PER_VERTEX * 3];
        int n = 0;
        float[][] poly = new float[4][];
        for (int t = 0; t + 2 < idx.length; t += 3) {
            int count = 0;
            for (int k = 0; k < 3; k++) {
                float[] a = vertex(src, idx[t + k]), b = vertex(src, idx[t + (k + 1) % 3]);
                boolean inA = a[1] < y == above, inB = b[1] < y == above;
                if (inA) poly[count++] = a;
                if (inA != inB) poly[count++] = lerp(a, b, (y - a[1]) / (b[1] - a[1]));
            }
            for (int k = 1; k + 1 < count; k++) {          // fan: at most a quad, two triangles
                for (float[] v : new float[][]{poly[0], poly[k], poly[k + 1]}) {
                    if (n + FLOATS_PER_VERTEX > out.length) out = Arrays.copyOf(out, out.length * 2);
                    System.arraycopy(v, 0, out, n, FLOATS_PER_VERTEX);
                    n += FLOATS_PER_VERTEX;
                }
            }
        }
        return n == 0 ? null : new SplitGeometry(Arrays.copyOf(out, n), whole.name() + (above ? "#above" : "#below") + y);
    }

    private static float[] vertex(float[] src, int i) {
        return Arrays.copyOfRange(src, i * FLOATS_PER_VERTEX, (i + 1) * FLOATS_PER_VERTEX);
    }

    private static float[] lerp(float[] a, float[] b, float t) {
        float[] v = new float[FLOATS_PER_VERTEX];
        for (int i = 0; i < FLOATS_PER_VERTEX; i++) v[i] = a[i] + (b[i] - a[i]) * t;
        return v;
    }

    @Override
    public void draw(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        // each triangle as a quad, its last corner twice, as WavefrontObj draws for the QUADS armour types
        for (int t = 0; t < indices.length; t += 3) {
            emit(t, pose, buffer, light, overlay, color);
            emit(t + 1, pose, buffer, light, overlay, color);
            emit(t + 2, pose, buffer, light, overlay, color);
            emit(t + 2, pose, buffer, light, overlay, color);
        }
    }

    private void emit(int i, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        int o = i * FLOATS_PER_VERTEX;
        Vertexes.vertex(buffer, pose, vertices[o], vertices[o + 1], vertices[o + 2], color, vertices[o + 3], vertices[o + 4], overlay, light,
                vertices[o + 5], vertices[o + 6], vertices[o + 7]);
    }

    @Override
    public float[] vertices() {
        return vertices;
    }

    @Override
    public int[] indices() {
        return indices;
    }

    @Override
    public int vertexCount() {
        return indices.length;
    }

    @Override
    public String name() {
        return name;
    }
}
