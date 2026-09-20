package com.hbm.tileentity.network;

import net.minecraft.core.BlockPos;

/**
 * 1.7 {@code com.hbm.tileentity.network.IDroneLinkable}: a stop on a transport drone route. The linker reads
 * {@link #getPoint} from the block it is aimed at and writes it into the previous stop, so the route is a chain of
 * single forward links.
 */
public interface IDroneLinkable {

    /** Where a drone sent to this stop hovers. */
    BlockPos getPoint();

    void setNextTarget(BlockPos target);
}
