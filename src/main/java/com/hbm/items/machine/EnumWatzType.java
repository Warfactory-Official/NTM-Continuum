package com.hbm.items.machine;

import com.hbm.util.Function;
import com.hbm.util.Function.FunctionLinear;
import com.hbm.util.Function.FunctionQuadratic;
import com.hbm.util.Function.FunctionSqrt;
import com.hbm.util.Function.FunctionSqrtFalling;
import org.jspecify.annotations.Nullable;

/**
 * The twelve Watz fuel and absorber types — 1.12.2's {@code ItemWatzPellet.EnumWatzType}, extracted to
 * its own file because the port registers one item per type instead of one item with twelve metas.
 *
 * <p>Values are verbatim. Note that upstream's constructor argument order
 * ({@code colorLight, colorDark, passive, heatEmission, mudContent, burn, heatDiv, absorb}) does not
 * match its field declaration order; the assignment below is what actually ran, including
 * {@code mudContent} being halved on the way in.
 */
public enum EnumWatzType {

    SCHRABIDIUM(0x32FFFF, 0x005C5C, 2_000, 20D, 0.01D,   new FunctionLinear(1.5D),  new FunctionSqrtFalling(10D), null),
    HES        (0x66DCD6, 0x023933, 1_750, 20D, 0.005D,  new FunctionLinear(1.25D), new FunctionSqrtFalling(15D), null),
    MES        (0xCBEADF, 0x28473C, 1_500, 15D, 0.0025D, new FunctionLinear(1.15D), new FunctionSqrtFalling(15D), null),
    LES        (0xABB4A8, 0x0C1105, 1_250, 15D, 0.00125D,new FunctionLinear(1D),    new FunctionSqrtFalling(20D), null),
    HEN        (0xA6B2A6, 0x030F03, 0,     10D, 0.0005D, new FunctionSqrt(100),     new FunctionSqrtFalling(10D), null),
    MEU        (0xC1C7BD, 0x2B3227, 0,     10D, 0.0005D, new FunctionSqrt(75),      new FunctionSqrtFalling(10D), null),
    MEP        (0x9AA3A0, 0x111A17, 0,     15D, 0.0005D, new FunctionSqrt(150),     new FunctionSqrtFalling(10D), null),
    /** Standard absorber, negative coefficient. */
    LEAD       (0xA6A6B2, 0x03030F, 0,     0,   0.0025D, null, null, new FunctionSqrt(10)),
    /** Improved absorber, linear. */
    BORON      (0xBDC8D2, 0x29343E, 0,     0,   0.0025D, null, null, new FunctionLinear(10)),
    /** Absorber with a positive coefficient. */
    DU         (0xC1C7BD, 0x2B3227, 0,     0,   0.0025D, null, null, new FunctionQuadratic(1D, 1D).withDiv(100)),
    NQD        (0x4B4B4B, 0x121212, 2_000, 20,  0.01D,   new FunctionLinear(2D),    new FunctionSqrt(1D / 25D).withOff(25D * 25D), null),
    NQR        (0x2D2D2D, 0x0B0B0B, 2_500, 30,  0.01D,   new FunctionLinear(1.5D),  new FunctionSqrt(1D / 25D).withOff(25D * 25D), null);

    public static final EnumWatzType[] VALUES = values();

    public final int colorLight;
    public final int colorDark;
    /** How much mud per reaction flux this produces. */
    public final double mudContent;
    /** Base flux emission — anything above zero is self-igniting. */
    public final double passive;
    /** Heat produced per outgoing flux. */
    public final double heatEmission;
    /** Flux to reactivity (classic reactivity). */
    public final @Nullable Function burnFunc;
    /** Temperature coefficient: scales reactivity by the reactor's heat. */
    public final @Nullable Function heatDiv;
    /** Flux to heat, for non-fissile absorber pellets. */
    public final @Nullable Function absorbFunc;
    public final double yield = 500_000_000D;

    EnumWatzType(int colorLight, int colorDark, double passive, double heatEmission, double mudContent,
                 @Nullable Function burnFunc, @Nullable Function heatDiv, @Nullable Function absorbFunc) {
        this.colorLight = colorLight;
        this.colorDark = colorDark;
        this.passive = passive;
        this.heatEmission = heatEmission;
        this.mudContent = mudContent / 2D;
        this.burnFunc = burnFunc;
        this.heatDiv = heatDiv;
        this.absorbFunc = absorbFunc;
    }

    /** The registry-name suffix for this type's pellet items. */
    public String itemSuffix() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** Upstream's desaturation of a pellet colour, used for the depleted sprites. */
    public static int desaturate(int color) {
        int r = (color & 0xff0000) >> 16;
        int g = (color & 0x00ff00) >> 8;
        int b = (color & 0x0000ff);

        int avg = (r + g + b) / 3;
        double approach = 0.9;
        double mult = 0.75;

        // Upstream used compound assignment on ints with double operands, i.e. the truncation happens
        // on the whole right-hand side (r = (int)(r - (r-avg)*0.9)), not on the subtrahend. Spelled out
        // here so the arithmetic stays bit-identical.
        r = (int) (r - (r - avg) * approach);
        g = (int) (g - (g - avg) * approach);
        b = (int) (b - (b - avg) * approach);

        r = (int) (r * mult);
        g = (int) (g * mult);
        b = (int) (b * mult);

        return (r << 16) | (g << 8) | b;
    }
}
