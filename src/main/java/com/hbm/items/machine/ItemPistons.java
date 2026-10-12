package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidCombustion.FuelGrade;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Combustion-engine piston set — 1:1 port of the 1.12.2 {@code items/machine/ItemPistons}. The old
 * metadata item becomes one registered item per {@link Variant} (id {@code piston_set_<variant>}); each
 * carries a per-fuel-grade efficiency array (indexed by {@link FuelGrade}) the combustion engine reads.
 * The tooltip lists the efficiency per grade, matching the old {@code addInformation}.
 */
public class ItemPistons extends CompatItem {

    /** Old {@code EnumPistonType} — eff indexed by {@link FuelGrade#ordinal()} (LOW, MEDIUM, HIGH, AERO, GAS). */
    public enum Variant {
        STEEL(1.00, 0.75, 0.25, 0.00, 0.00),
        DURA(0.50, 1.00, 0.90, 0.50, 0.00),
        DESH(0.00, 0.50, 1.00, 0.75, 0.00),
        STARMETAL(0.50, 0.75, 1.00, 0.90, 0.50);

        public static final Variant[] VALUES = values();
        public final double[] eff;

        Variant(double... eff) {
            this.eff = new double[Math.min(FuelGrade.values().length, eff.length)];
            System.arraycopy(eff, 0, this.eff, 0, this.eff.length);
        }
    }

    /** Old {@code FuelGrade.getGrade()} lang keys, in {@link FuelGrade} order. */
    private static final String[] GRADE_KEYS = {
            "trait.combustable.low", "trait.combustable.medium", "trait.combustable.high",
            "trait.combustable.avi", "trait.combustable.gas"
    };

    private final Variant variant;

    public ItemPistons(Properties props, Variant variant) {
        super(props);
        this.variant = variant;
    }

    public Variant getVariant() {
        return variant;
    }

    /**
     * Efficiency per fuel grade, indexed by {@link FuelGrade#ordinal()}: the {@code hbm:piston} entry (by grade name),
     * else HBM's enum constant.
     */
    public double[] efficiency() {
        java.util.Map<String, Double> data = com.hbm.api.data.MachinePartDataMaps.PISTON.get(this);
        if (data == null) return variant.eff;
        double[] eff = new double[FuelGrade.values().length];
        for (FuelGrade grade : FuelGrade.values()) eff[grade.ordinal()] = data.getOrDefault(grade.name(), 0D);
        return eff;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.item.pistonEfficiency").withStyle(ChatFormatting.YELLOW));
        double[] eff = efficiency();
        for (int i = 0; i < eff.length; i++) {
            tooltip.add(Component.literal(ChatFormatting.YELLOW + "-")
                    .append(Component.translatable(GRADE_KEYS[i]).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(ChatFormatting.YELLOW + ": " + ChatFormatting.RED + (int) (eff[i] * 100) + "%")));
        }
    }
}
