package com.hbm.items.tool;

import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ToolTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 1.7 {@code ItemSwordMeteorite}: one tier of the meteorite sword upgrade chain. Unbreakable, and its
 * {@code .desc} lang entry is shown in italics, one line per {@code $}-separated part.
 * 1.7's static {@code swords} list (every tier added itself, nothing read it) is not ported.
 */
public class ItemSwordMeteorite extends ItemSwordAbility {

    private final String descKey;

    public ItemSwordMeteorite(Properties properties, float damage, double movement, ToolTier material, String id) {
        super(properties, damage, movement, material);
        this.descKey = "item.hbm." + id + ".desc";
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, list, flag);

        String lines = Component.translatable(descKey).getString();
        for (String line : lines.split(java.util.regex.Pattern.quote("$"))) {
            list.add(Component.literal(line).withStyle(ChatFormatting.ITALIC));
        }
    }
}
