package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** A block item carrying the block's 1.7 {@code addInformation} lines (literal, coloured, as upstream wrote them). */
public class ExplosivesBlockItem extends CompatBlockItem {

    private final Supplier<List<Component>> lines;

    public ExplosivesBlockItem(Block block, Properties properties, Supplier<List<Component>> lines) {
        super(block, properties);
        this.lines = lines;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.addAll(lines.get());
    }
}
