package com.hbm.items.block;

import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Autosaw block item with the legacy seven-line fuel description. */
public class ItemBlockAutosaw extends CompatBlockItem {

    public ItemBlockAutosaw(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> lines, TooltipFlag flag) {
        for (int i = 0; i < 7; i++) {
            lines.add(Component.translatable("block.hbm.machine_autosaw.desc." + i)
                    .withStyle(ChatFormatting.YELLOW));
        }
    }
}
