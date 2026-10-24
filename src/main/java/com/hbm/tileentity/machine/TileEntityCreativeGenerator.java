package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/**
 * A creative (infinite) power source for testing the network. Always full; pushes power into all six
 * neighbours each tick through its six-sided port.
 */
public class TileEntityCreativeGenerator extends TileEntityLoadedBase implements IEnergyProviderMK2 {

    public static final long MAX = Long.MAX_VALUE / 4L;

    public TileEntityCreativeGenerator(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_GENERATOR.get(), pos, state);
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCreativeGenerator gen) {
        gen.setupPowerPorts(gen.getPorts());
        gen.updateAllPorts();
        gen.providePower();
    }

    // An infinite source has no finite owned buffer.
    @Override public boolean ownsEnergyBuffer() { return false; }

    @Override
    public long getPower() {
        return MAX;
    }

    @Override
    public void setPower(long power) {
        // Infinite source: ignore drains
    }

    @Override
    public long getMaxPower() {
        return MAX;
    }

    @Override
    public long getProviderSpeed() {
        // 100 MHE/t per side, 1000× the original 100 kHE to feed the high-power machines (mining laser etc.).
        return 100_000_000L;
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }
}
