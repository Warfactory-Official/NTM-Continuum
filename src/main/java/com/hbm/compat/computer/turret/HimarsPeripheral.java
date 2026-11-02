package com.hbm.compat.computer.turret;

import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import dev.golem.core.computer.ComputerMethod;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code TileEntityTurretHIMARS}: {@code ntm_artillery} plus an unchecked {@code addCoords}. */
public final class HimarsPeripheral extends ArtilleryPeripheral<TileEntityTurretHIMARS> {

    public HimarsPeripheral(TileEntityTurretHIMARS machine) {
        super(machine);
    }

    @ComputerMethod(mainThread = true)
    public final Object[] addCoords(double x, double y, double z) {
        TileEntityTurretHIMARS machine = machine();
        machine.mode = TileEntityTurretHIMARS.MODE_MANUAL;
        machine.addQueuedTarget(new Vec3(x, y, z));
        machine.setChanged();
        return new Object[]{};
    }
}
