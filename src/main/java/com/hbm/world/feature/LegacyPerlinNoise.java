package com.hbm.world.feature;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * A reimplementation of 1.12.2's {@code NoiseGeneratorPerlin}: {@code n} octaves of simplex
 * noise summed as {@code Σ simplex[i].getValue(x·f, y·f) / f} with {@code f} starting at 1 and halving.
 *
 * <p><b>Why not vanilla's {@link net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise}:</b> the
 * modern class <i>normalises</i> its output by {@code 1/(2^octaves − 1)}, so its values land in roughly ±1
 * while 1.12's land in the ±tens. HBM's noise features compare against raw thresholds from that
 * unnormalised range: 1.5 and 1.75 for the ore caves, 5 for the schist stratum, and 230/275/300 for the
 * <i>product of three</i> layer noises. A normalised generator would make those features never or always
 * fire, so the octave loop is reproduced here and the constants stay literal.
 *
 * <p>26.1's {@link SimplexNoise} is the same algorithm as 1.12's {@code NoiseGeneratorSimplex} (same
 * permutation shuffle, {@code xo/yo/zo} offsets and gradient maths), so a given seed produces the same field.
 */
public final class LegacyPerlinNoise {

    private final SimplexNoise[] noiseLevels;

    public LegacyPerlinNoise(RandomSource random, int octaves) {
        this.noiseLevels = new SimplexNoise[octaves];
        for (int i = 0; i < octaves; i++) {
            this.noiseLevels[i] = new SimplexNoise(random);
        }
    }

    /** The old {@code getValue(x, y)}: octave sum with amplitude and frequency both halving. */
    public double getValue(double x, double y) {
        double value = 0.0D;
        double factor = 1.0D;
        for (SimplexNoise level : this.noiseLevels) {
            value += level.getValue(x * factor, y * factor) / factor;
            factor /= 2.0D;
        }
        return value;
    }
}
