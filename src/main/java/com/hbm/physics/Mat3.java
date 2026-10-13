package com.hbm.physics;

/**
 * The subset of {@code javax.vecmath.Matrix3f} 1.12.2's physics used, with vecmath's semantics: float elements,
 * {@code mRC} is row R column C, {@link #mul(Mat3)} is {@code this = this * m}. 1.12 shipped vecmath; modern
 * Minecraft does not, and JOML's {@code mXY} is column X row Y, so a literal swap would transpose every product.
 */
public final class Mat3 {

    public float m00, m01, m02;
    public float m10, m11, m12;
    public float m20, m21, m22;

    /** The zero matrix, as {@code new Matrix3f()}. */
    public Mat3() {}

    public Mat3(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02;
        this.m10 = m10; this.m11 = m11; this.m12 = m12;
        this.m20 = m20; this.m21 = m21; this.m22 = m22;
    }

    public Mat3 copy() {
        return new Mat3(m00, m01, m02, m10, m11, m12, m20, m21, m22);
    }

    public void set(Mat3 m) {
        m00 = m.m00; m01 = m.m01; m02 = m.m02;
        m10 = m.m10; m11 = m.m11; m12 = m.m12;
        m20 = m.m20; m21 = m.m21; m22 = m.m22;
    }

    public void setIdentity() {
        m00 = 1; m01 = 0; m02 = 0;
        m10 = 0; m11 = 1; m12 = 0;
        m20 = 0; m21 = 0; m22 = 1;
    }

    /** {@code Matrix3f.set(AxisAngle4f)}. */
    public void setAxisAngle(float x, float y, float z, float angle) {
        double mag = Math.sqrt(x * x + y * y + z * z);
        if (mag < 1.0E-8) {
            setIdentity();
            return;
        }
        mag = 1.0 / mag;
        double ax = x * mag, ay = y * mag, az = z * mag;
        double sinTheta = Math.sin(angle);
        double cosTheta = Math.cos(angle);
        double t = 1.0 - cosTheta;
        double xz = ax * az, xy = ax * ay, yz = ay * az;
        m00 = (float) (t * ax * ax + cosTheta);
        m01 = (float) (t * xy - sinTheta * az);
        m02 = (float) (t * xz + sinTheta * ay);
        m10 = (float) (t * xy + sinTheta * az);
        m11 = (float) (t * ay * ay + cosTheta);
        m12 = (float) (t * yz - sinTheta * ax);
        m20 = (float) (t * xz - sinTheta * ay);
        m21 = (float) (t * yz + sinTheta * ax);
        m22 = (float) (t * az * az + cosTheta);
    }

    /** {@code this = this * m}. */
    public void mul(Mat3 m) {
        float a00 = m00 * m.m00 + m01 * m.m10 + m02 * m.m20;
        float a01 = m00 * m.m01 + m01 * m.m11 + m02 * m.m21;
        float a02 = m00 * m.m02 + m01 * m.m12 + m02 * m.m22;
        float a10 = m10 * m.m00 + m11 * m.m10 + m12 * m.m20;
        float a11 = m10 * m.m01 + m11 * m.m11 + m12 * m.m21;
        float a12 = m10 * m.m02 + m11 * m.m12 + m12 * m.m22;
        float a20 = m20 * m.m00 + m21 * m.m10 + m22 * m.m20;
        float a21 = m20 * m.m01 + m21 * m.m11 + m22 * m.m21;
        float a22 = m20 * m.m02 + m21 * m.m12 + m22 * m.m22;
        m00 = a00; m01 = a01; m02 = a02;
        m10 = a10; m11 = a11; m12 = a12;
        m20 = a20; m21 = a21; m22 = a22;
    }

    public void mul(float s) {
        m00 *= s; m01 *= s; m02 *= s;
        m10 *= s; m11 *= s; m12 *= s;
        m20 *= s; m21 *= s; m22 *= s;
    }

    public void add(Mat3 m) {
        m00 += m.m00; m01 += m.m01; m02 += m.m02;
        m10 += m.m10; m11 += m.m11; m12 += m.m12;
        m20 += m.m20; m21 += m.m21; m22 += m.m22;
    }

    public void sub(Mat3 m) {
        m00 -= m.m00; m01 -= m.m01; m02 -= m.m02;
        m10 -= m.m10; m11 -= m.m11; m12 -= m.m12;
        m20 -= m.m20; m21 -= m.m21; m22 -= m.m22;
    }

    public void transpose() {
        float t = m01; m01 = m10; m10 = t;
        t = m02; m02 = m20; m20 = t;
        t = m12; m12 = m21; m21 = t;
    }

    public float determinant() {
        return m00 * (m11 * m22 - m12 * m21) + m01 * (m12 * m20 - m10 * m22) + m02 * (m10 * m21 - m11 * m20);
    }

    public float get(int row, int column) {
        return switch (row * 3 + column) {
            case 0 -> m00; case 1 -> m01; case 2 -> m02;
            case 3 -> m10; case 4 -> m11; case 5 -> m12;
            case 6 -> m20; case 7 -> m21; default -> m22;
        };
    }

    /**
     * {@code Matrix3f.invert()}, in double precision as vecmath computed it. vecmath threw on a singular matrix; a
     * degenerate inertia tensor is filtered out before this is reached ({@link ConvexMeshCollider#isDegenerate}).
     */
    public void invert() {
        double a = m00, b = m01, c = m02, d = m10, e = m11, f = m12, g = m20, h = m21, i = m22;
        double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (det == 0) throw new IllegalStateException("singular matrix");
        double inv = 1.0 / det;
        m00 = (float) ((e * i - f * h) * inv);
        m01 = (float) ((c * h - b * i) * inv);
        m02 = (float) ((b * f - c * e) * inv);
        m10 = (float) ((f * g - d * i) * inv);
        m11 = (float) ((a * i - c * g) * inv);
        m12 = (float) ((c * d - a * f) * inv);
        m20 = (float) ((d * h - e * g) * inv);
        m21 = (float) ((b * g - a * h) * inv);
        m22 = (float) ((a * e - b * d) * inv);
    }

    /** {@code BobMathUtil.matrixFromQuat}. */
    public void setFromQuat(Quat q) {
        m00 = 1 - 2 * q.y * q.y - 2 * q.z * q.z;
        m01 = 2 * q.x * q.y - 2 * q.z * q.w;
        m02 = 2 * q.x * q.z + 2 * q.y * q.w;
        m10 = 2 * q.x * q.y + 2 * q.z * q.w;
        m11 = 1 - 2 * q.x * q.x - 2 * q.z * q.z;
        m12 = 2 * q.y * q.z - 2 * q.x * q.w;
        m20 = 2 * q.x * q.z - 2 * q.y * q.w;
        m21 = 2 * q.y * q.z + 2 * q.x * q.w;
        m22 = 1 - 2 * q.x * q.x - 2 * q.y * q.y;
    }
}
