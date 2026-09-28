package com.hbm.explosion.vanillant;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code Block.onBlockExploded} as the VNT block processor calls it: 1.7's {@code BlockProcessorStandard} handed
 * every broken block to its own {@code onBlockExploded} (with the explosion's exploder). A block that implements
 * this is asked first; answering true means it handled the hit itself and is not removed.
 */
public interface IBlockExplodedVNT {

    boolean onVNTExploded(ServerLevel level, BlockPos pos, BlockState state, ExplosionVNT explosion);
}
