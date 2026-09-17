package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.util.ExponentialMovingAverage;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fluid duct that reports what passes through it. Ported from 1.12.2's nested
 * {@code FluidDuctGauge.TileEntityPipeGauge}.
 *
 * <p>Extends the ordinary duct, so it carries a fluid type, joins that type's network, and is picked up
 * by the duct block's type-spreading walk exactly like any other segment. On top of that it reads the
 * fluid graph's own running total for the network its cell belongs to, into a per-tick figure and a
 * smoothed per-second one.
 *
 * <p>The per-second figure is what the look overlay draws, and it only changes on the 20-tick boundary,
 * so the client update is pushed there rather than every tick as the fork does. The per-tick figure is
 * read over redstone-over-radio, which is answered on the server, so it is never synced at all.
 */
public class TileEntityPipeGauge extends TileEntityFluidDuctNT implements IRORValueProvider {

    private long deltaTick;
    private long deltaSecond;
    public long lastSecond;
    private final ExponentialMovingAverage secondEMA = new ExponentialMovingAverage(0.05);

    public TileEntityPipeGauge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_GAUGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPipeGauge gauge) {
        if (!(level instanceof ServerLevel server) || gauge.getFluidType() == Fluids.NONE) return;

        NodeNetwork<?> net = FluidGraph.get(server).networkAt(pos.asLong());
        if (net == null) return;

        gauge.deltaTick = net.tracker;
        if (level.getGameTime() % 20L == 0) {
            gauge.secondEMA.next(gauge.deltaSecond);
            gauge.deltaSecond = 0;

            long smoothed = gauge.secondEMA.getValue();
            if (smoothed != gauge.lastSecond) {
                gauge.lastSecond = smoothed;
                gauge.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
        }
        gauge.deltaSecond += gauge.deltaTick;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("lastSecond", lastSecond);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        lastSecond = input.getLongOr("lastSecond", 0L);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "deltatick", PREFIX_VALUE + "deltasecond"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick;
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + lastSecond;
        return null;
    }
}
