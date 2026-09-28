package com.hbm.items.block;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.IntFunction;

/**
 * The block item of a 1.7 metadata block whose subtype lives in the block entity or the block state: the
 * bobbleheads, plushies, snowglobes, vending machines, filing cabinets and glyphid hive blocks. The 1.7 item
 * damage is the {@link ModDataComponents#ITEM_VARIANT} component here.
 *
 * <p>{@code name} answers the display name per variant (1.7 {@code ItemBlockMeta} kept one name for all,
 * {@code BlockPlushie.getOverrideDisplayName} formatted the type's label into it); {@code tooltip} is the
 * block's {@code addInformation}, or null.
 */
public class ItemBlockDecoVariant extends CompatBlockItem {

    private final int variantCount;
    private final IntFunction<Component> name;
    private final IntFunction<List<Component>> tooltip;

    public ItemBlockDecoVariant(Block block, Properties properties, int variantCount, IntFunction<Component> name,
                                IntFunction<List<Component>> tooltip) {
        super(block, properties);
        this.variantCount = variantCount;
        this.name = name;
        this.tooltip = tooltip;
    }

    public ItemStack of(int variant) {
        ItemStack stack = new ItemStack(this);
        ModDataComponents.ITEM_VARIANT.set(stack, Math.floorMod(variant, variantCount));
        return stack;
    }

    public int variant(ItemStack stack) {
        return Math.floorMod(ModDataComponents.ITEM_VARIANT.getOr(stack, 0), variantCount);
    }

    public int variantCount() {
        return variantCount;
    }

    @Override
    public Component getName(ItemStack stack) {
        return name.apply(variant(stack));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> lines,
                                         TooltipFlag flag) {
        if (tooltip != null) lines.addAll(tooltip.apply(variant(stack)));
    }
}
