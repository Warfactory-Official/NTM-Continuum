package com.hbm.compat.computercraft.bomb;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import com.hbm.tileentity.bomb.TileEntityLaunchPadLarge;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import dev.golem.core.gametest.GameTests;

import java.util.List;

/** Launch pads and the launch table: 1.7's OpenComputers components on CC: Tweaked. */
public final class BombRows {

    private BombRows() {}

    // 1.7 LaunchPad/LaunchPadLarge: the makeExtra cells are full proxies; the large pad's other cells are
    // inventory-only and do not connect. 1.7 LaunchTable's cells were plain dummies, so the table is its own block.
    // The rusted pad has no component, as in 1.7.
    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(TileEntityLaunchPad.class, LaunchPadPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityLaunchPadLarge.class, LaunchPadPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityLaunchTable.class, LaunchTablePeripheral::new, Reach.BLOCK)
    );

    public static GameTests.Suite tests() {
        return com.hbm.gametest.computer.BombPeripheralGameTests.TESTS;
    }
}
//?}
