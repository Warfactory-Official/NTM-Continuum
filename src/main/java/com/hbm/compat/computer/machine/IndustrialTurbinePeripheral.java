package com.hbm.compat.computer.machine;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import dev.golem.core.computer.ComputerMethod;

/** 1.7 {@code TileEntityMachineIndustrialTurbine}'s component {@code ntm_turbine}: no {@code setType}, a flywheel. */
public final class IndustrialTurbinePeripheral extends TurbinePeripheral<TileEntityMachineIndustrialTurbine> {

    private static final int FLYWHEEL = SLOTS;

    public IndustrialTurbinePeripheral(TileEntityMachineIndustrialTurbine machine) {
        super(machine, SLOTS + 1);
    }

    @Override
    protected FluidTankNTM input(TileEntityMachineIndustrialTurbine machine) {
        return machine.tanks[0];
    }

    @Override
    protected FluidTankNTM output(TileEntityMachineIndustrialTurbine machine) {
        return machine.tanks[1];
    }

    @Override
    protected long power(TileEntityMachineIndustrialTurbine machine) {
        return machine.powerBuffer;
    }

    @Override
    protected void capture(TileEntityMachineIndustrialTurbine machine) {
        super.capture(machine);
        put(FLYWHEEL, (int) (machine.spin * 100));
    }

    @ComputerMethod
    public final Object[] getFlywheel() {
        return read(s -> new Object[]{s.intAt(FLYWHEEL)});
    }

    @ComputerMethod
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.intAt(IN_FILL), s.intAt(IN_MAX), s.intAt(OUT_FILL), s.intAt(OUT_MAX),
                s.intAt(STEAM), s.longAt(POWER), s.intAt(FLYWHEEL)});
    }
}
