package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.uninos.graph.NodeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cable that reports what passes through it. Ported from 1.12.2's nested
 * {@code BlockCableGauge.TileEntityCableGauge}.
 *
 * <p>It conducts exactly as an ordinary cable does — its block mints the same power-graph cell — and
 * reads that network's throughput total, so the reading is the whole network's rather than this block's.
 * Two numbers come out of that: the current tick's total, and the previous whole second's, accumulated
 * and rolled over on the 20-tick boundary.
 *
 * <p><b>Deviation from the fork.</b> There the gauge extends the cable tile; here it extends
 * {@link TileEntityLoadedBase}, because the readout has to reach the client for the look overlay and only
 * that base carries the packet sync. The conduction contract is identical either way — the cell is the
 * block's, and nothing in the power engine tests for the cable class.
 */
public class TileEntityCableGauge extends TileEntityLoadedBase
        implements IEnergyConductorMK2, ILoadedTile, IRORValueProvider {

    public long deltaTick = 10;
    public long deltaLastSecond;
    private long deltaSecond;

    public TileEntityCableGauge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_GAUGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCableGauge gauge) {
        if (level instanceof ServerLevel server) {
            NodeNetwork<?> net = PowerGraph.get(server).networkAt(pos.asLong());
            if (net != null) {
                gauge.deltaTick = net.tracker;
                if (level.getGameTime() % 20 == 0) {
                    gauge.deltaLastSecond = gauge.deltaSecond;
                    gauge.deltaSecond = 0;
                }
                gauge.deltaSecond += gauge.deltaTick;
            }
        }

        gauge.networkPackNT();
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

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(deltaTick);
        buf.writeLong(deltaLastSecond);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        deltaTick = Math.max(buf.readLong(), 0L);
        deltaLastSecond = Math.max(buf.readLong(), 0L);
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
