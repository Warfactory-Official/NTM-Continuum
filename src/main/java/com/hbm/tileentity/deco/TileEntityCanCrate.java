package com.hbm.tileentity.deco;

import com.hbm.tileentity.ModDecoCrateBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The anchor of the conserve crate's OBJ. 1.7 drew {@code conservecrate.obj} into the chunk mesh through
 * {@code RenderConserve}, which a modern port has no equivalent of; like the supply crate that shares the
 * mesh, it is drawn by a block entity renderer instead. Holds nothing.
 */
public class TileEntityCanCrate extends CompatBlockEntity {

    public TileEntityCanCrate(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.CRATE_CAN.get(), pos, state);
    }
}
