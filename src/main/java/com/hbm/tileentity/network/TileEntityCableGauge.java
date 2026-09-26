package com.hbm.tileentity.network;

import com.hbm.network.power.CableData;
import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.network.power.PowerGraph;
import com.hbm.network.power.PowerGraphProvider;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import dev.golem.core.graph.Domains;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.graph.LevelNodeGraph;
import dev.golem.core.graph.NodeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.network.BlockCableGauge;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/**
 * 1.7's {@code BlockCableGauge.TileEntityCableGauge}: a cable that reads its net's energy tracker -- the
 * HE the last round delivered -- and keeps a one-second total for the look overlay and Redstone-over-Radio.
 */
public class TileEntityCableGauge extends SyncedBlockEntity implements IEnergyConductorMK2, IRORValueProvider {

    public long deltaTick = 0;
    private long deltaSecond = 0;
    public long deltaLastSecond = 0;

    public TileEntityCableGauge(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.CABLE_GAUGE.get(), pos, state);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel server) {
            LevelNodeGraph<CableData> graph = PowerGraph.get(server);
            long key = getBlockPos().asLong();
            if (!graph.containsCell(key)) {
                graph.addNode(key, PowerGraphProvider.INSTANCE.createData(getBlockState()), Domains.FACES_ALL);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCableGauge gauge) {
        if (!(level instanceof ServerLevel server)) return;
        NodeNetwork<CableData> net = PowerGraph.get(server).networkAt(pos.asLong());
        if (net != null) {
            gauge.deltaTick = net.tracker;
            if (level.getGameTime() % 20 == 0) {
                gauge.deltaLastSecond = gauge.deltaSecond;
                gauge.deltaSecond = 0;
            }
            gauge.deltaSecond += gauge.deltaTick;
        }
        gauge.syncState();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeLong(deltaTick);
        buf.writeLong(deltaLastSecond);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        deltaTick = Math.max(buf.readLong(), 0);
        deltaLastSecond = Math.max(buf.readLong(), 0);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        PowerGraph.dropCells(this, getBlockPos());
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "deltatick", PREFIX_VALUE + "deltasecond"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick;
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaLastSecond;
        return null;
    }
}
