package com.hbm.compat.computercraft;

import dev.golem.core.gametest.GameTests;

import java.util.List;

/**
 * 1.7's OpenComputers components, served to CC: Tweaked. OpenComputers has no
 * build on any target; CC: Tweaked is the computer mod every node has, as in ntm-next, which made the same swap.
 *
 * <p>Names no CC: Tweaked type, so it is safe to call without the mod: {@link Peripherals} and every peripheral
 * class are touched only when it is loaded. Core's {@code dev.golem.core.computer.PeripheralExposure} owns the
 * loader registration; the {@code computercraft} constant compiles the integration out on a node that declares no
 * CC coordinates.
 */
public final class ComputerCraft {

    private ComputerCraft() {}

    /** Common init, once, before the loaders' capability registration. */
    public static void init() {
        //? if computercraft {
        if (dev.golem.core.computer.PeripheralExposure.available()) Peripherals.init();
        //?}
    }

    /** The peripheral GameTest suites; empty without CC: Tweaked, whose types their bodies name. */
    public static List<GameTests.Suite> gameTestSuites() {
        //? if computercraft {
        if (dev.golem.core.computer.PeripheralExposure.available()) return Peripherals.gameTestSuites();
        //?}
        return List.of();
    }
}
