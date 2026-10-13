package com.hbm.tileentity.network;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/** The plain wooden / steel pylon: one wire, 25 m, cables join on the four horizontal faces of its foot. */
public class TileEntityPylon extends TileEntityPylonBase {

    public TileEntityPylon(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.PYLON.get(), pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SINGLE;
    }

    @Override
    public Vec3[] getMountPos() {
        return new Vec3[]{new Vec3(0.5, 5.5D, 0.5)};
    }

    @Override
    public double getMaxWireLength() {
        return 25;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.DOWN;
    }

    /** 1.7 lists the four horizontal neighbours of the foot as its cable connections. */
    @Override
    protected int getCellMask(int index) {
        return BlockMultiblockCore.MASK_HORIZONTAL;
    }
}
