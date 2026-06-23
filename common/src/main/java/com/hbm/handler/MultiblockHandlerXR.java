package com.hbm.handler;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

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
                    if (a == ox && b == oy && c == oz) continue;
                    if (!world.getBlockState(pos.set(a, b, c)).canBeReplaced()) return false;
                    if (++count > 2000) return false;
                }
            }
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
                    else continue;

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

    public static int[] rotate(int[] dim, Direction dir) {
        if (dim == null) return null;
        return switch (dir) {

            case NORTH -> new int[]{dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]};
            case EAST -> new int[]{dim[0], dim[1], dim[5], dim[4], dim[2], dim[3]};
            case WEST -> new int[]{dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]};
            default -> dim;
        };
    }
}
