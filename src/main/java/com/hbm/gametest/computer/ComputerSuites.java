package com.hbm.gametest.computer;

//? if computercraft {
import dev.golem.core.computer.PeripheralExposure;
//?}
import dev.golem.core.computer.Computers;
import dev.golem.core.gametest.GameTests;
import java.util.ArrayList;
import java.util.List;

/** The computer GameTest suites: CC's when CC: Tweaked is loaded (their bodies name its types), OC's with OC. */
public final class ComputerSuites {

    private ComputerSuites() {}

    public static List<GameTests.Suite> all() {
        List<GameTests.Suite> suites = new ArrayList<>();
        //? if computercraft {
        if (PeripheralExposure.available()) {
            suites.addAll(List.of(PowerPeripheralGameTests.TESTS, ReactorPeripheralGameTests.TESTS,
                    StoragePeripheralGameTests.TESTS, NetworkPeripheralGameTests.TESTS, RbmkPeripheralGameTests.TESTS,
                    AlbionPeripheralGameTests.TESTS, FusionPeripheralGameTests.TESTS, BombPeripheralGameTests.TESTS,
                    TurretPeripheralGameTests.TESTS, ComputerLuaGameTests.TESTS));
        }
        //?}
        if (Computers.openComputers()) suites.add(OpenComputersGameTests.TESTS);
        return suites;
    }
}
