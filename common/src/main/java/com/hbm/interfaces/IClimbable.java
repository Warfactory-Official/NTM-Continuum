package com.hbm.interfaces;

import com.hbm.handler.ClimbableRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Implemented by a BlockEntity that exposes a climbable AABB (e.g. the fluid tank's side ladder).
 * Register in the block-entity's tick (both sides) and unregister in {@code setRemoved()}.
 *
 * <p>26.1 port: the climb is applied by {@link com.hbm.mixin.MixinLivingEntity} injecting into
 * {@code LivingEntity.onClimbable()} (the 1.12.2 original used Forge's {@code CheckLadderEvent}).
 */
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
