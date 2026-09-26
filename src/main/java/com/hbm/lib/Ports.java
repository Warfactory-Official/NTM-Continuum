package com.hbm.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Multiblock network ports from {@code getConPos()}. */
public final class Ports {

    private Ports() {}

    /**
     * Whether a multiblock cell at {@code cell} exposes a real power port on side {@code side} — i.e. one
     * of the machine's network connection points ({@code getConPos()}) sits at the neighbouring block in
     * that direction, looking back. Lets a multiblock machine connect a cable
     * only at its true ports instead of every footprint face. {@code side} is the cell's side facing the
     * cable, matching the value passed to {@link com.hbm.api.energymk2.IEnergyConnectorBlock#canConnect}.
     */
    public static boolean isPort(DirPos[] connections, BlockPos cell, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        int cx = cell.getX() + dir.offsetX, cy = cell.getY() + dir.offsetY, cz = cell.getZ() + dir.offsetZ;
        for (DirPos dp : connections) {
            BlockPos p = dp.getPos();
            if (dp.getDir() == dir && p.getX() == cx && p.getY() == cy && p.getZ() == cz) return true;
        }
        return false;
    }

    /** As {@link #isPort(DirPos[], BlockPos, Direction)}, for machines whose {@code getConPos()} is a list. */
    public static boolean isPort(java.util.List<DirPos> connections, BlockPos cell, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        int cx = cell.getX() + dir.offsetX, cy = cell.getY() + dir.offsetY, cz = cell.getZ() + dir.offsetZ;
        for (DirPos dp : connections) {
            BlockPos p = dp.getPos();
            if (dp.getDir() == dir && p.getX() == cx && p.getY() == cy && p.getZ() == cz) return true;
        }
        return false;
    }
}
