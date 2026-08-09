package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * Marker for blocks that <b>seal</b> radiation: the chunk radiation engine's flood-fill treats a
 * rad-resistant block as a wall, so it separates air/solid volumes into distinct sealed pockets that
 * exchange no radiation. Ported 1:1 from 1.12.2's {@code IRadResistantBlock}.
 */
public interface IRadResistantBlock {

    default boolean isRadResistant(BlockGetter level, BlockPos pos) {
        return true;
    }
}
