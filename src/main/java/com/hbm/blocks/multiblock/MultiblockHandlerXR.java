package com.hbm.blocks.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Footprint geometry for folded multiblocks: the cell set as a read, separate from any world write. */
public final class MultiblockHandlerXR {

    private MultiblockHandlerXR() {}

    public static boolean checkSpace(Level level, BlockPos origin, int[] dim, BlockPos placed, Direction dir) {
        if (dim == null || dim.length != 6) return false;
        int[] rot = rotate(dim, dir);
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = ox - rot[4]; a <= ox + rot[5]; a++)
            for (int b = oy - rot[1]; b <= oy + rot[0]; b++)
                for (int c = oz - rot[2]; c <= oz + rot[3]; c++) {
                    if (a == placed.getX() && b == placed.getY() && c == placed.getZ()) continue;
                    if (!level.getBlockState(pos.set(a, b, c)).canBeReplaced()) return false;
                }
        // 1.7 checked blocks only: a player standing in the footprint does not stop the placement (vanilla already
        // keeps the clicked cell clear of entities). The machine is built around them, as in 1.7; one with
        // walkOutAfterPlacing (hbm-space's internalPlayers) lets them walk out through its cells.
        return true;
    }

    /**
     * Enumerate a box footprint without touching the world. The {@link BlockPos} handed to the visitor is
     * mutable and reused across cells.
     */
    public static void visitBox(BlockPos origin, int[] dim, Direction dir, BoxVisitor out) {
        if (dim == null || dim.length != 6) return;
        int[] rot = rotate(dim, dir);
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int a = ox - rot[4]; a <= ox + rot[5]; a++)
            for (int b = oy - rot[1]; b <= oy + rot[0]; b++)
                for (int c = oz - rot[2]; c <= oz + rot[3]; c++) {
                    Direction facingCore;
                    if (b < oy) facingCore = Direction.DOWN;
                    else if (b > oy) facingCore = Direction.UP;
                    else if (a < ox) facingCore = Direction.WEST;
                    else if (a > ox) facingCore = Direction.EAST;
                    else if (c < oz) facingCore = Direction.NORTH;
                    else if (c > oz) facingCore = Direction.SOUTH;
                    else continue;
                    out.cell(pos.set(a, b, c), facingCore);
                }
    }

    /**
     * Assembles a complete multiblock where {@code setPlacedBy} never runs: structure and feature
     * generation have no player, and like 1.7's {@code Component.placeCore} apply no {@code checkRequirement}.
     */
    public static void placeAssembled(Level level, BlockPos core, BlockMultiblockCore block, Direction dir) {
        level.setBlock(core, block.defaultBlockState().setValue(BlockMultiblockCore.FACING, dir), 3);
        block.fillSpace(level, core, dir);
    }

    /**
     * Re-notify every footprint cell's neighbours once the whole structure exists: a cable placed before
     * the multiblock latches "no connection" against the incomplete footprint.
     */
    public static void refreshSpace(Level level, BlockPos origin, int[] dim, Direction dir) {
        if (dim == null || dim.length != 6) return;
        int[] rot = rotate(dim, dir);
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
        for (int a = ox - rot[4]; a <= ox + rot[5]; a++)
            for (int b = oy - rot[1]; b <= oy + rot[0]; b++)
                for (int c = oz - rot[2]; c <= oz + rot[3]; c++) {
                    cell.set(a, b, c);
                    level.getBlockState(cell).updateNeighbourShapes(level, cell, 3);
                }
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

    /**
     * 1.7's {@code BlockDummyable#bounding} silhouette in one cell's frame; one implementation for both
     * bases, since a second rotation table would drift silently.
     */
    /**
     * The silhouette translated into a cell's frame and NOT rotated, as 1.7 {@code BlockDummyable#getShape}
     * moves the authored boxes by the core offset and leaves their orientation alone, so a machine whose
     * box is not rotationally symmetric keeps the same hitbox at all four facings. A machine that wants
     * its box to turn with it says so.
     */
    public static VoxelShape boundingSilhouette(List<AABB> bounding, int dx, int dy, int dz) {
        return boundingSilhouette(bounding, Direction.SOUTH, dx, dy, dz);
    }

    public static VoxelShape boundingSilhouette(List<AABB> bounding, Direction rot, int dx, int dy, int dz) {
        VoxelShape shape = Shapes.empty();
        for (AABB aabb : bounding) {
            shape = Shapes.joinUnoptimized(shape,
                    Shapes.create(rotateAabb(aabb, rot).move(dx + 0.5, dy, dz + 0.5)), BooleanOp.OR);
        }
        return shape.optimize();
    }

    /** Rotates a core-frame box about the vertical axis, {@code rot} read as a rotation away from south. */
    public static AABB rotateAabb(AABB box, Direction rot) {
        return switch (rot) {
            case WEST -> new AABB(box.minZ, box.minY, -box.maxX, box.maxZ, box.maxY, -box.minX);
            case NORTH -> new AABB(-box.maxX, box.minY, -box.maxZ, -box.minX, box.maxY, -box.minZ);
            case EAST -> new AABB(-box.maxZ, box.minY, box.minX, -box.minZ, box.maxY, box.maxX);
            default -> box;
        };
    }

    /** What {@link #visitBox} reports. See its note about the mutable position. */
    @FunctionalInterface public interface BoxVisitor { void cell(BlockPos pos, Direction facingCore); }
}
