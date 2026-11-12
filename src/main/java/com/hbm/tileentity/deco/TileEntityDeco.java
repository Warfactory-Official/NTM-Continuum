package com.hbm.tileentity.deco;

import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A do-nothing block entity that lets {@link com.hbm.client.render.DecoObjRenderer} draw the boxcar, boat and
 * satellite decorations, as 1.7's {@code TileEntityDecoBlock} let {@code RenderDecoBlock} draw them.
 */
public class TileEntityDeco extends BlockEntity {

    public TileEntityDeco(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECO.get(), pos, state);
    }
}
