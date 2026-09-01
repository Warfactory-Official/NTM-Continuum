package com.hbm.blocks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** The unmodified force and affected-volume arithmetic from the legacy fan. */
public final class FanPhysics {

    public static final int RANGE = 10;
    public static final double PUSH = 0.1D;

    private FanPhysics() {}

    /** Returns the per-tick signed force coefficient for one entity. */
    public static double coefficient(double distance, boolean falloff, boolean suck) {
        double coefficient = PUSH;
        if (falloff) coefficient *= 1.5D * (1D - distance / RANGE / 2D);
        return suck ? -coefficient : coefficient;
    }

    /** Reproduces the old zero-length-safe AABB followed by {@code grow(0.5, 0.5, 0.5)}. */
    public static AABB affectedBox(BlockPos pos, Direction direction, int effectiveRange) {
        double x = direction.getStepX() * effectiveRange;
        double y = direction.getStepY() * effectiveRange;
        double z = direction.getStepZ() * effectiveRange;
        double centreX = pos.getX() + 0.5D;
        double centreY = pos.getY() + 0.5D;
        double centreZ = pos.getZ() + 0.5D;
        return new AABB(centreX + Math.min(x, 0D), centreY + Math.min(y, 0D), centreZ + Math.min(z, 0D),
                centreX + Math.max(x, 0D), centreY + Math.max(y, 0D), centreZ + Math.max(z, 0D))
                .inflate(0.5D, 0.5D, 0.5D);
    }
}
