package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Shared molten-metal quad emitters for the foundry renderers (crucible pool, channel/caster levels,
 * pour streams). Draw these with {@code RenderTypes.entityTranslucentEmissive} — that pipeline is
 * no-cull (one winding shows both sides) and emissive (uniform brightness, the modern stand-in for the
 * old lighting-off + lightmap-240 TESR state). Do NOT emit reversed duplicate quads: two coplanar
 * windings z-fight, and the back face renders at 40% brightness (dark shimmer).
 */
public final class FoundryRenderHelper {

    private FoundryRenderHelper() {}

    /** Fullbright packed light (the old {@code setLightmapTextureCoords(240, 240)}). */
    public static final int FULLBRIGHT = 0xF000F0;

    /** An upward-facing quad spanning (x0,z0)..(x1,z1) at height {@code y}, UVs 0..1 (no-cull type = 2-sided). */
    public static void flatQuad(PoseStack.Pose pose, VertexConsumer buffer, float x0, float z0, float x1, float z1,
                                float y, int color) {
        // CCW seen from above (front face up).
        vertex(buffer, pose, x0, y, z0, 0F, 0F, color);
        vertex(buffer, pose, x0, y, z1, 0F, 1F, color);
        vertex(buffer, pose, x1, y, z1, 1F, 1F, color);
        vertex(buffer, pose, x1, y, z0, 1F, 0F, color);
    }

    /**
     * A falling molten stream: two crossed vertical quads centred at (cx, cz), from {@code yTop} down to
     * {@code yBottom}, {@code halfWidth} half-thickness.
     */
    public static void stream(PoseStack.Pose pose, VertexConsumer buffer, float cx, float cz,
                              float yTop, float yBottom, float halfWidth, int color) {
        // X-aligned sheet.
        verticalQuad(pose, buffer, cx - halfWidth, cz, cx + halfWidth, cz, yTop, yBottom, color);
        // Z-aligned sheet.
        verticalQuad(pose, buffer, cx, cz - halfWidth, cx, cz + halfWidth, yTop, yBottom, color);
    }

    /** A vertical quad between (x0,z0) and (x1,z1) spanning yTop..yBottom (no-cull type = 2-sided). */
    public static void verticalQuad(PoseStack.Pose pose, VertexConsumer buffer, float x0, float z0, float x1, float z1,
                                    float yTop, float yBottom, int color) {
        vertex(buffer, pose, x0, yBottom, z0, 0F, 1F, color);
        vertex(buffer, pose, x1, yBottom, z1, 1F, 1F, color);
        vertex(buffer, pose, x1, yTop, z1, 1F, 0F, color);
        vertex(buffer, pose, x0, yTop, z0, 0F, 0F, color);
    }

    /**
     * The falling molten stream — the 1.12.2 {@code ParticleFoundry} geometry, verbatim: a boxy
     * vertical column falling {@code length} blocks from the local origin, joined by a bent "faucet"
     * segment rising {@code base} and leaning {@code offset} back toward the machine (dir = the pour
     * direction). The texture V coordinate scrolls with real time (the old flowing animation), and the
     * colour is the molten colour brightened 70% toward white (the old particle's tint).
     * Draw at the pour point with the emissive lava_gray render type.
     */
    public static void pourStream(PoseStack.Pose pose, VertexConsumer buffer, com.hbm.lib.ForgeDirection dir,
                                  float length, float base, float offset, int moltenColor) {
        com.hbm.lib.ForgeDirection rot = dir.getRotation(com.hbm.lib.ForgeDirection.UP);
        float w = 0.09375F;  // the old particle animated 0.0625..0.125 over its life; fixed midpoint
        float g = 0.0625F;   // the old girth shrank 0.125..0; fixed midpoint

        int color = brighten(moltenColor);

        float dirXG = dir.offsetX * g;
        float dirZG = dir.offsetZ * g;
        float rotXW = rot.offsetX * w;
        float rotZW = rot.offsetZ * w;

        float uMin = 0.5F - w;
        float uMax = 0.5F + w;
        float vMin = 0.0F;
        float vMax = length;

        float add = (int) (System.currentTimeMillis() / 100 % 16) / 16F;

        // Each quad is emitted DOUBLE-wound (front + reversed): drawn on entitySolid (which culls, and
        // uses the vertex normal — all up — for uniform brightness), the cull keeps exactly the outward
        // face, so nothing disappears and every face is the same fullbright colour. (The old particle got
        // this for free with disableCull + fixed-function fullbright.)

        // Lower back
        quad(buffer, pose, color, rotXW, g, rotZW, uMax, vMax + add + g,
                -rotXW, g, -rotZW, uMin, vMax + add + g,
                -rotXW, -length, -rotZW, uMin, vMin + add,
                rotXW, -length, rotZW, uMax, vMin + add);

        // Lower front
        quad(buffer, pose, color, dirXG + rotXW, 0, dirZG + rotZW, uMax, vMax + add,
                dirXG - rotXW, 0, dirZG - rotZW, uMin, vMax + add,
                dirXG - rotXW, -length, dirZG - rotZW, uMin, vMin + add,
                dirXG + rotXW, -length, dirZG + rotZW, uMax, vMin + add);

        float wMin = 0.0F;
        float wMax = g;

        // Lower left
        quad(buffer, pose, color, rotXW, g, rotZW, wMin, vMax + add + g,
                dirXG + rotXW, 0, dirZG + rotZW, wMax, vMax + add,
                dirXG + rotXW, -length, dirZG + rotZW, wMax, vMin + add,
                rotXW, -length, rotZW, wMin, vMin + add);

        // Lower right
        quad(buffer, pose, color, -rotXW, g, -rotZW, wMin, vMax + add + g,
                dirXG - rotXW, 0, dirZG - rotZW, wMax, vMax + add,
                dirXG - rotXW, -length, dirZG - rotZW, wMax, vMin + add,
                -rotXW, -length, -rotZW, wMin, vMin + add);

        float dirOX = dir.offsetX * offset;
        float dirOZ = dir.offsetZ * offset;

        vMax = offset;

        // Upper back
        quad(buffer, pose, color, rotXW, 0, rotZW, uMax, vMax - add,
                -rotXW, 0, -rotZW, uMin, vMax - add,
                -rotXW - dirOX, base, -rotZW - dirOZ, uMin, vMin - add,
                rotXW - dirOX, base, rotZW - dirOZ, uMax, vMin - add);

        // Upper front
        quad(buffer, pose, color, rotXW, g, rotZW, uMax, vMax - add + 0.25F,
                -rotXW, g, -rotZW, uMin, vMax - add + 0.25F,
                -rotXW - dirOX, base + g, -rotZW - dirOZ, uMin, vMin - add + 0.25F,
                rotXW - dirOX, base + g, rotZW - dirOZ, uMax, vMin - add + 0.25F);

        // Upper left
        quad(buffer, pose, color, rotXW, 0, rotZW, wMax, vMax - add + 0.75F,
                rotXW, g, rotZW, wMin, vMax - add + 0.75F,
                rotXW - dirOX, base + g, rotZW - dirOZ, wMin, vMin - add + 0.75F,
                rotXW - dirOX, base, rotZW - dirOZ, wMax, vMin - add + 0.75F);

        // Upper right
        quad(buffer, pose, color, -rotXW, 0, -rotZW, wMax, vMax - add + 0.75F,
                -rotXW, g, -rotZW, wMin, vMax - add + 0.75F,
                -rotXW - dirOX, base + g, -rotZW - dirOZ, wMin, vMin - add + 0.75F,
                -rotXW - dirOX, base, -rotZW - dirOZ, wMax, vMin - add + 0.75F);

        // Bend
        quad(buffer, pose, color, dirXG + rotXW, 0, dirZG + rotZW, uMax, add + 0.75F,
                dirXG - rotXW, 0, dirZG - rotZW, uMin, add + 0.75F,
                -rotXW, g, -rotZW, uMin, 0.125F + add + 0.75F,
                rotXW, g, rotZW, uMax, 0.125F + add + 0.75F);
    }

    /** Emits a quad and its reverse winding, so a culling render type keeps whichever side faces the
     * camera (used for the opaque pour stream on {@code entitySolid}). */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int color,
                             float x0, float y0, float z0, float u0, float v0,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3) {
        vertex(buffer, pose, x0, y0, z0, u0, v0, color);
        vertex(buffer, pose, x1, y1, z1, u1, v1, color);
        vertex(buffer, pose, x2, y2, z2, u2, v2, color);
        vertex(buffer, pose, x3, y3, z3, u3, v3, color);
        vertex(buffer, pose, x3, y3, z3, u3, v3, color);
        vertex(buffer, pose, x2, y2, z2, u2, v2, color);
        vertex(buffer, pose, x1, y1, z1, u1, v1, color);
        vertex(buffer, pose, x0, y0, z0, u0, v0, color);
    }

    /** The old particle tint: {@code new Color(hex).brighter()} then blended 70% toward white. */
    public static int brighten(int hex) {
        int r = (hex >> 16) & 0xFF, gr = (hex >> 8) & 0xFF, b = hex & 0xFF;
        // Color.brighter(): scale by 1/0.7 (with the small dark-channel floor), clamped.
        r = Math.min(255, Math.max(r, 4) * 10 / 7);
        gr = Math.min(255, Math.max(gr, 4) * 10 / 7);
        b = Math.min(255, Math.max(b, 4) * 10 / 7);
        // then 70% toward white (the particle's brightener pass)
        r = (int) (255 - (255 - r) * 0.7D);
        gr = (int) (255 - (255 - gr) * 0.7D);
        b = (int) (255 - (255 - b) * 0.7D);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z, float u, float v, int color) {
        buffer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULLBRIGHT)
                .setNormal(pose, 0F, 1F, 0F);
    }
}
