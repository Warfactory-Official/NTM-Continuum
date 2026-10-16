package com.hbm.blocks;

import dev.golem.core.world.BlockTooltip;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** 1.7 {@code ITooltipProvider}: by default the block's item shows {@code addStandardInfo} for its own id. */
public interface ITooltipProvider extends BlockTooltip {

    @Override
    default void appendBlockTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        StandardInfo.add(tooltip, ((Block) this).getDescriptionId() + ".desc");
    }
}
