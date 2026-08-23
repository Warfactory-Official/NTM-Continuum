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
    public static RBMKFuelData getData(ItemStack stack) {
        RBMKFuelData data = ModDataComponents.RBMK_FUEL.get(stack).orElse(null);
        return data != null ? data : new RBMKFuelData(stack.getItem() instanceof ItemRBMKRod rod ? rod.yield : 0D, 0D, 20D, 20D);
    }
    public static double getYield(ItemStack s) { return getData(s).yield(); }
    public static void setYield(ItemStack s, double v) { ModDataComponents.RBMK_FUEL.set(s, getData(s).withYield(v)); }
    public static double getPoison(ItemStack s) { return getData(s).xenon(); }
    public static void setPoison(ItemStack s, double v) { ModDataComponents.RBMK_FUEL.set(s, getData(s).withXenon(v)); }
    public static double getCoreHeat(ItemStack s) { return getData(s).coreHeat(); }
    public static void setCoreHeat(ItemStack s, double v) { ModDataComponents.RBMK_FUEL.set(s, getData(s).withCoreHeat(v)); }
    public static double getHullHeat(ItemStack s) { return getData(s).hullHeat(); }
    public static void setHullHeat(ItemStack s, double v) { ModDataComponents.RBMK_FUEL.set(s, getData(s).withHullHeat(v)); }
    public static double getEnrichment(ItemStack s) { return getYield(s) / ((ItemRBMKRod) s.getItem()).yield; }
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
        inFlux += selfRate;
        if (RBMKDials.getXenon(world)) {
            double xenon = getPoison(stack) - xenonBurnFunc(inFlux);
            inFlux *= 1D - getPoisonLevel(stack);
            xenon += xenonGenFunc(inFlux);
            setPoison(stack, Mth.clamp(xenon, 0D, 100D));
        }
        double mult = 1D;
        double coreHeat = getCoreHeat(stack);
        if (heatCoeffStart != 0D && coreHeat >= heatCoeffStart) {
            double progress = Math.min((coreHeat - heatCoeffStart) / heatCoeffLength, 1D);
            mult = Math.sin((progress * Math.PI + Math.PI) / 2D);
        }
        double outFlux = reactivityFunc(inFlux, getEnrichment(stack) * mult) * RBMKDials.getReactivityMod(world);
        if (RBMKDials.getDepletion(world)) setYield(stack, Math.max(0D, getYield(stack) - inFlux));
        setCoreHeat(stack, rectify(coreHeat + outFlux * heat));
        return outFlux;
    }
    public void updateHeat(Level world, ItemStack stack, double mod) {
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
        if (hull > meltingPoint) { double avg = (heat + hull + getCoreHeat(stack)) / 3D; setCoreHeat(stack, avg); setHullHeat(stack, avg); return avg - heat; }
        if (hull <= heat) return 0D;
        double out = (hull - heat) / 2D * RBMKDials.getFuelHeatProvision(world) * mod;
        setHullHeat(stack, hull - out); return out;
    }
    public double reactivityFunc(double in, double enrichment) {
        double flux = in * reactivityModByEnrichment(enrichment);
        return switch (function) {
            case PASSIVE -> selfRate * enrichment;
            case LOG_TEN -> Math.log10(flux + 1D) * .5D * reactivity;
            case PLATEU -> (1D - Math.pow(Math.E, -flux / 25D)) * reactivity;
            case ARCH -> Math.max((flux - flux * flux / 10000D) / 100D * reactivity, 0D);
            case SIGMOID -> reactivity / (1D + Math.pow(Math.E, -(flux - 50D) / 10D));
            case SQUARE_ROOT -> Math.sqrt(flux) * reactivity / 10D;
            case LINEAR -> flux / 100D * reactivity;
            case QUADRATIC -> flux * flux / 10000D * reactivity;
            case EXPERIMENTAL -> flux * (Math.sin(flux) + 1D) * reactivity;
        };
    }
    public double reactivityModByEnrichment(double e) { return switch (depFunc) {
        case STATIC -> 1D; case BOOSTED_SLOPE -> e + Math.sin((e - 1D) * (e - 1D) * Math.PI);
        case RAISING_SLOPE -> e + Math.sin(e * Math.PI) / 2D; case GENTLE_SLOPE -> e + Math.sin(e * Math.PI) / 3D; default -> e;
    }; }
    public double xenonGenFunc(double flux) { return flux * xGen; }
    public double xenonBurnFunc(double flux) { return flux * flux / xBurn; }
    private static double rectify(double value) { return value > 1_000_000D ? 1_000_000D : value < 20D || Double.isNaN(value) ? 20D : value; }
    @Override public boolean isBarVisible(ItemStack s) { return getEnrichment(s) < 1D; }
    @Override public int getBarWidth(ItemStack s) { return (int) Math.round(13D * Mth.clamp(getEnrichment(s), 0D, 1D)); }
    @Override public int getBarColor(ItemStack s) { return 0x33FF33; }

    // The legacy tooltip displays the actual reaction curve, including its current enrichment modifier.
    public String getFuncDescription(ItemStack stack) {
        String function = switch (this.function) {
            case PASSIVE -> ChatFormatting.RED + "" + selfRate;
            case LOG_TEN -> "log10(%1$s + 1) * 0.5 * %2$s";
            case PLATEU -> "(1 - e^(-%1$s / 25)) * %2$s";
            case ARCH -> "(%1$s - %1$s² / 10000) / 100 * %2$s [0;∞]";
            case SIGMOID -> "%2$s / (1 + e^(-(%1$s - 50) / 10))";
            case SQUARE_ROOT -> "sqrt(%1$s) * %2$s / 10";
            case LINEAR -> "%1$s / 100 * %2$s";
            case QUADRATIC -> "%1$s² / 10000 * %2$s";
            case EXPERIMENTAL -> "%1$s * (sin(%1$s) + 1) * %2$s";
        };
        double enrichment = getEnrichment(stack);
        if (enrichment < 1D) {
            enrichment = reactivityModByEnrichment(enrichment);
            String reactivity = ChatFormatting.YELLOW + "" + ((int) (this.reactivity * enrichment * 1000D) / 1000D)
                    + ChatFormatting.WHITE;
            String enrichmentPercent = ChatFormatting.GOLD + " (" + ((int) (enrichment * 1000D) / 10D) + "%)";
            return String.format(function, selfRate > 0D ? "(x" + ChatFormatting.RED + " + " + selfRate
                    + ChatFormatting.WHITE + ")" : "x", reactivity).concat(enrichmentPercent);
        }
        return String.format(function, selfRate > 0D ? "(x" + ChatFormatting.RED + " + " + selfRate
                + ChatFormatting.WHITE + ")" : "x", reactivity);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal(fullName).withStyle(ChatFormatting.ITALIC));
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
    public enum EnumBurnFunc {
        PASSIVE("trait.rbmx.flux.passive"), LOG_TEN("trait.rbmx.flux.logten"), PLATEU("trait.rbmx.flux.euler"),
        ARCH("trait.rbmx.flux.arch"), SIGMOID("trait.rbmx.flux.sigmoid"), SQUARE_ROOT("trait.rbmx.flux.squrt"),
        LINEAR("trait.rbmx.flux.linear"), QUADRATIC("trait.rbmx.flux.quadratic"),
        EXPERIMENTAL("trait.rbmx.flux.experimental");
        public final String title;
        EnumBurnFunc(String title) { this.title = title; }
    }
    public enum EnumDepleteFunc { LINEAR, RAISING_SLOPE, BOOSTED_SLOPE, GENTLE_SLOPE, STATIC }
}
