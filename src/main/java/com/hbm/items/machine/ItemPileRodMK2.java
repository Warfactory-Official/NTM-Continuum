package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Chicago Pile's fuel and source rods. A rod carries its own burn-up, so depletion lives on the
 * stack rather than on the channel: the pile advances it while the rod sits in a fuel channel, and once
 * it passes the rod's lifetime the rod becomes whatever it breeds into.
 */
public class ItemPileRodMK2 extends dev.golem.core.world.CompatItem {

    public ItemPileRodMK2(Properties properties) {
        super(properties);
    }

    /** Persisted by name ({@code PILE_ROD}); {@link #ordinal()} = model index ({@code ITEM_VARIANT}), built-ins first. */
    public static final class EnumPileRod {

        private static final List<EnumPileRod> VALUES = new ArrayList<>();
        private static final List<EnumPileRod> VIEW = Collections.unmodifiableList(VALUES);

        static {
            dev.golem.core.content.SyncedIndex.register("hbm:pile_rods", () -> VALUES.stream().map(EnumPileRod::name).toList());
        }

        public static final EnumPileRod RA226BE = source("RA226BE", 1D, "");
        public static final EnumPileRod PO210BE = source("PO210BE", 1D, "_po210be");
        public static final EnumPileRod ZR = fuel("ZR", 0D, 0D, 0D, "ZR", "_zr");
        public static final EnumPileRod NU = fuel("NU", 1D, 25_000D, 0.25D, "PU239", "_nu");
        public static final EnumPileRod PU239 = fuel("PU239", 1D, 500D, 0.5D, "RGP", "_mk2_pu239");
        public static final EnumPileRod RGP = fuel("RGP", 1D, 1_000D, 0.5D, "WASTE", "_rgp");
        public static final EnumPileRod WASTE = fuel("WASTE", 1D, 0D, 1.5D, "WASTE", "_waste");
        public static final EnumPileRod THORIUM = fuel("THORIUM", 1D, 35_000D, 0.25D, "THORIUM_FUEL", "_thorium");
        public static final EnumPileRod THORIUM_FUEL = fuel("THORIUM_FUEL", 1D, 2_000D, 0.5D, "WASTE", "_thorium_fuel");

        private final String name;
        private final int ordinal;
        public final String model;
        public final boolean described;
        public final double reactionMult;
        public final double life;
        public final double heatMult;
        public final double neutronSource;
        private final @Nullable String turnsIntoName;

        private EnumPileRod(String name, double neutronSource, double reaction, double life, double heat,
                            @Nullable String turnsInto, String model, boolean described) {
            this.name = name;
            this.ordinal = VALUES.size();
            this.neutronSource = neutronSource;
            this.reactionMult = reaction;
            this.life = life;
            this.heatMult = heat;
            this.turnsIntoName = turnsInto;
            this.model = model;
            this.described = described;
        }

        private static EnumPileRod source(String name, double neutronSource, String suffix) {
            return create(name, neutronSource, 0D, 0D, 0D, null, "hbm:item/pile_rod" + suffix, true);
        }

        private static EnumPileRod fuel(String name, double reaction, double life, double heat, String turnsInto, String suffix) {
            return create(name, 0D, reaction, life, heat, turnsInto, "hbm:item/pile_rod" + suffix, true);
        }

        /** {@code turnsInto} resolves by name on first spend, so it may name a type declared later. */
        public static EnumPileRod create(String name, double neutronSource, double reaction, double life, double heat,
                                         @Nullable String turnsInto, String model, boolean described) {
            if (byName(name) != null) throw new IllegalArgumentException("Duplicate pile rod " + name);
            EnumPileRod rod = new EnumPileRod(name.toUpperCase(Locale.ROOT), neutronSource, reaction, life, heat,
                    turnsInto == null ? null : turnsInto.toUpperCase(Locale.ROOT), model, described);
            VALUES.add(rod);
            return rod;
        }

        public static List<EnumPileRod> values() {
            return VIEW;
        }

        public static @Nullable EnumPileRod byName(String name) {
            String key = name.toUpperCase(Locale.ROOT);
            for (EnumPileRod rod : VALUES) if (rod.name.equals(key)) return rod;
            return null;
        }

        public static @Nullable EnumPileRod byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.size() ? VALUES.get(ordinal) : null;
        }

        public String name() {
            return name;
        }

        public String id() {
            return name.toLowerCase(Locale.ROOT);
        }

        public int ordinal() {
            return ordinal;
        }

        public EnumPileRod turnsInto() {
            EnumPileRod target = turnsIntoName == null ? null : byName(turnsIntoName);
            if (target == null) throw new IllegalStateException("Pile rod " + name + " turns into unknown " + turnsIntoName);
            return target;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public ItemStack of(EnumPileRod type, int count) {
        ItemStack stack = new ItemStack(this, count);
        ModDataComponents.PILE_ROD.set(stack, type.name());
        ModDataComponents.ITEM_VARIANT.set(stack, type.ordinal());
        return stack;
    }

    public ItemStack of(EnumPileRod type) {
        return of(type, 1);
    }

    @Override
    public Component getName(ItemStack stack) {
        EnumPileRod rod = typeOf(stack);
        return rod == null ? super.getName(stack) : Component.translatable(getDescriptionId() + "." + rod.id());
    }

    public static EnumPileRod typeOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPileRodMK2)) return null;
        return EnumPileRod.byName(ModDataComponents.PILE_ROD.getOr(stack, EnumPileRod.RA226BE.name()));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        EnumPileRod rod = typeOf(stack);
        if (rod == null) return;

        if (rod.life > 0) {
            tooltip.add(Component.literal("Lifetime: " + (int) Math.round(rod.life)));
            double depletion = getDepletionPercent(stack);
            if (depletion > 0) tooltip.add(Component.literal("Depletion: " + (int) Math.round(depletion) + "%"));
        }

        if (rod.described) tooltip.add(Component.translatable(getDescriptionId() + "." + rod.id() + ".desc")
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
        EnumPileRod rod = typeOf(stack);
        if (rod == null || rod.life <= 0) return 0D;
        return (getDepletion(stack) / rod.life) * 100D;
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
        EnumPileRod rod = typeOf(stack);
        if (rod == null) return 0D;
        double outFlux = rod.neutronSource;
        if (rod.reactionMult > 0) outFlux += squirt(inFlux) * rod.reactionMult;
        return outFlux;
    }

    public static double getHeatPerNeutron(ItemStack stack) {
        EnumPileRod rod = typeOf(stack);
        return rod == null ? 0D : rod.heatMult;
    }

    /** Advances the rod's burn-up, returning the rod it breeds into once it is spent. */
    public static ItemStack react(ItemStack stack, double inFlux) {
        EnumPileRod rod = typeOf(stack);
        if (rod == null || rod.life <= 0) return stack;

        double depletion = getDepletion(stack) + inFlux;
        if (depletion < rod.life) {
            setDepletion(stack, depletion);
            return stack;
        }
        return ((ItemPileRodMK2) stack.getItem()).of(rod.turnsInto());
    }

    /** Upstream's {@code BobMathUtil.squirt}: a square root that stays finite and smooth through zero. */
    private static double squirt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }
}
