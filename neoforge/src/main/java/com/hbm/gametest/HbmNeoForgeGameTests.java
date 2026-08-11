package com.hbm.gametest;

import com.hbm.Constants;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestEnvironments;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Registers the common HBM GameTest functions as executable NeoForge test instances. */
public final class HbmNeoForgeGameTests {

    private HbmNeoForgeGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "test_environment"),
                new TestEnvironmentDefinition.Functions(Optional.empty(), Optional.empty()));
        register(event, environment, "cooling_recipes", HbmGameTestFunctions.COOLING_RECIPES.getKey(), 20);
        register(event, environment, "turbine_conversion", HbmGameTestFunctions.TURBINE_CONVERSION.getKey(), 20);
        register(event, environment, "turbine_identifier", HbmGameTestFunctions.TURBINE_IDENTIFIER.getKey(), 20);
        register(event, environment, "turbine_slots", HbmGameTestFunctions.TURBINE_SLOTS.getKey(), 20);
        register(event, environment, "turbine_menu", HbmGameTestFunctions.TURBINE_MENU.getKey(), 20);
        register(event, environment, "condenser_conversion", HbmGameTestFunctions.CONDENSER_CONVERSION.getKey(), 20);
        register(event, environment, "purex_recipe_cycle", HbmGameTestFunctions.PUREX_RECIPE_CYCLE.getKey(), 20);
        register(event, environment, "purex_hot_waste_rejected", HbmGameTestFunctions.PUREX_HOT_WASTE_REJECTED.getKey(), 20);
        register(event, environment, "purex_upgrades", HbmGameTestFunctions.PUREX_UPGRADES.getKey(), 20);
        register(event, environment, "purex_max_power", HbmGameTestFunctions.PUREX_MAX_POWER.getKey(), 20);
        register(event, environment, "purex_output_fluid", HbmGameTestFunctions.PUREX_OUTPUT_FLUID.getKey(), 20);
        register(event, environment, "purex_slots", HbmGameTestFunctions.PUREX_SLOTS.getKey(), 20);
        register(event, environment, "purex_menu", HbmGameTestFunctions.PUREX_MENU.getKey(), 20);
        register(event, environment, "purex_layout", HbmGameTestFunctions.PUREX_LAYOUT.getKey(), 100);
        register(event, environment, "zirnox_golden_tick", HbmGameTestFunctions.ZIRNOX_GOLDEN_TICK.getKey(), 20);
        register(event, environment, "zirnox_layout", HbmGameTestFunctions.ZIRNOX_LAYOUT.getKey(), 100);
        register(event, environment, "zirnox_world_tick", HbmGameTestFunctions.ZIRNOX_WORLD_TICK.getKey(), 100);
        register(event, environment, "zirnox_steam_output", HbmGameTestFunctions.ZIRNOX_STEAM_OUTPUT.getKey(), 300);
        register(event, environment, "zirnox_meltdown", HbmGameTestFunctions.ZIRNOX_MELTDOWN.getKey(), 100);
        register(event, environment, "zirnox_wreck_hazards", HbmGameTestFunctions.ZIRNOX_WRECK_HAZARDS.getKey(), 100);
        // ore worldgen — these carve a large scratch volume, so they get a longer budget
        register(event, environment, "ore_vein_shape", HbmGameTestFunctions.ORE_VEIN_SHAPE.getKey(), 200);
        register(event, environment, "ore_vein_offset", HbmGameTestFunctions.ORE_VEIN_OFFSET.getKey(), 200);
        register(event, environment, "ore_vein_predicate", HbmGameTestFunctions.ORE_VEIN_PREDICATE.getKey(), 200);
        register(event, environment, "ore_targets", HbmGameTestFunctions.ORE_TARGETS.getKey(), 200);
        register(event, environment, "ore_depth_deposit", HbmGameTestFunctions.ORE_DEPTH_DEPOSIT.getKey(), 200);
        register(event, environment, "ore_counts", HbmGameTestFunctions.ORE_COUNTS.getKey(), 200);
        register(event, environment, "ore_smoldering", HbmGameTestFunctions.ORE_SMOLDERING.getKey(), 300);
        // SILEX
        register(event, environment, "silex_process", HbmGameTestFunctions.SILEX_PROCESS.getKey(), 100);
        register(event, environment, "silex_load", HbmGameTestFunctions.SILEX_LOAD.getKey(), 100);
        register(event, environment, "silex_load_fluid", HbmGameTestFunctions.SILEX_LOAD_FLUID.getKey(), 100);
        register(event, environment, "silex_output_weights", HbmGameTestFunctions.SILEX_OUTPUT_WEIGHTS.getKey(), 100);
        register(event, environment, "silex_queue", HbmGameTestFunctions.SILEX_QUEUE.getKey(), 100);
        register(event, environment, "silex_slots", HbmGameTestFunctions.SILEX_SLOTS.getKey(), 100);
        register(event, environment, "silex_menu", HbmGameTestFunctions.SILEX_MENU.getKey(), 100);
        register(event, environment, "silex_layout", HbmGameTestFunctions.SILEX_LAYOUT.getKey(), 100);
        register(event, environment, "silex_mode_reset", HbmGameTestFunctions.SILEX_MODE_RESET.getKey(), 100);
        // FEL
        register(event, environment, "fel_beam", HbmGameTestFunctions.FEL_BEAM.getKey(), 100);
        register(event, environment, "fel_beam_illegal", HbmGameTestFunctions.FEL_BEAM_ILLEGAL.getKey(), 100);
        register(event, environment, "fel_rotation", HbmGameTestFunctions.FEL_ROTATION.getKey(), 100);
        register(event, environment, "fel_power", HbmGameTestFunctions.FEL_POWER.getKey(), 100);
        register(event, environment, "fel_burn", HbmGameTestFunctions.FEL_BURN.getKey(), 200);
        register(event, environment, "fel_burn_infrared", HbmGameTestFunctions.FEL_BURN_INFRARED.getKey(), 200);
        register(event, environment, "fel_power_port", HbmGameTestFunctions.FEL_POWER_PORT.getKey(), 100);
        register(event, environment, "fel_layout", HbmGameTestFunctions.FEL_LAYOUT.getKey(), 100);
        register(event, environment, "fel_slots", HbmGameTestFunctions.FEL_SLOTS.getKey(), 100);
        register(event, environment, "fel_menu", HbmGameTestFunctions.FEL_MENU.getKey(), 100);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
                                 String name, ResourceKey<Consumer<GameTestHelper>> function, int timeoutTicks) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment, Identifier.withDefaultNamespace("empty"), timeoutTicks, 0, true, Rotation.NONE,
                false, 1, 1, false, 0);
        event.registerTest(Identifier.fromNamespaceAndPath(Constants.MOD_ID, name),
                testData -> new FunctionGameTestInstance(function, testData), data);
    }
}
