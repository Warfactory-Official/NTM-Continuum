package com.hbm.items.block;

import com.hbm.blocks.generic.TrappedBrick;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** 1.7 {@code ItemTrapBlock}: the trapped brick's item names its trap, the enum constant as written. */
public class ItemTrapBlock extends CompatBlockItem {

    public ItemTrapBlock(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        if (getBlock() instanceof TrappedBrick brick) tooltip.add(Component.literal(brick.trap.toString()));
    }
}
