package com.hbm.compat.computercraft.albion;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.machine.albion.TileEntityPADetector;
import com.hbm.tileentity.machine.albion.TileEntityPADipole;
import com.hbm.tileentity.machine.albion.TileEntityPAQuadrupole;
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import com.hbm.tileentity.machine.albion.TileEntityPASource;
import dev.golem.core.gametest.GameTests;

import java.util.List;

/** Particle accelerator parts: 1.7's OpenComputers components on CC: Tweaked. */
public final class AlbionRows {

    private AlbionRows() {}

    // 1.7 BlockPA*: every makeExtra cell is a TileEntityProxyCombo flagged power and fluid
    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(TileEntityPASource.class, PASourcePeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityPADipole.class, PADipolePeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityPAQuadrupole.class, PAQuadrupolePeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityPARFC.class, PARFCPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityPADetector.class, PADetectorPeripheral::new, Reach.CELLS)
    );

    public static GameTests.Suite tests() {
        return com.hbm.gametest.computer.AlbionPeripheralGameTests.TESTS;
    }
}
//?}
