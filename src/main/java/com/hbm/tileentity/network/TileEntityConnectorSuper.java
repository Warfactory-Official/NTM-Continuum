package com.hbm.tileentity.network;

import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/** The super connector: the same wall mount with a 100 m reach. */
public class TileEntityConnectorSuper extends TileEntityConnector {

    public TileEntityConnectorSuper(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.CONNECTOR_SUPER.get(), pos, state);
    }

    @Override
    public Vec3[] getMountPos() {
        ForgeDirection dir = placedSide();
        return new Vec3[]{new Vec3(0.5 + dir.offsetX * 0.375, 0.5 + dir.offsetY * 0.375, 0.5 + dir.offsetZ * 0.375)};
    }

    @Override
    public double getMaxWireLength() {
        return 100;
    }
}
