package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IEnergyConductorMK2 extends IEnergyConnectorMK2 {

    default Nodespace.PowerNode createNode() {
        BlockEntity tile = (BlockEntity) this;
        int x = tile.getBlockPos().getX();
        int y = tile.getBlockPos().getY();
        int z = tile.getBlockPos().getZ();
        return new Nodespace.PowerNode(new BlockPos(x, y, z)).setConnections(
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y + 1, z, Library.POS_Y),
                new DirPos(x, y - 1, z, Library.NEG_Y),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z)
        );
    }
}
