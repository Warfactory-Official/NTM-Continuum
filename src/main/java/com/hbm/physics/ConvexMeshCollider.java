package com.hbm.physics;

/**
 * old-hbm {@code physics.ConvexMeshCollider}: a convex triangle mesh with its volume, centre of mass and inertia
 * tensor (melax.github.io/volint, as the old class cited), and the support function GJK queries.
 */
public final class ConvexMeshCollider extends Collider {

    public final float[] vertices;
    public final int[] indices;
    /** Triangle corners, three {@link V3}s per triangle. */
    private final V3[] corners;
    private final float volume;
    private final Box localBox;

    /** {@code new ConvexMeshCollider(indices, vertices, density)}: {@code vertices} is x, y, z per indexed vertex. */
    public ConvexMeshCollider(int[] indices, float[] vertices, float density) {
        this.indices = indices;
        this.vertices = vertices;
        this.corners = new V3[indices.length];
        for (int i = 0; i < indices.length; i++) {
            corners[i] = new V3(vertices[indices[i] * 3], vertices[indices[i] * 3 + 1], vertices[indices[i] * 3 + 2]);
        }
        this.localBox = Box.of(support(RigidBody.cardinals[3]).x(), support(RigidBody.cardinals[4]).y(),
                support(RigidBody.cardinals[5]).z(), support(RigidBody.cardinals[0]).x(),
                support(RigidBody.cardinals[1]).y(), support(RigidBody.cardinals[2]).z());
        this.volume = computeVolume();
        this.localCentroid = computeCenterOfMass();
        this.mass = volume * density;
        this.localInertiaTensor = computeInertia(localCentroid, mass);
    }

    /** The mesh's bounding box in body space ({@code localBox}). */
    public Box localBox() {
        return localBox;
    }

    /**
     * A flat or empty piece (a zero-thickness cube, a sliver left by a cut through a corner) has no volume, so its
     * centroid and inertia are 0/0. 1.12 let the NaN into the body (vecmath then threw inverting the tensor, losing the
     * whole effect); the port leaves such a piece out of the body's colliders and keeps it as geometry only.
     */
    public boolean isDegenerate() {
        return !(Math.abs(volume) > 1.0E-9F) || Double.isNaN(localCentroid.x()) || Double.isNaN(localCentroid.y())
                || Double.isNaN(localCentroid.z());
    }

    private static Mat3 rows(V3 a, V3 b, V3 c) {
        return new Mat3((float) a.x(), (float) a.y(), (float) a.z(), (float) b.x(), (float) b.y(), (float) b.z(),
                (float) c.x(), (float) c.y(), (float) c.z());
    }

    private float computeVolume() {
        float vol = 0;
        for (int t = 0; t + 2 < corners.length; t += 3) vol += rows(corners[t], corners[t + 1], corners[t + 2]).determinant();
        return vol / 6F;
    }

    private V3 computeCenterOfMass() {
        double cx = 0, cy = 0, cz = 0;
        float total = 0;
        for (int t = 0; t + 2 < corners.length; t += 3) {
            Mat3 mat = rows(corners[t], corners[t + 1], corners[t + 2]);
            float vol = mat.determinant();
            cx = cx + (vol * (mat.m00 + mat.m10 + mat.m20));
            cy = cy + (vol * (mat.m01 + mat.m11 + mat.m21));
            cz = cz + (vol * (mat.m02 + mat.m12 + mat.m22));
            total += vol;
        }
        return new V3(cx / (total * 4F), cy / (total * 4F), cz / (total * 4F));
    }

    private Mat3 computeInertia(V3 com, float mass) {
        float vol = 0;
        double[] diag = new double[3];
        double[] offd = new double[3];
        for (int t = 0; t + 2 < corners.length; t += 3) {
            Mat3 mat = rows(corners[t].subtract(com), corners[t + 1].subtract(com), corners[t + 2].subtract(com));
            float d = mat.determinant();
            vol += d;
            for (int j = 0; j < 3; j++) {
                int j1 = (j + 1) % 3;
                int j2 = (j + 2) % 3;
                diag[j] = diag[j] + (mat.get(0, j) * mat.get(1, j) + mat.get(1, j) * mat.get(2, j) + mat.get(2, j) * mat.get(0, j)
                        + mat.get(0, j) * mat.get(0, j) + mat.get(1, j) * mat.get(1, j) + mat.get(2, j) * mat.get(2, j)) * d;
                offd[j] = offd[j] + (mat.get(0, j1) * mat.get(1, j2) + mat.get(1, j1) * mat.get(2, j2) + mat.get(2, j1) * mat.get(0, j2)
                        + mat.get(0, j1) * mat.get(2, j2) + mat.get(1, j1) * mat.get(0, j2) + mat.get(2, j1) * mat.get(1, j2)
                        + mat.get(0, j1) * mat.get(0, j2) + mat.get(1, j1) * mat.get(1, j2) + mat.get(2, j1) * mat.get(2, j2)) * d;
            }
        }
        float volume2 = vol * (60F / 6F);
        for (int j = 0; j < 3; j++) diag[j] = diag[j] / volume2;
        volume2 = vol * (120F / 6F);
        for (int j = 0; j < 3; j++) offd[j] = offd[j] / volume2;
        for (int j = 0; j < 3; j++) {
            // Vec3NT.mult(float): the vectors were scaled by the float mass
            diag[j] = diag[j] * mass;
            offd[j] = offd[j] * mass;
        }
        return new Mat3(
                (float) (diag[1] + diag[2]), (float) -offd[2], (float) -offd[1],
                (float) -offd[2], (float) (diag[0] + diag[2]), (float) -offd[0],
                (float) -offd[1], (float) -offd[0], (float) (diag[0] + diag[1]));
    }

    @Override
    public V3 support(V3 dir) {
        double dot = -Float.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < vertices.length; i += 3) {
            double newDot = dir.x() * vertices[i] + dir.y() * vertices[i + 1] + dir.z() * vertices[i + 2];
            if (newDot > dot) {
                dot = newDot;
                index = i;
            }
        }
        return new V3(vertices[index], vertices[index + 1], vertices[index + 2]);
    }
}
