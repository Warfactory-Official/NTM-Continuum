package com.hbm.blocks.network;

import com.hbm.items.tool.ItemConveyorWand.ConveyorType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code blocks/network/BlockConveyorDouble}: two lanes, the item snapping to whichever half it sits in. */
public class BlockConveyorDouble extends BlockConveyorBendable {

    public BlockConveyorDouble(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code damageDropped}: the double wand. */
    @Override
    protected ConveyorType wandType() {
        return ConveyorType.DOUBLE;
    }

    @Override
    protected Snap snap(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);

        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;

        if (dir.getStepX() != 0) {
            posX = clamped.x;
            posZ += clamped.z > posZ ? 0.25 : -0.25;
        }
        if (dir.getStepZ() != 0) {
            posZ = clamped.z;
            posX += clamped.x > posX ? 0.25 : -0.25;
        }

        return new Snap(new Vec3(posX, pos.getY() + 0.25, posZ), clamped);
    }
}
