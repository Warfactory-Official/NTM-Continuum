package com.hbm.items.block;

import com.hbm.tileentity.machine.storage.TemplateCrateLayout;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Block item for the ordinary template crate, retaining the old slot-count tooltip. */
public class ItemBlockTemplateCrate extends CompatBlockItem {

    public ItemBlockTemplateCrate(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> lines, TooltipFlag flag) {
        int used = Compat.containerItemCount(stack, TemplateCrateLayout.SLOTS);
        float percent = Math.round(used * 1000.0F / TemplateCrateLayout.SLOTS) / 10.0F;
        ChatFormatting color = percent >= 75 ? ChatFormatting.RED
                : percent < 25 ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        lines.add(Component.literal(used + "/" + TemplateCrateLayout.SLOTS
                + " Slots used (" + percent + "%)").withStyle(color));
    }
}
