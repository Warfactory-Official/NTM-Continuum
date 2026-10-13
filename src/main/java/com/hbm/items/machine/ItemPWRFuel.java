package com.hbm.items.machine;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.items.ModPwrItems;
import com.hbm.util.Function;
import com.hbm.util.Function.FunctionLogarithmic;
import com.hbm.util.Function.FunctionSqrt;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * One PWR fuel rod. 1.7's {@code ItemPWRFuel} was one {@code ItemEnumMulti} over {@link EnumPWRFuel} selected
 * by damage; here the family is flattened to one registered item per constant ({@code pwr_fuel_<variant>}),
 * and the hot and depleted families ({@code pwr_fuel_hot_<variant>}, {@code pwr_fuel_depleted_<variant>}) are
 * plain items that only carry their constant.
 */
public class ItemPWRFuel extends CompatItem {

    /** The fuel constants, verbatim from 1.7: heat per flux, reaction function, and yield (flux per rod). */
    public enum EnumPWRFuel {
        MEU(        5.0D,  new FunctionLogarithmic(20 * 30).withDiv(2_500)),
        HEU233(     7.5D,  new FunctionSqrt(25)),
        HEU235(     7.5D,  new FunctionSqrt(22.5)),
        MEN(        7.5D,  new FunctionLogarithmic(22.5 * 30).withDiv(2_500)),
        HEN237(     7.5D,  new FunctionSqrt(27.5)),
        MOX(        7.5D,  new FunctionLogarithmic(20 * 30).withDiv(2_500)),
        MEP(        7.5D,  new FunctionLogarithmic(22.5 * 30).withDiv(2_500)),
        HEP239(     10.0D, new FunctionSqrt(22.5)),
        HEP241(     10.0D, new FunctionSqrt(25)),
        MEA(        7.5D,  new FunctionLogarithmic(25 * 30).withDiv(2_500)),
        HEA242(     10.0D, new FunctionSqrt(25)),
        HES326(     12.5D, new FunctionSqrt(27.5)),
        HES327(     12.5D, new FunctionSqrt(30)),
        BFB_AM_MIX( 2.5D,  new FunctionSqrt(15), 250_000_000),
        BFB_PU241(  2.5D,  new FunctionSqrt(15), 250_000_000);

        public static final EnumPWRFuel[] VALUES = values();

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

        EnumPWRFuel(double heatEmission, Function function, double declaredYield) {
            this.heatEmission = heatEmission;
            this.function = function;
            this.declaredYield = declaredYield;
            this.id = name().toLowerCase(Locale.US);
        }

        EnumPWRFuel(double heatEmission, Function function) {
            this(heatEmission, function, 1_000_000_000);
        }

        public static EnumPWRFuel byOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) return null;
            return VALUES[ordinal];
        }
    }

    public final EnumPWRFuel fuel;

    public ItemPWRFuel(Properties properties, EnumPWRFuel fuel) {
        super(properties);
        this.fuel = fuel;
    }

    /**
     * The fuel values of {@code item}: the {@code hbm:pwr_fuel} item data map, else HBM's enum constant for its own rods;
     * null when the item is not PWR fuel. An addon's fuel is any item with a data-map entry.
     */
    public static ReactorDataMaps.@org.jspecify.annotations.Nullable PWRFuel stats(Item item) {
        ReactorDataMaps.PWRFuel data = ReactorDataMaps.PWR_FUEL.get(item);
        if (data != null) return data;
        return codeStats(item);
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public static ReactorDataMaps.@org.jspecify.annotations.Nullable PWRFuel codeStats(Item item) {
        if (!(item instanceof ItemPWRFuel rod) || rod.fuel.function == null) return null;
        return new ReactorDataMaps.PWRFuel(rod.fuel.heatEmission, rod.fuel.function, rod.fuel.yield,
                key(ModPwrItems.hot(rod.fuel)), key(ModPwrItems.depleted(rod.fuel)));
    }

    private static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        ReactorDataMaps.PWRFuel fuel = stats(this);
        if (fuel == null) return;
        tooltip.add(Component.literal("Heat per flux: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.heat() + " TU").withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Reaction function: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.function().getLabelForFuel()).withStyle(ChatFormatting.WHITE)));
        tooltip.add(Component.literal("Fuel type: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(fuel.function().getDangerFromFuel()).withStyle(ChatFormatting.WHITE)));
    }
}
