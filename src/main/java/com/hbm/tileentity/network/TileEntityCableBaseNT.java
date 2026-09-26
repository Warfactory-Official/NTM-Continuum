package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.network.power.PowerGraph;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A power cable: a pure conductor, and nothing more. The conduction is a cell of the level's power
 * graph, minted and dropped by {@link com.hbm.blocks.network.BlockCable} with the block itself, so
 * this carries no node, no power and no tick. What is left is the marker a neighbour reads to decide
 * whether to draw an arm towards us, and something for the renderer to hang off.
 */
public class TileEntityCableBaseNT extends BlockEntity implements IEnergyConductorMK2, ILoadedTile {

    public TileEntityCableBaseNT(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RED_CABLE.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        PowerGraph.dropCells(this, getBlockPos());
    }

}
