package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Block item exposing the 1.7 dollar-separated {@code tile.<id>.desc} standard-info text as {@code block.hbm.<id>.desc.N} lines. */
public final class ConveyorBlockItem extends CompatBlockItem {

    public ConveyorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String prefix = getDescriptionId() + ".desc.";
        Language language = Language.getInstance();
        for (int i = 0; language.has(prefix + i); i++) tooltip.add(Component.translatable(prefix + i));
    }
}
