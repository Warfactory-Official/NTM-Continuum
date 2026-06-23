package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public interface IEnergyConnectorBlock {

    boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir);

    static boolean isPort(DirPos[] connections, BlockPos cell, ForgeDirection dir) {
        int cx = cell.getX() + dir.offsetX, cy = cell.getY() + dir.offsetY, cz = cell.getZ() + dir.offsetZ;
        for (DirPos dp : connections) {
            BlockPos p = dp.getPos();
            if (dp.getDir() == dir && p.getX() == cx && p.getY() == cy && p.getZ() == cz) return true;
        }
        return false;
    }
}
