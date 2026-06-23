package com.hbm.api.energymk2;

import com.hbm.api.tile.ILoadedTile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public interface IEnergyHandlerMK2 extends IEnergyConnectorMK2, ILoadedTile {

    long getPower();

    void setPower(long power);

    long getMaxPower();

    boolean particleDebug = false;

    default Vec3 getDebugParticlePosMK2() {
        BlockEntity te = (BlockEntity) this;
        return new Vec3(te.getBlockPos().getX() + 0.5, te.getBlockPos().getY() + 1, te.getBlockPos().getZ() + 0.5);
    }
}
