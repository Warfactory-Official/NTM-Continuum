package com.hbm.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.WatzPellet;
import com.hbm.util.Function;
import com.hbm.util.Function.FunctionLinear;
import com.hbm.util.Function.FunctionQuadratic;
import com.hbm.util.Function.FunctionSqrt;
import com.hbm.util.Function.FunctionSqrtFalling;
import dev.golem.core.data.DataMapSource;
import java.util.Locale;
import java.util.Optional;

/**
 * 1.7 {@code ItemWatzPellet.EnumWatzType}: colour, dark colour, passive flux, heat per flux, mud (1.7's constructor
 * halved it: {@code this.mudContent = mudContent / 2D}), burn, heat divisor and absorber functions.
 */
public final class WatzPelletSource extends DataMapSource<WatzPellet> {

    public WatzPelletSource() {
        super(ReactorDataMaps.WATZ_PELLET, false);
    }

    private void pellet(String type, int color, int colorDark, double passive, double heat, double mud, Function burnFunc,
                        Function heatDiv, Function absorbFunc) {
        String name = type.toLowerCase(Locale.ROOT);
        put("hbm:watz_pellet_" + name, new WatzPellet(color, colorDark, passive, heat, mud / 2D,
                Optional.ofNullable(burnFunc), Optional.ofNullable(heatDiv), Optional.ofNullable(absorbFunc), 500_000_000D,
                "hbm:watz_pellet_depleted_" + name));
    }

    @Override
    protected void values() {
        pellet("SCHRABIDIUM", 0x32FFFF, 0x005C5C, 2_000, 20D, 0.01D, new FunctionLinear(1.5D), new FunctionSqrtFalling(10D), null);
        pellet("HES", 0x66DCD6, 0x023933, 1_750, 20D, 0.005D, new FunctionLinear(1.25D), new FunctionSqrtFalling(15D), null);
        pellet("MES", 0xCBEADF, 0x28473C, 1_500, 15D, 0.0025D, new FunctionLinear(1.15D), new FunctionSqrtFalling(15D), null);
        pellet("LES", 0xABB4A8, 0x0C1105, 1_250, 15D, 0.00125D, new FunctionLinear(1D), new FunctionSqrtFalling(20D), null);
        pellet("HEN", 0xA6B2A6, 0x030F03, 0, 10D, 0.0005D, new FunctionSqrt(100), new FunctionSqrtFalling(10D), null);
        pellet("MEU", 0xC1C7BD, 0x2B3227, 0, 10D, 0.0005D, new FunctionSqrt(75), new FunctionSqrtFalling(10D), null);
        pellet("MEP", 0x9AA3A0, 0x111A17, 0, 15D, 0.0005D, new FunctionSqrt(150), new FunctionSqrtFalling(10D), null);
        pellet("LEAD", 0xA6A6B2, 0x03030F, 0, 0, 0.0025D, null, null, new FunctionSqrt(10)); // standard absorber, negative coefficient
        pellet("BORON", 0xBDC8D2, 0x29343E, 0, 0, 0.0025D, null, null, new FunctionLinear(10)); // improved absorber, linear
        pellet("DU", 0xC1C7BD, 0x2B3227, 0, 0, 0.0025D, null, null, new FunctionQuadratic(1D, 1D).withDiv(100)); // absorber with positive coefficient
        pellet("NQD", 0x4B4B4B, 0x121212, 2_000, 20, 0.01D, new FunctionLinear(2D), new FunctionSqrt(1D / 25D).withOff(25D * 25D), null);
        pellet("NQR", 0x2D2D2D, 0x0B0B0B, 2_500, 30, 0.01D, new FunctionLinear(1.5D), new FunctionSqrt(1D / 25D).withOff(25D * 25D), null);
    }
}
