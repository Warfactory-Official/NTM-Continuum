package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityChungus;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityChungus}'s component {@code ntm_turbine}; power is the tick's {@code powerBuffer}. */
public final class ChungusPeripheral extends TurbinePeripheral<TileEntityChungus> {

    public ChungusPeripheral(TileEntityChungus machine) {
        super(machine, SLOTS);
    }

    @Override
    protected FluidTankNTM input(TileEntityChungus machine) {
        return machine.tanks[0];
    }

    @Override
    protected FluidTankNTM output(TileEntityChungus machine) {
        return machine.tanks[1];
    }

    @Override
    protected long power(TileEntityChungus machine) {
        return machine.powerBuffer;
    }

    @LuaFunction(mainThread = true)
    public final Object[] setType(double type) {
        TileEntityChungus machine = machine();
        machine.tanks[0].setTankType(SteamTypes.fromInt((int) type));
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return info();
    }
}
//?}
