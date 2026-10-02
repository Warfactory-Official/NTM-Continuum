package com.hbm.physics;

/**
 * old-hbm {@code Vec3NT} as the physics code used it: an immutable double vector whose operations return new vectors.
 * Kept free of Minecraft types so the solver is plain Java. {@link #mult(float)} takes a float on purpose: the old
 * GJK/EPA code cast its scalars to float there, and that rounding is part of its numbers.
 */
public record V3(double x, double y, double z) {

    public static final V3 ZERO = new V3(0, 0, 0);

    public V3 add(V3 v) { return new V3(x + v.x, y + v.y, z + v.z); }

    public V3 add(double dx, double dy, double dz) { return new V3(x + dx, y + dy, z + dz); }

    public V3 subtract(V3 v) { return new V3(x - v.x, y - v.y, z - v.z); }

    public V3 subtract(double dx, double dy, double dz) { return new V3(x - dx, y - dy, z - dz); }

    public V3 mult(float m) { return new V3(x * m, y * m, z * m); }

    public V3 multd(double m) { return new V3(x * m, y * m, z * m); }

    public V3 scale(double m) { return new V3(x * m, y * m, z * m); }

    public V3 negate() { return new V3(-x, -y, -z); }

    public double dotProduct(V3 v) { return x * v.x + y * v.y + z * v.z; }

    public V3 crossProduct(V3 v) { return new V3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x); }

    public double lengthSquared() { return x * x + y * y + z * z; }

    public double length() { return Math.sqrt(x * x + y * y + z * z); }

    /** 1.12 {@code Vec3d.normalize}: a vector shorter than 1e-4 becomes zero. */
    public V3 normalize() {
        double len = Math.sqrt(x * x + y * y + z * z);
        return len < 1.0E-4D ? ZERO : new V3(x / len, y / len, z / len);
    }

    public V3 max(double d) { return new V3(Math.max(x, d), Math.max(y, d), Math.max(z, d)); }

    public V3 min(double d) { return new V3(Math.min(x, d), Math.min(y, d), Math.min(z, d)); }

    public double distanceTo(V3 v) { return subtract(v).length(); }

    /** {@code Vec3NT.matTransform}: the row-major product {@code M v}. */
    public V3 matTransform(Mat3 m) {
        return new V3(m.m00 * x + m.m01 * y + m.m02 * z,
                m.m10 * x + m.m11 * y + m.m12 * z,
                m.m20 * x + m.m21 * y + m.m22 * z);
    }

    /** {@code Vec3NT.outerProduct}, rounded to float as the old {@code Matrix3f} held it. */
    public Mat3 outerProduct(V3 o) {
        return new Mat3((float) (x * o.x), (float) (x * o.y), (float) (x * o.z),
                (float) (y * o.x), (float) (y * o.y), (float) (y * o.z),
                (float) (z * o.x), (float) (z * o.y), (float) (z * o.z));
    }

    /** 1.12 {@code Vec3d.rotatePitch}. */
    public V3 rotatePitch(float pitch) {
        float c = (float) Math.cos(pitch);
        float s = (float) Math.sin(pitch);
        return new V3(x, y * c + z * s, z * c - y * s);
    }

    /** 1.12 {@code Vec3d.rotateYaw}. */
    public V3 rotateYaw(float yaw) {
        float c = (float) Math.cos(yaw);
        float s = (float) Math.sin(yaw);
        return new V3(x * c + z * s, y, z * c - x * s);
    }
}
