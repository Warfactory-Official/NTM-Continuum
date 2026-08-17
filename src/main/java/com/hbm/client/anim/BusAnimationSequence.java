package com.hbm.client.anim;

import com.hbm.client.anim.BusAnimationKeyframe.IType;
import java.util.ArrayList;
import java.util.List;

/**
 * One bus's timeline: nine independent keyframe tracks (translate/rotate/scale × XYZ).
 *
 * <p>Ported from 1.12.2 {@code BusAnimationSequenceSedna}. The tracks are deliberately independent so a
 * part can, say, finish moving on Z while still rotating on X — upstream calls this "staggering", and
 * the revolver's reload leans on it heavily.
 *
 * <p>All values are absolute, not additive.
 */
public class BusAnimationSequence {

    public enum Dimension {
        TX, TY, TZ, RX, RY, RZ, SX, SY, SZ;

        public static final Dimension[] VALUES = values();
    }

    /** One keyframe list per dimension, indexed by {@link Dimension#ordinal()}. */
    private final List<List<BusAnimationKeyframe>> transformKeyframes = new ArrayList<>(9);

    /** Pivot subtracted after rotating, so a part can turn about a point other than the origin. */
    public double[] offset = new double[3];

    /** Axis order for the three rotations — "swizzle me timbers", as upstream puts it. */
    public double[] rotMode = new double[] {0, 1, 2};

    public BusAnimationSequence() {
        for (int i = 0; i < 9; i++) transformKeyframes.add(new ArrayList<>());
    }

    public BusAnimationSequence addKeyframe(Dimension dimension, BusAnimationKeyframe keyframe) {
        transformKeyframes.get(dimension.ordinal()).add(keyframe);
        return this;
    }

    public BusAnimationSequence addKeyframe(Dimension dimension, double value, int duration) {
        return addKeyframe(dimension, new BusAnimationKeyframe(value, duration));
    }

    /** A position reached instantly. */
    public BusAnimationSequence setPos(double x, double y, double z) {
        return addPos(x, y, z, 0, IType.LINEAR);
    }

    /** A position reached over {@code duration} ms, linearly. */
    public BusAnimationSequence addPos(double x, double y, double z, int duration) {
        return addPos(x, y, z, duration, IType.LINEAR);
    }

    public BusAnimationSequence addPos(double x, double y, double z, int duration, IType type) {
        addKeyframe(Dimension.TX, new BusAnimationKeyframe(x, duration, type));
        addKeyframe(Dimension.TY, new BusAnimationKeyframe(y, duration, type));
        addKeyframe(Dimension.TZ, new BusAnimationKeyframe(z, duration, type));
        return this;
    }

    public BusAnimationSequence addRot(double x, double y, double z, int duration) {
        addKeyframe(Dimension.RX, new BusAnimationKeyframe(x, duration));
        addKeyframe(Dimension.RY, new BusAnimationKeyframe(y, duration));
        addKeyframe(Dimension.RZ, new BusAnimationKeyframe(z, duration));
        return this;
    }

    /** Repeats the last position for {@code duration} ms — a pause. */
    public BusAnimationSequence hold(int duration) {
        addKeyframe(Dimension.TX, new BusAnimationKeyframe(getLast(Dimension.TX), duration));
        addKeyframe(Dimension.TY, new BusAnimationKeyframe(getLast(Dimension.TY), duration));
        addKeyframe(Dimension.TZ, new BusAnimationKeyframe(getLast(Dimension.TZ), duration));
        return this;
    }

    /** Pads this bus so it reaches {@code end} ms, for syncing buses against each other. */
    public BusAnimationSequence holdUntil(int end) {
        return hold(end - getTotalTime());
    }

    public BusAnimationSequence multiplyTime(double mult) {
        for (Dimension dim : Dimension.VALUES) {
            for (BusAnimationKeyframe keyframe : transformKeyframes.get(dim.ordinal())) {
                keyframe.duration = (int) (keyframe.originalDuration * mult);
            }
        }
        return this;
    }

    private double getLast(Dimension dim) {
        List<BusAnimationKeyframe> keyframes = transformKeyframes.get(dim.ordinal());
        return keyframes.isEmpty() ? 0D : keyframes.get(keyframes.size() - 1).value;
    }

    /**
     * The full 15-slot transform at {@code millis}: 3 translate, 3 rotate, 3 scale, 3 pivot, 3 axis order.
     *
     * <p>Past the end of a track the last value simply holds, which is what lets a shorter bus stay put
     * while a longer one finishes.
     */
    public double[] getTransformation(int millis) {
        double[] transform = new double[15];

        for (int i = 0; i < 9; i++) {
            List<BusAnimationKeyframe> keyframes = transformKeyframes.get(i);

            BusAnimationKeyframe currentFrame = null;
            BusAnimationKeyframe previousFrame = null;
            int startTime = 0;
            int endTime = 0;

            for (BusAnimationKeyframe keyframe : keyframes) {
                startTime = endTime;
                endTime += keyframe.duration;
                previousFrame = currentFrame;
                currentFrame = keyframe;
                if (millis < endTime) break;
            }

            if (currentFrame == null) {
                // Nothing declared on this track: scale rests at 1, everything else at 0.
                transform[i] = i >= 6 ? 1 : 0;
                continue;
            }

            if (millis >= endTime) {
                transform[i] = currentFrame.value;
                continue;
            }

            // A CONSTANT predecessor holds its own value for the whole gap and then snaps.
            if (previousFrame != null && previousFrame.interpolationType == IType.CONSTANT) {
                transform[i] = previousFrame.value;
                continue;
            }

            transform[i] = currentFrame.interpolate(startTime, millis, previousFrame);
        }

        transform[9] = offset[0];
        transform[10] = offset[1];
        transform[11] = offset[2];
        transform[12] = rotMode[0];
        transform[13] = rotMode[1];
        transform[14] = rotMode[2];
        return transform;
    }

    /** The length of the longest track, in ms. */
    public int getTotalTime() {
        int highest = 0;
        for (List<BusAnimationKeyframe> keyframes : transformKeyframes) {
            int time = 0;
            for (BusAnimationKeyframe frame : keyframes) time += frame.duration;
            highest = Math.max(time, highest);
        }
        return highest;
    }
}
