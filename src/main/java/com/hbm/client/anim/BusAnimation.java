package com.hbm.client.anim;

import java.util.HashMap;
import java.util.Map;

/**
 * A whole animation: several named buses playing at once.
 *
 * <p>Ported from 1.12.2 {@code BusAnimationSedna}. Upstream's own image is the clearest one — the buses
 * are film strips hanging side by side, and the player rolls down all of them at once, combining what it
 * picks up into a single movie. Each bus owns exactly one thing: the rotation of the hammer, the slide
 * of the drum, the recoil of the whole weapon.
 *
 * <p>A bus is addressed by name from the renderer, so a model's parts and its animation never have to
 * agree on an index — only on a string.
 */
public class BusAnimation {

    private final Map<String, BusAnimationSequence> animationBuses = new HashMap<>();

    /** Always the duration of the longest bus. */
    private int totalTime = 0;

    public BusAnimation addBus(String name, BusAnimationSequence bus) {
        animationBuses.put(name, bus);
        totalTime = Math.max(totalTime, bus.getTotalTime());
        return this;
    }

    /** Recomputes the duration, for buses whose keyframes were added after {@link #addBus}. */
    public void updateTime() {
        for (BusAnimationSequence sequence : animationBuses.values()) {
            totalTime = Math.max(totalTime, sequence.getTotalTime());
        }
    }

    public BusAnimationSequence getBus(String name) {
        return animationBuses.get(name);
    }

    /** Scales every keyframe duration. Below 1 plays faster. */
    public void setTimeMult(double mult) {
        for (BusAnimationSequence sequence : animationBuses.values()) sequence.multiplyTime(mult);
    }

    /** The state of one bus at {@code millis}, or null if this animation has no such bus. */
    public double[] getTimedTransformation(String name, int millis) {
        BusAnimationSequence bus = animationBuses.get(name);
        return bus == null ? null : bus.getTransformation(millis);
    }

    public int getDuration() {
        return totalTime;
    }
}
