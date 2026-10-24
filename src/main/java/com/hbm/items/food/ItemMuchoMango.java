package com.hbm.items.food;

import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** 1.7 {@code ItemMuchoMango}: an always-edible 10-hunger food drunk over a full 200 ticks. */
public class ItemMuchoMango extends ItemModFood {

    public ItemMuchoMango(Properties properties) {
        super(properties, 200, true);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("The Comically Large Can"));
    }
}
