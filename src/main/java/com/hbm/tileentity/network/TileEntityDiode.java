package com.hbm.tileentity.network;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.energymk2.PowerGraphProvider;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.ModEnergyNetBlockEntities;
import com.hbm.uninos.graph.LevelNodeGraph;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code CableDiode.TileEntityDiode}, deprecated: a plain cable on every face. A diode from an older save has no
 * cell yet and gets one when it loads.
 */
public class TileEntityDiode extends CompatBlockEntity implements IEnergyConductorMK2, ILoadedTile {

    public TileEntityDiode(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.CABLE_DIODE.get(), pos, state);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<CableData> graph = PowerGraph.get(server);
        long key = getBlockPos().asLong();
        if (!graph.containsCell(key)) graph.addNode(key, PowerGraphProvider.INSTANCE.createData(getBlockState()), MachineCaps.FACE_MASK_ALL);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        PowerGraph.dropCells(this, getBlockPos());
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
