package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

import java.util.List;

/** Component-backed RBMK fuel state and the upstream fuel reaction equations. */
public class ItemRBMKRod extends CompatItem {
    public ItemLike pellet;
    public final String fullName;
    public double reactivity, selfRate, xGen = 0.5D, xBurn = 50D, heat = 1D, yield, meltingPoint = 1000D,
            diffusion = 0.02D, heatCoeffStart, heatCoeffLength;
    public EnumBurnFunc function = EnumBurnFunc.LOG_TEN;
    public EnumDepleteFunc depFunc = EnumDepleteFunc.GENTLE_SLOPE;
    public NType nType = NType.SLOW, rType = NType.FAST;
    public int colorTint = 0x304825;

    public ItemRBMKRod(Properties properties, String fullName) { super(properties); this.fullName = fullName; }

    /**
     * This rod's reactor values: the {@code hbm:rbmk_fuel} item data map ({@link com.hbm.api.data.ReactorDataMaps#RBMK_FUEL}),
     * else what the public builder setters gave it in code (an addon may use either). Read at use time.
     */
    public com.hbm.api.data.ReactorDataMaps.RBMKFuel stats() {
        com.hbm.api.data.ReactorDataMaps.RBMKFuel data = com.hbm.api.data.ReactorDataMaps.RBMK_FUEL.get(this);
        if (data != null) return data;
        return codeStats();
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public com.hbm.api.data.ReactorDataMaps.RBMKFuel codeStats() {
        return new com.hbm.api.data.ReactorDataMaps.RBMKFuel(yield, reactivity, selfRate, function, depFunc, xGen, xBurn, heat,
                diffusion, meltingPoint, heatCoeffStart, heatCoeffLength, nType, rType, colorTint,
                pellet == null ? java.util.Optional.empty() : java.util.Optional.of(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(pellet.asItem()).toString()));
    }

    /** The recycling pellet of this rod, or null. */
    public net.minecraft.world.item.@org.jspecify.annotations.Nullable Item pelletItem() {
        return stats().pellet().map(id -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(dev.golem.core.Ids.parse(id)))
                .filter(item -> item != net.minecraft.world.item.Items.AIR).orElse(null);
    }
    public static RBMKFuelData getData(ItemStack stack) {
        RBMKFuelData data = ModDataComponents.RBMK_FUEL.get(stack).orElse(null);
        return data != null ? data : new RBMKFuelData(stack.getItem() instanceof ItemRBMKRod rod ? rod.stats().yield() : 0D, 0D, 20D, 20D);
    }
    public static double getYield(ItemStack s) { return getData(s).yield(); }
    public static void setYield(ItemStack s, double v) { ModDataComponents.RBMK_FUEL.set(s, getData(s).withYield(v)); }
    public static double getPoison(ItemStack s) { return getData(s).xenon(); }
    public static void setPoison(ItemStack s, double v) { ModDataComponents.RBMK_FUEL.set(s, getData(s).withXenon(v)); }
    public static double getCoreHeat(ItemStack s) { return getData(s).coreHeat(); }
    public static void setCoreHeat(ItemStack s, double v) { ModDataComponents.RBMK_FUEL.set(s, getData(s).withCoreHeat(v)); }
    public static double getHullHeat(ItemStack s) { return getData(s).hullHeat(); }
    public static void setHullHeat(ItemStack s, double v) { ModDataComponents.RBMK_FUEL.set(s, getData(s).withHullHeat(v)); }
    public static double getEnrichment(ItemStack s) { return getYield(s) / ((ItemRBMKRod) s.getItem()).stats().yield(); }
    public static double getPoisonLevel(ItemStack s) { return getPoison(s) / 100D; }
    public ItemRBMKRod setTint(int v) { colorTint = v; return this; }
    public ItemRBMKRod setYield(double v) { yield = v; return this; }
    public ItemRBMKRod setStats(double v) { return setStats(v, 0D); }
    public ItemRBMKRod setStats(double v, double self) { reactivity = v; selfRate = self; return this; }
    public ItemRBMKRod setFunction(EnumBurnFunc v) { function = v; return this; }
    public ItemRBMKRod setDepletionFunction(EnumDepleteFunc v) { depFunc = v; return this; }
    public ItemRBMKRod setHeatCoeff(double start, double length) { heatCoeffStart = start; heatCoeffLength = length; return this; }
    public ItemRBMKRod setXenon(double gen, double burn) { xGen = gen; xBurn = burn; return this; }
    public ItemRBMKRod setHeat(double v) { heat = v; return this; }
    public ItemRBMKRod setDiffusion(double v) { diffusion = v; return this; }
    public ItemRBMKRod setMeltingPoint(double v) { meltingPoint = v; return this; }
    public ItemRBMKRod setNeutronTypes(NType in, NType out) { nType = in; rType = out; return this; }

    public double burn(Level world, ItemStack stack, double inFlux) {
        com.hbm.api.data.ReactorDataMaps.RBMKFuel f = stats();
        inFlux += f.selfRate();
        if (RBMKDials.getXenon(world)) {
            double xenon = getPoison(stack) - xenonBurnFunc(inFlux);
            inFlux *= 1D - getPoisonLevel(stack);
            xenon += xenonGenFunc(inFlux);
            setPoison(stack, Mth.clamp(xenon, 0D, 100D));
        }
        double mult = 1D;
        double coreHeat = getCoreHeat(stack);
        if (f.heatCoeffStart() != 0D && coreHeat >= f.heatCoeffStart()) {
            double progress = Math.min((coreHeat - f.heatCoeffStart()) / f.heatCoeffLength(), 1D);
            mult = Math.sin((progress * Math.PI + Math.PI) / 2D);
        }
        double outFlux = reactivityFunc(inFlux, getEnrichment(stack) * mult) * RBMKDials.getReactivityMod(world);
        if (RBMKDials.getDepletion(world)) setYield(stack, Math.max(0D, getYield(stack) - inFlux));
        setCoreHeat(stack, rectify(coreHeat + outFlux * f.heat()));
        return outFlux;
    }
    public void updateHeat(Level world, ItemStack stack, double mod) {
        double diffusion = stats().diffusion();
        double core = getCoreHeat(stack), hull = getHullHeat(stack);
        if (core > hull) {
            double mid = (core - hull) / 2D;
            core -= mid * diffusion * RBMKDials.getFuelDiffusionMod(world) * mod;
            hull += mid * diffusion * RBMKDials.getFuelDiffusionMod(world) * mod;
            setCoreHeat(stack, rectify(core)); setHullHeat(stack, rectify(hull));
        }
    }
    public double provideHeat(Level world, ItemStack stack, double heat, double mod) {
        double hull = getHullHeat(stack);
        if (hull > stats().meltingPoint()) { double avg = (heat + hull + getCoreHeat(stack)) / 3D; setCoreHeat(stack, avg); setHullHeat(stack, avg); return avg - heat; }
        if (hull <= heat) return 0D;
        double out = (hull - heat) / 2D * RBMKDials.getFuelHeatProvision(world) * mod;
        setHullHeat(stack, hull - out); return out;
    }
    public double reactivityFunc(double in, double enrichment) {
        com.hbm.api.data.ReactorDataMaps.RBMKFuel f = stats();
        double flux = in * reactivityModByEnrichment(enrichment);
        return f.function().apply(flux, enrichment, f.reactivity(), f.selfRate());
    }
    public double reactivityModByEnrichment(double e) { return stats().depletion().apply(e); }
    public double xenonGenFunc(double flux) { return flux * stats().xenonGen(); }
    public double xenonBurnFunc(double flux) { return flux * flux / stats().xenonBurn(); }
    private static double rectify(double value) { return value > 1_000_000D ? 1_000_000D : value < 20D || Double.isNaN(value) ? 20D : value; }
    @Override public boolean isBarVisible(ItemStack s) { return getEnrichment(s) < 1D; }
    @Override public int getBarWidth(ItemStack s) { return (int) Math.round(13D * Mth.clamp(getEnrichment(s), 0D, 1D)); }
    @Override public int getBarColor(ItemStack s) { return 0x33FF33; }

    // The legacy tooltip displays the actual reaction curve, including its current enrichment modifier.
    public String getFuncDescription(ItemStack stack) {
        com.hbm.api.data.ReactorDataMaps.RBMKFuel f = stats();
        double selfRate = f.selfRate();
        String function = f.function().description(selfRate);
        double enrichment = getEnrichment(stack);
        if (enrichment < 1D) {
            enrichment = reactivityModByEnrichment(enrichment);
            String reactivity = ChatFormatting.YELLOW + "" + ((int) (f.reactivity() * enrichment * 1000D) / 1000D)
                    + ChatFormatting.WHITE;
            String enrichmentPercent = ChatFormatting.GOLD + " (" + ((int) (enrichment * 1000D) / 10D) + "%)";
            return String.format(function, selfRate > 0D ? "(x" + ChatFormatting.RED + " + " + selfRate
                    + ChatFormatting.WHITE + ")" : "x", reactivity).concat(enrichmentPercent);
        }
        return String.format(function, selfRate > 0D ? "(x" + ChatFormatting.RED + " + " + selfRate
                + ChatFormatting.WHITE + ")" : "x", f.reactivity());
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal(fullName).withStyle(ChatFormatting.ITALIC));
        com.hbm.api.data.ReactorDataMaps.RBMKFuel f = stats();
        double selfRate = f.selfRate(), yield = f.yield(), xGen = f.xenonGen(), xBurn = f.xenonBurn(), heat = f.heat(),
                diffusion = f.diffusion(), meltingPoint = f.meltingPoint();
        EnumBurnFunc function = f.function();
        NType nType = f.neutronIn(), rType = f.neutronOut();
        boolean rbmx = this == com.hbm.items.ModRBMKItems.rbmk_fuel_drx.get();
        String prefix = rbmx ? "trait.rbmx." : "trait.rbmk.";
        if (getHullHeat(stack) >= 50D || getCoreHeat(stack) >= 50D) {
            tooltip.add(Component.translatable("desc.item.wasteCooling").withStyle(ChatFormatting.GOLD));
        }
        if (selfRate > 0D || function == EnumBurnFunc.SIGMOID) {
            tooltip.add(Component.translatable(prefix + "source").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable(prefix + "depletion", ((int) (((yield - getYield(stack)) / yield) * 100000D)) / 1000D + "%")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(prefix + "xenon", ((int) (getPoison(stack) * 1000D) / 1000D) + "%")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable(prefix + "splitsWith", Component.translatable(nType.unlocalized + (rbmx ? ".x" : "")))
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable(prefix + "splitsInto", Component.translatable(rType.unlocalized + (rbmx ? ".x" : "")))
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable(prefix + "fluxFunc", Component.literal(getFuncDescription(stack)).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(prefix + "funcType", Component.translatable(function.title)).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(prefix + "xenonGen", Component.literal("x * " + xGen).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(prefix + "xenonBurn", Component.literal("x² / " + xBurn).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(prefix + "heat", heat + "°C").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(prefix + "diffusion", diffusion + "¹/²").withStyle(ChatFormatting.GOLD));
        String unit = rbmx ? "m" : "°C";
        tooltip.add(Component.translatable(prefix + "skinTemp", ((int) (getHullHeat(stack) * 10D) / 10D) + unit)
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(prefix + "coreTemp", ((int) (getCoreHeat(stack) * 10D) / 10D) + unit)
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable(prefix + "melt", meltingPoint + unit).withStyle(ChatFormatting.DARK_RED));
    }

    public enum NType {
        SLOW("trait.rbmk.neutron.slow"), FAST("trait.rbmk.neutron.fast"), ANY("trait.rbmk.neutron.any");
        public final String unlocalized;
        NType(String unlocalized) { this.unlocalized = unlocalized; }
    }

    /**
     * An RBMK rod's flux-to-reactivity curve (1.7 {@code EnumBurnFunc}), a registry since addons add curves (NTM: Space's
     * {@code SLOW_LINEAR}): {@link #register} at mod construction, then name it in {@code rbmk_fuel} JSON by its id. HBM's
     * ids are the 1.7 names.
     */
    public static final class EnumBurnFunc {
        /** {@code (flux after the depletion modifier, enrichment, reactivity, selfRate) -> outgoing flux}. */
        @FunctionalInterface public interface Curve { double apply(double flux, double enrichment, double reactivity, double selfRate); }

        private static final java.util.Map<String, EnumBurnFunc> BY_NAME = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>());

        public static final EnumBurnFunc PASSIVE = register("PASSIVE", "trait.rbmx.flux.passive", null, (x, e, r, s) -> s * e);
        public static final EnumBurnFunc LOG_TEN = register("LOG_TEN", "trait.rbmx.flux.logten", "log10(%1$s + 1) * 0.5 * %2$s", (x, e, r, s) -> Math.log10(x + 1D) * .5D * r);
        public static final EnumBurnFunc PLATEU = register("PLATEU", "trait.rbmx.flux.euler", "(1 - e^(-%1$s / 25)) * %2$s", (x, e, r, s) -> (1D - Math.pow(Math.E, -x / 25D)) * r);
        public static final EnumBurnFunc ARCH = register("ARCH", "trait.rbmx.flux.arch", "(%1$s - %1$s² / 10000) / 100 * %2$s [0;∞]", (x, e, r, s) -> Math.max((x - x * x / 10000D) / 100D * r, 0D));
        public static final EnumBurnFunc SIGMOID = register("SIGMOID", "trait.rbmx.flux.sigmoid", "%2$s / (1 + e^(-(%1$s - 50) / 10))", (x, e, r, s) -> r / (1D + Math.pow(Math.E, -(x - 50D) / 10D)));
        public static final EnumBurnFunc SQUARE_ROOT = register("SQUARE_ROOT", "trait.rbmx.flux.squrt", "sqrt(%1$s) * %2$s / 10", (x, e, r, s) -> Math.sqrt(x) * r / 10D);
        public static final EnumBurnFunc LINEAR = register("LINEAR", "trait.rbmx.flux.linear", "%1$s / 100 * %2$s", (x, e, r, s) -> x / 100D * r);
        public static final EnumBurnFunc QUADRATIC = register("QUADRATIC", "trait.rbmx.flux.quadratic", "%1$s² / 10000 * %2$s", (x, e, r, s) -> x * x / 10000D * r);
        public static final EnumBurnFunc EXPERIMENTAL = register("EXPERIMENTAL", "trait.rbmx.flux.experimental", "%1$s * (sin(%1$s) + 1) * %2$s", (x, e, r, s) -> x * (Math.sin(x) + 1D) * r);

        public static final com.mojang.serialization.Codec<EnumBurnFunc> CODEC = com.mojang.serialization.Codec.STRING.comapFlatMap(name -> {
            EnumBurnFunc f = BY_NAME.get(name);
            return f != null ? com.mojang.serialization.DataResult.success(f) : com.mojang.serialization.DataResult.error(() -> "Unknown RBMK burn function " + name);
        }, EnumBurnFunc::name);

        private final String name;
        /** Tooltip translation key of the curve's name. */
        public final String title;
        private final @org.jspecify.annotations.Nullable String format;
        private final Curve curve;

        private EnumBurnFunc(String name, String title, @org.jspecify.annotations.Nullable String format, Curve curve) {
            this.name = name; this.title = title; this.format = format; this.curve = curve;
        }

        /**
         * Registers a curve. {@code format} is its tooltip formula ({@code %1$s} the flux, {@code %2$s} the reactivity);
         * null shows the self rate, as 1.7's passive curve did.
         */
        public static EnumBurnFunc register(String name, String title, @org.jspecify.annotations.Nullable String format, Curve curve) {
            EnumBurnFunc f = new EnumBurnFunc(name, title, format, curve);
            if (BY_NAME.putIfAbsent(name, f) != null) throw new IllegalStateException("RBMK burn function " + name + " registered twice");
            return f;
        }

        public double apply(double flux, double enrichment, double reactivity, double selfRate) { return curve.apply(flux, enrichment, reactivity, selfRate); }
        public String description(double selfRate) { return format != null ? format : ChatFormatting.RED + "" + selfRate; }
        public String name() { return name; }
        @Override public String toString() { return name; }
        public static java.util.Collection<EnumBurnFunc> values() { synchronized (BY_NAME) { return java.util.List.copyOf(BY_NAME.values()); } }
    }

    /** How enrichment scales a rod's incoming flux (1.7 {@code EnumDepleteFunc}); a registry like {@link EnumBurnFunc}. */
    public static final class EnumDepleteFunc {
        private static final java.util.Map<String, EnumDepleteFunc> BY_NAME = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<>());

        public static final EnumDepleteFunc LINEAR = register("LINEAR", e -> e);
        public static final EnumDepleteFunc RAISING_SLOPE = register("RAISING_SLOPE", e -> e + Math.sin(e * Math.PI) / 2D);
        public static final EnumDepleteFunc BOOSTED_SLOPE = register("BOOSTED_SLOPE", e -> e + Math.sin((e - 1D) * (e - 1D) * Math.PI));
        public static final EnumDepleteFunc GENTLE_SLOPE = register("GENTLE_SLOPE", e -> e + Math.sin(e * Math.PI) / 3D);
        public static final EnumDepleteFunc STATIC = register("STATIC", e -> 1D);

        public static final com.mojang.serialization.Codec<EnumDepleteFunc> CODEC = com.mojang.serialization.Codec.STRING.comapFlatMap(name -> {
            EnumDepleteFunc f = BY_NAME.get(name);
            return f != null ? com.mojang.serialization.DataResult.success(f) : com.mojang.serialization.DataResult.error(() -> "Unknown RBMK depletion function " + name);
        }, EnumDepleteFunc::name);

        private final String name;
        private final java.util.function.DoubleUnaryOperator mod;

        private EnumDepleteFunc(String name, java.util.function.DoubleUnaryOperator mod) { this.name = name; this.mod = mod; }

        /** Registers a depletion modifier ({@code enrichment -> flux multiplier}). */
        public static EnumDepleteFunc register(String name, java.util.function.DoubleUnaryOperator mod) {
            EnumDepleteFunc f = new EnumDepleteFunc(name, mod);
            if (BY_NAME.putIfAbsent(name, f) != null) throw new IllegalStateException("RBMK depletion function " + name + " registered twice");
            return f;
        }

        public double apply(double enrichment) { return mod.applyAsDouble(enrichment); }
        public String name() { return name; }
        @Override public String toString() { return name; }
    }
}
