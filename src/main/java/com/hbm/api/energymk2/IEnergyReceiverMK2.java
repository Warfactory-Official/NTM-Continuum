package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** If it receives energy, use this. The contract is on {@link IEnergyHandlerMK2}; this adds subscription. */
public interface IEnergyReceiverMK2 extends IEnergyHandlerMK2 {

    default void trySubscribe(Level world, DirPos pos) {
        trySubscribe(world, pos.getPos(), pos.getDir());
    }

    default void trySubscribe(Level world, BlockPos pos, ForgeDirection dir) {
        trySubscribe(world, pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    default void trySubscribe(Level world, int x, int y, int z, ForgeDirection dir) {
        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));

        if (te instanceof IEnergyConductorMK2 con) {
            if (!con.canConnect(dir.getOpposite())) return;

            Nodespace.PowerNode node = Nodespace.getNode(world, new BlockPos(x, y, z));

            if (node != null && node.net != null) {
                node.net.addReceiver(this);
            }
        }
    }

    default void tryUnsubscribe(Level world, int x, int y, int z) {
        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));

        if (te instanceof IEnergyConductorMK2 con) {
            Nodespace.PowerNode node = con.createNode();

            if (node != null && node.net != null) {
                node.net.removeReceiver(this);
            }
        }
    }

}
