package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKCooler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKLever;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKTerminal;

import java.util.List;

/**
 * RBMK columns, consoles and panels: 1.7's OpenComputers components on CC: Tweaked. The rod row covers the ReaSim
 * channel (a subclass, same 1.7 component). Only the boiler, cooler, heater and outgasser flag their column-top proxy
 * for fluid, so only they answer at the top; a control column's top proxy carries no flags and a fuel channel's
 * only its inventory.
 */
public final class RbmkRows {

    private RbmkRows() {}

    public static final List<Peripherals.Row<?>> ROWS = List.of(
            Peripherals.row(BlockEntityRBMKRod.class, RBMKRodPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKControlManual.class, RBMKControlManualPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKControlAuto.class, RBMKControlPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKBoiler.class, RBMKBoilerPeripheral::new, Reach.RBMK_TOP),
            Peripherals.row(BlockEntityRBMKCooler.class, RBMKCoolerPeripheral::new, Reach.RBMK_TOP),
            Peripherals.row(BlockEntityRBMKHeater.class, RBMKHeaterPeripheral::new, Reach.RBMK_TOP),
            Peripherals.row(BlockEntityRBMKOutgasser.class, RBMKOutgasserPeripheral::new, Reach.RBMK_TOP),
            Peripherals.row(BlockEntityRBMKConsole.class, RBMKConsolePeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityCraneConsole.class, CraneConsolePeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKTerminal.class, RBMKTerminalPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKNumitron.class, RBMKNumitronPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKLever.class, RBMKLeverPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKKeyPad.class, RBMKKeyPadPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKIndicator.class, RBMKIndicatorPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKGraph.class, RBMKGraphPeripheral::new, Reach.BLOCK),
            Peripherals.row(BlockEntityRBMKGauge.class, RBMKGaugePeripheral::new, Reach.BLOCK)
    );
}
//?}
