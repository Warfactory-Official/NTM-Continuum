package com.hbm.util;

import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;

/**
 * The 1.7.10 {@code fauxpointtwelve.Rotation} + {@code BlockPos.rotate} pair the door math is written
 * against, kept so the open ranges and per-cell bounds land on exactly the cells the original
 * put them on. The folded multiblock's own local frame is not used here: the door code
 * rotates twice (once for ranges, once with an extra quarter turn for bounds) and the two disagree
 * with each other by design.
 */
public enum DoorRotation {
    NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90;

    public DoorRotation add(DoorRotation other) {
        return switch (other) {
            case CLOCKWISE_180 -> switch (this) {
                case NONE -> CLOCKWISE_180;
                case CLOCKWISE_90 -> COUNTERCLOCKWISE_90;
                case CLOCKWISE_180 -> NONE;
                case COUNTERCLOCKWISE_90 -> CLOCKWISE_90;
            };
            case COUNTERCLOCKWISE_90 -> switch (this) {
                case NONE -> COUNTERCLOCKWISE_90;
                case CLOCKWISE_90 -> NONE;
                case CLOCKWISE_180 -> CLOCKWISE_90;
                case COUNTERCLOCKWISE_90 -> CLOCKWISE_180;
            };
            case CLOCKWISE_90 -> switch (this) {
                case NONE -> CLOCKWISE_90;
                case CLOCKWISE_90 -> CLOCKWISE_180;
                case CLOCKWISE_180 -> COUNTERCLOCKWISE_90;
                case COUNTERCLOCKWISE_90 -> NONE;
            };
            default -> this;
        };
    }

    /** {@code Rotation.getBlockRotation(ForgeDirection)}. */
    public static DoorRotation getBlockRotation(ForgeDirection dir) {
        return switch (dir) {
            case SOUTH -> CLOCKWISE_180;
            case EAST -> COUNTERCLOCKWISE_90;
            case WEST -> CLOCKWISE_90;
            default -> NONE;
        };
    }

    /** {@code BlockPos.rotate(Rotation)} of the 1.7 shim, on a relative offset. */
    public BlockPos rotate(int x, int y, int z) {
        return switch (this) {
            case CLOCKWISE_90 -> new BlockPos(-z, y, x);
            case CLOCKWISE_180 -> new BlockPos(-x, y, -z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, -x);
            default -> new BlockPos(x, y, z);
        };
    }
}
