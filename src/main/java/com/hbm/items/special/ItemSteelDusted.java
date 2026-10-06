package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Ten dusted-steel forge stages (1.7 metadata 0..9, {@code hbm:forge_count} here) used by the hot anvil chain. */
public final class ItemSteelDusted extends ItemHot {

    public static final int VARIANTS = 10;

    public ItemSteelDusted(Item.Properties properties) {
        // The old ItemHot static scale ends at 200 after the legacy ItemHot declarations are constructed.
        super(properties, 200);
    }

    /**
     * An ingot forged {@code variant} times. 1.7 kept the count in the damage value; here it is {@code hbm:forge_count}
     * (ntm-next's FORGE_COUNT, a count, not an id), absent for an unforged ingot (History §19es-itemvariant).
     */
    public static ItemStack stack(int variant) {
        ItemStack stack = new ItemStack(com.hbm.items.ModJetpackDependencyItems.ingot_steel_dusted.get());
        int count = Math.max(0, Math.min(VARIANTS - 1, variant));
        if (count > 0) ModDataComponents.FORGE_COUNT.set(stack, count);
        return stack;
    }

    public static int variant(ItemStack stack) {
        return Math.floorMod(ModDataComponents.FORGE_COUNT.getOr(stack, 0), VARIANTS);
    }

    public static int getMaxHeat(ItemStack stack) {
        return Math.max(0, 200 - variant(stack) * 10);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hot_dusted.forged", variant(stack)));
    }
}
