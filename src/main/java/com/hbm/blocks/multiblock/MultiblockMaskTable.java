package com.hbm.blocks.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Arrays;

/** Baked local-cell face masks. A cell absent from the table is not part of the core's footprint. */
public final class MultiblockMaskTable {

    public static final int NOT_A_CELL = -1;
    private final byte[] masks;
    private final int minX, minY, minZ, sizeX, sizeY, sizeZ;

    private MultiblockMaskTable(byte[] masks, int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
        this.masks = masks;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public static MultiblockMaskTable bake(BlockMultiblockCore block) {
        Bounds bounds = new Bounds();
        block.visitCells(BlockPos.ZERO, Direction.SOUTH, bounds);
        bounds.include(0, 0, 0);
        int sx = bounds.maxX - bounds.minX + 1, sy = bounds.maxY - bounds.minY + 1, sz = bounds.maxZ - bounds.minZ + 1;
        byte[] masks = new byte[sx * sy * sz];
        Arrays.fill(masks, (byte) NOT_A_CELL);
        MultiblockMaskTable table = new MultiblockMaskTable(masks, bounds.minX, bounds.minY, bounds.minZ, sx, sy, sz);
        block.visitCells(BlockPos.ZERO, Direction.SOUTH, (pos, mask) -> table.put(pos.getX(), pos.getY(), pos.getZ(), mask));
        table.put(0, 0, 0, block.coreMask());
        return table;
    }

    public static int localX(int dx, int dy, int dz, Direction facing) {
        return switch (facing) { case WEST -> dz; case NORTH -> -dx; case EAST -> -dz; default -> dx; };
    }

    public static int localZ(int dx, int dy, int dz, Direction facing) {
        return switch (facing) { case WEST -> -dx; case NORTH -> -dz; case EAST -> dx; default -> dz; };
    }

    public static int worldX(int lx, int ly, int lz, Direction facing) {
        return switch (facing) { case WEST -> -lz; case NORTH -> -lx; case EAST -> lz; default -> lx; };
    }

    public static int worldZ(int lx, int ly, int lz, Direction facing) {
        return switch (facing) { case WEST -> lx; case NORTH -> -lz; case EAST -> -lx; default -> lz; };
    }

    private void put(int x, int y, int z, int mask) {
        if (mask < 0 || mask > 0b111111) throw new IllegalArgumentException("invalid multiblock face mask " + mask);
        masks[index(x, y, z)] = (byte) mask;
    }

    private boolean outside(int x, int y, int z) {
        return x < minX || y < minY || z < minZ || x >= minX + sizeX || y >= minY + sizeY || z >= minZ + sizeZ;
    }

    private int index(int x, int y, int z) {
        return ((y - minY) * sizeZ + z - minZ) * sizeX + x - minX;
    }

    public int maskAtLocal(int x, int y, int z) {
        return outside(x, y, z) ? NOT_A_CELL : masks[index(x, y, z)];
    }

    private static final class Bounds implements BlockMultiblockCore.CellVisitor {
        private int minX, minY, minZ, maxX, maxY, maxZ;
        @Override public void cell(BlockPos pos, int mask) { include(pos.getX(), pos.getY(), pos.getZ()); }
        private void include(int x, int y, int z) {
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        }
    }
}
