package com.hbm.tileentity.deco;

import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A do-nothing block entity that exists only so an OBJ-rendered decorative block ({@code BlockDecoObj})
 * can carry a {@link com.hbm.client.render.DecoObjRenderer}. The old mod baked these into static custom
 * models ({@code StaticDecoBakedModels}); 26.1 has no cross-loader static-baked-model seam, so — like the
 * machines — the model is drawn every frame by a {@code BlockEntityRenderer} keyed on this BE's block.
 * Ported behaviourally from {@code TileEntityDecoBlock} (which itself only widened the render distance).
 */
public class TileEntityDeco extends BlockEntity {

    public TileEntityDeco(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECO.get(), pos, state);
    }
}
