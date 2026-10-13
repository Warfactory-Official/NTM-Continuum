package com.hbm.client.crucible;

import com.hbm.physics.V3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.Ids;
import dev.golem.core.render.NoiseDisplacedRenderType;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.12.2's crucible lightning: {@code LightningGenerator} (subdivided, forked bolt) and {@code TrailRenderer2} (the bolt
 * as a camera-facing ribbon of triangles), drawn through the {@code crucible_lightning} shader pair
 * ({@code assets/hbm/shaders/core/crucible_lightning.*}, Golem Core {@code NoiseDisplacedRenderType}).
 *
 * <p>The vertices carry what the old VAO carried: position, {@code tex} (u quantised to an unsigned short, as
 * {@code TrailRenderer2} wrote it) and the colour (white: {@code TrailRenderer2} resets its colour to {@code 1,1,1,1}
 * whenever no colour getter is given, so the {@code TrailRenderer2.color} values the crucible sets are never read). The
 * shader's {@code time} uniform, set per particle, rides each vertex.
 */
public final class CrucibleLightning {

    private static final Random RAND = new Random();

    private CrucibleLightning() {}

    public static final class Node {
        @Nullable Node parent;
        final List<Node> children = new ArrayList<>(1);
        V3 pos;

        Node(V3 pos) {
            this.pos = pos;
        }
    }

    public static final class Info {
        public int subdivisions = 4;
        public int subdivRecurse = 2;
        public float randAmount = 0.2F;
        public float forkChance = 0.1F;
        public float forkSubdivMult = 1F;
        public float forkSubdivisions = 1;
        public int forkSubdivRecurse = 1;
        public float forkLengthRandom = 4;
        public float forkRandAmount = 0.2F;
        public float forkRandAmountSubdivMultiplier = 0.25F;
        public float randAmountSubdivMultiplier = 0.25F;
        public float forkConeDegrees = 25;
        public float subdivMult = 1.5F;
    }

    /**
     * {@code LightningGenerator.generateLightning(from, to, info)}: the shared random is reseeded with the world's total
     * time ({@code LightningGenerator.java:20-28}).
     */
    static Node generate(long worldTime, V3 from, V3 to, Info info) {
        return generate(worldTime, from, to, info, false);
    }

    /**
     * {@code alongChain} (third person only, a port addition: 1.12.2 drew no third-person lightning): the same seed and
     * the same random draws, so the main chain is identical, but each fork leaves inside the cone around the bolt's own
     * direction ({@link #randVecInCone(V3, float, Random, boolean)} with {@code intended}). 1.12's forks leave along the
     * drawing space's {@code -y}, which first person sees head-on but which in the held third-person pose points at the
     * ground (the bolts hang down from the hilt).
     */
    static Node generate(long worldTime, V3 from, V3 to, Info info, boolean alongChain) {
        RAND.setSeed(worldTime);
        Node lfrom = new Node(from);
        Node lto = new Node(to);
        lfrom.children.add(lto);
        lto.parent = lfrom;
        generate(lfrom, info, alongChain);
        return lfrom;
    }

    /** {@code LightningGenerator.generateLightning(node, info)} ({@code LightningGenerator.java:30-51}). */
    private static void generate(Node node, Info info, boolean alongChain) {
        V3 from = node.pos;
        V3 to = node.children.get(0).pos;
        subdivide(node, info.subdivisions, info.subdivMult, info.subdivRecurse - 1, info.randAmount,
                info.randAmountSubdivMultiplier);
        Node child = node.children.get(0);
        float value = 0;
        while (!child.children.isEmpty()) {
            value += 0.02F;
            Node next = child.children.get(0);
            if (RAND.nextFloat() < info.forkChance - value) {
                V3 randVec = randVecInCone(normalize(to.subtract(from)), info.forkConeDegrees, RAND, alongChain);
                Node fork1 = new Node(child.pos);
                float len = 1 + RAND.nextFloat() * info.forkLengthRandom;
                Node fork2 = new Node(child.pos.add(randVec.scale(len * length(from.subtract(to)) * 0.25F)));
                fork1.children.add(fork2);
                fork2.parent = fork1;
                subdivide(fork1, (int) (len * 0.75 * info.forkSubdivisions), info.forkSubdivMult, info.forkSubdivRecurse,
                        info.forkRandAmount * info.randAmount * RAND.nextFloat() * 0.8F, info.forkRandAmountSubdivMultiplier);
                child.children.add(fork1);
            }
            child = next;
        }
    }

    private static void subdivide(Node n, int subdivisions, float subdivMult, int recurse, float randAmount,
                                  float randAmountSubdivMultiplier) {
        Node parent = n;
        Node child = n.children.get(0);
        float subdivision = 1F / (float) (subdivisions + 1);
        for (int i = 1; i <= subdivisions; i++) {
            V3 newPos = mix(n.pos, child.pos, subdivision * i).add((RAND.nextFloat() * 2 - 1) * randAmount,
                    (RAND.nextFloat() * 2 - 1) * randAmount, (RAND.nextFloat() * 2 - 1) * randAmount);
            Node insert = new Node(newPos);
            insert.parent = parent;
            insert.children.add(child);
            parent.children.set(0, insert);
            child.parent = insert;
            parent = insert;
        }
        if (recurse <= 0) return;
        child = n;
        while (!child.children.isEmpty()) {
            Node next = child.children.get(0);
            subdivide(child, (int) (subdivisions * subdivMult), subdivMult, recurse - 1,
                    randAmount * randAmountSubdivMultiplier, randAmountSubdivMultiplier);
            child = next;
        }
    }

    private static V3 mix(V3 a, V3 b, float amount) {
        return new V3(a.x() + (b.x() - a.x()) * amount, a.y() + (b.y() - a.y()) * amount, a.z() + (b.z() - a.z()) * amount);
    }

    /**
     * {@code BobMathUtil.randVecInCone(coneDirection, angle, rand)} ({@code BobMathUtil.java:411-424}) as written, with
     * {@code getEulerAngles} ({@code :275-280}) inlined: that already returns {@code pitch - 90} and the caller subtracts
     * 90 again, so the result leaves along {@code -y} of the drawing space for any horizontal cone axis. Not corrected:
     * this is what 1.12.2 draws.
     */
    static V3 randVecInCone(V3 coneDirection, float angle, Random rand) {
        return randVecInCone(coneDirection, angle, rand, false);
    }

    /**
     * {@code intended}: the cone {@code randVecInCone} sets out to build, around {@code coneDirection} (the second
     * {@code - 90} left out). Same random draws as the 1.12 form. Third person only.
     */
    static V3 randVecInCone(V3 coneDirection, float angle, Random rand, boolean intended) {
        V3 up = new V3(0, 1, 0);
        up = rotatePitch(up, (float) Math.toRadians(rand.nextFloat() * (angle + rand.nextFloat() * angle)));
        up = rotateYaw(up, (float) Math.toRadians(rand.nextFloat() * 360));
        double yaw = Math.toDegrees(Math.atan2(coneDirection.x(), coneDirection.z()));
        double sqrt = (float) Math.sqrt(coneDirection.x() * coneDirection.x() + coneDirection.z() * coneDirection.z());
        double pitch = Math.toDegrees(Math.atan2(coneDirection.y(), sqrt)) - 90;
        V3 newDirection = rotatePitch(up, (float) Math.toRadians(intended ? pitch : pitch - 90));
        return rotateYaw(newDirection, (float) Math.toRadians(yaw));
    }

    // 1.12 Vec3d / MathHelper arithmetic: float square roots and the 65,536-entry sine table

    private static final float[] SIN_TABLE = new float[65536];

    static {
        for (int i = 0; i < 65536; i++) SIN_TABLE[i] = (float) Math.sin((double) i * Math.PI * 2.0D / 65536.0D);
    }

    /** {@code MathHelper.sin}. */
    private static float sin(float value) {
        return SIN_TABLE[(int) (value * 10430.378F) & 65535];
    }

    /** {@code MathHelper.cos}. */
    private static float cos(float value) {
        return SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
    }

    /** {@code Vec3d.rotatePitch}. */
    static V3 rotatePitch(V3 v, float pitch) {
        float f = cos(pitch);
        float f1 = sin(pitch);
        return new V3(v.x(), v.y() * (double) f + v.z() * (double) f1, v.z() * (double) f - v.y() * (double) f1);
    }

    /** {@code Vec3d.rotateYaw}. */
    static V3 rotateYaw(V3 v, float yaw) {
        float f = cos(yaw);
        float f1 = sin(yaw);
        return new V3(v.x() * (double) f + v.z() * (double) f1, v.y(), v.z() * (double) f - v.x() * (double) f1);
    }

    /** {@code Vec3d.lengthVector}: {@code MathHelper.sqrt}, a float square root. */
    static double length(V3 v) {
        return (double) (float) Math.sqrt(v.x() * v.x() + v.y() * v.y() + v.z() * v.z());
    }

    /** {@code Vec3d.normalize}: float square root, shorter than 1e-4 becomes zero. */
    static V3 normalize(V3 v) {
        double d0 = length(v);
        return d0 < 1.0E-4D ? V3.ZERO : new V3(v.x() / d0, v.y() / d0, v.z() / d0);
    }

    /**
     * {@code LightningGenerator.render(node, playerPos, 0.0015F, 0, 0, 0, true, null)} through the shader: forks at
     * half width first, then the main chain, each a {@code TrailRenderer2} ribbon.
     */
    static void render(Node n, V3 playerPos, float scale, float time, PoseStack.Pose pose, VertexConsumer buffer,
                       boolean cpu) {
        List<V3> points = new ArrayList<>();
        points.add(n.pos);
        while (!n.children.isEmpty()) {
            for (int i = 1; i < n.children.size(); i++) render(n.children.get(i), playerPos, scale * 0.5F, time, pose, buffer, cpu);
            n = n.children.get(0);
            points.add(n.pos);
        }
        trail(playerPos, points, scale, time, pose, buffer, cpu);
    }

    /** {@code TrailRenderer2.draw(playerPos, points, scale, fadeEnd = true, fadeEnd2 = true, null)}. */
    private static void trail(V3 playerPos, List<V3> points, float scale, float time, PoseStack.Pose pose,
                              VertexConsumer buffer, boolean cpu) {
        if (points.size() < 2) return;
        List<float[]> verts = new ArrayList<>(points.size() * 3 + 2);
        V3 first = points.get(0);
        V3 cross = normalize(points.get(1).subtract(first).crossProduct(playerPos.subtract(first))).scale(scale * 0.1F);
        verts.add(vertex(first.add(cross), 0F));
        verts.add(vertex(first.add(cross.scale(-1)), 0F));
        for (int i = 1; i < points.size(); i++) {
            V3 last = points.get(i - 1);
            V3 current = points.get(i);
            V3 next = i < points.size() - 1 ? points.get(i + 1) : points.get(i);
            V3 toNext = current.subtract(last);
            V3 tangent = next.subtract(last);
            float iN = (float) i / (float) (points.size() - 1);
            float bruh = 1 - Math.max(0, Math.min(1, (iN - 0.8F) * 5));
            bruh *= Math.max(0, Math.min(1, iN * 5));
            cross = normalize(tangent.crossProduct(playerPos.subtract(last))).scale(scale * Math.max(bruh, 0.1));
            float uMiddle = (i - 0.5F) / (float) (points.size() - 1);
            verts.add(vertex(last.add(toNext.scale(0.5)), uMiddle));
            verts.add(vertex(current.add(cross), iN));
            verts.add(vertex(current.add(cross.scale(-1)), iN));
        }
        if (cpu) for (float[] v : verts) CpuReference.displace(v, time);
        for (int i = 0; i < points.size() - 1; i++) {
            int o = i * 3;
            triangle(pose, buffer, verts.get(o), verts.get(2 + o), verts.get(1 + o), time, cpu);
            triangle(pose, buffer, verts.get(o), verts.get(3 + o), verts.get(2 + o), time, cpu);
            triangle(pose, buffer, verts.get(2 + o), verts.get(3 + o), verts.get(4 + o), time, cpu);
            triangle(pose, buffer, verts.get(2 + o), verts.get(4 + o), verts.get(1 + o), time, cpu);
        }
    }

    /** x, y, z and {@code u} as {@code TrailRenderer2} quantised it to an unsigned short. */
    private static float[] vertex(V3 p, float u) {
        return new float[]{(float) p.x(), (float) p.y(), (float) p.z(), ((int) (u * 65535) & 0xFFFF) / 65535F};
    }

    /** One triangle as a degenerate quad (QUADS mode): position, {@code (tex, time)}, white. */
    private static void triangle(PoseStack.Pose pose, VertexConsumer buffer, float[] a, float[] b, float[] c, float time,
                                 boolean cpu) {
        NoiseDisplacedRenderType.vertex(buffer, pose, a[0], a[1], a[2], a[3], time, 0xFFFFFFFF);
        NoiseDisplacedRenderType.vertex(buffer, pose, b[0], b[1], b[2], b[3], time, 0xFFFFFFFF);
        NoiseDisplacedRenderType.vertex(buffer, pose, c[0], c[1], c[2], c[3], time, 0xFFFFFFFF);
        NoiseDisplacedRenderType.vertex(buffer, pose, c[0], c[1], c[2], c[3], time, 0xFFFFFFFF);
    }

    /**
     * The shader's arithmetic on the CPU, for the harness's pixel comparison only ({@code hbm_crucible
     * lightning_mode=cpu}): {@code pos + (noise_a * noise_b.zxy - 0.5) * 0.1} from {@code noise_2} sampled bilinear
     * with repeat at level 0 (1.12.2 set the magnification filter to {@code GL_LINEAR}), as the vertex stage
     * computes it. The displaced ribbon goes through the same render type with a flat noise texture (every texel
     * 180, an offset of -0.00017, well under a pixel), so draw order, blend and depth are identical and only the
     * displacement's origin differs. Never used in normal play.
     */
    static final class CpuReference {
        private static dev.golem.core.render.@Nullable TexturePixels noise;

        static void reset() {
            noise = null;
        }

        static void displace(float[] v, float time) {
            if (noise == null) {
                try {
                    noise = dev.golem.core.render.TexturePixels.read(Ids.of("hbm", "textures/misc/noise_2.png"));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            float s = v[3];
            float[] a = noise.sampleLinearRepeat(s + time * 0.001F, s + time * -0.02F);
            float[] b = noise.sampleLinearRepeat(s + time * 0.01F, s + time * 0.015F);
            float ar = a[1], ag = a[2], ab = a[3];
            float br = b[1], bg = b[2], bb = b[3];
            v[0] += (ar * bb - 0.5F) * 0.1F;
            v[1] += (ag * br - 0.5F) * 0.1F;
            v[2] += (ab * bg - 0.5F) * 0.1F;
        }
    }
}
