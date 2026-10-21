package com.hbm.compat.computercraft.turret;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.turret.TileEntityTurretHIMARS;

import java.util.List;

/** Turrets: 1.7's OpenComputers components on CC: Tweaked. */
public final class TurretRows {

    private TurretRows() {}

    // 1.7: every NT turret inherits TileEntityTurretBaseNT's SimpleComponent, and its subclasses here are the same
    // thirteen turrets. The turret cells are non-extra proxies, so only the core answers, and only from below.
    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(TileEntityTurretBaseNT.class, TurretPeripheral::new, Reach.DOWN),
            Peripherals.row(TileEntityTurretArty.class, ArtyPeripheral::new, Reach.DOWN),
            Peripherals.row(TileEntityTurretHIMARS.class, HimarsPeripheral::new, Reach.DOWN)
    );
}
//?}
