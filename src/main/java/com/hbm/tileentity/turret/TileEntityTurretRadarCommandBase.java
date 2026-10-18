package com.hbm.tileentity.turret;

import com.hbm.api.block.IRadarCommandReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A turret a radar linker can point at: every tick the radar hands it the nearest blip. A radar-designated target
 * may be engaged at four times the detector range and is not lost to range afterwards.
 */
public abstract class TileEntityTurretRadarCommandBase extends TileEntityTurretBaseNT implements IRadarCommandReceiver {

    public boolean radarDesignatedTarget = false;

    protected TileEntityTurretRadarCommandBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean sendCommandEntity(Entity target) {
        if (this.target != null || !isOn) return false;
        if (target == null || !target.isAlive()) return false;

        // 1.7 sets this before the checks, so a refused blip still lifts the range limit for the next target
        radarDesignatedTarget = true;
        if (!entityAcceptableTarget(target) || !entityInLOS(target)) return false;

        Vec3 ent = getEntityPos(target);
        if (getTurretPos().distanceTo(ent) > getDetectorRange() * 4) {
            turnTowards(ent);
            return true;
        }

        this.target = target;
        return true;
    }

    @Override
    public boolean sendCommandPosition(int x, int y, int z) {
        if (!isOn) return false;
        turnTowards(new Vec3(x, y, z));
        return true;
    }

    @Override
    protected void targetLost() {
        radarDesignatedTarget = false;
    }

    @Override
    protected double maxSightRange() {
        return radarDesignatedTarget ? Double.POSITIVE_INFINITY : super.maxSightRange();
    }
}
