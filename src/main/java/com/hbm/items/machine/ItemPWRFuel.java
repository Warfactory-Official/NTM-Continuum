package com.hbm.items.machine;

import com.hbm.items.ItemEnumMulti;
import com.hbm.util.Function;
import com.hbm.util.Function.FunctionLogarithmic;
import com.hbm.util.Function.FunctionSqrt;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The pressurised water reactor's fuel pellets. Each grade carries the curve that turns the flux a rod
 * sees into the flux it gives back, how much heat rides along with that, and how much reaction it takes
 * before the pellet is spent.
 */
public class ItemPWRFuel extends ItemEnumMulti {

    public ItemPWRFuel(Properties properties) {
        super(EnumPWRFuel.class, true, true, properties);
    }

    public enum EnumPWRFuel {
        MEU(5.0D, new FunctionLogarithmic(20 * 30).withDiv(2_500)),
        HEU233(7.5D, new FunctionSqrt(25)),
        HEU235(7.5D, new FunctionSqrt(22.5)),
        MEN(7.5D, new FunctionLogarithmic(22.5 * 30).withDiv(2_500)),
        HEN237(7.5D, new FunctionSqrt(27.5)),
        MOX(7.5D, new FunctionLogarithmic(20 * 30).withDiv(2_500)),
        MEP(7.5D, new FunctionLogarithmic(22.5 * 30).withDiv(2_500)),
        HEP239(10.0D, new FunctionSqrt(22.5)),
        HEP241(10.0D, new FunctionSqrt(25)),
        MEA(7.5D, new FunctionLogarithmic(25 * 30).withDiv(2_500)),
        HEA242(10.0D, new FunctionSqrt(25)),
        HES326(12.5D, new FunctionSqrt(27.5)),
        HES327(12.5D, new FunctionSqrt(30)),
        BFB_AM_MIX(2.5D, new FunctionSqrt(15), 250_000_000),
        BFB_PU241(2.5D, new FunctionSqrt(15), 250_000_000);

        public static final EnumPWRFuel[] VALUES = values();

        public final double yield;
        public final double heatEmission;
        public final Function function;

        EnumPWRFuel(double heatEmission, Function function, double yield) {
            this.heatEmission = heatEmission;
            this.function = function;
            this.yield = yield;
        }

        EnumPWRFuel(double heatEmission, Function function) {
            this(heatEmission, function, 1_000_000_000);
        }
    }

    public static EnumPWRFuel typeOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPWRFuel fuel)) return null;
        return (EnumPWRFuel) fuel.getType(stack);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        EnumPWRFuel fuel = typeOf(stack);
        if (fuel == null) return;

        tooltip.add(Component.literal("Heat per flux: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.heatEmission + " TU").withStyle(ChatFormatting.RESET)));
        tooltip.add(Component.literal("Reaction function: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.function.getLabelForFuel()).withStyle(ChatFormatting.RESET)));
        tooltip.add(Component.literal("Fuel type: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.function.getDangerFromFuel()).withStyle(ChatFormatting.RESET)));
    }
}
