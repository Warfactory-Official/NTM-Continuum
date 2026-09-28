package com.hbm.tileentity.network;

import com.hbm.tileentity.ModEnergyBedrockBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The 1.7 {@code TileEntityCableBaseNT} of {@code red_cable_box_<size>}; its own type so the box renderer can bind to it. */
public class TileEntityCableBox extends TileEntityCableClassic {

    public TileEntityCableBox(BlockPos pos, BlockState state) {
        super(ModEnergyBedrockBlockEntities.ENERGY_CABLE_BOX.get(), pos, state);
    }
}
