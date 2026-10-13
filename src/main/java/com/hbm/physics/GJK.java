package com.hbm.physics;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 1.12.2 {@code physics.GJK}: Gilbert-Johnson-Keerthi intersection and the expanding polytope for the contact,
 * statement for statement (including the float rounding of {@code V3.mult(float)} where the old code cast). Shared
 * scratch state as in the original; client thread only.
 */
public final class GJK {

    public static final int gjkMaxIterations = 64;
    public static final int epaMaxIterations = 128;
    public static float margin = 0;

    public static final Simplex csoSimplex = new Simplex();

    private GJK() {}

    public static GJKInfo colliding(@Nullable RigidBody bodyA, @Nullable RigidBody bodyB, Collider a, Collider b) {
        return colliding(bodyA, bodyB, a, b, true);
    }

    /** {@code collidesAny}: anything but a clean separation counts, as the old early-out returned non-null. */
    public static boolean collidesAny(@Nullable RigidBody bodyA, @Nullable RigidBody bodyB, Collider a, Collider b) {
        return colliding(bodyA, bodyB, a, b, false) != null;
    }

    public static @Nullable GJKInfo colliding(@Nullable RigidBody bodyA, @Nullable RigidBody bodyB, Collider a, Collider b,
                                              boolean epa) {
        GJKInfo returnInfo = new GJKInfo();
        csoSimplex.reset();
        V3 direction = new V3(0, 0, 1);
        V3 supportCSO = doCSOSupport(bodyA, bodyB, a, b, direction).v;
        direction = supportCSO.negate();
        for (int iter = 0; iter < gjkMaxIterations; iter++) {
            supportCSO = doCSOSupport(bodyA, bodyB, a, b, direction).v;
            if (supportCSO.dotProduct(direction) < 0) {
                returnInfo.result = Result.SEPARATED;
                if (!epa) return null;
                return returnInfo;
            }
            switch (csoSimplex.size) {
                case 0, 1 -> {}
                case 2 -> {
                    V3 ab = csoSimplex.points[1].v.subtract(csoSimplex.points[0].v);
                    V3 ao = csoSimplex.points[0].v.negate();
                    if (ab.dotProduct(ao) > 0) {
                        direction = ab.crossProduct(ao).crossProduct(ab);
                    } else {
                        csoSimplex.points[1] = null;
                        csoSimplex.size--;
                        direction = csoSimplex.points[0].v.mult(-1);
                    }
                }
                case 3 -> {
                    V3 ab = csoSimplex.points[1].v.subtract(csoSimplex.points[0].v);
                    V3 ac = csoSimplex.points[2].v.subtract(csoSimplex.points[0].v);
                    V3 abc = ab.crossProduct(ac);
                    V3 ao = csoSimplex.points[0].v.negate();
                    direction = triangleCase(ab, ac, abc, ao);
                }
                case 4 -> {
                    V3 ab = csoSimplex.points[1].v.subtract(csoSimplex.points[0].v);
                    V3 ac = csoSimplex.points[2].v.subtract(csoSimplex.points[0].v);
                    V3 ad = csoSimplex.points[3].v.subtract(csoSimplex.points[0].v);
                    V3 ao = csoSimplex.points[0].v.negate();
                    V3 dir = tetraCase(ab, ac, ad, ao);
                    if (dir == null) {
                        if (epa) EPA(bodyA, bodyB, a, b, returnInfo);
                        return returnInfo;
                    }
                    direction = dir;
                }
                default -> {}
            }
        }
        returnInfo.result = Result.GJK_FAILED;
        return returnInfo;
    }

    static V3 triangleCase(V3 ab, V3 ac, V3 abc, V3 ao) {
        if (abc.crossProduct(ac).dotProduct(ao) > 0) {
            if (ac.dotProduct(ao) > 0) {
                csoSimplex.points[1] = csoSimplex.points[2];
                csoSimplex.points[2] = null;
                csoSimplex.size--;
                return ac.crossProduct(ao).crossProduct(ac);
            } else if (ab.dotProduct(ao) > 0) {
                csoSimplex.points[2] = null;
                csoSimplex.size--;
                return ab.crossProduct(ao).crossProduct(ab);
            } else {
                csoSimplex.points[1] = null;
                csoSimplex.points[2] = null;
                csoSimplex.size -= 2;
                return ao;
            }
        } else {
            if (ab.crossProduct(abc).dotProduct(ao) > 0) {
                if (ab.dotProduct(ao) > 0) {
                    csoSimplex.points[2] = null;
                    csoSimplex.size--;
                    return ab.crossProduct(ao).crossProduct(ab);
                } else {
                    csoSimplex.points[1] = null;
                    csoSimplex.points[2] = null;
                    csoSimplex.size -= 2;
                    return ao;
                }
            } else if (abc.dotProduct(ao) > 0) {
                return abc;
            } else {
                Mkv tmp = csoSimplex.points[2];
                csoSimplex.points[2] = csoSimplex.points[1];
                csoSimplex.points[1] = tmp;
                return abc.negate();
            }
        }
    }

    static @Nullable V3 tetraCase(V3 ab, V3 ac, V3 ad, V3 ao) {
        if (ab.crossProduct(ac).dotProduct(ao) > 0) {
            csoSimplex.points[3] = null;
            csoSimplex.size--;
            return triangleCase(ab, ac, ab.crossProduct(ac), ao);
        } else if (ac.crossProduct(ad).dotProduct(ao) > 0) {
            csoSimplex.points[1] = csoSimplex.points[2];
            csoSimplex.points[2] = csoSimplex.points[3];
            csoSimplex.points[3] = null;
            csoSimplex.size--;
            return triangleCase(ac, ad, ac.crossProduct(ad), ao);
        } else if (ad.crossProduct(ab).dotProduct(ao) > 0) {
            csoSimplex.points[2] = csoSimplex.points[1];
            csoSimplex.points[1] = csoSimplex.points[3];
            csoSimplex.points[3] = null;
            csoSimplex.size--;
            return triangleCase(ad, ab, ad.crossProduct(ab), ao);
        }
        return null;
    }

    static Mkv doCSOSupport(@Nullable RigidBody bodyA, @Nullable RigidBody bodyB, Collider a, Collider b, V3 direction) {
        V3 supportCSO = localSupport(bodyA, a, direction).subtract(localSupport(bodyB, b, direction.negate()));
        Mkv vert = new Mkv(supportCSO, direction);
        csoSimplex.pushBack(vert);
        return vert;
    }

    public static V3 localSupport(@Nullable RigidBody body, Collider c, V3 worldDir) {
        if (body != null) {
            V3 localDir = body.globalToLocalVec(worldDir);
            if (margin != 0) {
                localDir = localDir.normalize();
                return body.localToGlobalPos(c.support(localDir).add(localDir.mult(margin)));
            }
            return body.localToGlobalPos(c.support(localDir));
        }
        if (margin != 0) {
            worldDir = worldDir.normalize();
            return c.support(worldDir).add(worldDir.mult(margin));
        }
        return c.support(worldDir);
    }

    private static final List<Mkv[]> faces = new ArrayList<>();
    private static final List<Mkv[]> edges = new ArrayList<>();
    private static final V3[][] features = new V3[2][3];

    static void EPA(@Nullable RigidBody bodyA, @Nullable RigidBody bodyB, Collider a, Collider b, GJKInfo info) {
        faces.add(buildFace(csoSimplex.points[0], csoSimplex.points[1], csoSimplex.points[2]));
        faces.add(buildFace(csoSimplex.points[0], csoSimplex.points[2], csoSimplex.points[3]));
        faces.add(buildFace(csoSimplex.points[0], csoSimplex.points[3], csoSimplex.points[1]));
        faces.add(buildFace(csoSimplex.points[1], csoSimplex.points[2], csoSimplex.points[3]));
        for (int iter = 0; iter < epaMaxIterations; iter++) {
            Mkv[] closestFace = null;
            double smallestDist = Double.MAX_VALUE;
            for (Mkv[] face : faces) {
                double lenSq = originDistToPlaneSq(face);
                if (lenSq < smallestDist) {
                    smallestDist = lenSq;
                    closestFace = face;
                }
            }
            if (closestFace == null) break;
            Mkv support = doCSOSupport(bodyA, bodyB, a, b, closestFace[3].v);
            final float epsilon = 0.00001F;
            if (distToPlaneSq(closestFace, support.v) < epsilon) {
                info.result = Result.COLLIDING;
                V3 separation = planeProjectOrigin(closestFace);
                info.normal = separation.normalize();
                info.depth = (float) separation.length();
                for (int i = 0; i < 3; i++) {
                    features[0][i] = localSupport(bodyA, a, closestFace[i].r);
                    features[1][i] = localSupport(bodyB, b, closestFace[i].r.negate());
                }
                V3 bc = barycentricCoords(closestFace, separation);
                info.contactPointA = new V3(
                        features[0][0].x() * bc.x() + features[0][1].x() * bc.y() + features[0][2].x() * bc.z(),
                        features[0][0].y() * bc.x() + features[0][1].y() * bc.y() + features[0][2].y() * bc.z(),
                        features[0][0].z() * bc.x() + features[0][1].z() * bc.y() + features[0][2].z() * bc.z());
                info.contactPointB = new V3(
                        features[1][0].x() * bc.x() + features[1][1].x() * bc.y() + features[1][2].x() * bc.z(),
                        features[1][0].y() * bc.x() + features[1][1].y() * bc.y() + features[1][2].y() * bc.z(),
                        features[1][0].z() * bc.x() + features[1][1].z() * bc.y() + features[1][2].z() * bc.z());
                faces.clear();
                return;
            }
            Iterator<Mkv[]> itr = faces.iterator();
            while (itr.hasNext()) {
                Mkv[] face = itr.next();
                if (face[3].v.dotProduct(support.v.subtract(face[0].v)) > 0) {
                    itr.remove();
                    Mkv[] edge = new Mkv[]{face[1], face[0]};
                    if (!removeEdge(edge)) {
                        edge[0] = face[0];
                        edge[1] = face[1];
                        edges.add(edge);
                    }
                    edge = new Mkv[]{face[2], face[1]};
                    if (!removeEdge(edge)) {
                        edge[0] = face[1];
                        edge[1] = face[2];
                        edges.add(edge);
                    }
                    edge = new Mkv[]{face[0], face[2]};
                    if (!removeEdge(edge)) {
                        edge[0] = face[2];
                        edge[1] = face[0];
                        edges.add(edge);
                    }
                }
            }
            for (Mkv[] edge : edges) faces.add(buildFace(edge[0], edge[1], support));
            edges.clear();
        }
        faces.clear();
        edges.clear();
        info.result = Result.EPA_FAILED;
    }

    static boolean removeEdge(Mkv[] edge) {
        Iterator<Mkv[]> itr = edges.iterator();
        while (itr.hasNext()) {
            Mkv[] edge2 = itr.next();
            if (edge[0] == edge2[0] && edge[1] == edge2[1]) {
                itr.remove();
                return true;
            }
        }
        return false;
    }

    static V3 planeProjectOrigin(Mkv[] face) {
        V3 point = face[0].v.negate();
        double dot = face[3].v.dotProduct(point);
        return face[3].v.mult((float) dot).negate();
    }

    static double distToPlaneSq(Mkv[] face, V3 point) {
        double dot = face[3].v.dotProduct(point.subtract(face[0].v));
        return face[3].v.mult((float) dot).lengthSquared();
    }

    static double originDistToPlaneSq(Mkv[] face) {
        double dot = face[0].v.dotProduct(face[3].v);
        return face[3].v.mult((float) dot).lengthSquared();
    }

    static Mkv[] buildFace(Mkv a, Mkv b, Mkv c) {
        V3 ab = b.v.subtract(a.v);
        V3 ac = c.v.subtract(a.v);
        V3 ao = a.v.negate();
        V3 normal = ab.crossProduct(ac).normalize();
        if (normal.dotProduct(ao) < 0) return new Mkv[]{a, b, c, new Mkv(normal, null)};
        return new Mkv[]{a, c, b, new Mkv(normal.negate(), null)};
    }

    static V3 barycentricCoords(Mkv[] face, V3 point) {
        double u = face[1].v.subtract(point).crossProduct(face[2].v.subtract(point)).length();
        double v = face[0].v.subtract(point).crossProduct(face[2].v.subtract(point)).length();
        double w = face[0].v.subtract(point).crossProduct(face[1].v.subtract(point)).length();
        double uvw = u + v + w;
        return new V3(u, v, w).multd(1 / uvw);
    }

    public static final class Simplex {
        public int size = 0;
        public final Mkv[] points = new Mkv[4];

        void pushBack(Mkv vec) {
            for (int i = Math.min(size, 2); i >= 0; i--) points[i + 1] = points[i];
            points[0] = vec;
            size++;
            if (size > 4) size = 4;
        }

        void reset() {
            size = 0;
            for (int i = 0; i < 4; i++) points[i] = null;
        }
    }

    /** A Minkowski vertex and the direction that found it (for extracting the contact points). */
    public static final class Mkv {
        public final V3 v;
        public final V3 r;

        Mkv(V3 point, V3 direction) {
            this.v = point;
            this.r = direction;
        }
    }

    public static final class GJKInfo {
        public Result result;
        public V3 normal;
        public float depth;
        public V3 contactPointA;
        public V3 contactPointB;
    }

    public enum Result { COLLIDING, SEPARATED, GJK_FAILED, EPA_FAILED }
}
