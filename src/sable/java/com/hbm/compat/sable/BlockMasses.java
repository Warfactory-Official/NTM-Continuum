package com.hbm.compat.sable;

import com.hbm.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** HBM block masses in iron blocks: hardness over iron's. Every solid cell of a machine weighs as its core. */
final class BlockMasses {

    /** Obsidian's hardness: HBM's 100 and 150 blocks would outweigh a ship of iron by orders of magnitude. */
    static final float MAX_HARDNESS = 50.0F;
    /** Sable's lightest class (super_light, 0.25 kpg) against its iron block (4 kpg). */
    static final double MIN_MASS = 1.0 / 16.0;

    private BlockMasses() {}

    static double inIronBlocks(Block block) {
        if (!Constants.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) return Double.NaN;
        return Math.max(MIN_MASS, cappedHardness(block) / hardness(Blocks.IRON_BLOCK));
    }

    /** Unbreakable blocks (hardness -1) count as the heaviest breakable ones. */
    private static float cappedHardness(Block block) {
        float hardness = hardness(block);
        return hardness < 0.0F ? MAX_HARDNESS : Math.min(hardness, MAX_HARDNESS);
    }

    private static float hardness(Block block) {
        return block.defaultBlockState().getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
