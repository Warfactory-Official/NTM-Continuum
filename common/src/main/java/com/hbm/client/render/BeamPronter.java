package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The 1.12.2 {@code BeamPronter} SOLID/STRAIGHT beam, ported to the 26.1 submit API. The original drew
 * layered concentric quad prisms (4 axis-plane quads per layer) in POSITION_COLOR with additive blending,
 * no depth write and no cull; {@code RenderTypes.dragonRays()} is the exact modern pipeline match
 * (POSITION_COLOR triangles, {@code BlendFunction.LIGHTNING} additive, depth-write OFF) — each quad is
 * emitted as two triangles, double-wound to reproduce the original's cull-off visibility.
 *
 * <p>Layer colours interpolate inner→outer ({@code BobMathUtil.interpolateColor}), so a 3-layer beam is a
 * white core wrapped in progressively outer-coloured shells whose additive overlap forms the hot centre.
 */
public final class BeamPronter {

    private BeamPronter() {}

    /**
     * Draws a straight solid beam from the current pose origin along {@code skeleton} (the old
     * {@code prontBeam(skeleton, STRAIGHT, SOLID, outerColor, innerColor, start, 1, 0, layers, thickness)}).
     */
    public static void prontBeam(PoseStack ps, SubmitNodeCollector collector, Vec3 skeleton,
                                 int outerColor, int innerColor, int layers, float thickness) {

        ps.pushPose();

        // Align +Y with the beam vector (the old rotate(180,Y) → rotate(sYaw,Y) → rotate(sPitch-90,X)).
        float sYaw = (float) Math.toDegrees(Math.atan2(skeleton.x, skeleton.z));
        float sqrt = (float) Math.sqrt(skeleton.x * skeleton.x + skeleton.z * skeleton.z);
        float sPitch = (float) Math.toDegrees(Math.atan2(skeleton.y, sqrt));

        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(sYaw));
        ps.mulPose(Axis.XP.rotationDegrees(sPitch - 90));

        float length = (float) skeleton.length();
        float radius = thickness / layers;

        collector.submitCustomGeometry(ps, RenderTypes.dragonRays(), (pose, buffer) -> {
            for (int j = 1; j <= layers; j++) {
                int color = layers == 1 ? outerColor
                        : interpolateColor(innerColor, outerColor, (float) (j - 1) / (layers - 1));
                float radJ = radius * j;

                // The 4 axis-plane quads of the old SOLID beam (single segment: 0 → length along +Y).
                quad(pose, buffer, color, +radJ, +radJ, +radJ, -radJ, length);   // x = +radJ plane
                quad(pose, buffer, color, -radJ, +radJ, -radJ, -radJ, length);   // x = -radJ plane
                quadZ(pose, buffer, color, +radJ, length);                        // z = +radJ plane
                quadZ(pose, buffer, color, -radJ, length);                        // z = -radJ plane
            }
        });

        ps.popPose();
    }

    /** A vertical quad in the x=const plane between z0/z1 from y=0 to y=len (both windings). */
    private static void quad(PoseStack.Pose pose, VertexConsumer buf, int color,
                             float x0, float z0, float x1, float z1, float len) {
        tris(pose, buf, color,
                x0, 0, z0, x1, 0, z1, x1, len, z1, x0, len, z0);
    }

    /** A vertical quad in the z=const plane spanning x=-|z| .. +|z| from y=0 to y=len (both windings). */
    private static void quadZ(PoseStack.Pose pose, VertexConsumer buf, int color, float z, float len) {
        float r = Math.abs(z);
        tris(pose, buf, color,
                +r, 0, z, -r, 0, z, -r, len, z, +r, len, z);
    }

    /** Emits the quad (a,b,c,d) as two triangles, in both windings (the old no-cull additive pass). */
    private static void tris(PoseStack.Pose pose, VertexConsumer buf, int color,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        int argb = 0xFF000000 | (color & 0xFFFFFF);
        // front winding
        vert(pose, buf, argb, ax, ay, az); vert(pose, buf, argb, bx, by, bz); vert(pose, buf, argb, cx, cy, cz);
        vert(pose, buf, argb, ax, ay, az); vert(pose, buf, argb, cx, cy, cz); vert(pose, buf, argb, dx, dy, dz);
        // reverse winding (visible from the far side; cull discards whichever faces away)
        vert(pose, buf, argb, cx, cy, cz); vert(pose, buf, argb, bx, by, bz); vert(pose, buf, argb, ax, ay, az);
        vert(pose, buf, argb, dx, dy, dz); vert(pose, buf, argb, cx, cy, cz); vert(pose, buf, argb, ax, ay, az);
    }

    private static void vert(PoseStack.Pose pose, VertexConsumer buf, int argb, float x, float y, float z) {
        buf.addVertex(pose, x, y, z).setColor(argb);
    }

    /** Per-channel linear colour interpolation (the old {@code BobMathUtil.interpolateColor}). */
    private static int interpolateColor(int from, int to, float inter) {
        int r = Mth.lerpInt(inter, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = Mth.lerpInt(inter, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = Mth.lerpInt(inter, from & 0xFF, to & 0xFF);
        return (r << 16) | (g << 8) | b;
    }
}
