package com.hbm.client.jetpack;

/** The old fuel gradient, kept as a small immutable client-side value object. */
public final class JetpackGradient {

    private final float[][] keys;

    public JetpackGradient(float[]... keys) {
        this.keys = keys;
    }

    public float[] color(float position) {
        if (position <= keys[0][4]) return copyRgb(keys[0]);
        for (int i = 1; i < keys.length; i++) {
            float[] right = keys[i];
            if (position <= right[4]) {
                float[] left = keys[i - 1];
                float span = right[4] - left[4];
                float t = span <= 0F ? 1F : (position - left[4]) / span;
                return new float[] {
                        left[0] + (right[0] - left[0]) * t,
                        left[1] + (right[1] - left[1]) * t,
                        left[2] + (right[2] - left[2]) * t,
                        left[3] + (right[3] - left[3]) * t,
                };
            }
        }
        return copyRgb(keys[keys.length - 1]);
    }

    private static float[] copyRgb(float[] key) {
        return new float[] {key[0], key[1], key[2], key[3]};
    }
}
