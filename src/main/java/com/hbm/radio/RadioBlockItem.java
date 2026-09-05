package com.hbm.radio;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Block item that exposes the old dollar-separated standard-info text as contiguous translation lines. */
public final class RadioBlockItem extends CompatBlockItem {
    public RadioBlockItem(Block block, Properties properties) { super(block, properties); }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        String prefix = getDescriptionId() + ".desc.";
        Language language = Language.getInstance();
        for (int i = 0; language.has(prefix + i); i++) tooltip.add(Component.translatable(prefix + i));
    }
}
