package com.hbm.inventory.fluid.trait;

import com.hbm.inventory.fluid.FluidType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Thermal conversion data; steps are stored in ascending heat order. */
public class FT_Heatable {

    private final List<HeatingStep> steps = new ArrayList<>();
    private final Map<HeatingType, Double> efficiency = new EnumMap<>(HeatingType.class);

    public FT_Heatable addStep(int heat, int req, FluidType type, int prod) {
        steps.add(new HeatingStep(req, heat, type, prod));
        return this;
    }

    public FT_Heatable setEff(HeatingType type, double eff) {
        efficiency.put(type, eff);
        return this;
    }

    public double getEfficiency(HeatingType type) { return efficiency.getOrDefault(type, 0.0D); }
    public HeatingStep getFirstStep() { return steps.get(0); }

    public enum HeatingType { BOILER, HEATEXCHANGER, PWR, ICF, PA }

    public static final class HeatingStep {
        public final int amountReq, heatReq, amountProduced;
        public final FluidType typeProduced;
        public HeatingStep(int amountReq, int heatReq, FluidType typeProduced, int amountProduced) {
            this.amountReq = amountReq;
            this.heatReq = heatReq;
            this.typeProduced = typeProduced;
            this.amountProduced = amountProduced;
        }
    }
}
