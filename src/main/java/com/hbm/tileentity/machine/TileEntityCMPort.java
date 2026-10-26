package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModCustomMachineBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.uninos.PortNodes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The block entity of a {@code cm_port}: 1.7's {@code TileEntityProxyCombo().inventory().power().fluid()}. It
 * points nowhere until a custom machine's structure check finds it and hands it the controller's position (1.7
 * {@code proxy.cachedPosition}); from then on it answers the energy and fluid graphs, and through
 * {@link com.hbm.blocks.machine.BlockCMPort} hoppers, with the controller's buffers.
 *
 * <p>1.7's proxy was a receiver only, and the controller pushed its generated power, output fluids and smoke into
 * the cables and ducts touching the ports itself. The port's graphs do not take pushes; they ask each face what
 * stands behind it. So the proxy answers as a provider too, which is where 1.7's pushes arrived.
 */
public class TileEntityCMPort extends TileEntityLoadedBase
        implements IEnergyReceiverMK2, IEnergyProviderMK2, IFluidReceiverMK2, IFluidProviderMK2,
        PortNodes.Delegate {

    /** The controller owns the ports here; this block only fronts it. */
    @Override
    public @Nullable Object portOwner() {
        return getCore();
    }

    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];

    private @Nullable BlockPos core;

    public TileEntityCMPort(BlockPos pos, BlockState state) {
        super(ModCustomMachineBlockEntities.CM_PORT.get(), pos, state);
    }

    public void setCore(BlockPos core) {
        this.core = core.immutable();
        setChanged();
    }

    public @Nullable BlockPos getCorePos() {
        return core;
    }

    /** 1.7 {@code TileEntityProxyBase.getTE}: the controller at the cached position, if it is loaded and still there. */
    public @Nullable TileEntityCustomMachine getCore() {
        if (core == null || level == null) return null;
        if (!level.hasChunk(core.getX() >> 4, core.getZ() >> 4)) return null;
        BlockEntity be = level.getBlockEntity(core);
        return be instanceof TileEntityCustomMachine machine ? machine : null;
    }

    // The core owns the charge; this port only delegates to it.
    @Override public boolean ownsEnergyBuffer() { return false; }

    // --- power ---

    @Override public long getPower() { TileEntityCustomMachine c = getCore(); return c != null ? c.getPower() : 0; }
    @Override public void setPower(long power) { TileEntityCustomMachine c = getCore(); if (c != null) c.setPower(power); }
    @Override public long getMaxPower() { TileEntityCustomMachine c = getCore(); return c != null ? c.getMaxPower() : 0; }
    @Override public long getReceiverSpeed() { TileEntityCustomMachine c = getCore(); return c != null ? c.getReceiverSpeed() : 0; }
    @Override public long getProviderSpeed() { TileEntityCustomMachine c = getCore(); return c != null ? c.getProviderSpeed() : 0; }

    @Override
    public long transferPower(long power, boolean simulate) {
        TileEntityCustomMachine c = getCore();
        return c != null ? c.transferPower(power, simulate) : power;
    }

    // --- fluid ---

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        TileEntityCustomMachine c = getCore();
        return c != null ? c.getAllTanks() : NO_TANKS;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        TileEntityCustomMachine c = getCore();
        return c != null ? c.transferFluid(type, pressure, amount) : amount;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        TileEntityCustomMachine c = getCore();
        return c != null ? c.getDemand(type, pressure) : 0;
    }

    @Override
    public int[] getReceivingPressureRange(FluidType type) {
        TileEntityCustomMachine c = getCore();
        return c != null ? c.getReceivingPressureRange(type) : DEFAULT_PRESSURE_RANGE;
    }

    @Override
    public void useUpFluid(FluidType type, int pressure, long amount) {
        TileEntityCustomMachine c = getCore();
        if (c != null) c.useUpFluid(type, pressure, amount);
    }

    @Override
    public long getFluidAvailable(FluidType type, int pressure) {
        TileEntityCustomMachine c = getCore();
        return c != null ? c.getFluidAvailable(type, pressure) : 0;
    }

    @Override
    public int[] getProvidingPressureRange(FluidType type) {
        TileEntityCustomMachine c = getCore();
        return c != null ? c.getProvidingPressureRange(type) : DEFAULT_PRESSURE_RANGE;
    }
}
