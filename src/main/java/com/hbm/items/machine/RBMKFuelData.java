package com.hbm.items.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-rod fuel state on the fuel-rod stack (the 1.7 NBT keys yield/xenon/core/hull, collapsed into one component). Immutable; the burn cycle read-modify-writes via with*.
 */
public record RBMKFuelData(double yield, double xenon, double coreHeat, double hullHeat) {

    public static final Codec<RBMKFuelData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("yield").forGetter(RBMKFuelData::yield),
            Codec.DOUBLE.fieldOf("xenon").forGetter(RBMKFuelData::xenon),
            Codec.DOUBLE.fieldOf("core").forGetter(RBMKFuelData::coreHeat),
            Codec.DOUBLE.fieldOf("hull").forGetter(RBMKFuelData::hullHeat)
    ).apply(i, RBMKFuelData::new));

    public RBMKFuelData withYield(double y) {
        return new RBMKFuelData(y, xenon, coreHeat, hullHeat);
    }

    public RBMKFuelData withXenon(double x) {
        return new RBMKFuelData(yield, x, coreHeat, hullHeat);
    }

    public RBMKFuelData withCoreHeat(double c) {
        return new RBMKFuelData(yield, xenon, c, hullHeat);
    }

    public RBMKFuelData withHullHeat(double h) {
        return new RBMKFuelData(yield, xenon, coreHeat, h);
    }
}
