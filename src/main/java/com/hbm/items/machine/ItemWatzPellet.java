package com.hbm.items.machine;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModWatzItems;
import dev.golem.core.world.CompatItem;
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
 * Watz Isotropic Fuel, Oxidized: one pellet type of the Watz power plant, either fresh or depleted.
 *
 * <p>1.12.2 packed all twelve types into one item via metadata and recoloured the sprite at runtime;
 * 1.21+ has no metadata, so the port registers <b>one item per type × freshness</b> with a pre-baked
 * texture, the same shape {@code ItemAutogen} uses. Remaining yield rides
 * {@link ModDataComponents#WATZ_YIELD} instead of the old NBT {@code "yield"} double.
 */
public class ItemWatzPellet extends CompatItem {

    public final EnumWatzType type;
    public final boolean depleted;

    public ItemWatzPellet(Properties properties, EnumWatzType type, boolean depleted) {
        super(properties);
        this.type = type;
        this.depleted = depleted;
    }

    /**
     * The values of the pellet type {@code item} belongs to (fresh or depleted): the {@code hbm:watz_pellet} entry of the
     * fresh pellet, else HBM's enum constant. An addon's pellet is its own {@code ItemWatzPellet} pair with an entry.
     */
    public static ReactorDataMaps.@org.jspecify.annotations.Nullable WatzPellet stats(net.minecraft.world.item.Item item) {
        net.minecraft.world.item.Item fresh = freshOf(item);
        if (fresh == null) return null;
        ReactorDataMaps.WatzPellet data = ReactorDataMaps.WATZ_PELLET.get(fresh);
        if (data != null) return data;
        return codeStats(item);
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public static ReactorDataMaps.@org.jspecify.annotations.Nullable WatzPellet codeStats(net.minecraft.world.item.Item item) {
        net.minecraft.world.item.Item fresh = freshOf(item);
        if (fresh == null) return null;
        EnumWatzType t = ((ItemWatzPellet) fresh).type;
        return new ReactorDataMaps.WatzPellet(t.colorLight, t.colorDark, t.passive, t.heatEmission, t.mudContent,
                Optional.ofNullable(t.burnFunc), Optional.ofNullable(t.heatDiv), Optional.ofNullable(t.absorbFunc),
                t.yield, BuiltInRegistries.ITEM.getKey(ModWatzItems.depleted(t)).toString());
    }

    /** The fresh pellet of {@code item}'s type: itself when fresh, the pellet whose entry names it as depleted, else HBM's pair. */
    public static net.minecraft.world.item.@org.jspecify.annotations.Nullable Item freshOf(net.minecraft.world.item.Item item) {
        if (!(item instanceof ItemWatzPellet pellet)) return null;
        if (!pellet.depleted) return item;
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        for (var entry : ReactorDataMaps.WATZ_PELLET.byId().entrySet()) {
            if (entry.getValue().depleted().equals(id)) return ReactorDataMaps.itemOf(entry.getKey());
        }
        return ModWatzItems.fresh(pellet.type);
    }

    private double yieldOf() {
        var stats = stats(this);
        return stats == null ? type.yield : stats.yield();
    }

    /** A full-yield pellet of this type. */
    public ItemStack make() {
        ItemStack stack = new ItemStack(this);
        setYield(stack, yieldOf());
        return stack;
    }

    public static double getYield(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemWatzPellet pellet)) return 0D;
        // Absence means "untouched", so a freshly crafted pellet needs no component to be full.
        return ModDataComponents.WATZ_YIELD.getOr(stack, pellet.yieldOf());
    }

    public static void setYield(ItemStack stack, double yield) {
        ModDataComponents.WATZ_YIELD.set(stack, yield);
    }

    /** Remaining fraction of the pellet's original yield, 1 = fresh. */
    public static double getEnrichment(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemWatzPellet pellet)) return 0D;
        return getYield(stack) / pellet.yieldOf();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !depleted && getEnrichment(stack) < 1D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(0, Math.round((float) getEnrichment(stack) * 13F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        var stats = stats(this);
        return stats == null ? type.colorLight : stats.color();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (depleted) return;
        var type = stats(this);
        if (type == null) return;

        tooltip.add(Component.literal("Depletion: "
                + String.format(Locale.US, "%.1f", (1D - getEnrichment(stack)) * 100D) + "%")
                .withStyle(ChatFormatting.GREEN));

        if (type.passive() > 0) {
            tooltip.add(gold("Base fission rate: ", String.valueOf(type.passive())));
            tooltip.add(Component.literal("Self-igniting!").withStyle(ChatFormatting.RED));
        }
        if (type.heat() > 0) tooltip.add(gold("Heat per flux: ", type.heat() + " TU"));
        if (type.burn().isPresent()) {
            tooltip.add(gold("Reaction function: ", type.burn().get().getLabelForFuel()));
            tooltip.add(gold("Fuel type: ", type.burn().get().getDangerFromFuel()));
        }
        if (type.heatDiv().isPresent()) tooltip.add(gold("Thermal multiplier: ", type.heatDiv().get().getLabelForFuel() + " TU\u207b\u00b9"));
        if (type.absorb().isPresent()) tooltip.add(gold("Flux capture: ", type.absorb().get().getLabelForFuel()));
    }

    /** A gold label and a white value: 1.7's {@code GOLD + label + RESET + value}, where RESET means white in a tooltip. */
    private static Component gold(String label, String value) {
        return Component.literal(label).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }
}
