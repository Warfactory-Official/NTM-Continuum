package com.hbm.util;

import net.minecraft.ChatFormatting;

import java.util.Locale;

/**
 * A response curve — the 1.12.2 {@code com.hbm.util.Function}, used to describe how a nuclear fuel
 * reacts to flux and heat. Each subclass is one shape (linear, sqrt, quadratic, log, flat), with an
 * optional input divisor and offset applied before the curve.
 *
 * <p>Ported verbatim, including the deliberately-odd {@code sqrt} approximation from
 * {@code BobMathUtil} (kept here so a fuel's numbers match upstream's exactly) and the display strings
 * the fuel tooltips print.
 */
public abstract class Function {

    protected double div = 1D;
    protected double off = 0D;

    /**
     * A curve as data: {@code {"type": "sqrt", "level": 22.5, "div": 1.0, "off": 0.0}}; types {@code linear},
     * {@code sqrt}, {@code quadratic} (with {@code v_off}), {@code log}, {@code passive}. 1.7's falling square root is a
     * {@code sqrt} with {@code level} 1/f and {@code off} f². Used by the reactor fuel data maps (PWR, Watz).
     */
    public static final com.mojang.serialization.Codec<Function> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.<Function>create(i -> i.group(
            com.mojang.serialization.Codec.STRING.fieldOf("type").forGetter(Function::typeName),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("level", 0D).forGetter(Function::levelValue),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("v_off", 0D).forGetter(f -> f instanceof FunctionDoubleArg d ? d.vOff : 0D),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("div", 1D).forGetter(f -> f.div),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("off", 0D).forGetter(f -> f.off)
    ).apply(i, (type, level, vOff, div, off) -> {
        Function f = switch (type) {
            case "linear" -> new FunctionLinear(level);
            case "sqrt" -> new FunctionSqrt(level);
            case "quadratic" -> new FunctionQuadratic(level, vOff);
            case "log" -> new FunctionLogarithmic(level);
            case "passive" -> new FunctionPassive(level);
            default -> throw new IllegalArgumentException("Unknown function type " + type);
        };
        return f.withDiv(div).withOff(off);
    }));

    private String typeName() {
        if (this instanceof FunctionLinear) return "linear";
        if (this instanceof FunctionSqrt) return "sqrt";
        if (this instanceof FunctionQuadratic) return "quadratic";
        if (this instanceof FunctionLogarithmic) return "log";
        if (this instanceof FunctionPassive) return "passive";
        throw new IllegalStateException("Unnamed function " + getClass());
    }

    private double levelValue() {
        if (this instanceof FunctionSingleArg s) return s.level;
        if (this instanceof FunctionDoubleArg d) return d.level;
        return 0D;
    }

    /** The German pronunciation of f(x) — "F von X", tee hee. */
    public abstract double effonix(double x);

    public abstract String getLabelForFuel();

    public abstract String getDangerFromFuel();

    public Function withDiv(double div) { this.div = div; return this; }
    public Function withOff(double off) { this.off = off; return this; }

    public double getX(double x) { return x / div + off; }

    public String getXName() { return getXName(true); }

    public String getXName(boolean brackets) {
        String x = "x";
        if (div != 1D) x += " / " + String.format(Locale.US, "%,.1f", div);
        if (off != 0D) x += " + " + String.format(Locale.US, "%,.1f", off);
        return x;
    }

    /** Upstream's cheap square-root approximation; kept so fuel curves reproduce exactly. */
    public static double sqrt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }

    public static abstract class FunctionSingleArg extends Function {
        protected final double level;
        public FunctionSingleArg(double level) { this.level = level; }
    }

    public static abstract class FunctionDoubleArg extends Function {
        protected final double level;
        protected final double vOff;
        public FunctionDoubleArg(double level, double vOff) { this.level = level; this.vOff = vOff; }
    }

    public static class FunctionLogarithmic extends FunctionSingleArg {
        public FunctionLogarithmic(double level) { super(level); this.withOff(1D); }
        @Override public double effonix(double x) { return Math.log10(getX(x)) * level; }
        @Override public String getLabelForFuel() { return "log10(" + getXName(false) + ") * " + String.format(Locale.US, "%,.1f", this.level); }
        @Override public String getDangerFromFuel() { return ChatFormatting.YELLOW + "MEDIUM / LOGARITHMIC"; }
    }

    public static class FunctionPassive extends FunctionSingleArg {
        public FunctionPassive(double level) { super(level); }
        @Override public double effonix(double x) { return this.level; }
        @Override public String getLabelForFuel() { return String.format(Locale.US, "%,.1f", this.level); }
        @Override public String getDangerFromFuel() { return ChatFormatting.DARK_GREEN + "SAFE / PASSIVE"; }
    }

    public static class FunctionSqrt extends FunctionSingleArg {
        public FunctionSqrt(double level) { super(level); }
        @Override public double effonix(double x) { return Function.sqrt(getX(x)) * this.level; }
        @Override public String getLabelForFuel() { return "sqrt(" + getXName(false) + ") * " + String.format(Locale.US, "%,.3f", this.level); }
        @Override public String getDangerFromFuel() { return ChatFormatting.YELLOW + "MEDIUM / SQUARE ROOT"; }
    }

    public static class FunctionSqrtFalling extends FunctionSqrt {
        public FunctionSqrtFalling(double fallFactor) {
            super(1D / fallFactor);
            this.withOff(fallFactor * fallFactor);
        }
    }

    public static class FunctionLinear extends FunctionSingleArg {
        public FunctionLinear(double level) { super(level); }
        @Override public double effonix(double x) { return getX(x) * this.level; }
        @Override public String getLabelForFuel() { return getXName(true) + " * " + String.format(Locale.US, "%,.1f", this.level); }
        @Override public String getDangerFromFuel() { return ChatFormatting.RED + "DANGEROUS / LINEAR"; }
    }

    public static class FunctionQuadratic extends FunctionDoubleArg {
        public FunctionQuadratic(double level) { super(level, 0D); }
        public FunctionQuadratic(double level, double vOff) { super(level, vOff); }
        @Override public double effonix(double x) { return getX(x) * getX(x) * this.level + this.vOff; }
        @Override public String getLabelForFuel() {
            return getXName(true) + "\u00b2 * " + String.format(Locale.US, "%,.1f", this.level)
                    + (vOff != 0 ? (" + " + String.format(Locale.US, "%,.1f", vOff)) : "");
        }
        @Override public String getDangerFromFuel() { return ChatFormatting.RED + "DANGEROUS / QUADRATIC"; }
    }
}
