package com.hbm.tileentity.network;

import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.blocks.network.ConnectorRedWire;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/** The wall connector: a single 10 m wire, conducting only into the block it is mounted on. */
public class TileEntityConnector extends TileEntityPylonBase {

    public TileEntityConnector(BlockPos pos, BlockState state) {
        this(ModEnergyNetBlockEntities.CONNECTOR.get(), pos, state);
    }

    protected TileEntityConnector(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 1.7's metadata: the side of the neighbour that was clicked, so the wall is on its opposite. */
    public ForgeDirection placedSide() {
        return ForgeDirection.getOrientation(getBlockState().getValue(ConnectorRedWire.FACING));
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.SINGLE;
    }

    @Override
    public Vec3[] getMountPos() {
        return new Vec3[]{new Vec3(0.5, 0.5, 0.5)};
    }

    @Override
    public double getMaxWireLength() {
        return 10;
    }

    @Override
    public boolean canConnect(ForgeDirection dir) { // i've about had it with your fucking bullshit
        return placedSide().getOpposite() == dir;
    }

    @Override
    protected int getCellMask(int index) {
        return 1 << placedSide().getOpposite().toDirection().ordinal();
    }
}
