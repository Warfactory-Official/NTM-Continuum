package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * 1.7 {@code BlockPorous} ({@code stone_porous}): a {@code BlockStone} subclass (hardness 1.5, blast 30)
 * laid down around bedrock oil. Drops cobblestone like stone, and its silk-touch stack was overridden to
 * plain smooth stone rather than itself ({@code createStackedBlock}).
 */
public class BlockPorous extends CompatBlock {

    public BlockPorous(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(OreDrops.silkTouch(params) ? Blocks.STONE : Blocks.COBBLESTONE));
    }
}
