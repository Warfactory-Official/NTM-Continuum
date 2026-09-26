package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.network.power.PowerGraph;
import com.hbm.api.tile.ILoadedTile;
import net.minecraft.core.BlockPos;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/**
 * The classic / paintable cables' marker block entity: exactly {@code TileEntityCableBaseNT} (the cell
 * lives with the block, this only says "a cable is here" to its neighbours) under this family's types.
 */
public class TileEntityCableClassic extends CompatBlockEntity implements IEnergyConductorMK2, ILoadedTile {

    public TileEntityCableClassic(BlockPos pos, BlockState state) {
        this(ModEnergyNetBlockEntities.RED_CABLE_CLASSIC.get(), pos, state);
    }

    protected TileEntityCableClassic(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        PowerGraph.dropCells(this, getBlockPos());
    }

}
