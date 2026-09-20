package com.hbm.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.client.render.SecurityObjRenderer;

/**
 * A do-nothing block entity so the OBJ-drawn perimeter blocks (barbed wire, spikes) can carry a
 * {@code SecurityObjRenderer}. 1.7.10 drew these with a static {@code ISimpleBlockRenderingHandler};
 * the port has no cross-loader static OBJ block model, so — like {@code TileEntityDeco} — the mesh
 * is drawn by a block-entity renderer keyed on this entity's block.
 */
public class TileEntitySecurityObj extends BlockEntity {

    public TileEntitySecurityObj(BlockPos pos, BlockState state) {
        super(ModSecurityBlockEntities.SECURITY_OBJ.get(), pos, state);
    }
}
