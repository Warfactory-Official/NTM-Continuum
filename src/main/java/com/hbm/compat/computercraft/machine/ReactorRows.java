package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.machine.TileEntityGeiger;
import com.hbm.tileentity.machine.TileEntityICF;
import com.hbm.tileentity.machine.TileEntityMachineRadarLarge;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import com.hbm.tileentity.machine.TileEntityMicrowave;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.machine.TileEntityReactorControl;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.hbm.tileentity.machine.TileEntityWatz;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import dev.golem.core.gametest.GameTests;

import java.util.List;

/** Reactors, radar, ground station, Geiger counter and microwave: 1.7's OpenComputers components on CC: Tweaked. */
public final class ReactorRows {

    private ReactorRows() {}

    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(TileEntityGeiger.class, GeigerPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityMicrowave.class, MicrowavePeripheral::new, Reach.BLOCK),
            // 1.7 makeExtra cells: TileEntityProxyCombo(true, true, true)
            Peripherals.row(TileEntityReactorZirnox.class, ZirnoxPeripheral::new, Reach.CELLS),
            // 1.7 ReactorResearch stamps no makeExtra cell
            Peripherals.row(TileEntityReactorResearch.class, ResearchReactorPeripheral::new, Reach.BLOCK),
            // 1.7 MachineReactorBreeding's cells are TileEntityProxyInventory, never a component
            Peripherals.row(TileEntityMachineReactorBreeding.class, BreedingReactorPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityReactorControl.class, ReactorControlPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityPWRController.class, PWRControllerPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityPileLoader.class, PileLoaderPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityPileControl.class, PileControlPeripheral::new, Reach.BLOCK),
            // 1.7 makeExtra cells: TileEntityProxyCombo().inventory().fluid()
            Peripherals.row(TileEntityWatz.class, WatzPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityICF.class, ICFPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityMachineRadarNT.class, RadarPeripheral::new, Reach.BLOCK),
            // 1.7 makeExtra cells: TileEntityProxyCombo().power()
            Peripherals.row(TileEntityMachineRadarLarge.class, RadarPeripheral::new, Reach.CELLS),
            // 1.7 MachineSatLink's cells are a flagless TileEntityProxyCombo, never a component
            Peripherals.row(TileEntityMachineSatLink.class, SatLinkPeripheral::new, Reach.BLOCK)
    );

    public static GameTests.Suite tests() {
        return com.hbm.gametest.computer.ReactorPeripheralGameTests.TESTS;
    }
}
//?}
