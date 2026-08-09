package com.hbm.items.block;

import com.hbm.blocks.machine.NTMAnvil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** Block item for the {@link NTMAnvil}s — adds the gold "Tier: N" tooltip the 1.12.2 block showed. */
public class ItemBlockAnvil extends BlockItem {

    public ItemBlockAnvil(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        if (getBlock() instanceof NTMAnvil anvil) {
            adder.accept(Component.literal("Tier: " + anvil.tier).withStyle(ChatFormatting.GOLD));
        }
    }
}
