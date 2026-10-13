package com.hbm.tileentity.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.client.render.DecoModelRenderer;
import com.hbm.tileentity.ModDecoBlockEntities;

/**
 * A do-nothing block entity carried by the OBJ-drawn decorative blocks of this family (spotlights,
 * poles, tape recorder, ...) so {@code DecoModelRenderer} runs for them. The same invisible-block +
 * renderer pattern as {@code TileEntityDeco}; the renderer looks the model up from the block.
 */
public class TileEntityDecoModel extends BlockEntity {

    public TileEntityDecoModel(BlockPos pos, BlockState state) {
        super(ModDecoBlockEntities.DECO_MODEL.get(), pos, state);
    }
}
