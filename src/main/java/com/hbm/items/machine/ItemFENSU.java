package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** FEnSU block item with the legacy dye hint and absurd-capacity readout. */
public class ItemFENSU extends CompatBlockItem {

    public ItemFENSU(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Change color using dyes"));
        long charge = Math.max(0L, Math.min(ModDataComponents.fensuPower(stack), Long.MAX_VALUE));
        if (charge == 0L) {
            tooltip.add(Component.literal("0/9.22EHE (0.0%)").withStyle(ChatFormatting.RED));
            return;
        }
        double percent = Math.round(1000D * ((double) charge / (double) Long.MAX_VALUE)) * 0.1D;
        ChatFormatting color = percent < 25D ? ChatFormatting.RED
                : percent >= 75D ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        tooltip.add(Component.literal(Library.getShortNumber(charge) + "/9.22EHE (" + percent + "%)")
                .withStyle(color));
    }
}
