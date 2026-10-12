package com.hbm.api.fluidmk2;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.PortClaims;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;

/**
 * A tile that draws fluid from a network. The network finds it by its faces; {@link #trySubscribe} says, as in 1.7,
 * at which ports it takes which fluid (see {@link PortClaims}).
 */
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

    default void trySubscribe(FluidType type, Level world, DirPos pos) { trySubscribe(type, world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir()); }
    default void trySubscribe(FluidType type, Level world, BlockPos pos, ForgeDirection dir) { trySubscribe(type, world, pos.getX(), pos.getY(), pos.getZ(), dir); }

    /**
     * 1.7 {@code IFluidReceiverMK2.trySubscribe}: this tile receives {@code type} from the net of the duct at
     * {@code x, y, z}, which it touches on its side {@code dir}, for the next three seconds. Called every tick from
     * each port, as in 1.7; a net reaching the tile through a port it did not subscribe at gives it nothing.
     */
    default void trySubscribe(FluidType type, Level world, int x, int y, int z, ForgeDirection dir) {
        PortClaims.claim(world, x, y, z, dir, this, type, PortClaims.SUBSCRIBE);
    }

    /** 1.7 {@code IFluidStandardReceiver.subscribeToAllAround}: {@link #trySubscribe} on all six sides of {@code tile}. */
    default void subscribeToAllAround(FluidType type, net.minecraft.world.level.block.entity.BlockEntity tile) {
        BlockPos pos = tile.getBlockPos();
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            trySubscribe(type, tile.getLevel(), pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }
    }

    /** 1.7 {@code IFluidStandardReceiver.unsubscribeToAllAround}. */
    default void unsubscribeToAllAround(FluidType type, net.minecraft.world.level.block.entity.BlockEntity tile) {
        BlockPos pos = tile.getBlockPos();
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            tryUnsubscribe(type, tile.getLevel(), pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ);
        }
    }

    /** 1.7 {@code IFluidStandardReceiver.tryUnsubscribe}: stop receiving {@code type} at the duct at {@code x, y, z} now. */
    default void tryUnsubscribe(FluidType type, Level world, int x, int y, int z) {
        PortClaims.withdraw(world, x, y, z, this, type, PortClaims.SUBSCRIBE);
    }

    default IEnergyReceiverMK2.ConnectionPriority getFluidPriority() {
        return IEnergyReceiverMK2.ConnectionPriority.NORMAL;
    }
}
