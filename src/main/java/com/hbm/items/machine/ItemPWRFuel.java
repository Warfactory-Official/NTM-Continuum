package com.hbm.items.machine;

import com.hbm.util.Function;
import com.hbm.util.Function.FunctionLogarithmic;
import com.hbm.util.Function.FunctionSqrt;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One PWR fuel rod. 1.7's {@code ItemPWRFuel} was one {@code ItemEnumMulti} over {@link EnumPWRFuel} selected
 * by damage; here the family is flattened to one registered item per constant ({@code pwr_fuel_<variant>}),
 * and the hot and depleted families ({@code pwr_fuel_hot_<variant>}, {@code pwr_fuel_depleted_<variant>}) are
 * plain items that only carry their constant.
 */
public class ItemPWRFuel extends CompatItem {

    /** The fuel constants, verbatim from 1.7: heat per flux, reaction function, and yield (flux per rod). */
    public static final class EnumPWRFuel {

        private static final List<EnumPWRFuel> REGISTERED = new ArrayList<>();
        private static final Map<String, EnumPWRFuel> BY_NAME = new HashMap<>();
        public static final List<EnumPWRFuel> VALUES = Collections.unmodifiableList(REGISTERED);

        static {
            dev.golem.core.content.SyncedIndex.register("hbm:pwr_fuels", () -> REGISTERED.stream().map(EnumPWRFuel::name).toList());
        }

        public static final EnumPWRFuel MEU =        new EnumPWRFuel("MEU",        5.0D,  new FunctionLogarithmic(20 * 30).withDiv(2_500));
        public static final EnumPWRFuel HEU233 =     new EnumPWRFuel("HEU233",     7.5D,  new FunctionSqrt(25));
        public static final EnumPWRFuel HEU235 =     new EnumPWRFuel("HEU235",     7.5D,  new FunctionSqrt(22.5));
        public static final EnumPWRFuel MEN =        new EnumPWRFuel("MEN",        7.5D,  new FunctionLogarithmic(22.5 * 30).withDiv(2_500));
        public static final EnumPWRFuel HEN237 =     new EnumPWRFuel("HEN237",     7.5D,  new FunctionSqrt(27.5));
        public static final EnumPWRFuel MOX =        new EnumPWRFuel("MOX",        7.5D,  new FunctionLogarithmic(20 * 30).withDiv(2_500));
        public static final EnumPWRFuel MEP =        new EnumPWRFuel("MEP",        7.5D,  new FunctionLogarithmic(22.5 * 30).withDiv(2_500));
        public static final EnumPWRFuel HEP239 =     new EnumPWRFuel("HEP239",     10.0D, new FunctionSqrt(22.5));
        public static final EnumPWRFuel HEP241 =     new EnumPWRFuel("HEP241",     10.0D, new FunctionSqrt(25));
        public static final EnumPWRFuel MEA =        new EnumPWRFuel("MEA",        7.5D,  new FunctionLogarithmic(25 * 30).withDiv(2_500));
        public static final EnumPWRFuel HEA242 =     new EnumPWRFuel("HEA242",     10.0D, new FunctionSqrt(25));
        public static final EnumPWRFuel HES326 =     new EnumPWRFuel("HES326",     12.5D, new FunctionSqrt(27.5));
        public static final EnumPWRFuel HES327 =     new EnumPWRFuel("HES327",     12.5D, new FunctionSqrt(30));
        public static final EnumPWRFuel BFB_AM_MIX = new EnumPWRFuel("BFB_AM_MIX", 2.5D,  new FunctionSqrt(15), 250_000_000);
        public static final EnumPWRFuel BFB_PU241 =  new EnumPWRFuel("BFB_PU241",  2.5D,  new FunctionSqrt(15), 250_000_000);

        /**
         * 1.7's three-arg constructor <i>ignored</i> the yield argument (a field initialiser of 1e9 that the
         * constructor never overwrote), so the two BFB rods really run at the full 1,000,000,000 too. Kept
         * exactly so the depletion rate matches upstream; the intended value is preserved in
         * {@link #declaredYield} for the record.
         */
        public final double yield = 1_000_000_000;
        public final double declaredYield;
        public final double heatEmission;
        public final Function function;
        public final String id;
        public final int index;
        private final String name;

        private EnumPWRFuel(String name, double heatEmission, Function function, double declaredYield) {
            if (BY_NAME.containsKey(name)) throw new IllegalArgumentException("Duplicate PWR fuel " + name);
            this.name = name;
            this.heatEmission = heatEmission;
            this.function = function;
            this.declaredYield = declaredYield;
            this.id = name.toLowerCase(Locale.US);
            this.index = REGISTERED.size();
            REGISTERED.add(this);
            BY_NAME.put(name, this);
        }

        private EnumPWRFuel(String name, double heatEmission, Function function) {
            this(name, heatEmission, function, 1_000_000_000);
        }

        public static EnumPWRFuel create(String name, double heatEmission, Function function) {
            return new EnumPWRFuel(name.toUpperCase(Locale.US), heatEmission, function);
        }

        public static EnumPWRFuel[] values() {
            return REGISTERED.toArray(new EnumPWRFuel[0]);
        }

        public static EnumPWRFuel byName(String name) {
            return BY_NAME.get(name.toUpperCase(Locale.US));
        }

        public static EnumPWRFuel byIndex(int index) {
            if (index < 0 || index >= REGISTERED.size()) return null;
            return REGISTERED.get(index);
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final EnumPWRFuel fuel;

    public ItemPWRFuel(Properties properties, EnumPWRFuel fuel) {
        super(properties);
        this.fuel = fuel;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Heat per flux: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.heatEmission + " TU").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Reaction function: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.function.getLabelForFuel()).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Fuel type: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.function.getDangerFromFuel()).withStyle(ChatFormatting.WHITE)));
    }
}
