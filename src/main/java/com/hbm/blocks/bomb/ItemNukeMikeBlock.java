package com.hbm.blocks.bomb;

import net.minecraft.world.level.block.Block;

/** Ivy Mike block item, retaining the legacy thermonuclear tooltip policy. */
public class ItemNukeMikeBlock extends ItemNukeBlock {

    public ItemNukeMikeBlock(Block block, Properties properties) {
        super(block, properties, Type.IVY_MIKE);
    }
}
