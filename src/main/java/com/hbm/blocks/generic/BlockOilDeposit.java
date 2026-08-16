package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The live oil deposit ({@code ore_oil}) — port of the 1.12.2 {@code BlockNTMOre} OIL paths
 * (old BlockNTMOre L58-108 + {@code OreEnumUtil.OIL}): oil "sinks" — when the block below is a
 * drained {@code ore_oil_empty}, the two swap so drills always find the live oil at the bottom of
 * the pocket. Drops exactly 1× oil_tar_crude with <b>no</b> silk-touch branch and no XP (loot table
 * {@code hbm:blocks/ore_oil} is hand-authored, see gen_wave1 {@code CUSTOM_LOOT_IDS}).
 */
public class BlockOilDeposit extends dev.golem.core.world.CompatBlock {

    public BlockOilDeposit(Properties props) {
        super(props);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos,
                                     net.minecraft.world.level.block.Block block, boolean movedByPiston) {
        if (!level.isClientSide() && level.getBlockState(pos.below()).getBlock() == ModBlocks.ore_oil_empty.get()) {
            level.setBlockAndUpdate(pos, ModBlocks.ore_oil_empty.get().defaultBlockState());
            level.setBlockAndUpdate(pos.below(), this.defaultBlockState());
        }
    }
}
