package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.DuctData;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityPipeExhaustAnchor}: the pipe anchor of the exhaust network. 1.7 kept one node per smoke; the
 * port's one cell carries all three smokes, as the exhaust duct's does. It has no fluid of its own.
 */
public class TileEntityPipeExhaustAnchor extends TileEntityFluidPipeAnchor {

    public TileEntityPipeExhaustAnchor(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.ANCHOR_EXHAUST.get(), pos, state);
    }

    @Override
    public NetworkType getNetworkType() {
        return NetworkType.EXHAUST;
    }

    @Override
    protected DuctData ductData() {
        return DuctData.SMOKE_ANY;
    }

    @Override
    public void setFluidType(FluidType type) { }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir == openSide() && DuctData.isSmoke(type);
    }

    @Override
    public int canConnect(TileEntityPipelineBase other) {
        return 0;
    }
}
