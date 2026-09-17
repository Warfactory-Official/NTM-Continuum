package com.hbm.blocks.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 1.7 BlockDetonatable.onShot: a sedna bullet hitting the block detonates it instantly. 1.7 gates it on a per-instance {@code detonateOnShot} boolean, true only for {@code RedBarrel(flammable)} - red_barrel and pink_barrel; here that gate is interface membership.
 */
public interface BlockDetonatable {

    void onShot(Level level, BlockPos pos);
}
