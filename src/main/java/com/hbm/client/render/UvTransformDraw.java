package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.Vertexes;
import dev.golem.core.render.gem.Geometry;

/**
 * Draws a {@link Geometry} with its texture coordinates run through what 1.7 set on the GL texture matrix:
 * {@code uv' = uv * scale + offset}. {@code WavefrontObj.renderPart} bakes an offset alone; the geothermal
 * core also scales ({@code glScalef(0.5, 0.5, 0.5)} before the scroll), so the transform is applied here
 * to the mesh the Gem geometry already holds, vertex by vertex.
 */
public final class UvTransformDraw {

    private UvTransformDraw() {}

    /**
     * @param color ARGB vertex tint, or {@code -1} for none
     * @param scale the texture-matrix scale, applied first
     * @param uOffset the texture-matrix translation, applied before the scale as GL composed it
     */
    static void draw(Geometry geometry, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                     int color, float scale, float uOffset, float vOffset) {
        float[] vertices = geometry.vertices();
        int[] indices = geometry.indices();
        // Triangles, emitted as quads with the third vertex doubled, the way WavefrontObj fans its faces.
        for (int i = 0; i + 2 < indices.length; i += 3) {
            emit(vertices, indices[i], pose, buffer, light, overlay, color, scale, uOffset, vOffset);
            emit(vertices, indices[i + 1], pose, buffer, light, overlay, color, scale, uOffset, vOffset);
            emit(vertices, indices[i + 2], pose, buffer, light, overlay, color, scale, uOffset, vOffset);
            emit(vertices, indices[i + 2], pose, buffer, light, overlay, color, scale, uOffset, vOffset);
        }
    }

    private static void emit(float[] vertices, int index, PoseStack.Pose pose, VertexConsumer buffer, int light,
                             int overlay, int color, float scale, float uOffset, float vOffset) {
        int base = index * 8;
        // GL: glScalef then glTranslatef gives M = S * T, so uv' = S * (uv + t).
        float u = (vertices[base + 3] + uOffset) * scale;
        float v = (vertices[base + 4] + vOffset) * scale;
        Vertexes.vertex(buffer, pose, vertices[base], vertices[base + 1], vertices[base + 2], color, u, v,
                overlay, light, vertices[base + 5], vertices[base + 6], vertices[base + 7]);
    }
}
