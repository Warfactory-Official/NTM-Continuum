package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineTurbine;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityMachineTurbine}'s component {@code ntm_turbine}. */
public final class SmallTurbinePeripheral extends TurbinePeripheral<TileEntityMachineTurbine> {

    public SmallTurbinePeripheral(TileEntityMachineTurbine machine) {
        super(machine, SLOTS);
    }

    @Override
    protected FluidTankNTM input(TileEntityMachineTurbine machine) {
        return machine.input;
    }

    @Override
    protected FluidTankNTM output(TileEntityMachineTurbine machine) {
        return machine.output;
    }

    @Override
    protected long power(TileEntityMachineTurbine machine) {
        return machine.power;
    }

    // the small turbine alone answers {true}; the large ones answer nothing
    @LuaFunction(mainThread = true)
    public final Object[] setType(double type) {
        TileEntityMachineTurbine machine = machine();
        machine.input.setTankType(SteamTypes.fromInt((int) type));
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return info();
    }
}
//?}
