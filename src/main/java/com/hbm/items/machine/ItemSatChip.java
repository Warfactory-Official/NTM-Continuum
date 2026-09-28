package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 1.7 {@code ItemSatChip}: an item that carries a satellite frequency. The legacy chips, Gerald, the ID chip,
 * the relay chip, and (as subclasses) the satellite designator and the laser designator.
 */
public class ItemSatChip extends CompatItem implements ISatChip {

    public ItemSatChip(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal(Language.getInstance().getOrDefault("satchip.frequency") + ": " + getFreq(stack)));

        Item item = stack.getItem();
        if (item == ModSatelliteItems.sat_foeq.get()) list.add(Component.translatable("satchip.foeq"));

        if (item == ModSatelliteItems.sat_gerald.get()) {
            // 1.7 I18nUtil.resolveKeyArray: one translation split on '$'
            for (String line : Language.getInstance().getOrDefault("satchip.gerald.desc").split("\\$")) list.add(Component.literal(line));
        }

        if (item == ModSatelliteItems.sat_laser.get()) list.add(Component.translatable("satchip.laser"));
        if (item == ModSatelliteItems.sat_mapper.get()) list.add(Component.translatable("satchip.mapper"));
        if (item == ModSatelliteItems.sat_miner.get()) list.add(Component.translatable("satchip.miner"));
        if (item == ModSatelliteItems.sat_lunar_miner.get()) list.add(Component.translatable("satchip.lunar_miner"));
        if (item == ModSatelliteItems.sat_radar.get()) list.add(Component.translatable("satchip.radar"));
        if (item == ModSatelliteItems.sat_resonator.get()) list.add(Component.translatable("satchip.resonator"));
        if (item == ModSatelliteItems.sat_scanner.get()) list.add(Component.translatable("satchip.scanner"));
    }
}
