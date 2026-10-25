package com.hbm.blocks.generic;

import com.hbm.items.LegacyMeta;
import dev.golem.core.world.CompatBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * One block item per state of a 1.7 metadata block (concrete colours, lightstone, the structure wand): each places
 * its own state. The item knows its 1.7 metadata ({@link #variant()}), which is never stored on the stack.
 * The 1.7 name {@code block.hbm.<block>.<variant>} is kept.
 */
public final class MaterialCompletionBlockItem extends CompatBlockItem implements LegacyMeta {

    private final String blockId;
    private final int variant;
    private final String variantName;

    public MaterialCompletionBlockItem(Block block, Properties properties, String blockId, int variant,
                                       String variantName) {
        super(block, properties);
        this.blockId = blockId;
        this.variant = variant;
        this.variantName = variantName;
    }

    /** The 1.7 metadata this item places. */
    public int variant() {
        return variant;
    }

    /** As {@link #variant()}; the stack is not consulted. */
    public int variant(ItemStack stack) {
        return variant;
    }

    @Override
    public int legacyMeta() {
        return variant;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("block.hbm." + blockId + "." + variantName);
    }
}
