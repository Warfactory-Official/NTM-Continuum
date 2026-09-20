package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import com.hbm.blocks.ModTerrainBlocks;

/**
 * 1.7 {@code BlockMushHuge} ({@code mush_block} / {@code mush_block_stem}): the huge glowing mushroom's
 * cap and stem. Drops {@code rand(10) - 7} (0..2) glowing mushrooms. (The old pick-block override returning the
 * mushroom is not ported: the clone-stack hook's signature differs per version.)
 */
public class BlockMushHuge extends CompatBlock {

    public BlockMushHuge(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int count = params.getLevel().getRandom().nextInt(10) - 7;
        if (count <= 0) return List.of();
        return List.of(new ItemStack(ModTerrainBlocks.mush.get(), count));
    }
}
