package com.hbm.client.anim;

/**
 * One keyframe on one dimension of one bus: a target value, how long it takes to get there, and how it
 * travels.
 *
 * <p>Ported from 1.12.2 {@code BusAnimationKeyframeSedna}. LINEAR, the three sine curves, and BEZIER
 * are implemented. The remaining Blender easing families are declared for parity and fall back to
 * linear, so an imported animation degrades instead of failing.
 */
public class BusAnimationKeyframe {

    /** How a keyframe travels from the previous value to its own. */
    public enum IType {
        /** Teleport: hold the previous value, then snap. */
        CONSTANT,
        /** Straight line. */
        LINEAR,
        /** Quarter sine, neutral into rising — starts slow. */
        SIN_UP,
        /** Quarter sine, rising back to neutral — ends slow. */
        SIN_DOWN,
        /** Half sine: accelerate then decelerate. The smooth one. */
        SIN_FULL,

        // Declared for parity with upstream's Blender import path; only these remaining families fall back.
        BEZIER, SINE, QUAD, CUBIC, QUART, QUINT, EXPO, CIRC, BOUNCE, ELASTIC, BACK
    }

    public double value;
    public IType interpolationType;
    public int originalDuration;
    public int duration;
    /** Blender handle coordinates, used when the preceding frame owns a BEZIER segment. */
    public double leftX;
    public double leftY;
    public double rightX;
    public double rightY;

    public BusAnimationKeyframe() {
        this(0D, 0, IType.LINEAR);
    }

    public BusAnimationKeyframe(double value, int duration) {
        this(value, duration, IType.LINEAR);
    }

    public BusAnimationKeyframe(double value, int duration, IType type) {
        this.value = value;
        this.duration = duration;
        this.originalDuration = duration;
        this.interpolationType = type;
    }

    /**
     * The value at {@code currentTime}, easing out of {@code previous}.
     *
     * <p>Note which frame owns the curve: the <em>previous</em> frame's type decides how the gap is
     * crossed (that is what makes {@code CONSTANT} a hold-then-snap), while this frame owns the duration.
     */
    public double interpolate(double startTime, double currentTime, BusAnimationKeyframe previous) {
        if (previous == null) previous = new BusAnimationKeyframe();

        double to = value;
        double from = previous.value;
        // Identical endpoints: skip the maths entirely, as upstream does.
        if (Math.abs(from - to) < 0.000001D) return to;

        // Imported Sedna animations store absolute-time Blender handles on each frame. The preceding
        // frame selects the curve; this mirrors AnimationLoader/BusAnimationKeyframeSedna for the LAG.
        if (previous.interpolationType == IType.BEZIER) {
            double v1x = startTime;
            double v1y = previous.value;
            double v2x = previous.rightX;
            double v2y = previous.rightY;
            double v3x = leftX;
            double v3y = leftY;
            double v4x = startTime + duration;
            double v4y = value;

            double h1x = v1x - v2x;
            double h1y = v1y - v2y;
            double h2x = v4x - v3x;
            double h2y = v4y - v3y;
            double len = v4x - v1x;
            double len1 = Math.abs(h1x);
            double len2 = Math.abs(h2x);
            if (len1 + len2 != 0) {
                if (len1 > len) {
                    double fac = len / len1;
                    v2x = v1x - fac * h1x;
                    v2y = v1y - fac * h1y;
                }
                if (len2 > len) {
                    double fac = len / len2;
                    v3x = v4x - fac * h2x;
                    v3y = v4y - fac * h2y;
                }
            }

            return cubicBezier(v1y, v2y, v3y, v4y, findZero(currentTime, v1x, v2x, v3x, v4x));
        }

        return (to - from) * time(startTime, currentTime, duration) + from;
    }

    /** Maps elapsed time onto the [0;1] curve position for this frame's interpolation type. */
    private double time(double start, double end, double duration) {
        if (duration <= 0) return 1D;
        double t = (end - start) / duration;
        return switch (interpolationType) {
            case SIN_UP -> -Math.sin((t * Math.PI + Math.PI) / 2D) + 1D;
            case SIN_DOWN -> Math.sin(t * Math.PI / 2D);
            case SIN_FULL -> (-Math.cos(t * Math.PI) + 1D) / 2D;
            default -> t;
        };
    }

    private static double findZero(double target, double x1, double x2, double x3, double x4) {
        return solveCubic(x1 - target, 3D * (x2 - x1),
                3D * (x1 - 2D * x2 + x3), x4 - x1 + 3D * (x2 - x3));
    }

    private static double cubicBezier(double y1, double y2, double y3, double y4, double t) {
        return y1 + t * 3D * (y2 - y1)
                + t * t * 3D * (y1 - 2D * y2 + y3)
                + t * t * t * (y4 - y1 + 3D * (y2 - y3));
    }

    /** The cubic solver used by the old Blender-curve importer. */
    private static double solveCubic(double c0, double c1, double c2, double c3) {
        if (c3 > 0.000001D) {
            double a = c2 / c3;
            double b = c1 / c3;
            double c = c0 / c3;
            a /= 3D;
            double p = b / 3D - a * a;
            double q = (2D * a * a * a - a * b + c) / 2D;
            double d = q * q + p * p * p;
            if (d > 0.000001D) {
                double root = Math.sqrt(d);
                return cubeRoot(-q + root) + cubeRoot(-q - root) - a;
            }
            if (d > -0.000001D) {
                double root = cubeRoot(-q);
                double result = 2D * root - a;
                return result < 0.000001D || result > 1.000001D ? -root - a : result;
            }
            double phi = Math.acos(-q / Math.sqrt(-(p * p * p)));
            double root = Math.sqrt(-p);
            double cos = Math.cos(phi / 3D);
            double sin = Math.sqrt(3D - 3D * cos * cos);
            double result = 2D * root * cos - a;
            if (result < 0.000001D || result > 1.000001D) result = -root * (cos + sin) - a;
            if (result < 0.000001D || result > 1.000001D) result = -root * (cos - sin) - a;
            return result;
        }

        if (c2 > 0.000001D) {
            double discriminant = c1 * c1 - 4D * c2 * c0;
            if (discriminant > 0.000001D) {
                double root = Math.sqrt(discriminant);
                double result = (-c1 - root) / (2D * c2);
                return result < 0.000001D || result > 1.000001D
                        ? (-c1 + root) / (2D * c2) : result;
            }
            if (discriminant > -0.000001D) return -c1 / (2D * c2);
        }
        return c1 > 0.000001D ? -c0 / c1 : 0D;
    }

    private static double cubeRoot(double value) {
        if (value > 0.000001D) return Math.exp(Math.log(value) / 3D);
        if (value < -0.000001D) return -Math.exp(Math.log(-value) / 3D);
        return 0D;
    }
}
