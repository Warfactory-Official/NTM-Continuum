package com.hbm.inventory.fluid.trait;

import com.hbm.api.fluid.FluidType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable cooling conversion data carried by a {@link FluidType}. This is the typed equivalent of
 * the old fluid-trait record: one input fluid cools to one output fluid, with one heat value and a
 * separate efficiency for each consumer.
 */
public final class FT_Coolable {

    public enum CoolingType {
        TURBINE("trait.ctype.turbine"),
        HEATEXCHANGER("trait.ctype.heatexch");

        public static final CoolingType[] VALUES = values();
        public final String name;

        CoolingType(String name) {
            this.name = name;
        }
    }

    public static final CoolingType[] VALUES = CoolingType.values();

    public final FluidType coolsTo;
    public final int amountReq;
    public final int amountProduced;
    public final int heatEnergy;

    private final Map<CoolingType, Double> efficiency;

    public FT_Coolable(FluidType coolsTo, int amountReq, int amountProduced, int heatEnergy) {
        this(coolsTo, amountReq, amountProduced, heatEnergy, new EnumMap<>(CoolingType.class));
    }

    private FT_Coolable(FluidType coolsTo, int amountReq, int amountProduced, int heatEnergy,
                        Map<CoolingType, Double> efficiency) {
        this.coolsTo = coolsTo;
        this.amountReq = amountReq;
        this.amountProduced = amountProduced;
        this.heatEnergy = heatEnergy;
        EnumMap<CoolingType, Double> copy = new EnumMap<>(CoolingType.class);
        copy.putAll(efficiency);
        this.efficiency = Collections.unmodifiableMap(copy);
    }

    /** Returns a copy with this consumer efficiency added, preserving immutable registration data. */
    public FT_Coolable setEff(CoolingType type, double eff) {
        EnumMap<CoolingType, Double> next = new EnumMap<>(CoolingType.class);
        next.putAll(efficiency);
        next.put(type, eff);
        return new FT_Coolable(coolsTo, amountReq, amountProduced, heatEnergy, next);
    }

    public double getEfficiency(CoolingType type) {
        return efficiency.getOrDefault(type, 0.0D);
    }

    public Map<CoolingType, Double> getEfficiencies() {
        return efficiency;
    }
}
