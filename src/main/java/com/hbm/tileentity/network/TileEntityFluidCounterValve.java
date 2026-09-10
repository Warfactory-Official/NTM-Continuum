package com.hbm.tileentity.network;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.network.FluidValve;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A valve that totals everything its network has moved since it was last reset. Ported from 1.12.2's
 * {@code TileEntityFluidCounterValve}.
 *
 * <p>It is an ordinary valve first — same gate, same typing, same connection rules — with a running total
 * on top, accumulated from {@code FluidNetMK2.fluidTracker} while the gate is open. Closing the valve
 * tears its node down, so a closed counter stops counting rather than reading a network it is no longer
 * part of.
 *
 * <p>Redstone-over-radio can both read the total and work the gate, and {@code reset} banks the current
 * tick before zeroing so nothing in flight is lost.
 */
public class TileEntityFluidCounterValve extends TileEntityFluidValve
        implements IRORValueProvider, IRORInteractive {

    private long counter;

    public TileEntityFluidCounterValve(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_COUNTER_VALVE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            TileEntityFluidCounterValve valve) {
        TileEntityFluidValve.serverTick(level, pos, state, valve);
        valve.accumulate();
    }

    private void accumulate() {
        if (node != null && node.net != null && getFluidType() != Fluids.NONE) {
            counter += node.net.fluidTracker;
        }
    }

    public long getCounter() {
        return counter;
    }

    private void setState(int open) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(FluidValve.OPEN)) return;

        boolean shouldOpen = open == 1;
        if (state.getValue(FluidValve.OPEN) == shouldOpen) return;

        level.setBlock(worldPosition, state.setValue(FluidValve.OPEN, shouldOpen), 3);
        level.playSound(null, worldPosition, ModSounds.REACTOR_START.get(), SoundSource.BLOCKS,
                1.0F, shouldOpen ? 1.0F : 0.85F);
        level.updateNeighborsAt(worldPosition, state.getBlock());
        updateState();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("counter", counter);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        counter = input.getLongOr("counter", 0L);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "value", PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "reset",
                PREFIX_FUNCTION + "setState" + NAME_SEPARATOR + "state"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "value").equals(name)) return String.valueOf(counter);
        if ((PREFIX_VALUE + "state").equals(name)) return String.valueOf(isOpen() ? 1 : 0);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "reset").equals(name)) {
            accumulate();
            counter = 0;
            setChanged();
        } else if ((PREFIX_FUNCTION + "setState").equals(name) && params.length > 0) {
            setState(IRORInteractive.parseInt(params[0], 0, 1));
        }
        return null;
    }
}
