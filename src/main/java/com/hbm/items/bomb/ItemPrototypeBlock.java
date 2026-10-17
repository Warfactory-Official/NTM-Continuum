package com.hbm.items.bomb;

import com.hbm.blocks.bomb.ItemNukeBlock;
import com.hbm.explosion.BlastInfo;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** 1.7 {@code ItemPrototypeBlock}: the Prototype's three hard-coded tooltip lines, after its blast. */
public class ItemPrototypeBlock extends ItemNukeBlock {

    public ItemPrototypeBlock(Block block, Item.Properties properties) {
        super(block, properties, BlastInfo.PROTOTYPE);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("It didn't have to be like this."));
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("You monster."));
    }
}
