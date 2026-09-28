package com.hbm.tileentity.network;

import com.hbm.tileentity.ModEnergyBedrockBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The 1.7 {@code TileEntityCableBaseNT} of {@code red_wire_coated}, under its own block-entity type. */
public class TileEntityWireCoated extends TileEntityCableClassic {

    public TileEntityWireCoated(BlockPos pos, BlockState state) {
        super(ModEnergyBedrockBlockEntities.ENERGY_WIRE_COATED.get(), pos, state);
    }
}
