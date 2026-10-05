package com.hbm.compat.computercraft.turret;

//? if computercraft {
import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code TileEntityTurretHIMARS}: {@code ntm_artillery} plus an unchecked {@code addCoords}. */
public final class HimarsPeripheral extends ArtilleryPeripheral<TileEntityTurretHIMARS> {

    public HimarsPeripheral(TileEntityTurretHIMARS machine) {
        super(machine);
    }

    @LuaFunction(mainThread = true)
    public final Object[] addCoords(double x, double y, double z) {
        TileEntityTurretHIMARS machine = machine();
        machine.mode = TileEntityTurretHIMARS.MODE_MANUAL;
        machine.addQueuedTarget(new Vec3(x, y, z));
        machine.setChanged();
        return new Object[]{};
    }
}
//?}
