package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * A block item with 1.7 {@code ITooltipProvider.addStandardInfo}'s description lines
 * ({@code block.hbm.<id>.desc.N}, yellow). 1.7 hid them behind a held shift, which only the client can read;
 * like the autosaw's item they are always shown here.
 */
public class DescBlockItem extends CompatBlockItem {

    private final String key;
    private final int lines;

    public DescBlockItem(Block block, Properties properties, String id, int lines) {
        super(block, properties);
        this.key = "block.hbm." + id + ".desc.";
        this.lines = lines;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        for (int i = 0; i < lines; i++) {
            tooltip.add(Component.translatable(key + i).withStyle(ChatFormatting.YELLOW));
        }
    }
}
