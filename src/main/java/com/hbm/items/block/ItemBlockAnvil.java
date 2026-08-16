package com.hbm.items.block;

import com.hbm.blocks.machine.NTMAnvil;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;


/** Block item for the {@link NTMAnvil}s — adds the gold "Tier: N" tooltip the 1.12.2 block showed. */
public class ItemBlockAnvil extends CompatBlockItem {

    public ItemBlockAnvil(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         java.util.List<Component> lines, TooltipFlag flag) {
        if (getBlock() instanceof NTMAnvil anvil) {
            lines.add(Component.literal("Tier: " + anvil.tier).withStyle(ChatFormatting.GOLD));
        }
    }
}
