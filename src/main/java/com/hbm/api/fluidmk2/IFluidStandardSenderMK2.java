package com.hbm.api.fluidmk2;

import com.hbm.api.fluid.FluidType;
import com.hbm.api.fluid.FluidTank;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

/**
 * {@link IFluidProviderMK2} with a standard implementation of provision/removal backed by the tile's own
 * sending tanks. {@code tryProvide} is now only the direct, network-less transfer to a touching
 * receiver: a duct network finds its own providers by probing the faces of its cells.
 */
public interface IFluidStandardSenderMK2 extends IFluidProviderMK2 {

    default void tryProvide(FluidTank tank, Level world, int x, int y, int z, Direction dir) { tryProvide(tank.getTankType(), tank.getPressure(), world, x, y, z, dir); }
    default void tryProvide(FluidTank tank, Level world, BlockPos pos, Direction dir) { tryProvide(tank.getTankType(), tank.getPressure(), world, pos.getX(), pos.getY(), pos.getZ(), dir); }
    default void tryProvide(FluidType type, Level world, int x, int y, int z, Direction dir) { tryProvide(type, 0, world, x, y, z, dir); }
    default void tryProvide(FluidType type, Level world, BlockPos pos, Direction dir) { tryProvide(type, 0, world, pos.getX(), pos.getY(), pos.getZ(), dir); }
    default void tryProvide(FluidType type, int pressure, Level world, BlockPos pos, Direction dir) { tryProvide(type, pressure, world, pos.getX(), pos.getY(), pos.getZ(), dir); }

    default void tryProvide(FluidType type, int pressure, Level world, int x, int y, int z, Direction dir) {

        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));

        if (te != this && te instanceof IFluidReceiverMK2 rec) {
            if (rec.canConnect(type, dir.getOpposite())) {
                long provides = Math.min(this.getFluidAvailable(type, pressure), this.getProviderSpeed(type, pressure));
                long receives = Math.min(rec.getDemand(type, pressure), rec.getReceiverSpeed(type, pressure));
                long toTransfer = Math.min(provides, receives);
                toTransfer -= rec.transferFluid(type, pressure, toTransfer);
                this.useUpFluid(type, pressure, toTransfer);
            }
        }
    }

    @NonNull FluidTank[] getSendingTanks();

    @Override
    default long getFluidAvailable(FluidType type, int pressure) {
        long amount = 0;
        for (FluidTank tank : getSendingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) amount += tank.getFill();
        }
        return amount;
    }

    @Override
    default void useUpFluid(FluidType type, int pressure, long amount) {
        int tanks = 0;
        for (FluidTank tank : getSendingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) tanks++;
        }
        if (tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for (FluidTank tank : getSendingTanks()) {
                if (tank.getTankType() == type && tank.getPressure() == pressure) {
                    int toRem = Math.min(firstRound, tank.getFill());
                    tank.setFill(tank.getFill() - toRem);
                    amount -= toRem;
                }
            }
        }
        if (amount > 0) for (FluidTank tank : getSendingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) {
                int toRem = (int) Math.min(amount, tank.getFill());
                tank.setFill(tank.getFill() - toRem);
                amount -= toRem;
            }
        }
    }

    @Override
    default int[] getProvidingPressureRange(FluidType type) {
        int lowest = HIGHEST_VALID_PRESSURE;
        int highest = 0;

        for (FluidTank tank : getSendingTanks()) {
            if (tank.getTankType() == type) {
                if (tank.getPressure() < lowest) lowest = tank.getPressure();
                if (tank.getPressure() > highest) highest = tank.getPressure();
            }
        }

        return lowest <= highest ? new int[]{lowest, highest} : DEFAULT_PRESSURE_RANGE;
    }

    @Override
    default long getProviderSpeed(FluidType type, int pressure) {
        return 1_000_000_000;
    }
}
