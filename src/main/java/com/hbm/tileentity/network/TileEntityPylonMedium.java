package com.hbm.tileentity.network;

import net.minecraft.core.Direction;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.blocks.network.PylonMedium;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/** The medium (three-wire) pylon; only the transformer variants take a cable, on the face behind them. */
public class TileEntityPylonMedium extends TileEntityPylonBase {

    public TileEntityPylonMedium(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.PYLON_MEDIUM.get(), pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.TRIPLE;
    }

    @Override
    public Vec3[] getMountPos() {
        ForgeDirection dir = ForgeDirection.getOrientation(facingMeta());
        double height = 7.5D;
        return new Vec3[]{
                new Vec3(0.5, height, 0.5),
                new Vec3(0.5 + dir.offsetX, height, 0.5 + dir.offsetZ),
                new Vec3(0.5 + dir.offsetX * 2, height, 0.5 + dir.offsetZ * 2),
        };
    }

    @Override
    public double getMaxWireLength() {
        return 45;
    }

    @Override
    public boolean canConnect(Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return hasTransformer() && ForgeDirection.getOrientation(facingMeta()).getOpposite() == dir;
    }

    @Override
    protected int getCellMask(int index) {
        if (!hasTransformer()) return 0;
        return 1 << ForgeDirection.getOrientation(facingMeta()).getOpposite().toDirection().ordinal();
    }

    public boolean hasTransformer() {
        return getBlockState().getBlock() instanceof PylonMedium pylon && pylon.transformer;
    }
}
