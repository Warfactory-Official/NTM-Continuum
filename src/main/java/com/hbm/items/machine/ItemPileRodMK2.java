package com.hbm.items.machine;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.items.ItemEnumMulti;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatTooltipContext;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * The Chicago Pile's fuel and source rods. A rod carries its own burn-up, so depletion lives on the
 * stack rather than on the channel: the pile advances it while the rod sits in a fuel channel, and once
 * it passes the rod's lifetime the rod becomes whatever it breeds into.
 */
public class ItemPileRodMK2 extends ItemEnumMulti {

    public ItemPileRodMK2(Properties properties, EnumPileRod type) {
        super(EnumPileRod.class, type, properties);
    }

    public enum EnumPileRod {
        RA226BE(1D),
        PO210BE(1D),
        ZR(0D, 0D, 0D, 2),
        NU(1D, 25_000D, 0.25D, 4),
        PU239(1D, 500D, 0.5D, 5),
        RGP(1D, 1_000D, 0.5D, 6),
        WASTE(1D, 0D, 1.5D, 6),
        THORIUM(1D, 35_000D, 0.25D, 8),
        THORIUM_FUEL(1D, 2_000D, 0.5D, 6);

        public static final EnumPileRod[] VALUES = values();

        public double reactionMult;
        public double life;
        public double heatMult;
        public double neutronSource;
        public int turnsInto;

        EnumPileRod(double neutronSource) {
            this.neutronSource = neutronSource;
            this.reactionMult = 0D;
            this.life = 0D;
            this.heatMult = 0D;
        }

        EnumPileRod(double reaction, double life, double heat, int turnsInto) {
            this.reactionMult = reaction;
            this.life = life;
            this.heatMult = heat;
            this.turnsInto = turnsInto;
        }
    }

    /**
     * The rod values of the stack: the {@code hbm:pile_rod} item data map, else HBM's enum constant; null when the stack is
     * not a pile rod. An addon's rod is any {@code ItemPileRodMK2} with an entry.
     */
    public static ReactorDataMaps.@org.jspecify.annotations.Nullable PileRod stats(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPileRodMK2 item)) return null;
        ReactorDataMaps.PileRod data = ReactorDataMaps.PILE_ROD.get(stack.getItem());
        if (data != null) return data;
        return codeStats(stack);
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public static ReactorDataMaps.@org.jspecify.annotations.Nullable PileRod codeStats(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPileRodMK2 item)) return null;
        EnumPileRod rod = typeOf(stack);
        Optional<String> into = rod.life <= 0 ? Optional.empty() : Optional.of(
                BuiltInRegistries.ITEM.getKey(item.of(EnumPileRod.VALUES[rod.turnsInto]).getItem()).toString());
        return new ReactorDataMaps.PileRod(rod.reactionMult, rod.life, rod.heatMult, rod.neutronSource, into);
    }

    public static EnumPileRod typeOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPileRodMK2 rod)) return null;
        return (EnumPileRod) rod.getType(stack);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        EnumPileRod rod = typeOf(stack);
        var stats = stats(stack);
        if (rod == null || stats == null) return;

        if (stats.life() > 0) {
            tooltip.add(Component.literal("Lifetime: " + (int) Math.round(stats.life())));
            double depletion = getDepletionPercent(stack);
            if (depletion > 0) tooltip.add(Component.literal("Depletion: " + (int) Math.round(depletion) + "%"));
        }

        tooltip.add(Component.translatable("item.hbm.pile_rod." + rod.name().toLowerCase(Locale.US) + ".desc")
                .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDepletionPercent(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return 13 - (int) Math.round(getDepletionPercent(stack) * 13D / 100D);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00;
    }

    public static double getDepletionPercent(ItemStack stack) {
        var rod = stats(stack);
        if (rod == null || rod.life() <= 0) return 0D;
        return (getDepletion(stack) / rod.life()) * 100D;
    }

    public static double getDepletion(ItemStack stack) {
        return ModDataComponents.PILE_ROD_DEPLETION.getOr(stack, 0D);
    }

    public static void setDepletion(ItemStack stack, double depletion) {
        ModDataComponents.PILE_ROD_DEPLETION.set(stack, depletion);
    }

    public static void clearDepletion(ItemStack stack) {
        ModDataComponents.PILE_ROD_DEPLETION.remove(stack);
    }

    /** How many neutrons this rod emits at the given incoming flux: its source term plus its fission. */
    public static double getReactivity(ItemStack stack, double inFlux) {
        var rod = stats(stack);
        if (rod == null) return 0D;
        double outFlux = rod.source();
        if (rod.reaction() > 0) outFlux += squirt(inFlux) * rod.reaction();
        return outFlux;
    }

    public static double getHeatPerNeutron(ItemStack stack) {
        var rod = stats(stack);
        return rod == null ? 0D : rod.heat();
    }

    /** Advances the rod's burn-up, returning the rod it breeds into once it is spent. */
    public static ItemStack react(ItemStack stack, double inFlux) {
        var rod = stats(stack);
        if (rod == null || rod.life() <= 0) return stack;

        double depletion = getDepletion(stack) + inFlux;
        if (depletion < rod.life()) {
            setDepletion(stack, depletion);
            return stack;
        }
        return rod.turnsInto().map(id -> new ItemStack(ReactorDataMaps.itemOf(id))).orElse(stack);
    }

    /** Upstream's {@code BobMathUtil.squirt}: a square root that stays finite and smooth through zero. */
    private static double squirt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }
}
