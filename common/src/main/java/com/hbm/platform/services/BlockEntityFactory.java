package com.hbm.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Factory for a {@link BlockEntity}. Vanilla's {@code BlockEntityType.BlockEntitySupplier} and the
 * {@code BlockEntityType} constructor are private, so common code can't build a type directly; this
 * common functional interface is bridged to each loader's API by
 * {@link IPlatformHelper#blockEntityType}.
 */
@FunctionalInterface
public interface BlockEntityFactory<T extends BlockEntity> {
    T create(BlockPos pos, BlockState state);
}
