package com.hbm.api.fluidmk2;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Contract;

/** A tile that draws fluid from a network. */
public interface IFluidReceiverMK2 extends IFluidUserMK2 {

    /**
     * Sends fluid of the desired type and pressure to the receiver, returns the remainder that couldn't
     * be accepted.
     */
    @Contract(mutates = "this")
    long transferFluid(FluidType type, int pressure, long amount);

    default long getReceiverSpeed(FluidType type, int pressure) { return 1_000_000_000; }

    @Contract(pure = true)
    long getDemand(FluidType type, int pressure);

    default int[] getReceivingPressureRange(FluidType type) { return DEFAULT_PRESSURE_RANGE; }

    default void trySubscribe(FluidType type, Level world, DirPos pos) {
        trySubscribe(type, world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir());
    }

    default void trySubscribe(FluidType type, Level world, BlockPos pos, ForgeDirection dir) {
        trySubscribe(type, world, pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    default void trySubscribe(FluidType type, Level world, int x, int y, int z, ForgeDirection dir) {

        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));

        if (te instanceof IFluidConnectorMK2 con) {
            if (!con.canConnect(type, dir.getOpposite())) return;

            GenNode<FluidNetMK2> node = UniNodespace.getNode(world, new BlockPos(x, y, z), type.getNetworkProvider());

            if (node != null && node.net != null) {
                node.net.addReceiver(this);
            }
        }
    }

    default IEnergyReceiverMK2.ConnectionPriority getFluidPriority() {
        return IEnergyReceiverMK2.ConnectionPriority.NORMAL;
    }
}
