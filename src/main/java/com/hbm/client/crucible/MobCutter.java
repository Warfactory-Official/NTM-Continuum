package com.hbm.client.crucible;

import com.hbm.physics.Box;
import com.hbm.physics.CollisionSource;
import com.hbm.physics.ConvexMeshCollider;
import com.hbm.physics.GJK;
import com.hbm.physics.Mat3;
import com.hbm.physics.RigidBody;
import com.hbm.physics.V3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The geometry half of old-hbm {@code ModelRendererUtil.generateCutParticles}: every cube of the victim's posed model
 * is cut by the plane into two convex pieces plus the cap polygon between them, the pieces on each side are grouped
 * into touching islands, and each island becomes a rigid body thrown 3.5 along the plane normal (the far side against
 * it). Plain math over {@code float[]} cubes ({@code EntityMeshes}' 24 vertices of x, y, z, u, v); no Minecraft type.
 */
public final class MobCutter {

    private MobCutter() {}

    /** {@code Triangle.TexVertex}. */
    public record TexVertex(V3 pos, float texX, float texY) {}

    /** {@code render.util.Triangle}. */
    public record Triangle(TexVertex p1, TexVertex p2, TexVertex p3) {
        Triangle(V3 a, V3 b, V3 c, float[] tex) {
            this(new TexVertex(a, tex[0], tex[1]), new TexVertex(b, tex[2], tex[3]), new TexVertex(c, tex[4], tex[5]));
        }
    }

    /** {@code ModelRendererUtil.VertexData}: indexed, epsilon-merged positions and per-corner texture coordinates. */
    public static final class VertexData {
        public V3[] positions = new V3[0];
        public int[] positionIndices = new int[0];
        public float[] texCoords = new float[0];

        public float[] vertexArray() {
            float[] verts = new float[positions.length * 3];
            for (int i = 0; i < positions.length; i++) {
                verts[i * 3] = (float) positions[i].x();
                verts[i * 3 + 1] = (float) positions[i].y();
                verts[i * 3 + 2] = (float) positions[i].z();
            }
            return verts;
        }

        public boolean isEmpty() {
            return positionIndices.length == 0;
        }
    }

    /** {@code CutModelData}. */
    public static final class CutModelData {
        public final VertexData data;
        public VertexData cap;
        public final boolean flip;
        public final ConvexMeshCollider collider;

        CutModelData(VertexData data, boolean flip, ConvexMeshCollider collider) {
            this.data = data;
            this.flip = flip;
            this.collider = collider;
        }
    }

    /** One thrown piece: its body and the pieces drawn with it. */
    public record Piece(RigidBody body, List<CutModelData> parts) {}

    /** What a cut produced: the thrown pieces and the top side's cap triangles (for blood and sparks). */
    public record Result(List<Piece> pieces, List<Triangle> capTriangles) {}

    /**
     * {@code generateCutParticles}: {@code plane} is {@code (nx, ny, nz, d)} in the victim's space; {@code x, y, z} the
     * victim's position, which every body starts from.
     */
    public static Result cut(List<float[]> cubes, float[] plane, double x, double y, double z, CollisionSource world) {
        List<CutModelData> top = new ArrayList<>();
        List<CutModelData> bottom = new ArrayList<>();
        for (float[] cube : cubes) {
            VertexData[] dat = cutAndCapConvex(triangulate(cube), plane);
            CutModelData tp = null;
            CutModelData bt = null;
            if (!dat[0].isEmpty()) {
                tp = new CutModelData(dat[0], false, new ConvexMeshCollider(dat[0].positionIndices, dat[0].vertexArray(), 1));
                top.add(tp);
            }
            if (!dat[1].isEmpty()) {
                bt = new CutModelData(dat[1], true, new ConvexMeshCollider(dat[1].positionIndices, dat[1].vertexArray(), 1));
                bottom.add(bt);
            }
            if (!dat[2].isEmpty()) {
                if (tp != null) tp.cap = dat[2];
                if (bt != null) bt.cap = dat[2];
            }
        }

        List<Triangle> capTris = new ArrayList<>();
        for (CutModelData d : top) {
            if (d.cap != null) capTris.addAll(decompress(d.cap));
        }

        List<List<CutModelData>> chunks = new ArrayList<>();
        generateChunks(chunks, top);
        generateChunks(chunks, bottom);

        List<Piece> pieces = new ArrayList<>();
        for (List<CutModelData> l : chunks) {
            float scale = 3.5F;
            if (l.get(0).flip) scale = -scale;
            List<ConvexMeshCollider> colliders = new ArrayList<>();
            for (CutModelData dat : l) {
                // see ConvexMeshCollider.isDegenerate: a flat sliver has no mass to give the body
                if (!dat.collider.isDegenerate()) colliders.add(dat.collider);
            }
            if (colliders.isEmpty()) continue;
            RigidBody body = new RigidBody(world, x, y, z);
            body.addColliders(colliders.toArray(new ConvexMeshCollider[0]));
            body.impulseVelocityDirect(new V3(plane[0] * scale, plane[1] * scale, plane[2] * scale), body.globalCentroid);
            pieces.add(new Piece(body, l));
        }
        return new Result(pieces, capTris);
    }

    /** {@code triangulate(ModelBox, matrix)}: each quad {@code v0 v1 v2 v3} as {@code (v0, v1, v2)} and {@code (v2, v3, v0)}. */
    static Triangle[] triangulate(float[] cube) {
        int quads = cube.length / 20;
        Triangle[] tris = new Triangle[quads * 2];
        for (int q = 0; q < quads; q++) {
            V3[] v = new V3[4];
            float[] u = new float[4];
            float[] w = new float[4];
            for (int k = 0; k < 4; k++) {
                int o = (q * 4 + k) * 5;
                v[k] = new V3(cube[o], cube[o + 1], cube[o + 2]);
                u[k] = cube[o + 3];
                w[k] = cube[o + 4];
            }
            tris[q * 2] = new Triangle(v[0], v[1], v[2], new float[]{u[0], w[0], u[1], w[1], u[2], w[2]});
            tris[q * 2 + 1] = new Triangle(v[2], v[3], v[0], new float[]{u[2], w[2], u[3], w[3], u[0], w[0]});
        }
        return tris;
    }

    static List<Triangle> decompress(VertexData vertices) {
        List<Triangle> tris = new ArrayList<>(vertices.positionIndices.length / 3);
        for (int i = 0; i < vertices.positionIndices.length; i += 3) {
            float[] tex = new float[]{
                    vertices.texCoords[i * 2], vertices.texCoords[i * 2 + 1],
                    vertices.texCoords[(i + 1) * 2], vertices.texCoords[(i + 1) * 2 + 1],
                    vertices.texCoords[(i + 2) * 2], vertices.texCoords[(i + 2) * 2 + 1]};
            tris.add(new Triangle(vertices.positions[vertices.positionIndices[i]],
                    vertices.positions[vertices.positionIndices[i + 1]],
                    vertices.positions[vertices.positionIndices[i + 2]], tex));
        }
        return tris;
    }

    static VertexData compress(List<Triangle> tris) {
        List<V3> vertices = new ArrayList<>(tris.size() * 3);
        int[] indices = new int[tris.size() * 3];
        float[] texCoords = new float[tris.size() * 6];
        for (int i = 0; i < tris.size(); i++) {
            Triangle tri = tris.get(i);
            indices[i * 3] = indexOf(vertices, tri.p1().pos());
            indices[i * 3 + 1] = indexOf(vertices, tri.p2().pos());
            indices[i * 3 + 2] = indexOf(vertices, tri.p3().pos());
            texCoords[i * 6] = tri.p1().texX();
            texCoords[i * 6 + 1] = tri.p1().texY();
            texCoords[i * 6 + 2] = tri.p2().texX();
            texCoords[i * 6 + 3] = tri.p2().texY();
            texCoords[i * 6 + 4] = tri.p3().texX();
            texCoords[i * 6 + 5] = tri.p3().texY();
        }
        VertexData data = new VertexData();
        data.positions = vertices.toArray(new V3[0]);
        data.positionIndices = indices;
        data.texCoords = texCoords;
        return data;
    }

    private static int indexOf(List<V3> vertices, V3 vec) {
        int idx = epsIndexOf(vertices, vec, 0.00001D);
        if (idx != -1) return idx;
        vertices.add(vec);
        return vertices.size() - 1;
    }

    private static int epsIndexOf(List<V3> l, V3 vec, double eps) {
        for (int i = 0; i < l.size(); i++) {
            if (epsilonEquals(vec, l.get(i), eps)) return i;
        }
        return -1;
    }

    private static boolean epsilonEquals(V3 a, V3 b, double eps) {
        return Math.abs(a.x() - b.x()) < eps && Math.abs(a.y() - b.y()) < eps && Math.abs(a.z() - b.z()) < eps;
    }

    /** {@code cutAndCapConvex}: positive side, negative side, cap (planar UVs from the plane's own frame). */
    static VertexData[] cutAndCapConvex(Triangle[] tris, float[] plane) {
        VertexData[] returnData = {null, null, new VertexData()};
        List<Triangle> side1 = new ArrayList<>();
        List<Triangle> side2 = new ArrayList<>();
        List<V3[]> clippedEdges = new ArrayList<>();
        for (Triangle t : tris) {
            boolean p1 = side(t.p1().pos(), plane);
            boolean p2 = side(t.p2().pos(), plane);
            boolean p3 = side(t.p3().pos(), plane);
            if (p1 && p2 && p3) {
                side1.add(t);
            } else if (!p1 && !p2 && !p3) {
                side2.add(t);
            } else if (p1 ^ p2 ^ p3) {
                TexVertex a, b, c;
                if (p1) { a = t.p1(); b = t.p2(); c = t.p3(); }
                else if (p2) { a = t.p2(); b = t.p3(); c = t.p1(); }
                else { a = t.p3(); b = t.p1(); c = t.p2(); }
                Split s = split(a, b, c, plane);
                side2.add(new Triangle(s.d, b.pos(), s.e, new float[]{s.deTex[0], s.deTex[1], b.texX(), b.texY(), s.deTex[2], s.deTex[3]}));
                side2.add(new Triangle(b.pos(), c.pos(), s.e, new float[]{b.texX(), b.texY(), c.texX(), c.texY(), s.deTex[2], s.deTex[3]}));
                side1.add(new Triangle(a.pos(), s.d, s.e, new float[]{a.texX(), a.texY(), s.deTex[0], s.deTex[1], s.deTex[2], s.deTex[3]}));
                clippedEdges.add(new V3[]{s.d, s.e});
            } else {
                TexVertex a, b, c;
                if (!p1) { a = t.p1(); b = t.p2(); c = t.p3(); }
                else if (!p2) { a = t.p2(); b = t.p3(); c = t.p1(); }
                else { a = t.p3(); b = t.p1(); c = t.p2(); }
                Split s = split(a, b, c, plane);
                side1.add(new Triangle(s.d, b.pos(), s.e, new float[]{s.deTex[0], s.deTex[1], b.texX(), b.texY(), s.deTex[2], s.deTex[3]}));
                side1.add(new Triangle(b.pos(), c.pos(), s.e, new float[]{b.texX(), b.texY(), c.texX(), c.texY(), s.deTex[2], s.deTex[3]}));
                side2.add(new Triangle(a.pos(), s.d, s.e, new float[]{a.texX(), a.texY(), s.deTex[0], s.deTex[1], s.deTex[2], s.deTex[3]}));
                clippedEdges.add(new V3[]{s.e, s.d});
            }
        }
        if (!clippedEdges.isEmpty()) {
            Mat3 mat = normalToMat(new V3(plane[0], plane[1], plane[2]));
            List<V3> ordered = new ArrayList<>();
            ordered.add(clippedEdges.get(0)[0]);
            boolean closed = true;
            while (!clippedEdges.isEmpty()) {
                V3 next = getNext(clippedEdges, ordered.get(ordered.size() - 1));
                if (next == null) {
                    // 1.12 threw "Didn't find next in loop!" here and lost the whole effect; an open loop (a cube with
                    // missing faces) just gets no cap
                    closed = false;
                    break;
                }
                ordered.add(next);
            }
            if (closed && ordered.size() >= 3) {
                float[] uv1 = transform(mat, ordered.get(0));
                List<Triangle> cap = new ArrayList<>();
                for (int i = 0; i < ordered.size() - 2; i++) {
                    float[] uv2 = transform(mat, ordered.get(i + 2));
                    float[] uv3 = transform(mat, ordered.get(i + 1));
                    cap.add(new Triangle(ordered.get(0), ordered.get(i + 2), ordered.get(i + 1),
                            new float[]{uv1[0], uv1[1], uv2[0], uv2[1], uv3[0], uv3[1]}));
                    side1.add(new Triangle(ordered.get(0), ordered.get(i + 2), ordered.get(i + 1), new float[6]));
                    side2.add(new Triangle(ordered.get(0), ordered.get(i + 1), ordered.get(i + 2), new float[6]));
                }
                returnData[2] = compress(cap);
            }
        }
        returnData[0] = compress(side1);
        returnData[1] = compress(side2);
        return returnData;
    }

    private record Split(V3 d, V3 e, float[] deTex) {}

    private static Split split(TexVertex a, TexVertex b, TexVertex c, float[] plane) {
        V3 rAB = b.pos().subtract(a.pos());
        V3 rAC = c.pos().subtract(a.pos());
        float interceptAB = (float) rayPlaneIntercept(a.pos(), rAB, plane);
        float interceptAC = (float) rayPlaneIntercept(a.pos(), rAC, plane);
        V3 d = a.pos().add(rAB.scale(interceptAB));
        V3 e = a.pos().add(rAC.scale(interceptAC));
        float[] deTex = {
                a.texX() + (b.texX() - a.texX()) * interceptAB,
                a.texY() + (b.texY() - a.texY()) * interceptAB,
                a.texX() + (c.texX() - a.texX()) * interceptAC,
                a.texY() + (c.texY() - a.texY()) * interceptAC};
        return new Split(d, e, deTex);
    }

    private static boolean side(V3 p, float[] plane) {
        return p.x() * plane[0] + p.y() * plane[1] + p.z() * plane[2] + plane[3] > 0;
    }

    private static V3 getNext(List<V3[]> edges, V3 first) {
        Iterator<V3[]> itr = edges.iterator();
        while (itr.hasNext()) {
            V3[] v = itr.next();
            if (epsilonEquals(v[0], first, 0.00001D)) {
                itr.remove();
                return v[1];
            } else if (epsilonEquals(v[1], first, 0.00001D)) {
                itr.remove();
                return v[0];
            }
        }
        return null;
    }

    static double rayPlaneIntercept(V3 start, V3 ray, float[] plane) {
        double num = -(plane[0] * start.x() + plane[1] * start.y() + plane[2] * start.z() + plane[3]);
        double denom = plane[0] * ray.x() + plane[1] * ray.y() + plane[2] * ray.z();
        return num / denom;
    }

    /**
     * {@code BakedModelUtil.normalToMat(normal, 0)}: {@code eulerToMat(yaw, pitch + 90, 0)} from {@code getEulerAngles},
     * i.e. {@code rotZ(0) * rotX(pitch) * rotY(-yaw)} in vecmath's row-major convention.
     */
    static Mat3 normalToMat(V3 normal) {
        double yaw = Math.toDegrees(Math.atan2(normal.x(), normal.z()));
        double sqrt = Math.sqrt(normal.x() * normal.x() + normal.z() * normal.z());
        double pitch = Math.toDegrees(Math.atan2(normal.y(), sqrt)) - 90;
        float y = (float) Math.toRadians(yaw);
        float p = (float) Math.toRadians(pitch + 90);
        Mat3 mY = rotY(-y);
        Mat3 mP = rotX(p);
        Mat3 mR = rotZ(0);
        mR.mul(mP);
        mR.mul(mY);
        return mR;
    }

    private static Mat3 rotX(float angle) {
        float s = (float) Math.sin(angle), c = (float) Math.cos(angle);
        return new Mat3(1, 0, 0, 0, c, -s, 0, s, c);
    }

    private static Mat3 rotY(float angle) {
        float s = (float) Math.sin(angle), c = (float) Math.cos(angle);
        return new Mat3(c, 0, s, 0, 1, 0, -s, 0, c);
    }

    private static Mat3 rotZ(float angle) {
        float s = (float) Math.sin(angle), c = (float) Math.cos(angle);
        return new Mat3(c, -s, 0, s, c, 0, 0, 0, 1);
    }

    private static float[] transform(Mat3 m, V3 v) {
        float x = (float) v.x(), y = (float) v.y(), z = (float) v.z();
        return new float[]{m.m00 * x + m.m01 * y + m.m02 * z, m.m10 * x + m.m11 * y + m.m12 * z};
    }

    /**
     * {@code generateChunks}: islands of pieces whose boxes (grown 0.01) overlap and whose shapes touch (GJK with a
     * 0.01 margin), so no floating fragment is thrown on its own.
     */
    static void generateChunks(List<List<CutModelData>> chunks, List<CutModelData> toSort) {
        GJK.margin = 0.01F;
        try {
            List<CutModelData> chunk = new ArrayList<>();
            while (!toSort.isEmpty()) {
                boolean removed = false;
                List<CutModelData> toAdd = new ArrayList<>(2);
                for (CutModelData c : chunk) {
                    Iterator<CutModelData> itr = toSort.iterator();
                    while (itr.hasNext()) {
                        CutModelData d = itr.next();
                        if (grow(d.collider.localBox(), 0.01F).intersects(c.collider.localBox())
                                && GJK.collidesAny(null, null, c.collider, d.collider)) {
                            removed = true;
                            toAdd.add(d);
                            itr.remove();
                        }
                    }
                }
                chunk.addAll(toAdd);
                if (!removed) {
                    if (!chunk.isEmpty()) {
                        chunks.add(chunk);
                        chunk = new ArrayList<>();
                    }
                    chunk.add(toSort.remove(0));
                }
            }
            if (!chunk.isEmpty()) chunks.add(chunk);
        } finally {
            GJK.margin = 0;
        }
    }

    private static Box grow(Box b, double g) {
        return new Box(b.minX() - g, b.minY() - g, b.minZ() - g, b.maxX() + g, b.maxY() + g, b.maxZ() + g);
    }
}
