package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;

/** Direction calculations shared by the six-way fan and the horizontal intake renderer. */
public final class IntakeFanDirections {

    private IntakeFanDirections() {}

    /** Exact shape of 1.12.2 {@code EnumFacing.getDirectionFromEntityLiving}. */
    public static Direction fromEntity(BlockPos pos, LivingEntity entity) {
        double dx = entity.getX() - (pos.getX() + 0.5D);
        double dy = entity.getY() + entity.getEyeHeight() - (pos.getY() + 0.5D);
        double dz = entity.getZ() - (pos.getZ() + 0.5D);
        double ax = Math.abs((float) dx);
        double ay = Math.abs((float) dy);
        double az = Math.abs((float) dz);

        if (ax > ay && ax > az) return dx < 0D ? Direction.WEST : Direction.EAST;
        if (az > ay) return dz < 0D ? Direction.NORTH : Direction.SOUTH;
        return dy < 0D ? Direction.DOWN : Direction.UP;
    }

    public static float intakeYaw(ForgeDirection direction) {
        return switch (direction) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
    }
}
