package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Supplier;

/**
 * Block item carrying the old {@code ITooltipProvider.addInformation} lines of a decorative block
 * ("Purely decorative", the no-spawn warning, the depth trait, the asphalt speed bonus).
 */
public class DecoBlockItem extends CompatBlockItem {

    private final List<Supplier<Component>> lines;

    public DecoBlockItem(Block block, Properties properties, List<Supplier<Component>> lines) {
        super(block, properties);
        this.lines = lines;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        for (Supplier<Component> line : lines) {
            tooltip.add(line.get());
        }
    }
}
