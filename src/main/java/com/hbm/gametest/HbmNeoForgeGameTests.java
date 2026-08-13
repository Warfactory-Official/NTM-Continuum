package com.hbm.gametest;

//? if neoforge {

import dev.golem.core.gametest.GameTests;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Registers the common HBM GameTest functions as executable NeoForge test instances. */
public final class HbmNeoForgeGameTests {

    private HbmNeoForgeGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, HbmGameTestFunctions.COOLING_RECIPES, 20);
        GameTests.register(event, HbmGameTestFunctions.TURBINE_CONVERSION, 20);
        GameTests.register(event, HbmGameTestFunctions.TURBINE_IDENTIFIER, 20);
        GameTests.register(event, HbmGameTestFunctions.TURBINE_SLOTS, 20);
        GameTests.register(event, HbmGameTestFunctions.TURBINE_MENU, 20);
        GameTests.register(event, HbmGameTestFunctions.CONDENSER_CONVERSION, 20);
        GameTests.register(event, HbmGameTestFunctions.PUREX_RECIPE_CYCLE, 20);
        GameTests.register(event, HbmGameTestFunctions.PUREX_HOT_WASTE_REJECTED, 20);
        GameTests.register(event, HbmGameTestFunctions.PUREX_UPGRADES, 20);
        GameTests.register(event, HbmGameTestFunctions.PUREX_MAX_POWER, 20);
        GameTests.register(event, HbmGameTestFunctions.PUREX_OUTPUT_FLUID, 20);
        GameTests.register(event, HbmGameTestFunctions.PUREX_SLOTS, 20);
        GameTests.register(event, HbmGameTestFunctions.PUREX_MENU, 20);
        GameTests.register(event, HbmGameTestFunctions.PUREX_LAYOUT, 100);
        GameTests.register(event, HbmGameTestFunctions.ZIRNOX_GOLDEN_TICK, 20);
        GameTests.register(event, HbmGameTestFunctions.ZIRNOX_LAYOUT, 100);
        GameTests.register(event, HbmGameTestFunctions.ZIRNOX_WORLD_TICK, 100);
        GameTests.register(event, HbmGameTestFunctions.ZIRNOX_STEAM_OUTPUT, 300);
        GameTests.register(event, HbmGameTestFunctions.ZIRNOX_MELTDOWN, 100);
        GameTests.register(event, HbmGameTestFunctions.ZIRNOX_WRECK_HAZARDS, 100);
        // ore worldgen — these carve a large scratch volume, so they get a longer budget
        GameTests.register(event, HbmGameTestFunctions.ORE_VEIN_SHAPE, 200);
        GameTests.register(event, HbmGameTestFunctions.ORE_VEIN_OFFSET, 200);
        GameTests.register(event, HbmGameTestFunctions.ORE_VEIN_PREDICATE, 200);
        GameTests.register(event, HbmGameTestFunctions.ORE_TARGETS, 200);
        GameTests.register(event, HbmGameTestFunctions.ORE_DEPTH_DEPOSIT, 200);
        GameTests.register(event, HbmGameTestFunctions.ORE_COUNTS, 200);
        GameTests.register(event, HbmGameTestFunctions.ORE_SMOLDERING, 300);
        // SILEX
        GameTests.register(event, HbmGameTestFunctions.SILEX_PROCESS, 100);
        GameTests.register(event, HbmGameTestFunctions.SILEX_LOAD, 100);
        GameTests.register(event, HbmGameTestFunctions.SILEX_LOAD_FLUID, 100);
        GameTests.register(event, HbmGameTestFunctions.SILEX_OUTPUT_WEIGHTS, 100);
        GameTests.register(event, HbmGameTestFunctions.SILEX_QUEUE, 100);
        GameTests.register(event, HbmGameTestFunctions.SILEX_SLOTS, 100);
        GameTests.register(event, HbmGameTestFunctions.SILEX_MENU, 100);
        GameTests.register(event, HbmGameTestFunctions.SILEX_LAYOUT, 100);
        GameTests.register(event, HbmGameTestFunctions.SILEX_MODE_RESET, 100);
        // FEL
        GameTests.register(event, HbmGameTestFunctions.FEL_BEAM, 100);
        GameTests.register(event, HbmGameTestFunctions.FEL_BEAM_ILLEGAL, 100);
        GameTests.register(event, HbmGameTestFunctions.FEL_ROTATION, 100);
        GameTests.register(event, HbmGameTestFunctions.FEL_POWER, 100);
        GameTests.register(event, HbmGameTestFunctions.FEL_BURN, 200);
        GameTests.register(event, HbmGameTestFunctions.FEL_BURN_INFRARED, 200);
        GameTests.register(event, HbmGameTestFunctions.FEL_POWER_PORT, 100);
        GameTests.register(event, HbmGameTestFunctions.FEL_LAYOUT, 100);
        GameTests.register(event, HbmGameTestFunctions.FEL_SLOTS, 100);
        GameTests.register(event, HbmGameTestFunctions.FEL_MENU, 100);
    }

}
//?}
