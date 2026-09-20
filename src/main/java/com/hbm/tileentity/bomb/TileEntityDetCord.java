package com.hbm.tileentity.bomb;

import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModExplosivesBlockEntities;

/**
 * Exists only so det cord has a block-entity renderer to draw its cable OBJ; 1.7 drew it from an
 * {@code ISimpleBlockRenderingHandler} and had no tile. Holds no state and never ticks.
 */
public class TileEntityDetCord extends CompatBlockEntity {

    public TileEntityDetCord(BlockPos pos, BlockState state) {
        super(ModExplosivesBlockEntities.DET_CORD.get(), pos, state);
    }
}
