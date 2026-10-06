package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnumMulti;
import com.hbm.items.ModSatelliteItems;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code com.hbm.items.special.ItemSatellite}: the launchable satellite, one item per kind (1.7's metadata
 * {@link EnumSatType#ordinal()} is the member's constant), carrying the frequency it will answer on.
 */
public class ItemSatellite extends ItemEnumMulti implements ISatChip {

    public ItemSatellite(Properties properties, EnumSatType type) {
        super(EnumSatType.class, type, properties);
    }

    /** 1.7 {@code ItemSatellite.EnumSatType}, in 1.7 order; the ordinal is the item's metadata. */
    public enum EnumSatType {
        SPY,
        SCANNER,
        RADAR,
        MINER_ASTRO,
        MINER_LUNAR,
        PRECISION_LASER,
        DEATH_RAY,
        XENIUM_RESONATOR,
        RELAY,
        DETECTOR,
        RAY_SCAN;

        public static final EnumSatType[] VALUES = values();
    }

    public @Nullable EnumSatType typeOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSatellite)) return null;
        return (EnumSatType) getType(stack);
    }

    public static ItemStack make(EnumSatType type) {
        return ModSatelliteItems.satellite.stack(type);
    }

    /** 1.7: the frequency and nothing else. */
    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(Language.getInstance().getOrDefault("satchip.frequency") + ": " + getFreq(stack)));
    }
}
