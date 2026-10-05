package com.hbm.tileentity.network;

import com.hbm.api.fluidmk2.DuctData;
import com.hbm.api.fluidmk2.FluidGraph;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.network.FluidValve;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.sound.ModSounds;
import com.hbm.uninos.graph.NodeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code TileEntityFluidCounterValve}: a valve that, while open, adds its net's per-tick
 * transfer to a running counter. Opened/closed by hand like the plain valve, or over Redstone-over-Radio.
 */
public class TileEntityFluidCounterValve extends TileEntityFluidSwitch implements IRORValueProvider, IRORInteractive {

    private long counter;

    public TileEntityFluidCounterValve(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.COUNTER_VALVE.get(), pos, state);
    }

    public long getCounter() {
        return counter;
    }

    /** 1.7 {@code resetCounter} (OpenComputers and RoR {@code reset}). */
    public void resetCounter() {
        counter = 0;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFluidCounterValve valve) {
        if (!(level instanceof ServerLevel server)) return;
        if (valve.isOpen() && valve.getFluidType() != Fluids.NONE) {
            NodeNetwork<DuctData> net = FluidGraph.get(server).networkAt(pos.asLong());
            if (net != null && net.tracker != 0) {
                valve.counter += net.tracker;
                valve.setChanged();
            }
        }
        // 1.7 networkPackNT(25) ran every tick regardless of flow, so a burst that ends before the boundary still shows.
        if (level.getGameTime() % 25 == 0) level.sendBlockUpdated(pos, state, state, 3);
    }

    /** Opens (1) or closes (0) the valve; 1.7's private {@code setState}, public for the computer peripheral. */
    public void setState(int state) {
        if (level == null) return;
        boolean open = state == 1;
        level.setBlock(getBlockPos(), getBlockState().setValue(FluidValve.OPEN, open), 3);
        level.playSound(null, getBlockPos(), ModSounds.REACTOR_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        updateState();
    }

    @Override
    protected void saveData(dev.golem.core.data.DataOut output) {
        super.saveData(output);
        output.putLong("counter", counter);
    }

    @Override
    protected void loadData(dev.golem.core.data.DataIn input) {
        super.loadData(input);
        this.counter = Math.max(input.getLongOr("counter", 0L), 0);
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "value").equals(name)) return String.valueOf(counter);
        if ((PREFIX_VALUE + "state").equals(name)) return String.valueOf(isOpen() ? 1 : 0);
        return null;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "value",
                PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "reset",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state",
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if (name.equals(PREFIX_FUNCTION + "reset")) {
            counter = 0;
            setChanged();
        } else if (name.equals(PREFIX_FUNCTION + "setstate")) {
            setState(IRORInteractive.parseInt(params[0], 0, 1));
        }
        return null;
    }
}
