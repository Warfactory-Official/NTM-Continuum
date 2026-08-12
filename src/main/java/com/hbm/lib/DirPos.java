package com.hbm.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A block position paired with a direction — a connection point for the node networks. */
public class DirPos {

    protected ForgeDirection dir;
    protected BlockPos pos;

    public DirPos(int x, int y, int z, ForgeDirection dir) {
        this.pos = new BlockPos(x, y, z);
        this.dir = dir;
    }

    public DirPos(BlockPos pos, ForgeDirection dir) {
        this.pos = pos;
        this.dir = dir;
    }

    public DirPos(BlockEntity te, ForgeDirection dir) {
        this.pos = te.getBlockPos();
        this.dir = dir;
    }

    public DirPos(double x, double y, double z, ForgeDirection dir) {
        this.pos = BlockPos.containing(x, y, z);
        this.dir = dir;
    }

    public ForgeDirection getDir() {
        return this.dir;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public boolean compare(int x, int y, int z) {
        return this.pos.getX() == x && this.pos.getY() == y && this.pos.getZ() == z;
    }
}
