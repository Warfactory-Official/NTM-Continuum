package com.hbm.inventory.fluid.trait;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

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

    /**
     * 1.7 {@code addInfoHidden}: {@code "Thermal capacity: 200 TU per 1mB"} in red from the first step, then one
     * {@code "[Boilable] Efficiency: 100%"} line per heating type with a positive efficiency, in enum order.
     */
    public void addInfoHidden(List<Component> info) {
        HeatingStep first = getFirstStep();
        info.add(Component.translatable("hbmfluid.trait.thermalCapacity")
                .append(": " + first.heatReq + " ").append(Component.translatable("hbmfluid.trait.perTU"))
                .append(" " + first.amountReq + "mB").withStyle(ChatFormatting.RED));
        for (HeatingType type : HeatingType.values()) {
            double eff = getEfficiency(type);
            if (eff > 0) info.add(efficiencyLine(type.langKey(), eff));
        }
    }

    /** {@code "[name] "} in yellow, {@code "Efficiency: 100%"} in aqua (shared with {@link FT_Coolable}). */
    static Component efficiencyLine(String key, double eff) {
        return Component.literal("[").append(Component.translatable(key)).append("] ").withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("hbmfluid.trait.efficiency").append(": " + ((int) (eff * 100D)) + "%")
                        .withStyle(ChatFormatting.AQUA));
    }

    public enum HeatingType {
        BOILER, HEATEXCHANGER, PWR, ICF, PA;

        /** 1.7's {@code hbmfluid.trait.<name>} for each type. */
        public String langKey() {
            return switch (this) {
                case BOILER -> "hbmfluid.trait.boilable";
                case HEATEXCHANGER -> "hbmfluid.trait.heatable";
                case PWR -> "hbmfluid.trait.coolantPWR";
                case ICF -> "hbmfluid.trait.coolantICF";
                case PA -> "hbmfluid.trait.coolantPA";
            };
        }
    }

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
