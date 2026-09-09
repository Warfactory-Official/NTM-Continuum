package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Ten metadata-equivalent dusted-steel stages used by the hot anvil chain. */
public final class ItemSteelDusted extends ItemHot {

    public static final int VARIANTS = 10;

    public ItemSteelDusted(Item.Properties properties) {
        // The old ItemHot static scale ends at 200 after the legacy ItemHot declarations are constructed.
        super(properties, 200);
    }

    public static ItemStack stack(int variant) {
        ItemStack stack = new ItemStack(com.hbm.items.ModJetpackDependencyItems.ingot_steel_dusted.get());
        ModDataComponents.ITEM_VARIANT.set(stack, Math.max(0, Math.min(VARIANTS - 1, variant)));
        return stack;
    }

    public static int variant(ItemStack stack) {
        return Math.floorMod(ModDataComponents.ITEM_VARIANT.getOr(stack, 0), VARIANTS);
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
