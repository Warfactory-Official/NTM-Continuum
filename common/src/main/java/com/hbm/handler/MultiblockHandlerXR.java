package com.hbm.handler;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Multiblock footprint helper, ported from HBM's {@code MultiblockHandlerXR}. Given a
 * {@link BlockDummyable#getDimensions() dimensions} array [UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT]
 * (oriented as if facing south) and a placement direction, it checks whether the footprint is free
 * ({@link #checkSpace}) and fills it with dummy blocks pointing back toward the core
 * ({@link #fillSpace}). The player-eye-occupancy check from the original is dropped (minor).
 */
public final class MultiblockHandlerXR {

    private MultiblockHandlerXR() {}

    public static boolean checkSpace(Level world, int x, int y, int z, int[] dim, int ox, int oy, int oz, ForgeDirection dir) {
        return checkSpace(world, x, y, z, dim, ox, oy, oz, dir.toDirection());
    }

    public static boolean checkSpace(Level world, int x, int y, int z, int[] dim, int ox, int oy, int oz, Direction dir) {
        if (dim == null || dim.length != 6 || dir == null) return false;

        int[] rot = rotate(dim, dir);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 0;

        for (int a = x - rot[4]; a <= x + rot[5]; a++) {
            for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                for (int c = z - rot[2]; c <= z + rot[3]; c++) {
                    if (a == ox && b == oy && c == oz) continue; // the clicked block's own cell counts as free
                    if (!world.getBlockState(pos.set(a, b, c)).canBeReplaced()) return false;
                    if (++count > 2000) return false;
                }
            }
        }

        // The old checkForPlayerEyePositions: refuse placement when a player would end up inside the
        // structure (26.1's suffocation logic would otherwise shove them out of the invisible cells).
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                x - rot[4], y - rot[1], z - rot[2],
                x + rot[5] + 1, y + rot[0] + 1, z + rot[3] + 1);
        for (net.minecraft.world.entity.player.Player player : world.players()) {
            if (box.contains(player.getEyePosition()) || box.intersects(player.getBoundingBox())) return false;
        }
        return true;
    }

    public static void fillSpace(Level world, int x, int y, int z, int[] dim, BlockDummyable block, ForgeDirection dir) {
        fillSpace(world, x, y, z, dim, block, dir.toDirection());
    }

    public static void fillSpace(Level world, int x, int y, int z, int[] dim, BlockDummyable block, Direction dir) {
        if (dim == null || dim.length != 6 || dir == null) return;

        int[] rot = rotate(dim, dir);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 0;

        BlockDummyable.safeRem = true;
        for (int a = x - rot[4]; a <= x + rot[5]; a++) {
            for (int b = y - rot[1]; b <= y + rot[0]; b++) {
                for (int c = z - rot[2]; c <= z + rot[3]; c++) {
                    int meta;
                    if (b < y) meta = ForgeDirection.DOWN.ordinal();
                    else if (b > y) meta = ForgeDirection.UP.ordinal();
                    else if (a < x) meta = ForgeDirection.WEST.ordinal();
                    else if (a > x) meta = ForgeDirection.EAST.ordinal();
                    else if (c < z) meta = ForgeDirection.NORTH.ordinal();
                    else if (c > z) meta = ForgeDirection.SOUTH.ordinal();
                    else continue; // the core cell itself

                    world.setBlock(pos.set(a, b, c), block.defaultBlockState().setValue(BlockDummyable.META, meta), 3);
                    if (++count > 2000) {
                        BlockDummyable.safeRem = false;
                        return;
                    }
                }
            }
        }
        BlockDummyable.safeRem = false;
    }

    /** Rotates the south-oriented dimensions to the placement direction. */
    public static int[] rotate(int[] dim, Direction dir) {
        if (dim == null) return null;
        return switch (dir) {
            //                       U       D       N       S       W       E
            case NORTH -> new int[]{dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]};
            case EAST -> new int[]{dim[0], dim[1], dim[5], dim[4], dim[2], dim[3]};
            case WEST -> new int[]{dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]};
            default -> dim; // SOUTH (and any vertical, which dummyables don't use)
        };
    }
}
