package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IFluidPipeMK2 extends IFluidConnectorMK2 {

    default FluidNode createNode(FluidType type) {
        BlockEntity tile = (BlockEntity) this;
        BlockPos p = tile.getBlockPos();
        return new FluidNode(type.getNetworkProvider(), p).setConnections(
                new DirPos(p.getX() + 1, p.getY(), p.getZ(), Library.POS_X),
                new DirPos(p.getX() - 1, p.getY(), p.getZ(), Library.NEG_X),
                new DirPos(p.getX(), p.getY() + 1, p.getZ(), Library.POS_Y),
                new DirPos(p.getX(), p.getY() - 1, p.getZ(), Library.NEG_Y),
                new DirPos(p.getX(), p.getY(), p.getZ() + 1, Library.POS_Z),
                new DirPos(p.getX(), p.getY(), p.getZ() - 1, Library.NEG_Z)
        );
    }
}
