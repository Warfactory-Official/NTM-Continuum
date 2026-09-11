package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnumMulti;
import com.hbm.items.ModSatelliteItems;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The launchable satellite, one variant per kind, carrying the frequency it will answer on. */
public class ItemSatellite extends ItemEnumMulti implements ISatChip {

    public ItemSatellite(Properties properties) {
        super(EnumSatType.class, true, true, properties);
    }

    public enum EnumSatType {
        SPY("satchip.mapper"),
        SCANNER("satchip.scanner"),
        RADAR("satchip.radar"),
        MINER_ASTRO("satchip.miner"),
        MINER_LUNAR("satchip.lunar_miner"),
        PRECISION_LASER("satchip.precision_laser"),
        DEATH_RAY("satchip.laser"),
        XENIUM_RESONATOR("satchip.resonator"),
        RELAY("satchip.foeq"),
        DETECTOR("satchip.detector"),
        RAY_SCAN("satchip.ray_scanner"),
        SCIENCE("satchip.science"),
        SCIENCE_ASSEMBLER("satchip.science_assembler"),
        SCIENCE_SENSOR("satchip.science_sensor");

        public static final EnumSatType[] VALUES = values();

        public final String descKey;

        EnumSatType(String descKey) {
            this.descKey = descKey;
        }
    }

    public @Nullable EnumSatType typeOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemSatellite)) return null;
        return (EnumSatType) getType(stack);
    }

    public static ItemStack make(EnumSatType type) {
        return ModSatelliteItems.satellite.get().of(type);
    }

    /** The name key a satellite reports as, so a sat link can print what it is talking to. */
    public static String nameKey(EnumSatType type) {
        return ModSatelliteItems.satellite.get().getDescriptionId()
                + "." + type.name().toLowerCase(java.util.Locale.US);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        EnumSatType type = typeOf(stack);
        if (type == null) return;

        tooltip.add(Component.translatable("satchip.frequency")
                .append(Component.literal(": " + getFreq(stack))).withStyle(ChatFormatting.AQUA));

        String base = type.descKey + ".";
        Language language = Language.getInstance();
        for (int i = 0; language.has(base + i); i++) {
            tooltip.add(Component.translatable(base + i));
        }
    }
}
