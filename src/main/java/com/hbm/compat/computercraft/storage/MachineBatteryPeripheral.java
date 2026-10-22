package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1.7 {@code TileEntityMachineBattery}'s component {@code ntm_energy_storage_legacy}.
 */
public final class MachineBatteryPeripheral<BE extends BlockEntity & IEnergyHandlerMK2> extends SnapshotPeripheral<BE> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;

    public MachineBatteryPeripheral(BE machine) {
        super(machine, "ntm_energy_storage_legacy", 2, 0);
    }

    @Override
    protected void capture(BE machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER)});
    }
}
//?}
