package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** 1.7 {@code ITooltipProvider.addStandardInfo}: the block's {@code desc} lines, one per {@code $} split, in yellow. */
public class ItemBlockProductionDesc extends CompatBlockItem {

    private final int lines;

    public ItemBlockProductionDesc(Block block, Properties properties, int lines) {
        super(block, properties);
        this.lines = lines;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(getBlock()).getPath();
        for (int i = 0; i < lines; i++) {
            tooltip.add(Component.translatable("block.hbm." + id + ".desc." + i).withStyle(ChatFormatting.YELLOW));
        }
    }
}
