package com.hbm.blocks.generic;

import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import dev.golem.core.world.CompatBlock;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Meteorite treasure block — original-hbm (1.7.10) {@code BlockMeteoriteTreasure}: breaking it draws
 * {@code 1 + rand(3)} stacks from {@code POOL_METEORITE_TREASURE} (Fortune ignored). A plain opaque cube
 * without a tile entity, so silk touch keeps the block (1.7 checks silk touch before {@code getDrops}).
 */
public class BlockMeteoriteTreasure extends CompatBlock {

    public BlockMeteoriteTreasure(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (OreDrops.silkTouch(params)) return List.of(new ItemStack(this));
        RandomSource rand = params.getLevel().getRandom();
        int count = 1 + rand.nextInt(3);
        List<ItemStack> drops = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemPool.getStack(ItemPools.POOL_METEORITE_TREASURE, rand);
            if (!stack.isEmpty()) drops.add(stack);
        }
        return drops;
    }
}
