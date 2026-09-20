package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code blocks/network/BlockConveyorExpress}: three times the belt speed. */
public class BlockConveyorExpress extends BlockConveyorBendable {

    public BlockConveyorExpress(Properties properties) {
        super(properties);
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        return super.getTravelLocation(level, pos, itemPos, speed * 3);
    }
}
