package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/** One flattened legacy drillbit metadata variant. */
public class ItemDrillbit extends CompatItem {

    public final DrillBitType type;

    /** This bit's values: its {@code hbm:drill_bit} entry, else HBM's enum constant. */
    public com.hbm.api.data.MachinePartDataMaps.DrillBit stats() {
        com.hbm.api.data.MachinePartDataMaps.DrillBit data = com.hbm.api.data.MachinePartDataMaps.DRILL_BIT.get(this);
        return data != null ? data : codeStats();
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public com.hbm.api.data.MachinePartDataMaps.DrillBit codeStats() {
        return new com.hbm.api.data.MachinePartDataMaps.DrillBit(this.type.speed, this.type.tier, this.type.fortune, this.type.vein, this.type.silk);
    }

    public ItemDrillbit(Properties properties, DrillBitType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatableWithFallback(
                "item.hbm.drillbit_" + type.name().toLowerCase(Locale.ROOT), type.displayName);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        var type = stats();
        tooltip.add(Component.translatableWithFallback("desc.speed", "Speed:")
                .append(" " + (int) (type.speed() * 100D) + "%").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatableWithFallback("desc.tier", "Tier: %s", type.tier())
                .withStyle(ChatFormatting.YELLOW));
        if (type.fortune() > 0) {
            tooltip.add(Component.translatableWithFallback("desc.fortune", "Fortune:")
                    .append(" " + type.fortune()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (type.vein()) tooltip.add(Component.translatableWithFallback("desc.veinminer", "Vein Miner")
                .withStyle(ChatFormatting.GREEN));
        if (type.silk()) tooltip.add(Component.translatableWithFallback("desc.silktouch", "Silk Touch")
                .withStyle(ChatFormatting.GREEN));
    }
}
