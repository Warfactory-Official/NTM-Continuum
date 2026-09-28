package com.hbm.tileentity.machine;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityMachineRadarLarge}: the same radar with three times the range, fed and read through
 * the four edge-centre cells of its 3x3 footprint, so its connection points sit two blocks out.
 */
public class TileEntityMachineRadarLarge extends TileEntityMachineRadarNT {

    public static int radarLargeRange = 3_000;

    public TileEntityMachineRadarLarge(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.RADAR_LARGE.get(), pos, state);
    }

    @Override
    public int getRange() {
        return radarLargeRange;
    }

    @Override
    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z, Library.POS_X),
                new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y, z + 2, Library.POS_Z),
                new DirPos(x, y, z - 2, Library.NEG_Z),
        };
    }

    /** Only the proxy cells carry power; the core itself is buried in the middle of the footprint. */
    @Override
    public boolean canConnect(ForgeDirection dir) {
        return true;
    }

    /** The emitting cells are one block in from the connection points: notify them, then the points. */
    @Override
    protected void updateRedstone(Level level) {
        for (DirPos pos : getConPos()) {
            BlockPos cell = worldPosition.offset(pos.getDir().offsetX, 0, pos.getDir().offsetZ);
            level.updateNeighborsAt(cell, getBlockState().getBlock());
            level.updateNeighborsAt(pos.getPos(), getBlockState().getBlock());
        }
    }
}
