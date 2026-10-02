package com.hbm.physics;

/** The subset of {@code javax.vecmath.Quat4f} old-hbm's physics used, with vecmath's semantics. */
public final class Quat {

    private static final double EPS = 0.000001;

    public float x, y, z, w;

    public Quat() {}

    public Quat(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void normalize() {
        float norm = x * x + y * y + z * z + w * w;
        if (norm > 0.0f) {
            norm = 1.0f / (float) Math.sqrt(norm);
            x *= norm; y *= norm; z *= norm; w *= norm;
        } else {
            x = 0; y = 0; z = 0; w = 0;
        }
    }

    /**
     * {@code Quat4f.interpolate(q1, alpha)}: this becomes the spherical interpolation from this towards {@code q1}.
     * Like vecmath it flips {@code q1} in place when the two lie on opposite hemispheres.
     */
    public void interpolate(Quat q1, float alpha) {
        double dot = x * q1.x + y * q1.y + z * q1.z + w * q1.w;
        if (dot < 0) {
            q1.x = -q1.x; q1.y = -q1.y; q1.z = -q1.z; q1.w = -q1.w;
            dot = -dot;
        }
        double s1, s2;
        if ((1.0 - dot) > EPS) {
            double om = Math.acos(dot);
            double sinom = Math.sin(om);
            s1 = Math.sin((1.0 - alpha) * om) / sinom;
            s2 = Math.sin(alpha * om) / sinom;
        } else {
            s1 = 1.0 - alpha;
            s2 = alpha;
        }
        w = (float) (s1 * w + s2 * q1.w);
        x = (float) (s1 * x + s2 * q1.x);
        y = (float) (s1 * y + s2 * q1.y);
        z = (float) (s1 * z + s2 * q1.z);
    }

    /** {@code RigidBody.setFromMat}: LWJGL's matrix-to-quaternion, which stays right for axis-aligned rotations. */
    public void setFromMat(Mat3 m) {
        float s;
        float tr = m.m00 + m.m11 + m.m22;
        if (tr >= 0.0) {
            s = (float) Math.sqrt(tr + 1.0);
            w = s * 0.5f;
            s = 0.5f / s;
            x = (m.m21 - m.m12) * s;
            y = (m.m02 - m.m20) * s;
            z = (m.m10 - m.m01) * s;
        } else {
            float max = Math.max(Math.max(m.m00, m.m11), m.m22);
            if (max == m.m00) {
                s = (float) Math.sqrt(m.m00 - (m.m11 + m.m22) + 1.0);
                x = s * 0.5f;
                s = 0.5f / s;
                y = (m.m01 + m.m10) * s;
                z = (m.m20 + m.m02) * s;
                w = (m.m21 - m.m12) * s;
            } else if (max == m.m11) {
                s = (float) Math.sqrt(m.m11 - (m.m22 + m.m00) + 1.0);
                y = s * 0.5f;
                s = 0.5f / s;
                z = (m.m12 + m.m21) * s;
                x = (m.m01 + m.m10) * s;
                w = (m.m02 - m.m20) * s;
            } else {
                s = (float) Math.sqrt(m.m22 - (m.m00 + m.m11) + 1.0);
                z = s * 0.5f;
                s = 0.5f / s;
                x = (m.m20 + m.m02) * s;
                y = (m.m12 + m.m21) * s;
                w = (m.m10 - m.m01) * s;
            }
        }
    }
}
