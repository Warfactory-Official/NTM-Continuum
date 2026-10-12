package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.PortClaims;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

/**
 * {@link IFluidProviderMK2} with a standard implementation of provision/removal backed by the tile's own
 * sending tanks. {@code tryProvide} is 1.7's: it makes the tile a provider of the net of the duct at that port (a
 * {@link PortClaims} claim, renewed every tick) and hands fluid straight to a touching receiver.
 */
public interface IFluidStandardSenderMK2 extends IFluidProviderMK2 {

    default void tryProvide(FluidTankNTM tank, Level world, DirPos pos) { tryProvide(tank.getTankType(), tank.getPressure(), world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir()); }
    default void tryProvide(FluidType type, Level world, DirPos pos) { tryProvide(type, 0, world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir()); }
    default void tryProvide(FluidType type, int pressure, Level world, DirPos pos) { tryProvide(type, pressure, world, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir()); }

    default void tryProvide(FluidTankNTM tank, Level world, int x, int y, int z, ForgeDirection dir) { tryProvide(tank.getTankType(), tank.getPressure(), world, x, y, z, dir); }
    default void tryProvide(FluidTankNTM tank, Level world, BlockPos pos, ForgeDirection dir) { tryProvide(tank.getTankType(), tank.getPressure(), world, pos.getX(), pos.getY(), pos.getZ(), dir); }
    default void tryProvide(FluidType type, Level world, int x, int y, int z, ForgeDirection dir) { tryProvide(type, 0, world, x, y, z, dir); }

    default void tryProvide(FluidType type, int pressure, Level world, int x, int y, int z, ForgeDirection dir) {

        PortClaims.claim(world, x, y, z, dir, this, type, PortClaims.PROVIDE);
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

    /** 1.7 {@code IFluidStandardSender.sendFluidToAll}: {@link #tryProvide} on all six sides of {@code tile}. */
    default void sendFluidToAll(FluidTankNTM tank, BlockEntity tile) {
        BlockPos pos = tile.getBlockPos();
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            tryProvide(tank, tile.getLevel(), pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }
    }

    /** 1.7 {@code net.removeProvider(this)} at the duct at {@code x, y, z}: stop providing {@code type} there now. */
    default void tryUnprovide(FluidType type, Level world, int x, int y, int z) {
        PortClaims.withdraw(world, x, y, z, this, type, PortClaims.PROVIDE);
    }

    @NonNull FluidTankNTM[] getSendingTanks();

    @Override
    default long getFluidAvailable(FluidType type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) amount += tank.getFill();
        }
        return amount;
    }

    @Override
    default void useUpFluid(FluidType type, int pressure, long amount) {
        int tanks = 0;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) tanks++;
        }
        if (tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for (FluidTankNTM tank : getSendingTanks()) {
                if (tank.getTankType() == type && tank.getPressure() == pressure) {
                    int toRem = Math.min(firstRound, tank.getFill());
                    tank.setFill(tank.getFill() - toRem);
                    amount -= toRem;
                }
            }
        }
        if (amount > 0) for (FluidTankNTM tank : getSendingTanks()) {
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

        for (FluidTankNTM tank : getSendingTanks()) {
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
