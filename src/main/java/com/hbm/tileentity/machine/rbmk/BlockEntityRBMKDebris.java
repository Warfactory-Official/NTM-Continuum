package com.hbm.tileentity.machine.rbmk;

import com.hbm.tileentity.ModRBMKBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Render-only carrier for the four static RBMK wreck meshes. */
public final class BlockEntityRBMKDebris extends BlockEntity {
    public BlockEntityRBMKDebris(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_DEBRIS.get(), pos, state);
    }
}
