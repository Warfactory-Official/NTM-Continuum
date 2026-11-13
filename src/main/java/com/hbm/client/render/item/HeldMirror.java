package com.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.GeometryPoints;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The off-hand turn of a held body: the frame's left hand mirrors it through {@code z = 1/2} (a sprite's middle),
 * and this replaces that plane by the body's own: of the three principal planes through its vertex centroid, the one
 * across the view it is most nearly symmetric about. The planes are measured once per item and hand slot.
 */
final class HeldMirror {

    private HeldMirror() {}

    /** A body's measured planes: centre, the three principal normals, and how far it is from symmetric about each. */
    private record Shape(Vector3f centre, Vector3f[] normals, double[] miss, double size) {}

    /** The frame's left-hand mirror through {@code z = 1/2}; its own inverse. */
    private static final Matrix4f FRAME_FLIP = new Matrix4f().translate(0F, 0F, 0.5F).scale(1F, 1F, -1F)
            .translate(0F, 0F, -0.5F);
    private static final Shape EMPTY = new Shape(new Vector3f(), new Vector3f[0], new double[0], 0);
    /** How far from symmetric a plane may be, as a share of the body's size, and still count as its mirror plane. */
    private static final double SYMMETRIC = 0.03;
    private static final Map<Item, Shape[]> CACHE = new ConcurrentHashMap<>();

    /**
     * The body-space turn for a left-hand slot drawn at {@code pose}, or null for any other slot or an empty body. A
     * {@link CustomItemRenderer#mirrorsExactly} body only has the frame's flip undone. Otherwise a body with a mirror
     * plane turns about it and keeps its front (a stop sign); one without turns about the plane facing the camera,
     * which keeps its outline and shows its other side, as a sprite does.
     */
    static @Nullable Matrix4f of(CustomItemRenderer renderer, ItemStack stack, ItemDisplayContext context,
                                 PoseStack.Pose pose, int light, int overlay, @Nullable LivingEntity owner) {
        int slot = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? 0
                : context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? 1 : -1;
        if (slot < 0) return null;
        if (renderer.mirrorsExactly()) return new Matrix4f(FRAME_FLIP);
        Shape[] shapes = CACHE.computeIfAbsent(stack.getItem(), item -> new Shape[2]);
        if (shapes[slot] == null) {
            float[] points = GeometryPoints.of(sink -> renderer.render(stack, context, new PoseStack(), sink, light,
                    overlay, owner));
            shapes[slot] = points.length == 0 ? EMPTY : shape(points);
        }
        Shape shape = shapes[slot];
        if (shape == EMPTY) return null;
        int best = 0;
        for (int i = 1; i < 3; i++) if (shape.miss()[i] < shape.miss()[best]) best = i;
        Vector3f normal = shape.normals()[best];
        if (shape.miss()[best] > SYMMETRIC * shape.size()) {
            Vector3f camera = new Matrix4f(pose.pose()).invert().transformPosition(new Vector3f());
            normal = new Vector3f(shape.centre()).sub(camera).normalize();
        }
        Vector3f centre = shape.centre();
        // the frame's mirror through z = 1/2 undone, then the mirror through the body's own plane
        return new Matrix4f(FRAME_FLIP).reflect(normal.x, normal.y, normal.z, centre.x, centre.y, centre.z);
    }

    private static Shape shape(float[] points) {
        int n = points.length / 3;
        Vector3f centre = new Vector3f();
        for (int i = 0; i < n; i++) centre.add(points[i * 3], points[i * 3 + 1], points[i * 3 + 2]);
        centre.div(n);
        double[][] spread = new double[3][3];
        for (int i = 0; i < n; i++) {
            double[] d = {points[i * 3] - centre.x, points[i * 3 + 1] - centre.y, points[i * 3 + 2] - centre.z};
            for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) spread[a][b] += d[a] * d[b];
        }
        Vector3f[] normals = axes(spread);
        double[] miss = new double[3];
        for (int i = 0; i < 3; i++) miss[i] = asymmetry(points, centre, normals[i]);
        Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
        for (int i = 0; i < n; i++) {
            min.min(new Vector3f(points[i * 3], points[i * 3 + 1], points[i * 3 + 2]));
            max.max(new Vector3f(points[i * 3], points[i * 3 + 1], points[i * 3 + 2]));
        }
        return new Shape(centre, normals, miss, max.distance(min));
    }

    /** Mean distance from a sample of the points, mirrored through the plane, to the nearest point. */
    private static double asymmetry(float[] points, Vector3f centre, Vector3f normal) {
        int n = points.length / 3;
        int step = Math.max(1, n / 400);
        double total = 0;
        int count = 0;
        Vector3f p = new Vector3f();
        for (int i = 0; i < n; i += step) {
            p.set(points[i * 3], points[i * 3 + 1], points[i * 3 + 2]);
            float d = 2F * normal.dot(p.x - centre.x, p.y - centre.y, p.z - centre.z);
            p.sub(normal.x * d, normal.y * d, normal.z * d);
            double nearest = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                double dx = points[j * 3] - p.x, dy = points[j * 3 + 1] - p.y, dz = points[j * 3 + 2] - p.z;
                nearest = Math.min(nearest, dx * dx + dy * dy + dz * dz);
            }
            total += Math.sqrt(nearest);
            count++;
        }
        return total / count;
    }

    /** The principal axes of a symmetric 3x3 spread (Jacobi rotations). */
    private static Vector3f[] axes(double[][] a) {
        double[][] v = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        for (int sweep = 0; sweep < 32; sweep++) {
            for (int p = 0; p < 2; p++) {
                for (int q = p + 1; q < 3; q++) {
                    if (Math.abs(a[p][q]) < 1e-12) continue;
                    double theta = 0.5 * Math.atan2(2 * a[p][q], a[q][q] - a[p][p]);
                    double c = Math.cos(theta), s = Math.sin(theta);
                    for (int k = 0; k < 3; k++) {
                        double kp = a[k][p], kq = a[k][q];
                        a[k][p] = c * kp - s * kq;
                        a[k][q] = s * kp + c * kq;
                    }
                    for (int k = 0; k < 3; k++) {
                        double pk = a[p][k], qk = a[q][k];
                        a[p][k] = c * pk - s * qk;
                        a[q][k] = s * pk + c * qk;
                    }
                    for (int k = 0; k < 3; k++) {
                        double kp = v[k][p], kq = v[k][q];
                        v[k][p] = c * kp - s * kq;
                        v[k][q] = s * kp + c * kq;
                    }
                }
            }
        }
        Vector3f[] out = new Vector3f[3];
        for (int i = 0; i < 3; i++) out[i] = new Vector3f((float) v[0][i], (float) v[1][i], (float) v[2][i]).normalize();
        return out;
    }
}
