package com.hbm.items.bomb;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** 1.7 {@code ItemPrototypeBlock}: the Prototype's three hard-coded tooltip lines. */
public class ItemPrototypeBlock extends CompatBlockItem {

    public ItemPrototypeBlock(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("It didn't have to be like this."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("You monster."));
    }
}
