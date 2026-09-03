package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The warning tooltip carried by the old taint block item. */
public class ItemBlockTaint extends CompatBlockItem {

    public ItemBlockTaint(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("DO NOT TOUCH, BREATHE OR STARE AT.").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
