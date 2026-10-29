package com.hbm.blocks.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Transient grants for the exact multiblocks a player is leaving after placement.
 */
public interface PlacementEscape {
    void hbm$grantPlacementEscape(ResourceKey<Level> dimension, long core);

    void hbm$refreshPlacementEscape();

    boolean hbm$hasPlacementEscape();

    boolean hbm$ignoresPlacementCollision(Level level, BlockPos pos);
}
