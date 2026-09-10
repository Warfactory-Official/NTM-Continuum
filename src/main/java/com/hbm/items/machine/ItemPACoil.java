package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The superconducting coil a quadrupole or dipole needs to bend the beam. Each grade has a momentum
 * window: below the minimum the magnet still works but draws ten times the power, above the maximum the
 * particle crashes.
 *
 * <p>Ported from 1.12.2's {@code ItemPACoil}, whose four metadata subtypes each become their own
 * registered item ({@code pa_coil_gold} … {@code pa_coil_chlorophyte}), the same flattening
 * {@link com.hbm.items.ModIntermediateItems} applies to the other {@code ItemEnumMulti} families.
 */
public class ItemPACoil extends CompatItem {

    public final EnumCoilType type;

    public ItemPACoil(EnumCoilType type, Item.Properties properties) {
        super(properties);
        this.type = type;
    }

    /** The coil grade of {@code stack}, or null when it is not a coil. */
    public static @Nullable EnumCoilType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemPACoil coil ? coil.type : null;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Quadrupole operational range: ").withStyle(ChatFormatting.BLUE)
                .append(Component.literal(range(type.quadMin, type.quadMax)).withStyle(ChatFormatting.RESET)));
        tooltip.add(Component.literal("Dipole operational range: ").withStyle(ChatFormatting.BLUE)
                .append(Component.literal(range(type.diMin, type.diMax)).withStyle(ChatFormatting.RESET)));
        tooltip.add(Component.literal("Dipole minimum side length: ").withStyle(ChatFormatting.BLUE)
                .append(Component.literal("" + type.diDistMin).withStyle(ChatFormatting.RESET)));
        tooltip.add(Component.literal("Minimums not met result in a power draw penalty!").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Maximums exceeded result in the particle crashing!").withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("Particles will crash in dipoles if both penalties take effect!").withStyle(ChatFormatting.RED));
    }

    private static String range(int min, int max) {
        return String.format(Locale.US, "%,d", min) + " - " + String.format(Locale.US, "%,d", max);
    }

    public enum EnumCoilType {
        GOLD(0, 2_200, 0, 2_200, 15),
        NIOBIUM(1_500, 8_400, 1_500, 8_400, 21),
        BSCCO(7_500, 15_000, 7_500, 15_000, 27),
        CHLOROPHYTE(14_500, 75_000, 14_500, 75_000, 51);

        public static final EnumCoilType[] VALUES = values();

        public final int quadMin;
        public final int quadMax;
        public final int diMin;
        public final int diMax;
        public final int diDistMin;

        EnumCoilType(int quadMin, int quadMax, int diMin, int diMax, int diDistMin) {
            this.quadMin = quadMin;
            this.quadMax = quadMax;
            this.diMin = diMin;
            this.diMax = diMax;
            this.diDistMin = diDistMin;
        }
    }
}
