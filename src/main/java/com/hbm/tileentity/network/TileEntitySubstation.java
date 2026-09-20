package com.hbm.tileentity.network;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/**
 * The substation: a four-wire pylon whose footprint conducts. 1.7 held the core and the four diagonal
 * corners in one node with cable connections on the outer faces of those corners (world axes, whichever
 * way it faces); here that is five linked cells with the same faces open.
 */
public class TileEntitySubstation extends TileEntityPylonBase {

    public TileEntitySubstation(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.SUBSTATION.get(), pos, state);
    }

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.QUAD;
    }

    @Override
    public Vec3[] getMountPos() {
        double topOff = 5.25;
        Vec3 vec = new Vec3(1, 0, 0);
        switch (facingMeta()) {
            case 2, 3 -> vec = vec.yRot((float) Math.PI * 0.0F);
            case 4, 5 -> vec = vec.yRot((float) Math.PI * 0.5F);
            default -> { }
        }
        return new Vec3[]{
                new Vec3(0.5 + vec.x * 0.5, topOff, 0.5 + vec.z * 0.5),
                new Vec3(0.5 + vec.x * 1.5, topOff, 0.5 + vec.z * 1.5),
                new Vec3(0.5 - vec.x * 0.5, topOff, 0.5 - vec.z * 0.5),
                new Vec3(0.5 - vec.x * 1.5, topOff, 0.5 - vec.z * 1.5),
        };
    }

    @Override
    public Vec3 getConnectionPoint() {
        return new Vec3(getBlockPos().getX() + 0.5, getBlockPos().getY() + 5.25, getBlockPos().getZ() + 0.5);
    }

    @Override
    public double getMaxWireLength() {
        return 20;
    }

    @Override
    public boolean canConnect(com.hbm.lib.ForgeDirection dir) {
        return false;
    }

    @Override
    protected BlockPos[] getCellPos() {
        BlockPos p = getBlockPos();
        return new BlockPos[]{p, p.offset(1, 0, 1), p.offset(1, 0, -1), p.offset(-1, 0, 1), p.offset(-1, 0, -1)};
    }

    /** Index order of {@link #getCellPos}: core, (+1,+1), (+1,-1), (-1,+1), (-1,-1). */
    public static int cornerMask(int index) {
        return switch (index) {
            case 1 -> BlockMultiblockCore.MASK_EAST | BlockMultiblockCore.MASK_SOUTH;
            case 2 -> BlockMultiblockCore.MASK_EAST | BlockMultiblockCore.MASK_NORTH;
            case 3 -> BlockMultiblockCore.MASK_WEST | BlockMultiblockCore.MASK_SOUTH;
            case 4 -> BlockMultiblockCore.MASK_WEST | BlockMultiblockCore.MASK_NORTH;
            default -> BlockMultiblockCore.MASK_NONE;
        };
    }

    @Override
    protected int getCellMask(int index) {
        return cornerMask(index);
    }
}
