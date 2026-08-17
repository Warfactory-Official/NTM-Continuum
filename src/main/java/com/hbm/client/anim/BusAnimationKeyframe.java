package com.hbm.client.anim;

/**
 * One keyframe on one dimension of one bus: a target value, how long it takes to get there, and how it
 * travels.
 *
 * <p>Ported from 1.12.2 {@code BusAnimationKeyframeSedna}. Upstream's file is largely a transcription of
 * Blender's easing library (bezier solvers, bounce/elastic/back, the quad→quint family) which exists to
 * replay animations imported by its Collada loader. None of the hand-written Sedna gun animations use
 * any of it — they use LINEAR and the three sine curves — so only those are ported. The remaining
 * {@link IType} constants are declared but fall back to linear, so an imported animation degrades
 * instead of failing, and the Blender curves can be filled in when that loader lands.
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

        // Declared for parity with upstream's Blender import path; all currently behave as LINEAR.
        BEZIER, SINE, QUAD, CUBIC, QUART, QUINT, EXPO, CIRC, BOUNCE, ELASTIC, BACK
    }

    public double value;
    public IType interpolationType;
    public int originalDuration;
    public int duration;

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
}
