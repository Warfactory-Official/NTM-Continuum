package com.hbm.blocks.material;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.function.IntFunction;

/** Block item for a legacy metadata block whose modern identity is an item component. */
public final class MaterialCompletionBlockItem extends CompatBlockItem {

    private final int variantCount;
    private final String blockId;
    private final IntFunction<String> variantName;

    public MaterialCompletionBlockItem(Block block, Properties properties, String blockId, int variantCount,
                                       IntFunction<String> variantName) {
        super(block, properties);
        this.blockId = blockId;
        this.variantCount = variantCount;
        this.variantName = variantName;
    }

    public ItemStack of(int variant) {
        ItemStack stack = new ItemStack(this);
        ModDataComponents.ITEM_VARIANT.set(stack, Math.floorMod(variant, variantCount));
        return stack;
    }

    public int variant(ItemStack stack) {
        return Math.floorMod(ModDataComponents.ITEM_VARIANT.getOr(stack, 0), variantCount);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.hbm." + blockId + "." + variantName.apply(variant(stack)));
    }
}
