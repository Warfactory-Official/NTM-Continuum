package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * Interface for blocks that should visually connect to cables without having an IEnergyConnector tile
 * entity. This is meant for BLOCKS. Used for rendering only.
 *
 * @author hbm
 */
public interface IEnergyConnectorBlock {

    boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir);

    /**
     * Whether a multiblock cell at {@code cell} exposes a real power port on side {@code dir} — i.e. one
     * of the machine's network connection points ({@code getConPos()}) sits at the neighbouring block in
     * that direction, looking back. Lets a {@link com.hbm.blocks.BlockDummyable} machine connect a cable
     * only at its true ports instead of every footprint face. {@code dir} is the cell's side facing the
     * cable, matching the value passed to {@link #canConnect}.
     */
    static boolean isPort(DirPos[] connections, BlockPos cell, ForgeDirection dir) {
        int cx = cell.getX() + dir.offsetX, cy = cell.getY() + dir.offsetY, cz = cell.getZ() + dir.offsetZ;
        for (DirPos dp : connections) {
            BlockPos p = dp.getPos();
            if (dp.getDir() == dir && p.getX() == cx && p.getY() == cy && p.getZ() == cz) return true;
        }
        return false;
    }

    /** As {@link #isPort(DirPos[], BlockPos, ForgeDirection)}, for machines whose {@code getConPos()} is a list. */
    static boolean isPort(java.util.List<DirPos> connections, BlockPos cell, ForgeDirection dir) {
        int cx = cell.getX() + dir.offsetX, cy = cell.getY() + dir.offsetY, cz = cell.getZ() + dir.offsetZ;
        for (DirPos dp : connections) {
            BlockPos p = dp.getPos();
            if (dp.getDir() == dir && p.getX() == cx && p.getY() == cy && p.getZ() == cz) return true;
        }
        return false;
    }
}
