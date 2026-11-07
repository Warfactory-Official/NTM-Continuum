package com.hbm.client.render;

import net.minecraft.util.Mth;
import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * The 1.12.2 {@code BeamPronter} SOLID/STRAIGHT beam, ported to the 26.1 submit API. The original drew
 * layered concentric quad prisms (4 axis-plane quads per layer) in POSITION_COLOR with additive blending,
 * no depth write and {@code glDisable(GL_CULL_FACE)}; {@code CompatRenderTypes.additiveTriangles()} is that state.
 * Not {@code dragonRays()}: it culls, and the prism faces are not consistently wound (opposite faces share a
 * vertex order), so a culled beam lost faces by view direction and faded out as the camera circled it.
 * The source emitted each quad once with culling disabled, so the submit path does not duplicate its additive energy.
 *
 * <p>The legacy helper also emitted randomized/spiral line beams. Those use the same thin crossed-prism
 * geometry here because the modern lightning pipeline does not expose the old immediate-mode line width.
 */
public final class BeamPronter {

    private BeamPronter() {}
    /** Authored machine-beam opacity; ordinary beams and line geometry keep their own opacity. */
    public static float alpha = 1F;

    public static void prontMachineBeam(PoseStack ps, SubmitSink collector, Vec3 skeleton,
                                        int outerColor, int innerColor, int layers, float thickness) {
        prontMachineBeam(ps, collector, skeleton, EnumWaveType.STRAIGHT, EnumBeamType.SOLID,
                0, 1, 0F, outerColor, innerColor, layers, thickness);
    }

    public static void prontMachineBeam(PoseStack ps, SubmitSink collector, Vec3 skeleton,
                                        EnumWaveType wave, EnumBeamType beam, int start, int segments,
                                        float size, int outerColor, int innerColor, int layers, float thickness) {
        prontBeam(ps, collector, skeleton, wave, beam, start, segments, size, outerColor, innerColor,
                layers, thickness, alpha);
    }

    /**
     * Draws a straight solid beam from the current pose origin along {@code skeleton} (the old
     * {@code prontBeam(skeleton, STRAIGHT, SOLID, outerColor, innerColor, start, 1, 0, layers, thickness)}).
     */
    public static void prontBeam(PoseStack ps, SubmitSink collector, Vec3 skeleton,
                                 int outerColor, int innerColor, int layers, float thickness) {
        prontBeam(ps, collector, skeleton, EnumWaveType.STRAIGHT, EnumBeamType.SOLID,
                0, 1, 0F, outerColor, innerColor, layers, thickness);
    }

    /** Full legacy beam contract used by the Exposure Chamber's animated line effects. */
    public static void prontBeam(PoseStack ps, SubmitSink collector, Vec3 skeleton,
                                 EnumWaveType wave, EnumBeamType beam, int start, int segments,
                                 float size, int outerColor, int innerColor, int layers, float thickness) {
        prontBeam(ps, collector, skeleton, wave, beam, start, segments, size, outerColor, innerColor,
                layers, thickness, 1F);
    }

    public static void prontBeam(PoseStack ps, SubmitSink collector, Vec3 skeleton,
                                 EnumWaveType wave, EnumBeamType beam, int start, int segments,
                                 float size, int outerColor, int innerColor, int layers, float thickness,
                                 float opacity) {
        pront(ps, collector, skeleton, wave, beam, start, segments, size, outerColor, innerColor, layers, thickness,
                opacity, CompatRenderTypes.additiveTriangles());
    }

    /** A projectile beam: 1.7's {@code RenderBeam} draws every beam renderer inside {@code glDisable(GL_FOG)}. */
    public static void prontBeamWithoutFog(PoseStack ps, SubmitSink collector, Vec3 skeleton,
                                           EnumWaveType wave, EnumBeamType beam, int start, int segments,
                                           float size, int outerColor, int innerColor, int layers, float thickness) {
        pront(ps, collector, skeleton, wave, beam, start, segments, size, outerColor, innerColor, layers, thickness,
                1F, CompatRenderTypes.additiveTrianglesWithoutFog());
    }

    private static void pront(PoseStack ps, SubmitSink collector, Vec3 skeleton,
                              EnumWaveType wave, EnumBeamType beam, int start, int segments,
                              float size, int outerColor, int innerColor, int layers, float thickness,
                              float opacity, RenderType type) {

        ps.pushPose();

        // Align +Y with the beam vector (the old rotate(180,Y) → rotate(sYaw,Y) → rotate(sPitch-90,X)).
        float sYaw = (float) Math.toDegrees(Math.atan2(skeleton.x, skeleton.z));
        float sqrt = (float) Math.sqrt(skeleton.x * skeleton.x + skeleton.z * skeleton.z);
        float sPitch = (float) Math.toDegrees(Math.atan2(skeleton.y, sqrt));

        ps.mulPose(Axis.YP.rotationDegrees(180));
        ps.mulPose(Axis.YP.rotationDegrees(sYaw));
        ps.mulPose(Axis.XP.rotationDegrees(sPitch - 90));

        int segmentCount = Math.max(1, segments);
        float segmentLength = (float) skeleton.length() / segmentCount;

        if (beam == EnumBeamType.LINE) {
            collector.submitCustomGeometry(ps, type,
                    (pose, buffer) -> drawLineBeam(pose, buffer, wave, start, size, segmentCount, segmentLength,
                            outerColor, innerColor));
        } else {
            collector.submitCustomGeometry(ps, type,
                    (pose, buffer) -> drawSolidBeam(pose, buffer, wave, start, size, layers, thickness,
                            segmentCount, segmentLength, outerColor, innerColor, opacity));
        }

        ps.popPose();
    }

    private static void drawSolidBeam(PoseStack.Pose pose, VertexConsumer buffer, EnumWaveType wave, int start,
                                      float size, int layers, float thickness, int segments, float segmentLength,
                                      int outerColor, int innerColor, float opacity) {
        opacity = Mth.clamp(opacity, 0F, 1F);
        Random random = new Random(start);
        float lastX = 0F, lastY = 0F, lastZ = 0F;
        float radius = layers <= 0 ? 0F : thickness / layers;
        for (int i = 0; i <= segments; i++) {
            float[] offset = waveOffset(wave, size, start, i, random);
            float x = offset[0];
            float y = segmentLength * i;
            float z = offset[1];
            if (i > 0) {
                for (int layer = 1; layer <= layers; layer++) {
                    float inter = layers <= 1 ? 0F : (float) (layer - 1) / (layers - 1);
                    // 1.7 lerps outer -> inner as the layer grows: the thinnest prism takes the outer colour and the
                    // widest the inner one. Under additive blending the core is the sum of every layer either way,
                    // so what this decides is the colour of the beam's edge.
                    int color = layers == 1 ? outerColor : interpolateColor(outerColor, innerColor, inter);
                    segmentPrism(pose, buffer, color, lastX, lastY, lastZ, x, y, z, radius * layer, opacity);
                }
            }
            lastX = x;
            lastY = y;
            lastZ = z;
        }
    }

    private static void drawLineBeam(PoseStack.Pose pose, VertexConsumer buffer, EnumWaveType wave, int start,
                                     float size, int segments, float segmentLength, int outerColor, int innerColor) {
        Random random = new Random(start);
        float lastX = 0F, lastY = 0F, lastZ = 0F;
        for (int i = 0; i <= segments; i++) {
            float[] offset = waveOffset(wave, size, start, i, random);
            float x = offset[0];
            float y = segmentLength * i;
            float z = offset[1];
            if (i > 0) segmentPrism(pose, buffer, outerColor, lastX, lastY, lastZ, x, y, z, 0.01F, 1F);
            lastX = x;
            lastY = y;
            lastZ = z;
        }
        segmentPrism(pose, buffer, innerColor, 0F, 0F, 0F, 0F, segmentLength * segments, 0F, 0.01F, 1F);
    }

    private static float[] waveOffset(EnumWaveType wave, float size, int start, int segment, Random random) {
        if (wave == EnumWaveType.SPIRAL) {
            double angle = Math.toRadians(start + 45F * segment);
            return new float[]{(float) (Math.cos(angle) * size), (float) (-Math.sin(angle) * size)};
        }
        if (wave == EnumWaveType.RANDOM) {
            double angle = (random.nextFloat() + random.nextFloat()) * Math.PI * 2D;
            return new float[]{(float) (Math.cos(angle) * size), (float) (-Math.sin(angle) * size)};
        }
        return new float[]{0F, 0F};
    }

    private static void segmentPrism(PoseStack.Pose pose, VertexConsumer buffer, int color,
                                     float x0, float y0, float z0, float x1, float y1, float z1, float radius,
                                     float opacity) {
        quad(pose, buffer, color, x0 + radius, y0, z0 + radius, x0 + radius, y0, z0 - radius,
                x1 + radius, y1, z1 - radius, x1 + radius, y1, z1 + radius, opacity);
        quad(pose, buffer, color, x0 - radius, y0, z0 + radius, x0 - radius, y0, z0 - radius,
                x1 - radius, y1, z1 - radius, x1 - radius, y1, z1 + radius, opacity);
        quad(pose, buffer, color, x0 + radius, y0, z0 + radius, x0 - radius, y0, z0 + radius,
                x1 - radius, y1, z1 + radius, x1 + radius, y1, z1 + radius, opacity);
        quad(pose, buffer, color, x0 + radius, y0, z0 - radius, x0 - radius, y0, z0 - radius,
                x1 - radius, y1, z1 - radius, x1 + radius, y1, z1 - radius, opacity);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, int color,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float opacity) {
        tris(pose, buffer, color, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, opacity);
    }

    /** Emits the quad (a,b,c,d) as two triangles, matching the source's single no-cull quad. */
    private static void tris(PoseStack.Pose pose, VertexConsumer buf, int color,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float opacity) {
        // The source disables culling instead of submitting a second winding.
        vert(pose, buf, color, ax, ay, az, opacity);
        vert(pose, buf, color, bx, by, bz, opacity);
        vert(pose, buf, color, cx, cy, cz, opacity);
        vert(pose, buf, color, ax, ay, az, opacity);
        vert(pose, buf, color, cx, cy, cz, opacity);
        vert(pose, buf, color, dx, dy, dz, opacity);
    }

    private static void vert(PoseStack.Pose pose, VertexConsumer buf, int color, float x, float y, float z,
                             float opacity) {
        // Source beam tables use RGB literals; modern VertexConsumer.setColor(int) reads ARGB.
        // Preserve explicit alpha for fading callers, but never turn an RGB beam fully transparent.
        int argb = (color & 0xFF000000) == 0 ? 0xFF000000 | (color & 0xFFFFFF) : color;
        int coverage = (int) ((argb >>> 24) * opacity);
        Vertexes.color(buf, pose, x, y, z, coverage << 24 | argb & 0xFFFFFF);
    }

    /** Per-channel linear colour interpolation (the old {@code BobMathUtil.interpolateColor}). */
    private static int interpolateColor(int from, int to, float inter) {
        int a = (int) ((from >> 24 & 0xFF) + ((to >> 24 & 0xFF) - (from >> 24 & 0xFF)) * inter);
        int r = (int) ((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * inter);
        int g = (int) ((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * inter);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * inter);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public enum EnumWaveType { STRAIGHT, RANDOM, SPIRAL }
    public enum EnumBeamType { SOLID, LINE }
}
