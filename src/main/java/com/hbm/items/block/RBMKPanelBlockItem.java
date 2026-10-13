package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 1.7 {@code ITooltipProvider.addStandardInfo} for the RBMK console peripherals, whose descriptions are
 * single {@code tile.<id>.desc} keys with {@code $} line breaks: one yellow line per segment. Like the
 * other ported description items it shows the lines without the old held-shift gate.
 */
public class RBMKPanelBlockItem extends CompatBlockItem {

    private final String key;

    public RBMKPanelBlockItem(Block block, Properties properties, String id) {
        super(block, properties);
        this.key = "tile." + id + ".desc";
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        String text = Component.translatable(key).getString();
        if (text.equals(key)) return;
        for (String line : text.split(Pattern.quote("$"))) tooltip.add(Component.literal(line).withStyle(ChatFormatting.YELLOW));
    }
}
