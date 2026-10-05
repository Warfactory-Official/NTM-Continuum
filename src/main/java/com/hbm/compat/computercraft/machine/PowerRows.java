package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.machine.TileEntityChungus;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import com.hbm.tileentity.machine.TileEntityMachineTurbine;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
import dev.golem.core.gametest.GameTests;

import java.util.List;

/** Turbines, DFC, combustion engine and coker: 1.7's OpenComputers components on CC: Tweaked. */
public final class PowerRows {

    private PowerRows() {}

    // CELLS: the 1.7 block's makeExtra cells are TileEntityProxyCombo flagged power and/or fluid
    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(TileEntityMachineTurbine.class, SmallTurbinePeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityMachineLargeTurbine.class, LargeTurbinePeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityChungus.class, ChungusPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityMachineIndustrialTurbine.class, IndustrialTurbinePeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityMachineTurbineGas.class, GasTurbinePeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityCoreEmitter.class, CoreEmitterPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityCoreReceiver.class, CoreReceiverPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityCoreInjector.class, CoreInjectorPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityCoreStabilizer.class, CoreStabilizerPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityMachineCombustionEngine.class, CombustionEnginePeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityMachineCoker.class, CokerPeripheral::new, Reach.CELLS)
    );

    public static GameTests.Suite tests() {
        return com.hbm.gametest.computer.PowerPeripheralGameTests.TESTS;
    }
}
//?}
