package com.hbm.inventory.fluid.trait;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable cooling conversion data carried by a {@link FluidType}. This is the typed equivalent of
 * the old fluid-trait record: one input fluid cools to one output fluid, with one heat value and a
 * separate efficiency for each consumer.
 */
public final class FT_Coolable {

    public enum CoolingType {
        TURBINE("hbmfluid.trait.steam"),
        HEATEXCHANGER("hbmfluid.trait.coolable");

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

    /**
     * 1.7 {@code addInfoHidden}: {@code "Thermal capacity: 200 TU per 100mB"} in red, then one
     * {@code "[Turbine Steam] Efficiency: 100%"} line per cooling type with a positive efficiency.
     */
    public void addInfoHidden(List<Component> info) {
        info.add(Component.translatable("hbmfluid.trait.thermalCapacity")
                .append(": " + heatEnergy + " ").append(Component.translatable("hbmfluid.trait.perTU"))
                .append(" " + amountReq + "mB").withStyle(ChatFormatting.RED));
        for (CoolingType type : CoolingType.values()) {
            double eff = getEfficiency(type);
            if (eff > 0) info.add(FT_Heatable.efficiencyLine(type.name, eff));
        }
    }
}
