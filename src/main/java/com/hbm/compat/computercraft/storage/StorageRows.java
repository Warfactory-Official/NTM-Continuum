package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.Peripherals.Reach;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.TileEntityMachineGiantTank;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import com.hbm.tileentity.machine.storage.TileEntityMachineFENSU;
import com.hbm.tileentity.machine.storage.TileEntityMachineOrbus;
import com.hbm.tileentity.network.TileEntityCapacitor;
import dev.golem.core.gametest.GameTests;

import java.util.List;

/** Batteries, tanks, barrels and capacitors: 1.7's OpenComputers components on CC: Tweaked. */
public final class StorageRows {

    private StorageRows() {}

    public static final List<Peripherals.Row<?>> ROWS = List.of(
            // 1.7 BlockFluidBarrel: a plain block; the Orbus, big-ass tank and BAT-9000 makeExtra fluid proxies
            Peripherals.row(TileEntityBarrel.class, BarrelPeripheral::new, Reach.BLOCK),
            Peripherals.row(TileEntityMachineOrbus.class, BarrelPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityMachineGiantTank.class, BarrelPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityMachineFluidTank.class, MachineFluidTankPeripheral::new, Reach.CELLS),
            // 1.7 MachineBattery and MachineFENSU: no makeExtra cells (the tiered batteries are machine batteries)
            Peripherals.row(TileEntityMachineBattery.class, MachineBatteryPeripheral<TileEntityMachineBattery>::new,
                    Reach.BLOCK),
            Peripherals.row(TileEntityMachineFENSU.class, MachineBatteryPeripheral<TileEntityMachineFENSU>::new,
                    Reach.BLOCK),
            // 1.7 socket and REDD: makeExtra power proxies
            Peripherals.row(TileEntityBatterySocket.class, BatterySocketPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityBatteryREDD.class, BatteryREDDPeripheral::new, Reach.CELLS),
            Peripherals.row(TileEntityCapacitor.class, CapacitorPeripheral::new, Reach.BLOCK)
    );

    public static GameTests.Suite tests() {
        return com.hbm.gametest.computer.StoragePeripheralGameTests.TESTS;
    }
}
//?}
