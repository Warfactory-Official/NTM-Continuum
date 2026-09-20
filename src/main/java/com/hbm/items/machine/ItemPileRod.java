package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The legacy Chicago pile rods: the shared insertion hint, then the rod's own numbered lines. */
@Deprecated
public class ItemPileRod extends CompatItem {

    public ItemPileRod(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Language language = Language.getInstance();
        for (int i = 0; language.has("desc.item.pileRod." + i); i++) tooltip.add(Component.translatable("desc.item.pileRod." + i));
        String base = getDescriptionId() + ".desc.";
        for (int i = 0; language.has(base + i); i++) tooltip.add(Component.translatable(base + i));
    }
}
