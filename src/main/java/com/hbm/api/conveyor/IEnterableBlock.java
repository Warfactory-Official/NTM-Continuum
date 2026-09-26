package com.hbm.api.conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** A diagonal entry has no side: {@code dir} null. */
public interface IEnterableBlock {

    /**
     * Returns true if the moving item can enter from the given side. When this happens, the
     * {@link IConveyorItem} calls {@link #onItemEnter} and despawns.
     */
    boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity);

    void onItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity);

    boolean canPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity);

    void onPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity);
}
