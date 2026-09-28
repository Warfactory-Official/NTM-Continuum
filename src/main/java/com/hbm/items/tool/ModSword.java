package com.hbm.items.tool;

import dev.golem.core.world.CompatSwordItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 1.7 {@code ModSword}: a vanilla sword ({@code 4 +} material damage) with the manual override's and the
 * Reer Graar's tooltip lines. The {@code this == ModItems.pipe_lead} checks are the {@code lore} passed in.
 */
public class ModSword extends CompatSwordItem {

    private final String[] lore;

    public ModSword(Properties properties, ToolTier tier, String... lore) {
        super(properties, tier, 4.0F + tier.attackDamageBonus(), Tools.SWORD_SPEED, 0.0D);
        this.lore = lore;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        for (String line : lore) list.add(Component.literal(line));
    }
}
