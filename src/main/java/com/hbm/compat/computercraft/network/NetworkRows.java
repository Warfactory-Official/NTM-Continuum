package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.network.TileEntityCableGauge;
import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import com.hbm.tileentity.network.TileEntityFluidPump;
import com.hbm.tileentity.network.TileEntityPipeGauge;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import com.hbm.tileentity.network.TileEntityRadioTorchBase;
import dev.golem.core.gametest.GameTests;

import java.util.List;

/**
 * Gauges, pumps, valves and Redstone-over-Radio blocks: 1.7's OpenComputers components on CC: Tweaked. 1.7's ROR
 * controller, reader and AUTOCAL declare no component, so they have no row.
 */
public final class NetworkRows {

    private NetworkRows() {}

    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(TileEntityCableGauge.class, CableGaugePeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityPipeGauge.class, PipeGaugePeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityFluidPump.class, FluidPumpPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityFluidCounterValve.class, FluidCounterValvePeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityRadioTelex.class, RadioTelexPeripheral::new, Reach.BLOCK),
            // the sender and the receiver
            Peripherals.row(TileEntityRadioTorchBase.class, RadioTorchPeripheral::new, Reach.BLOCK)
    );

    public static GameTests.Suite tests() {
        return com.hbm.gametest.computer.NetworkPeripheralGameTests.TESTS;
    }
}
//?}
