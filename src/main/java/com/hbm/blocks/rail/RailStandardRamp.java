package com.hbm.blocks.rail;

import dev.golem.core.multiblock.MultiblockFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class RailStandardRamp extends BlockRailNTM {

    private static final int[] DIMENSIONS = {0, 0, 2, 2, 1, 0};
    // 1.7 ctor bounding: five 1-long steps 0.1 -> 0.9 along the run; the run is lz in the SOUTH frame and rises toward -lz
    private static final double[] STEP_HEIGHTS = {0.9D, 0.7D, 0.5D, 0.3D, 0.1D};

    public RailStandardRamp(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    /* 1.7's boxes reach only the lower layer; the cells above stay empty. */
    @Override
    public @Nullable VoxelShape cellShape(int lx, int ly, int lz, Direction facing, boolean open,
                                          boolean forCollision) {
        if (ly > 0) return Shapes.empty();
        return Shapes.box(0, 0, 0, 1, STEP_HEIGHTS[lz + 2], 1);
    }

    /* Very simple function determining the snapping position and adding the motion value to it, if desired. */
    @Override
    public void getTravelLocation(Level level, int x, int y, int z, double trainX, double trainY, double trainZ, double motionX, double motionY, double motionZ, double speed, RailContext info, MoveContext context) {
        Owner owner = info.owner;
        int cX = owner.coreX;
        int cY = owner.coreY;
        int cZ = owner.coreZ;
        Direction dir = owner.facing;
        Direction rot = dir.getClockWise();

        if (dir == Direction.EAST || dir == Direction.WEST) {
            double targetX = trainX;
            if (motionX > 0) {
                targetX += speed;
                info.yaw(-90F);
            } else {
                targetX -= speed;
                info.yaw(90F);
            }
            double dist = (cX + 0.5 - targetX + 2.5) / 5;
            double vecX = Mth.clamp(targetX, cX - 2, cX + 3);
            double vecY = Mth.clamp(dir == Direction.EAST ? cY + dist : cY + 1 - dist, cY, cY + 1) + 0.1875;
            info.dist(Math.abs(targetX - vecX) * Math.signum(speed));
            info.pos(cX + (motionX * speed > 0 ? 3 : -3), cY + (motionX * speed > 0 ^ dir == Direction.EAST ? 1 : 0), cZ);
            info.at(vecX, vecY, cZ + 0.5 + rot.getStepZ() * 0.5);
        } else {
            double targetZ = trainZ;
            if (motionZ > 0) {
                targetZ += speed;
                info.yaw(0F);
            } else {
                targetZ -= speed;
                info.yaw(180F);
            }
            double dist = (cZ + 0.5 - targetZ + 2.5) / 5;
            double vecY = Mth.clamp(dir == Direction.SOUTH ? cY + dist : cY + 1 - dist, cY, cY + 1) + 0.1875;
            double vecZ = Mth.clamp(targetZ, cZ - 2, cZ + 3);
            info.dist(Math.abs(targetZ - vecZ) * Math.signum(speed));
            info.pos(cX, cY + (motionZ * speed > 0 ^ dir == Direction.SOUTH ? 1 : 0), cZ + (motionZ * speed > 0 ? 3 : -3));
            info.at(cX + 0.5 + rot.getStepX() * 0.5, vecY, vecZ);
        }
    }

    @Override
    public TrackGauge getGauge(Level level, int x, int y, int z) {
        return TrackGauge.STANDARD;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockFootprint.visitBox(core.above(), getDimensions(), facing, visitor);
    }

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction dir, BlockPos placed) {
        BlockPos origin = core;
        return MultiblockFootprint.checkSpace(level, origin, getDimensions(), placed, dir)
                && MultiblockFootprint.checkSpace(level, origin.above(), getDimensions(), placed, dir);
    }
}
