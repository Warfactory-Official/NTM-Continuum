package com.hbm.interfaces;

import com.hbm.handler.ClimbableRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IClimbable {

    default Level world() {
        return ((BlockEntity) this).getLevel();
    }

    default BlockPos pos() {
        return ((BlockEntity) this).getBlockPos();
    }

    boolean isEntityInClimbAABB(LivingEntity entity);

    default void registerClimbable() {
        ClimbableRegistry.register(this);
    }

    default void unregisterClimbable() {
        ClimbableRegistry.unregister(this);
    }
}
