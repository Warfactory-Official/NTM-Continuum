package com.hbm.tileentity.network;

import net.minecraft.core.Direction;
import com.hbm.network.power.CableData;
import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.network.power.PowerGraph;
import com.hbm.network.power.PowerGraphProvider;
import com.hbm.api.tile.ILoadedTile;
import dev.golem.core.graph.Domains;
import dev.golem.core.graph.LevelNodeGraph;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.network.CableSwitch;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/**
 * The switchable conductor behind {@code cable_switch} and {@code cable_detector}: 1.7's
 * {@code TileEntityCableSwitch}, which only created its node while the block was ON. On the graph that is
 * the whole of it -- the cell exists exactly while the switch conducts, as the fluid valve does.
 */
public class TileEntityCableSwitch extends CompatBlockEntity implements IEnergyConductorMK2, ILoadedTile {

    public TileEntityCableSwitch(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.CABLE_SWITCH.get(), pos, state);
    }

    public boolean isOn() {
        return getBlockState().hasProperty(CableSwitch.ON) && getBlockState().getValue(CableSwitch.ON);
    }

    public void updateState() {
        syncCell();
        setChanged();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        syncCell();
    }

    private void syncCell() {
        if (!(level instanceof ServerLevel server)) return;
        LevelNodeGraph<CableData> graph = PowerGraph.get(server);
        long key = getBlockPos().asLong();
        if (!isOn()) {
            if (graph.containsCell(key)) graph.removeNode(key);
        } else if (!graph.containsCell(key)) {
            graph.addNode(key, PowerGraphProvider.INSTANCE.createData(getBlockState()), Domains.FACES_ALL);
        }
    }

    @Override
    public boolean canConnect(Direction dir) {
        return dir != null;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        PowerGraph.dropCells(this, getBlockPos());
    }

}
