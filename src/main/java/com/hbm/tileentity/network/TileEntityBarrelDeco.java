package com.hbm.tileentity.network;

import com.hbm.tileentity.ModFluidNetBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A do-nothing block entity that exists so the effect barrels (red/pink/lox/taint/yellow/vitrified)
 * can be drawn by the {@code barrel.obj} renderer. 1.7 drew them with a block
 * rendering handler that needed no tile; the port has no OBJ baked-model path, so the renderer needs a
 * block entity to hang from.
 */
public class TileEntityBarrelDeco extends CompatBlockEntity {

    public TileEntityBarrelDeco(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.BARREL_DECO.get(), pos, state);
    }
}
