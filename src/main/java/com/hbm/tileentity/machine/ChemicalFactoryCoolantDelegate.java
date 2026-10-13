package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.uninos.graph.PortClaims;

/** Network-facing coolant view used only by the factory's four coolant proxy positions. */
public final class ChemicalFactoryCoolantDelegate implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2,
        PortClaims.Delegate {

    /** 1.7's {@code delegate} subscribes at the coolant positions on the factory's behalf. */
    @Override public Object portClaimOwner() { return owner; }

    private final TileEntityMachineChemicalFactory owner;

    public ChemicalFactoryCoolantDelegate(TileEntityMachineChemicalFactory owner) {
        this.owner = owner;
    }

    @Override public long getPower() { return owner.getPower(); }
    @Override public void setPower(long power) { owner.setPower(power); }
    @Override public long getMaxPower() { return owner.getMaxPower(); }
    @Override public boolean isLoaded() { return owner.isLoaded(); }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{owner.water}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{owner.lps}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{owner.water, owner.lps}; }
}
