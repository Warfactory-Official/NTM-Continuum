package com.hbm.compat.computer.turret;

import com.hbm.tileentity.turret.TileEntityTurretBaseArtillery;
import dev.golem.core.computer.ComputerMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code TileEntityTurretBaseArtillery}: the turret component renamed {@code ntm_artillery}. */
public abstract class ArtilleryPeripheral<BE extends TileEntityTurretBaseArtillery> extends TurretPeripheral<BE> {

    protected ArtilleryPeripheral(BE machine) {
        super(machine, "ntm_artillery", SLOTS, 0);
    }

    /** 1.7's distance from the block coordinates, not the barrel. */
    protected static double distance(BlockPos pos, double x, double y, double z) {
        return Math.sqrt(Math.pow(pos.getX() - x, 2) + Math.pow(pos.getY() - y, 2) + Math.pow(pos.getZ() - z, 2));
    }

    @ComputerMethod(mainThread = true)
    public final Object[] getCurrentTarget() {
        BE machine = machine();
        // 1.7 reads index 0 of an empty queue; OpenComputers answered with the JVM 8 exception message
        if (machine.queuedTargets() == 0) return new Object[]{null, "Index: 0, Size: 0"};
        Vec3 target = machine.queuedTarget(0);
        return new Object[]{target.x, target.y, target.z};
    }

    @ComputerMethod
    public final Object[] getTargetDistance(double x, double y, double z) {
        return read(s -> new Object[]{Math.sqrt(Math.pow(s.intAt(X) - x, 2) + Math.pow(s.intAt(Y) - y, 2)
                + Math.pow(s.intAt(Z) - z, 2))});
    }
}
