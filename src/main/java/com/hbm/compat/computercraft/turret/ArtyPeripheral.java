package com.hbm.compat.computercraft.turret;

//? if computercraft {
import com.hbm.tileentity.turret.TileEntityTurretArty;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code TileEntityTurretArty}: {@code ntm_artillery} plus a range-checked {@code addCoords}. */
public final class ArtyPeripheral extends ArtilleryPeripheral<TileEntityTurretArty> {

    public ArtyPeripheral(TileEntityTurretArty machine) {
        super(machine);
    }

    // 1.7 switches to manual first, so the range checked is manual mode's
    @LuaFunction(mainThread = true)
    public final Object[] addCoords(double x, double y, double z) {
        TileEntityTurretArty machine = machine();
        machine.mode = TileEntityTurretArty.MODE_MANUAL;
        machine.setChanged();
        if (distance(machine.getBlockPos(), x, y, z) >= machine.getDetectorRange()) return new Object[]{false};
        machine.addQueuedTarget(new Vec3(x, y, z));
        return new Object[]{true};
    }
}
//?}
