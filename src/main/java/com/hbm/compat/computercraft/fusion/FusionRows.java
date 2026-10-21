package com.hbm.compat.computercraft.fusion;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;

import java.util.List;

/** Fusion reactor parts: 1.7's OpenComputers components on CC: Tweaked. */
public final class FusionRows {

    private FusionRows() {}

    // 1.7 MachineFusion*: every makeExtra cell is a TileEntityProxyCombo flagged power and/or fluid.
    // The creative klystron is a klystron subclass here, but 1.7's extended TileEntityLoadedBase and had no component.
    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(TileEntityFusionTorus.class, FusionTorusPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityFusionKlystron.class, FusionKlystronPeripheral::new, Reach.CELLS),
            Peripherals.none(com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative.class),
            Peripherals.row(TileEntityFusionBoiler.class, FusionBoilerPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityFusionBreeder.class, FusionBreederPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityFusionMHDT.class, FusionMHDTPeripheral::new, Reach.CELLS)
    );
}
//?}
