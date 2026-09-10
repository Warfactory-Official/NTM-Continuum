package com.hbm.util;

/**
 * A running average that weights recent samples more heavily, ported from 1.12.2's
 * {@code ExponentialMovingAverage} by BallOfEnergy01.
 *
 * <p>The first sample seeds the average outright rather than being blended against zero, so a gauge does
 * not have to climb out of a cold start before it reads true.
 */
public class ExponentialMovingAverage {

    private final double alpha;
    private double ema = -1;

    public ExponentialMovingAverage(double alpha) {
        this.alpha = alpha;
    }

    public void next(double value) {
        if (ema == -1) {
            ema = value;
        } else {
            ema = alpha * value + (1 - alpha) * ema;
        }
    }

    public long getValue() {
        return (long) ema;
    }
}
