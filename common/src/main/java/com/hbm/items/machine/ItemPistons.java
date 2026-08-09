package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidCombustion.FuelGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Combustion-engine piston set — 1:1 port of the 1.12.2 {@code items/machine/ItemPistons}. The old
 * metadata item becomes one registered item per {@link Variant} (id {@code piston_set_<variant>}); each
 * carries a per-fuel-grade efficiency array (indexed by {@link FuelGrade}) the combustion engine reads.
 * The tooltip lists the efficiency per grade, matching the old {@code addInformation}.
 */
public class ItemPistons extends Item {

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

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        adder.accept(Component.translatable("desc.item.pistonEfficiency").withStyle(ChatFormatting.YELLOW));
        for (int i = 0; i < variant.eff.length; i++) {
            adder.accept(Component.literal(ChatFormatting.YELLOW + "-")
                    .append(Component.translatable(GRADE_KEYS[i]).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(ChatFormatting.YELLOW + ": " + ChatFormatting.RED + (int) (variant.eff[i] * 100) + "%")));
        }
    }
}
