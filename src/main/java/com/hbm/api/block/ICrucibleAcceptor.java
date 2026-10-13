package com.hbm.api.block;

import com.hbm.inventory.material.Mats;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A block that can receive molten material (the 1.12.2 {@code ICrucibleAcceptor}). Implemented
 * on the <b>Block</b> (the pour raytrace only sees blocks); implementations delegate to their
 * BlockEntity. Both transfer methods return the LEFTOVER stack ({@code null} = fully accepted).
 */
public interface ICrucibleAcceptor {

    /*
     * Pouring: the metal leaves the channel/crucible and usually (but not always) falls down. The
     * additional double coords give a more precise impact location.
     */
    boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, Mats.MaterialStack stack);

    Mats.MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, Mats.MaterialStack stack);

    /*
     * Flowing: the "safe" transfer of metal using a channel or other means, usually from block to block
     * and usually horizontally (but not necessarily).
     */
    boolean canAcceptPartialFlow(Level world, BlockPos pos, ForgeDirection side, Mats.MaterialStack stack);

    Mats.MaterialStack flow(Level world, BlockPos pos, ForgeDirection side, Mats.MaterialStack stack);
}
