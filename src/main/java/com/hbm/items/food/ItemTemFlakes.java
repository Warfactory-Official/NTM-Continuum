package com.hbm.items.food;

import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemTemFlakes}: zero-hunger, always edible, heals 2 HP. The three metadata tiers only differ in
 * their crafting cost and tooltip; the port registers {@code tem_flakes}, {@code tem_flakes_normal} and
 * {@code tem_flakes_expensive}.
 */
public class ItemTemFlakes extends ItemModFood {

    private final int tier;

    public ItemTemFlakes(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        switch (tier) {
            case 0 -> tooltip.add(Component.literal("Heals 2HP DISCOUNT FOOD OF TEM!!!"));
            case 1 -> tooltip.add(Component.literal("Heals 2HP food of tem"));
            default -> tooltip.add(Component.literal("Heals food of tem (expensiv)"));
        }
    }
}
