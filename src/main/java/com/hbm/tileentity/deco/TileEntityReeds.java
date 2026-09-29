package com.hbm.tileentity.deco;

import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A do-nothing block entity carried by {@code plant_reeds} so {@link com.hbm.client.render.RenderReeds} can
 * draw the stem down through the whole water column, the way 1.7's {@code RenderReeds} did. A block model
 * reaches one block below its own cell at most; the column can be any depth.
 */
public class TileEntityReeds extends BlockEntity {

    public TileEntityReeds(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REEDS.get(), pos, state);
    }
}
