package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/**
 * A creative (infinite) fluid source for testing the network, the fluid analogue of
 * {@link TileEntityCreativeGenerator}. Holds a single {@code WATER} sending tank kept permanently full;
 * offers it on all six sides.
 */
public class TileEntityCreativeFluidSource extends TileEntityLoadedBase implements IFluidStandardSenderMK2 {

    public static final int CAPACITY = 1_000_000;
    private final FluidTankNTM tank = new FluidTankNTM(Fluids.WATER, CAPACITY);

    public TileEntityCreativeFluidSource(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_FLUID_SOURCE.get(), pos, state);
        tank.setFill(CAPACITY);
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCreativeFluidSource source) {
        source.tank.setFill(CAPACITY); // infinite: refill before providing
        source.setupFluidPorts(source.getPorts());
        source.updatePortFIFO();
    }

    @Override
    public void useUpFluid(FluidType type, int pressure, long amount) {
        // infinite source, never actually drains
    }

    @Override
    public @NonNull FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public long getProviderSpeed(FluidType type, int pressure) {
        return 100_000L;
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
