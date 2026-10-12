package com.hbm.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.PWRFuel;
import com.hbm.util.Function;
import com.hbm.util.Function.FunctionLogarithmic;
import com.hbm.util.Function.FunctionSqrt;
import dev.golem.core.data.DataMapSource;
import java.util.Locale;

/** 1.7 {@code ItemPWRFuel.EnumPWRFuel} (heat, function, yield); each fresh rod names its hot and depleted rods. */
public final class PWRFuelSource extends DataMapSource<PWRFuel> {

    public PWRFuelSource() {
        super(ReactorDataMaps.PWR_FUEL, false);
    }

    private void fuel(String type, double heat, Function function) {
        fuel(type, heat, function, 1_000_000_000D);
    }

    private void fuel(String type, double heat, Function function, double yield) {
        String name = type.toLowerCase(Locale.ROOT);
        put("hbm:pwr_fuel_" + name, new PWRFuel(heat, function, yield, "hbm:pwr_fuel_hot_" + name,
                "hbm:pwr_fuel_depleted_" + name));
    }

    @Override
    protected void values() {
        fuel("MEU", 05.0D, new FunctionLogarithmic(20 * 30).withDiv(2_500));
        fuel("HEU233", 07.5D, new FunctionSqrt(25));
        fuel("HEU235", 07.5D, new FunctionSqrt(22.5));
        fuel("MEN", 07.5D, new FunctionLogarithmic(22.5 * 30).withDiv(2_500));
        fuel("HEN237", 07.5D, new FunctionSqrt(27.5));
        fuel("MOX", 07.5D, new FunctionLogarithmic(20 * 30).withDiv(2_500));
        fuel("MEP", 07.5D, new FunctionLogarithmic(22.5 * 30).withDiv(2_500));
        fuel("HEP239", 10.0D, new FunctionSqrt(22.5));
        fuel("HEP241", 10.0D, new FunctionSqrt(25));
        fuel("MEA", 07.5D, new FunctionLogarithmic(25 * 30).withDiv(2_500));
        fuel("HEA242", 10.0D, new FunctionSqrt(25));
        fuel("HES326", 12.5D, new FunctionSqrt(27.5));
        fuel("HES327", 12.5D, new FunctionSqrt(30));
        // 1.7 BFB_AM_MIX / BFB_PU241 pass 250_000_000, but EnumPWRFuel's three-argument constructor never assigns it, so
        // the rods run at the field's 1_000_000_000 (the default here)
        fuel("BFB_AM_MIX", 2.5D, new FunctionSqrt(15));
        fuel("BFB_PU241", 2.5D, new FunctionSqrt(15));
    }
}
