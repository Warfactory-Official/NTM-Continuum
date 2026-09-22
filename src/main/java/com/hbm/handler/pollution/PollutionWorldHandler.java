package com.hbm.handler.pollution;

import dev.golem.core.world.Vegetation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7: poison-driven surface destruction. Modern: dirt meta 1 is {@code COARSE_DIRT}. */
public final class PollutionWorldHandler {

    private PollutionWorldHandler() {}

    public static void destroyBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 1.7 gates dirt on meta 0, so the plain block alone is the faithful test here.
        if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT) {
            level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        // 1.7's Material.plants; Vegetation.isPlant covers the BushBlock/VegetationBlock split, which
        // is what keeps tall grass and ferns reachable on the newer API.
        } else if (Vegetation.isLeaves(state) || Vegetation.isPlant(state)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
