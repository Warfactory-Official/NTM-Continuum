package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.items.machine.ItemPistons;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code TileEntityMachineCombustionEngine}'s component {@code ntm_combustion_engine}. */
public final class CombustionEnginePeripheral extends SnapshotPeripheral<TileEntityMachineCombustionEngine> {

    private static final int FILL = 0;
    private static final int MAX = 1;
    private static final int POWER = 2;
    private static final int SETTING = 3;
    private static final int ON = 4;
    private static final int HAS_EFFICIENCY = 5;
    private static final int EFFICIENCY = 6;
    private static final int TYPE = 0;

    public CombustionEnginePeripheral(TileEntityMachineCombustionEngine machine) {
        super(machine, "ntm_combustion_engine", 7, 1);
    }

    @Override
    protected void capture(TileEntityMachineCombustionEngine machine) {
        put(FILL, machine.tank.getFill());
        put(MAX, machine.tank.getMaxFill());
        put(POWER, machine.power);
        put(SETTING, machine.setting);
        put(ON, machine.isOn);
        putRef(TYPE, machine.tank.getTankType().getName());
        // 1.7 getEfficiency/getInfo dereference the piston slot and the FT_Combustible trait unguarded: an empty
        // slot or an incombustible fluid threw, which OpenComputers answered as "unknown error"
        ItemStack stack = machine.inventory.getStackInSlot(2);
        FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(machine.tank.getTankType());
        boolean known = !stack.isEmpty() && fuel != null;
        put(HAS_EFFICIENCY, known);
        if (known) {
            // 1.7 grabEnumSafely read any item's damage; only piston sets fit the slot here, so a stranger is STEEL
            ItemPistons.Variant piston = stack.getItem() instanceof ItemPistons
                    ? ((ItemPistons) stack.getItem()).getVariant() : ItemPistons.Variant.VALUES[0];
            put(EFFICIENCY, piston.eff[fuel.grade().ordinal()]);
        }
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.intAt(FILL), s.intAt(MAX)});
    }

    @LuaFunction("getType")
    public final Object[] fluidType() {
        return read(s -> new Object[]{s.refAt(TYPE)});
    }

    @LuaFunction
    public final Object[] getPower() {
        return read(s -> new Object[]{s.longAt(POWER)});
    }

    @LuaFunction
    public final Object[] getThrottle() {
        return read(s -> new Object[]{s.intAt(SETTING)});
    }

    @LuaFunction
    public final Object[] getState() {
        return read(s -> new Object[]{s.booleanAt(ON)});
    }

    @LuaFunction
    public final Object[] getEfficiency() {
        return read(s -> s.booleanAt(HAS_EFFICIENCY) ? new Object[]{s.doubleAt(EFFICIENCY)}
                : OpenComputers.unknownError());
    }

    @LuaFunction(mainThread = true)
    public final Object[] setThrottle(double request) {
        int throttle = (int) request;
        if (throttle < 0 || throttle > 30) return new Object[]{false, "Throttle request outside of range 0-30"};
        TileEntityMachineCombustionEngine machine = machine();
        machine.setting = throttle;
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] start() {
        TileEntityMachineCombustionEngine machine = machine();
        machine.isOn = true;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] stop() {
        TileEntityMachineCombustionEngine machine = machine();
        machine.isOn = false;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> s.booleanAt(HAS_EFFICIENCY)
                ? new Object[]{s.intAt(SETTING), s.booleanAt(ON), s.longAt(POWER), s.doubleAt(EFFICIENCY),
                s.intAt(FILL), s.intAt(MAX), s.refAt(TYPE)}
                : OpenComputers.unknownError());
    }
}
//?}
