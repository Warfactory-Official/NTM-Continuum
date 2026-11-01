package com.hbm.gametest.computer;

//? if computercraft {
import dev.golem.core.computer.PeripheralExposure;
//?}
import dev.golem.core.gametest.GameTests;
import java.util.List;

/** The peripheral GameTest suites; empty without CC: Tweaked, whose types their bodies name. */
public final class ComputerSuites {

    private ComputerSuites() {}

    public static List<GameTests.Suite> all() {
        var list = new java.util.ArrayList<GameTests.Suite>();
        //? if computercraft {
        if (PeripheralExposure.available()) {
            list.addAll(List.of(PowerPeripheralGameTests.TESTS, ReactorPeripheralGameTests.TESTS,
                    StoragePeripheralGameTests.TESTS, NetworkPeripheralGameTests.TESTS, RbmkPeripheralGameTests.TESTS,
                    AlbionPeripheralGameTests.TESTS, FusionPeripheralGameTests.TESTS, BombPeripheralGameTests.TESTS,
                    TurretPeripheralGameTests.TESTS, ComputerLuaGameTests.TESTS));
        }
        //?}
        //? if opencomputers {
        /*if (com.hbm.compat.opencomputers.OpenComputersCompat.isAvailable()) {
            list.add(OpenComputersGameTests.TESTS);
        }
        *///?}
        return list;
    }
}
