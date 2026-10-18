package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.network.TileEntityCableGauge;
import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import com.hbm.tileentity.network.TileEntityPipeGauge;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import com.hbm.tileentity.network.TileEntityRadioTorchBase;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import dev.golem.core.gametest.GameTests;

import java.util.List;

/**
 * Gauges, pumps, valves and Redstone-over-Radio blocks: 1.7's OpenComputers components on CC: Tweaked.
 */
public final class NetworkRows {

    private NetworkRows() {}

    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(TileEntityCableGauge.class, CableGaugePeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityPipeGauge.class, PipeGaugePeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityFluidCounterValve.class, FluidCounterValvePeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityRadioTelex.class, RadioTelexPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityRadioAUTOCAL.class, RadioAutocalPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityRadioTorchReader.class, RadioReaderPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityRadioTorchController.class, RadioControllerPeripheral::new, Reach.BLOCK),
            // the sender and the receiver
            Peripherals.row(TileEntityRadioTorchBase.class, RadioTorchPeripheral::new, Reach.BLOCK)
    );

    public static GameTests.Suite tests() {
        return com.hbm.gametest.computer.NetworkPeripheralGameTests.TESTS;
    }
}
//?}
