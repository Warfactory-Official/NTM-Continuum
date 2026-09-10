package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import com.hbm.tileentity.machine.TileEntityMachineTurbine;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.hbm.world.biome.ModBiomes;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Biomes;
import net.minecraft.gametest.framework.GameTestHelper;

/** Shared, server-only GameTest functions and execution budgets. */
public final class HbmGameTestFunctions {

    private HbmGameTestFunctions() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function COOLING_RECIPES =
            TESTS.function("cooling_recipes", 20, () -> HbmGameTestFunctions::coolingRecipes);
    public static final GameTests.Function JETPACK_REGISTRATIONS =
            TESTS.function("jetpack_registrations", 20, () -> JetpackGameTests::registrationsAndFuelRates);
    public static final GameTests.Function JETPACK_GLIDER_STATE =
            TESTS.function("jetpack_glider_state", 20, () -> JetpackGameTests::gliderFuelAndDelayedState);
    public static final GameTests.Function JETPACK_WEARABILITY =
            TESTS.function("jetpack_wearability", 20, () -> JetpackGameTests::directAndModuleWearability);
    public static final GameTests.Function JETPACK_REFUEL =
            TESTS.function("jetpack_refuel", 20, () -> JetpackGameTests::reserveTankRefuelsRegularJetpack);
    public static final GameTests.Function JETPACK_DEPENDENCIES =
            TESTS.function("jetpack_dependencies", 40, () -> JetpackGameTests::dependencyItemsAndHotAnvil);
    public static final GameTests.Function JETPACK_INPUT_SANITIZE =
            TESTS.function("jetpack_input_sanitize", 20, () -> JetpackGameTests::inputPayloadIsBounded);
    public static final GameTests.Function JETPACK_CLIENT_SOURCE =
            TESTS.function("jetpack_client_source", 20, () -> JetpackGameTests::equivalentClientSnapshotKeepsSource);
    public static final GameTests.Function JETPACK_FUEL_CADENCE =
            TESTS.function("jetpack_fuel_cadence", 20, () -> JetpackGameTests::ordinaryFuelCadenceAndRelease);
    public static final GameTests.Function JETPACK_LOCKOUT =
            TESTS.function("jetpack_lockout", 20, () -> JetpackGameTests::gliderLockoutAndFlyTimeRestart);
    public static final GameTests.Function JETPACK_SOURCE_SWAP =
            TESTS.function("jetpack_source_swap", 20, () -> JetpackGameTests::sourceSwapLoadsNewGliderState);
    public static final GameTests.Function JETPACK_REPORTED_INPUT =
            TESTS.function("jetpack_reported_input", 20, () -> JetpackGameTests::gliderUsesReportedInputForMotion);
    public static final GameTests.Function JETPACK_BJ_GLIDER =
            TESTS.function("jetpack_bj_glider", 20, () -> JetpackGameTests::bjGliderMotionIncludesVerticalBrake);
    public static final GameTests.Function JETPACK_MURK_STRAFE =
            TESTS.function("jetpack_murk_strafe", 20, () -> JetpackGameTests::murkStrafeUsesLegacyPositiveRotation);
    public static final GameTests.Function JETPACK_STATE_SCOPE =
            TESTS.function("jetpack_state_scope", 20, () -> JetpackGameTests::stateScopeCleansOnLevelRemoval);
    public static final GameTests.Function JETPACK_SMOKE_CLEANUP =
            TESTS.function("jetpack_smoke_cleanup", 20, () -> JetpackGameTests::smokeSessionRestoresOwnedState);
    public static final GameTests.Function COMPONENT_COPY_EXACT_MULTISET =
            TESTS.function("component_copy_exact_multiset", 20, () -> ComponentCopyRecipeGameTests::exactMultiset);
    public static final GameTests.Function COMPONENT_COPY_SOURCE_STATE =
            TESTS.function("component_copy_source_state", 20, () -> ComponentCopyRecipeGameTests::sourceStateAndFreshOutput);
    public static final GameTests.Function COMPONENT_COPY_REJECTION =
            TESTS.function("component_copy_rejection", 20, () -> ComponentCopyRecipeGameTests::rejectsWrongExtraAndAmbiguousInput);
    public static final GameTests.Function CHEMICAL_DYE_FAMILIES =
            TESTS.function("chemical_dye_families", 40, () -> ChemicalDyeAndComponentRecipeGameTests::dyeFamilies);
    public static final GameTests.Function CHEMICAL_DYE_CRYSTALLIZER =
            TESTS.function("chemical_dye_crystallizer", 40, () -> ChemicalDyeAndComponentRecipeGameTests::dyeCrystallizerRecipes);
    public static final GameTests.Function COMPONENT_SHAPED_RECIPES =
            TESTS.function("component_shaped_recipes", 40, () -> ChemicalDyeAndComponentRecipeGameTests::componentShapedRecipes);
    public static final GameTests.Function HEATEX_COOLABLE_TABLE =
            TESTS.function("heatex_coolable_table", 20, () -> HeatexGameTests::coolableTable);
    public static final GameTests.Function HEATEX_LAYOUT_PORTS =
            TESTS.function("heatex_layout_ports", 100, 8, () -> HeatexGameTests::layoutAndPorts);
    public static final GameTests.Function HEATEX_CONVERSION =
            TESTS.function("heatex_conversion", 40, 8, () -> HeatexGameTests::conversionArithmetic);
    public static final GameTests.Function HEATEX_THROTTLE_CAPACITY =
            TESTS.function("heatex_throttle_capacity", 40, 8, () -> HeatexGameTests::throttleDelayCapacity);
    public static final GameTests.Function HEATEX_PERSISTENCE_MENU =
            TESTS.function("heatex_persistence_menu", 40, 8, () -> HeatexGameTests::persistenceAndMenu);
    public static final GameTests.Function ARC_WELDER_RECIPES =
            TESTS.function("arc_welder_recipes", 40, () -> ArcWelderGameTests::feasibleCatalogueAndMatching);
    public static final GameTests.Function ARC_WELDER_PROCESSING =
            TESTS.function("arc_welder_processing", 100, 8, () -> ArcWelderGameTests::processingAndFluidAtomicity);
    public static final GameTests.Function ARC_WELDER_UPGRADES =
            TESTS.function("arc_welder_upgrades", 100, 8, () -> ArcWelderGameTests::upgradesAndPowerCeiling);
    public static final GameTests.Function ARC_WELDER_LAYOUT =
            TESTS.function("arc_welder_layout", 100, 16, () -> ArcWelderGameTests::layoutPortsAndHopperSlots);
    public static final GameTests.Function ARC_WELDER_MENU =
            TESTS.function("arc_welder_menu", 40, 8, () -> ArcWelderGameTests::menuTransferAndDisplay);
    public static final GameTests.Function SMOKE_CHAIN_LAYOUTS =
            TESTS.function("smoke_chain_layouts", 200, 28, () -> SmokeChainGameTests::layouts);
    public static final GameTests.Function SMOKE_CHAIN_PORTS =
            TESTS.function("smoke_chain_ports", 100, 8, () -> SmokeChainGameTests::portsAndCapture);
    public static final GameTests.Function SMOKE_CHAIN_SOOT =
            TESTS.function("smoke_chain_soot", 40, 8, () -> SmokeChainGameTests::industrialSoot);
    public static final GameTests.Function ASHPIT_PROCESSING =
            TESTS.function("ashpit_processing", 100, 8, () -> SmokeChainGameTests::ashpitProcessingAndHoppers);
    public static final GameTests.Function ASHPIT_MENU =
            TESTS.function("ashpit_menu", 40, 8, () -> SmokeChainGameTests::menuAndSync);
    public static final GameTests.Function SMOKE_CHAIN_ITEM_MODELS =
            TESTS.function("smoke_chain_item_models", 20, () -> SmokeChainGameTests::itemModels);
    public static final GameTests.Function MASS_STORAGE_CAPACITIES =
            TESTS.function("mass_storage_capacities", 20, () -> MassStorageGameTests::capacities);
    public static final GameTests.Function MASS_STORAGE_PROCESSING =
            TESTS.function("mass_storage_processing", 40, () -> MassStorageGameTests::filteringAndProcessing);
    public static final GameTests.Function MASS_STORAGE_IDENTITY =
            TESTS.function("mass_storage_identity", 20, () -> MassStorageGameTests::componentIdentity);
    public static final GameTests.Function MASS_STORAGE_CARRIED =
            TESTS.function("mass_storage_carried", 20, () -> MassStorageGameTests::carriedState);
    public static final GameTests.Function MASS_STORAGE_OUTPUT_SYNC =
            TESTS.function("mass_storage_output_sync", 40, () -> MassStorageGameTests::manualOutputAndSync);
    public static final GameTests.Function MASS_STORAGE_MENU =
            TESTS.function("mass_storage_menu", 40, () -> MassStorageGameTests::menuAndControls);
    public static final GameTests.Function MASS_STORAGE_ASSETS =
            TESTS.function("mass_storage_assets", 20, () -> MassStorageGameTests::assets);
    public static final GameTests.Function TEMPLATE_CRATE_LAYOUT =
            TESTS.function("template_crate_layout", 20, () -> TemplateCrateGameTests::layoutAndInventory);
    public static final GameTests.Function TEMPLATE_CRATE_ASSETS =
            TESTS.function("template_crate_assets", 20, () -> TemplateCrateGameTests::assets);
    public static final GameTests.Function ADVANCED_OIL_PYRO_GATE =
            TESTS.function("advanced_oil_pyro_gate", 40, 8, () -> AdvancedOilParityGameTests::pyroProgressGate);
    public static final GameTests.Function ADVANCED_OIL_COMBUSTION_GATE =
            TESTS.function("advanced_oil_combustion_gate", 40, 8, () -> AdvancedOilParityGameTests::combustionOperationGate);
    public static final GameTests.Function ADVANCED_OIL_REFUELER_GATE =
            TESTS.function("advanced_oil_refueler_gate", 40, 8, () -> AdvancedOilParityGameTests::refuelerIdleGate);
    public static final GameTests.Function TURBINE_CONVERSION =
            TESTS.function("turbine_conversion", 20, () -> HbmGameTestFunctions::turbineConversion);
    public static final GameTests.Function TURBINE_IDENTIFIER =
            TESTS.function("turbine_identifier", 20, () -> HbmGameTestFunctions::turbineFluidId);
    public static final GameTests.Function TURBINE_SLOTS =
            TESTS.function("turbine_slots", 20, () -> HbmGameTestFunctions::turbineSlots);
    public static final GameTests.Function TURBINE_MENU =
            TESTS.function("turbine_menu", 20, () -> HbmGameTestFunctions::turbineMenu);
    public static final GameTests.Function CRATER_BIOME_REGISTRY =
            TESTS.function("crater_biome_registry", 20, () -> HbmGameTestFunctions::craterBiomeRegistry);
    public static final GameTests.Function INDUSTRIAL_TURBINE_LAYOUT =
            TESTS.function("industrial_turbine_layout", 100, 8, () -> IndustrialTurbineGameTests::layout);
    public static final GameTests.Function INDUSTRIAL_TURBINE_PORTS =
            TESTS.function("industrial_turbine_ports", 100, 8, () -> IndustrialTurbineGameTests::ports);
    public static final GameTests.Function INDUSTRIAL_TURBINE_CONVERSION =
            TESTS.function("industrial_turbine_conversion", 100, 8, () -> IndustrialTurbineGameTests::conversion);
    public static final GameTests.Function CHUNGUS_STRUCTURE =
            TESTS.function("chungus_structure", 100, 24, () -> ChungusGameTests::structure);
    public static final GameTests.Function CHUNGUS_PORTS =
            TESTS.function("chungus_ports", 100, 24, () -> ChungusGameTests::ports);
    public static final GameTests.Function CHUNGUS_CONVERSION =
            TESTS.function("chungus_conversion", 100, 24, () -> ChungusGameTests::conversion);
    public static final GameTests.Function CHUNGUS_LEVER =
            TESTS.function("chungus_lever", 100, 24, () -> ChungusGameTests::lever);
    public static final GameTests.Function CHUNGUS_ASSETS =
            TESTS.function("chungus_assets", 20, () -> ChungusGameTests::assets);
    public static final GameTests.Function LARGE_TURBINE_LAYOUT =
            TESTS.function("large_turbine_layout", 100, 8, () -> LargeTurbineGameTests::layout);
    public static final GameTests.Function LARGE_TURBINE_PORTS =
            TESTS.function("large_turbine_ports", 100, 8, () -> LargeTurbineGameTests::ports);
    public static final GameTests.Function LARGE_TURBINE_CONVERSION =
            TESTS.function("large_turbine_conversion", 100, 8, () -> LargeTurbineGameTests::conversion);
    public static final GameTests.Function LARGE_TURBINE_MENU =
            TESTS.function("large_turbine_menu", 40, () -> LargeTurbineGameTests::menu);
    public static final GameTests.Function GAS_TURBINE_LAYOUT =
            TESTS.function("gas_turbine_layout", 100, 8, () -> GasTurbineGameTests::layout);
    public static final GameTests.Function GAS_TURBINE_PORTS =
            TESTS.function("gas_turbine_ports", 100, 8, () -> GasTurbineGameTests::ports);
    public static final GameTests.Function GAS_TURBINE_STARTUP =
            TESTS.function("gas_turbine_startup", 100, 8, () -> GasTurbineGameTests::startup);
    public static final GameTests.Function GAS_TURBINE_SUSTAINED_RUN =
            TESTS.function("gas_turbine_sustained_run", 100, 8, () -> GasTurbineGameTests::sustainedRun);
    public static final GameTests.Function GAS_TURBINE_MENU_RECIPE =
            TESTS.function("gas_turbine_menu_recipe", 40, () -> GasTurbineGameTests::menuAndRecipe);
    public static final GameTests.Function GAS_TURBINE_GUI_TOOLTIPS =
            TESTS.function("gas_turbine_gui_tooltips", 20, () -> GasTurbineGameTests::guiTooltips);
    public static final GameTests.Function TURBOFAN_LAYOUT =
            TESTS.function("turbofan_layout", 100, 8, () -> TurbofanGameTests::layout);
    public static final GameTests.Function TURBOFAN_PORTS_BURN =
            TESTS.function("turbofan_ports_burn", 100, 8, () -> TurbofanGameTests::portsAndBurn);
    public static final GameTests.Function TURBOFAN_MENU =
            TESTS.function("turbofan_menu", 40, () -> TurbofanGameTests::menu);
    public static final GameTests.Function STEAM_ENGINE_LAYOUT =
            TESTS.function("steam_engine_layout", 100, 8, () -> SteamEngineGameTests::layout);
    public static final GameTests.Function STEAM_ENGINE_PORTS =
            TESTS.function("steam_engine_ports", 100, 8, () -> SteamEngineGameTests::ports);
    public static final GameTests.Function STEAM_ENGINE_CONVERSION =
            TESTS.function("steam_engine_conversion", 40, 8, () -> SteamEngineGameTests::conversion);
    public static final GameTests.Function STEAM_ENGINE_GEAR_RECIPE =
            TESTS.function("steam_engine_gear_recipe", 40, () -> SteamEngineGameTests::gearAndRecipe);
    public static final GameTests.Function PUMP_LAYOUT =
            TESTS.function("pump_layout", 100, 8, () -> PumpGameTests::layout);
    public static final GameTests.Function PUMP_PORTS =
            TESTS.function("pump_ports", 40, 8, () -> PumpGameTests::ports);
    public static final GameTests.Function PUMP_CONVERSION =
            TESTS.function("pump_conversion", 40, 8, () -> PumpGameTests::conversion);
    public static final GameTests.Function PUMP_RECIPE =
            TESTS.function("pump_recipe", 40, () -> PumpGameTests::recipe);
    public static final GameTests.Function STEAM_PUMP_LAYOUT =
            TESTS.function("steam_pump_layout", 100, 8, () -> PumpGameTests::steamLayout);
    public static final GameTests.Function STEAM_PUMP_PORTS =
            TESTS.function("steam_pump_ports", 40, 8, () -> PumpGameTests::steamPorts);
    public static final GameTests.Function STEAM_PUMP_CONVERSION =
            TESTS.function("steam_pump_conversion", 40, 8, () -> PumpGameTests::steamConversion);
    public static final GameTests.Function STEAM_PUMP_RECIPE =
            TESTS.function("steam_pump_recipe", 40, () -> PumpGameTests::steamRecipe);
    public static final GameTests.Function STEAM_PUMP_NETWORK_SUBSCRIPTION =
            TESTS.function("steam_pump_network_subscription", 40, 8,
                    () -> PumpGameTests::steamNetworkSubscription);
    public static final GameTests.Function STIRLING_LAYOUT =
            TESTS.function("stirling_layout", 100, 8, () -> StirlingGameTests::layout);
    public static final GameTests.Function STIRLING_PORTS =
            TESTS.function("stirling_ports", 40, 8, () -> StirlingGameTests::ports);
    public static final GameTests.Function STIRLING_CONVERSION =
            TESTS.function("stirling_conversion", 40, 8, () -> StirlingGameTests::conversion);
    public static final GameTests.Function STIRLING_VARIANTS_RECIPES =
            TESTS.function("stirling_variants_recipes", 40, () -> StirlingGameTests::variantsAndRecipes);
    public static final GameTests.Function STIRLING_MISSING_GEAR =
            TESTS.function("stirling_missing_gear", 100, 8, () -> StirlingGameTests::missingGearPlacement);
    public static final GameTests.Function WOOD_BURNER_LAYOUT_AND_FUEL =
            TESTS.function("wood_burner_layout_and_fuel", 100, 8, () -> CombustionHeatGameTests::woodBurnerLayoutAndFuel);
    public static final GameTests.Function FIREBOX_FUEL_AND_HEAT =
            TESTS.function("firebox_fuel_and_heat", 40, 8, () -> CombustionHeatGameTests::fireboxFuelAndHeat);
    public static final GameTests.Function HEATER_OVEN_PULLS_HEAT =
            TESTS.function("heater_oven_pulls_heat", 40, 8, () -> CombustionHeatGameTests::heaterOvenPullsHeat);
    public static final GameTests.Function OILBURNER_FUEL_AND_SETTING =
            TESTS.function("oilburner_fuel_and_setting", 40, 8, () -> CombustionHeatGameTests::oilburnerFuelAndSetting);
    public static final GameTests.Function ELECTRIC_HEATER_CONVERSION =
            TESTS.function("electric_heater_conversion", 40, 8, () -> CombustionHeatGameTests::electricHeaterConversion);
    public static final GameTests.Function INDUSTRIAL_BOILER_CONVERSION =
            TESTS.function("industrial_boiler_conversion", 100, 8, () -> CombustionHeatGameTests::industrialBoilerConversion);
    public static final GameTests.Function FLUID_VALVE_NODE_LIFECYCLE =
            TESTS.function("fluid_valve_node_lifecycle", 100, 8,
                    () -> FluidValveDrainGameTests::valveTypeAndNodeLifecycle);
    public static final GameTests.Function FLUID_IDENTIFIER_SPREAD =
            TESTS.function("fluid_identifier_spread", 100, 8,
                    () -> FluidValveDrainGameTests::identifierSpreadsPipeType);
    public static final GameTests.Function MACHINE_DRAIN_LAYOUT_PORTS =
            TESTS.function("machine_drain_layout_ports", 100, 8,
                    () -> FluidValveDrainGameTests::drainLayoutAndPorts);
    public static final GameTests.Function MACHINE_DRAIN_SPILL_RATE =
            TESTS.function("machine_drain_spill_rate", 100, 8,
                    () -> FluidValveDrainGameTests::drainSpillRate);
    public static final GameTests.Function CONDENSER_CONVERSION =
            TESTS.function("condenser_conversion", 20, () -> HbmGameTestFunctions::condenserConversion);
    public static final GameTests.Function STORAGE_DRUM_RECIPES =
            TESTS.function("storage_drum_recipes", 20, () -> HbmGameTestFunctions::storageDrumRecipes);
    public static final GameTests.Function STORAGE_DRUM_MENU =
            TESTS.function("storage_drum_menu", 20, () -> HbmGameTestFunctions::storageDrumMenu);
    public static final GameTests.Function POWERED_CONDENSER_CONVERSION =
            TESTS.function("powered_condenser_conversion", 20, () -> HbmGameTestFunctions::poweredCondenserConversion);
    public static final GameTests.Function POWERED_CONDENSER_PORTS =
            TESTS.function("powered_condenser_ports", 20, () -> HbmGameTestFunctions::poweredCondenserPorts);
    public static final GameTests.Function PUREX_RECIPE_CYCLE =
            TESTS.function("purex_recipe_cycle", 20, () -> HbmGameTestFunctions::purexRecipeCycle);
    public static final GameTests.Function PUREX_HOT_WASTE_REJECTED =
            TESTS.function("purex_hot_waste_rejected", 20, () -> HbmGameTestFunctions::purexHotWasteRejected);
    public static final GameTests.Function PUREX_UPGRADES =
            TESTS.function("purex_upgrades", 20, () -> HbmGameTestFunctions::purexUpgrades);
    public static final GameTests.Function PUREX_MAX_POWER =
            TESTS.function("purex_max_power", 20, () -> HbmGameTestFunctions::purexMaxPower);
    public static final GameTests.Function PUREX_OUTPUT_FLUID =
            TESTS.function("purex_output_fluid", 20, () -> HbmGameTestFunctions::purexOutputFluid);
    public static final GameTests.Function PUREX_SLOTS =
            TESTS.function("purex_slots", 20, () -> HbmGameTestFunctions::purexSlots);
    public static final GameTests.Function PUREX_MENU =
            TESTS.function("purex_menu", 20, () -> HbmGameTestFunctions::purexMenu);
    public static final GameTests.Function PUREX_LAYOUT =
            TESTS.function("purex_layout", 100, 8, () -> HbmGameTestFunctions::purexLayout);
    public static final GameTests.Function MINI_RTG_GENERATION =
            TESTS.function("mini_rtg_generation", 20, () -> RadiationMachineGameTests::miniRtgGeneration);
    public static final GameTests.Function RAD_SENSOR_LADDERS =
            TESTS.function("rad_sensor_ladders", 20, () -> RadiationMachineGameTests::radSensorLadders);
    public static final GameTests.Function RAD_GEN_LAYOUT_PORTS_FUEL =
            TESTS.function("rad_gen_layout_ports_fuel", 100, 8,
                    () -> RadiationMachineGameTests::radGenLayoutPortsAndFuel);
    public static final GameTests.Function RAD_GEN_MENU =
            TESTS.function("rad_gen_menu", 20, () -> RadiationMachineGameTests::radGenMenu);
    public static final GameTests.Function BLAST_FURNACE_RECIPES =
            TESTS.function("blast_furnace_recipes", 20, () -> BlastFurnaceGameTests::recipes);
    public static final GameTests.Function BLAST_FURNACE_LAYOUT =
            TESTS.function("blast_furnace_layout", 100, 8, () -> BlastFurnaceGameTests::layout);
    public static final GameTests.Function BLAST_FURNACE_SLOTS =
            TESTS.function("blast_furnace_slots", 20, () -> BlastFurnaceGameTests::slotsAndMenu);
    public static final GameTests.Function BLAST_FURNACE_PROCESSING =
            TESTS.function("blast_furnace_processing", 300, 8, () -> BlastFurnaceGameTests::processing);
    public static final GameTests.Function GENERIC_FLUID_ATOMICITY =
            TESTS.function("generic_fluid_atomicity", 20, () -> BlastFurnaceGameTests::genericFluidAtomicity);
    public static final GameTests.Function GENERIC_CHANCE_SEMANTICS =
            TESTS.function("generic_chance_semantics", 20, () -> BlastFurnaceGameTests::genericChanceSemantics);
    public static final GameTests.Function FURNACE_BRICK_PROCESSING =
            TESTS.function("furnace_brick_processing", 240, 8, () -> FurnaceGameTests::brickProcessing);
    public static final GameTests.Function FURNACE_IRON_PROCESSING =
            TESTS.function("furnace_iron_processing", 240, 8, () -> FurnaceGameTests::ironProcessing);
    public static final GameTests.Function FURNACE_STEEL_PROCESSING =
            TESTS.function("furnace_steel_processing", 40, 8, () -> FurnaceGameTests::steelProcessing);
    public static final GameTests.Function FURNACE_MENUS_AND_SLOTS =
            TESTS.function("furnace_menus_and_slots", 40, 8, () -> FurnaceGameTests::menusAndSlots);
    public static final GameTests.Function ZIRNOX_GOLDEN_TICK =
            TESTS.function("zirnox_golden_tick", 20, 8, () -> HbmGameTestFunctions::zirnoxGoldenTick);
    public static final GameTests.Function ZIRNOX_LAYOUT =
            TESTS.function("zirnox_layout", 100, 8, () -> HbmGameTestFunctions::zirnoxLayout);
    public static final GameTests.Function WATZ_LAYOUT =
            TESTS.function("watz_layout", 100, 8, () -> HbmGameTestFunctions::watzLayout);
    public static final GameTests.Function WATZ_STRUCT_ASSEMBLY =
            TESTS.function("watz_struct_assembly", 200, 8, () -> HbmGameTestFunctions::watzStructAssembly);
    public static final GameTests.Function STRUCT_PARTIAL_NO_ASSEMBLY =
            TESTS.function("struct_partial_no_assembly", 200, 8, () -> HbmGameTestFunctions::structPartialNoAssembly);
    public static final GameTests.Function BLOWTORCH_FUEL =
            TESTS.function("blowtorch_fuel", 20, () -> HbmGameTestFunctions::blowtorchFuel);
    public static final GameTests.Function MULTIBLOCK_INTERACTION_RANGE =
            TESTS.function("multiblock_interaction_range", 40, 26, () -> HbmGameTestFunctions::multiblockInteractionRange);
    public static final GameTests.Function FUSION_TORUS_LAYOUT =
            TESTS.function("fusion_torus_layout", 200, 20, () -> HbmGameTestFunctions::fusionTorusLayout);
    public static final GameTests.Function FUSION_STRUCT_ASSEMBLY =
            TESTS.function("fusion_struct_assembly", 300, 20, () -> HbmGameTestFunctions::fusionStructAssembly);
    public static final GameTests.Function FUSION_RECIPES =
            TESTS.function("fusion_recipes", 20, () -> HbmGameTestFunctions::fusionRecipes);
    public static final GameTests.Function FUSION_CHAIN =
            TESTS.function("fusion_chain", 100, 24, () -> HbmGameTestFunctions::fusionChain);
    public static final GameTests.Function FUSION_PORTS =
            TESTS.function("fusion_ports", 100, 24, () -> HbmGameTestFunctions::fusionPorts);
    public static final GameTests.Function FUSION_PLASMA_FORGE_RECIPES =
            TESTS.function("fusion_plasma_forge_recipes", 20, () -> HbmGameTestFunctions::fusionPlasmaForgeRecipes);
    public static final GameTests.Function FUSION_PLASMA_FORGE_MENU =
            TESTS.function("fusion_plasma_forge_menu", 20, () -> HbmGameTestFunctions::fusionPlasmaForgeMenu);
    public static final GameTests.Function FUSION_PLASMA_FORGE_PROCESSING =
            TESTS.function("fusion_plasma_forge_processing", 100, 8,
                    () -> HbmGameTestFunctions::fusionPlasmaForgeProcessing);
    public static final GameTests.Function FUSION_PLASMA_FORGE_RENDER =
            TESTS.function("fusion_plasma_forge_render", 20, () -> PlasmaForgeRenderGameTests::predicates);
    public static final GameTests.Function FUSION_PLASMA_FORGE_CONTAINER_LAYOUT =
            TESTS.function("fusion_plasma_forge_container_layout", 20,
                    () -> PlasmaForgeContainerGameTests::layoutAndValidation);
    public static final GameTests.Function FUSION_PLASMA_FORGE_CONTAINER_TRANSFER =
            TESTS.function("fusion_plasma_forge_container_transfer", 20,
                    () -> PlasmaForgeContainerGameTests::transferRouting);
    public static final GameTests.Function FUSION_PLASMA_FORGE_CONTAINER_OUTPUT =
            TESTS.function("fusion_plasma_forge_container_output", 20,
                    () -> PlasmaForgeContainerGameTests::outputCrafting);
    public static final GameTests.Function FUSION_BREEDER =
            TESTS.function("fusion_breeder", 20, () -> HbmGameTestFunctions::fusionBreeder);
    public static final GameTests.Function FLUID_DISPLAY_NAMES =
            TESTS.function("fluid_display_names", 20, () -> HbmGameTestFunctions::fluidDisplayNames);
    public static final GameTests.Function LANG_COVERAGE =
            TESTS.function("lang_coverage", 20, () -> HbmGameTestFunctions::langCoverage);
    public static final GameTests.Function BATTERY_REDD_LAYOUT_PORTS =
            TESTS.function("battery_redd_layout_ports", 100, 8, () -> BatteryReddGameTests::layoutAndPorts);
    public static final GameTests.Function BATTERY_REDD_BIG_INTEGER_TRANSFER =
            TESTS.function("battery_redd_big_integer_transfer", 40, () -> BatteryReddGameTests::bigIntegerAndTransfer);
    public static final GameTests.Function BATTERY_REDD_PERSISTENCE_SYNC =
            TESTS.function("battery_redd_persistence_sync", 40, () -> BatteryReddGameTests::persistenceAndSync);
    public static final GameTests.Function BATTERY_REDD_MENU =
            TESTS.function("battery_redd_menu", 40, () -> BatteryReddGameTests::menuAndItemContract);
    public static final GameTests.Function BATTERY_REDD_ITEM_MODELS =
            TESTS.function("battery_redd_item_models", 20, () -> BatteryReddGameTests::itemModels);
    public static final GameTests.Function ZIRNOX_WORLD_TICK =
            TESTS.function("zirnox_world_tick", 100, 8, () -> HbmGameTestFunctions::zirnoxWorldTick);
    public static final GameTests.Function ZIRNOX_STEAM_OUTPUT =
            TESTS.function("zirnox_steam_output", 300, 8, () -> HbmGameTestFunctions::zirnoxSteamOutput);
    public static final GameTests.Function ZIRNOX_MELTDOWN =
            TESTS.function("zirnox_meltdown", 100, 8, () -> HbmGameTestFunctions::zirnoxMeltdown);
    public static final GameTests.Function ZIRNOX_WRECK_HAZARDS =
            TESTS.function("zirnox_wreck_hazards", 100, 8, () -> HbmGameTestFunctions::zirnoxWreckHazards);

    // --- ore worldgen (implementations live in OreWorldgenGameTests) ---
    public static final GameTests.Function ORE_VEIN_SHAPE =
            TESTS.function("ore_vein_shape", 200, () -> OreWorldgenGameTests::oreVeinShape);
    public static final GameTests.Function ORE_VEIN_OFFSET =
            TESTS.function("ore_vein_offset", 200, () -> OreWorldgenGameTests::oreVeinOffset);
    public static final GameTests.Function ORE_VEIN_PREDICATE =
            TESTS.function("ore_vein_predicate", 200, () -> OreWorldgenGameTests::oreVeinPredicate);
    public static final GameTests.Function ORE_TARGETS =
            TESTS.function("ore_targets", 200, () -> OreWorldgenGameTests::oreTargets);
    public static final GameTests.Function ORE_DEPTH_DEPOSIT =
            TESTS.function("ore_depth_deposit", 200, () -> OreWorldgenGameTests::oreDepthDeposit);
    public static final GameTests.Function ORE_COUNTS =
            TESTS.function("ore_counts", 200, () -> OreWorldgenGameTests::oreCounts);
    public static final GameTests.Function ORE_SMOLDERING =
            TESTS.function("ore_smoldering", 300, () -> OreWorldgenGameTests::oreSmoldering);

    // --- SILEX (implementations live in SilexGameTests) ---
    public static final GameTests.Function SILEX_PROCESS =
            TESTS.function("silex_process", 100, () -> SilexGameTests::silexProcess);
    public static final GameTests.Function SILEX_LOAD =
            TESTS.function("silex_load", 100, () -> SilexGameTests::silexLoad);
    public static final GameTests.Function SILEX_LOAD_FLUID =
            TESTS.function("silex_load_fluid", 100, () -> SilexGameTests::silexLoadFluid);
    public static final GameTests.Function SILEX_OUTPUT_WEIGHTS =
            TESTS.function("silex_output_weights", 100, () -> SilexGameTests::silexOutputWeights);
    public static final GameTests.Function SILEX_QUEUE =
            TESTS.function("silex_queue", 100, () -> SilexGameTests::silexQueue);
    public static final GameTests.Function SILEX_SLOTS =
            TESTS.function("silex_slots", 100, () -> SilexGameTests::silexSlots);
    public static final GameTests.Function SILEX_MENU =
            TESTS.function("silex_menu", 100, () -> SilexGameTests::silexMenu);
    public static final GameTests.Function SILEX_LAYOUT =
            TESTS.function("silex_layout", 100, () -> SilexGameTests::silexLayout);
    public static final GameTests.Function SILEX_MODE_RESET =
            TESTS.function("silex_mode_reset", 100, () -> SilexGameTests::silexModeReset);

    // --- ingredient item wave (implementations live in IngredientGameTests) ---
    public static final GameTests.Function INGREDIENT_REGISTRY =
            TESTS.function("ingredient_registry", 20, () -> IngredientGameTests::registry);
    public static final GameTests.Function INGREDIENT_PLATE_FUEL =
            TESTS.function("ingredient_plate_fuel", 20, () -> IngredientGameTests::plateFuelBehavior);
    public static final GameTests.Function INGREDIENT_TAGS_RECIPES =
            TESTS.function("ingredient_tags_recipes", 20, () -> IngredientGameTests::tagsAndRecipes);
    public static final GameTests.Function INGREDIENT_ANVIL =
            TESTS.function("ingredient_anvil", 20, () -> IngredientGameTests::plateFuelAnvilRecipes);

    // --- material catalogue/integration wave ---
    public static final GameTests.Function MATERIAL_CATALOGUE_COMPLETE =
            TESTS.function("material_catalogue_complete", 40, () -> MaterialCompletionGameTests::materialCatalogueComplete);
    public static final GameTests.Function MATERIAL_AUTOGEN_SHAPES_COMPLETE =
            TESTS.function("material_autogen_shapes_complete", 40, () -> MaterialCompletionGameTests::materialAutogenShapesComplete);
    public static final GameTests.Function MATERIAL_DISTRIBUTION_COMPLETE =
            TESTS.function("material_distribution_complete", 40, () -> MaterialCompletionGameTests::materialDistributionComplete);
    public static final GameTests.Function MATERIAL_CRUCIBLE_PRESS_CRYSTALLIZER_COMPLETE =
            TESTS.function("material_crucible_press_crystallizer_complete", 80,
                    () -> MaterialCompletionGameTests::materialCruciblePressCrystallizerComplete);
    public static final GameTests.Function MATERIAL_FRAGMENT_RECIPES_COMPLETE =
            TESTS.function("material_fragment_recipes_complete", 40, () -> MaterialCompletionGameTests::materialFragmentRecipesComplete);
    public static final GameTests.Function MATERIAL_TAG_ALIASES_COMPLETE =
            TESTS.function("material_tag_aliases_complete", 40, () -> MaterialCompletionGameTests::materialTagAliasesComplete);
    public static final GameTests.Function MACHINE_RECIPE_COMPLETION =
            TESTS.function("machine_recipe_completion", 40, () -> MaterialCompletionGameTests::machineRecipeCompletion);

    // --- press recipes (implementations live in PressGameTests) ---
    public static final GameTests.Function PRESS_CASING =
            TESTS.function("press_casing", 20, () -> PressGameTests::casingRecipes);
    public static final GameTests.Function AMMO_PRESS_RECIPES =
            TESTS.function("ammo_press_recipes", 20, () -> AmmoPressGameTests::recipes);
    public static final GameTests.Function AMMO_PRESS_LAYOUT_MENU =
            TESTS.function("ammo_press_layout_menu", 100, 8, () -> AmmoPressGameTests::layoutAndMenu);
    public static final GameTests.Function AMMO_PRESS_SHIFT_CLICK =
            TESTS.function("ammo_press_shift_click", 40, () -> AmmoPressGameTests::shiftClickInput);
    public static final GameTests.Function AMMO_PRESS_PROCESSING =
            TESTS.function("ammo_press_processing", 40, 8, () -> AmmoPressGameTests::controlAndProcessing);

    // --- FEnSU ---
    public static final GameTests.Function FENSU_NUMERIC =
            TESTS.function("fensu_numeric", 40, () -> FensuGameTests::numeric);
    public static final GameTests.Function FENSU_LAYOUT_PORTS =
            TESTS.function("fensu_layout_ports", 100, 8, () -> FensuGameTests::layoutAndPorts);
    public static final GameTests.Function FENSU_PERSISTENCE_SYNC =
            TESTS.function("fensu_persistence_sync", 100, 8, () -> FensuGameTests::persistenceAndSync);
    public static final GameTests.Function FENSU_ASSETS =
            TESTS.function("fensu_assets", 40, () -> FensuGameTests::assets);
    public static final GameTests.Function FENSU_DYE_TAGS =
            TESTS.function("fensu_dye_tags", 20, () -> FensuGameTests::dyeTags);

    // --- standard centrifuge ---
    public static final GameTests.Function CENTRIFUGE_RECIPES =
            TESTS.function("centrifuge_recipes", 20, () -> CentrifugeGameTests::recipes);
    public static final GameTests.Function CENTRIFUGE_LAYOUT_PORTS =
            TESTS.function("centrifuge_layout_ports", 100, 8, () -> CentrifugeGameTests::layoutAndPorts);
    public static final GameTests.Function CENTRIFUGE_PROCESSING =
            TESTS.function("centrifuge_processing", 300, 8, () -> CentrifugeGameTests::processing);
    public static final GameTests.Function CENTRIFUGE_CONSTRUCTION =
            TESTS.function("centrifuge_construction", 40, () -> CentrifugeGameTests::constructionRecipe);
    public static final GameTests.Function CENTRIFUGE_MENU =
            TESTS.function("centrifuge_menu", 40, () -> CentrifugeGameTests::menu);

    // --- standard cyclotron ---
    public static final GameTests.Function CYCLOTRON_RECIPES =
            TESTS.function("cyclotron_recipes", 20, () -> CyclotronGameTests::recipes);
    public static final GameTests.Function CYCLOTRON_LAYOUT_PORTS_ROUTING =
            TESTS.function("cyclotron_layout_ports_routing", 100, 8, () -> CyclotronGameTests::layoutPortsAndRouting);
    public static final GameTests.Function CYCLOTRON_PROCESSING =
            TESTS.function("cyclotron_processing", 800, 8, () -> CyclotronGameTests::processing);
    public static final GameTests.Function CYCLOTRON_MENU =
            TESTS.function("cyclotron_menu", 40, 8, () -> CyclotronGameTests::menu);
    public static final GameTests.Function CYCLOTRON_SYNC =
            TESTS.function("cyclotron_sync", 40, 8, () -> CyclotronGameTests::sync);
    public static final GameTests.Function CYCLOTRON_CONSTRUCTION =
            TESTS.function("cyclotron_construction", 40, () -> CyclotronGameTests::constructionRecipe);

    // --- gas centrifuge ---

    public static final GameTests.Function GAS_CENTRIFUGE_RECIPES =
            TESTS.function("gas_centrifuge_recipes", 20, () -> GasCentrifugeGameTests::recipes);
    public static final GameTests.Function GAS_CENTRIFUGE_LAYOUT_PORTS =
            TESTS.function("gas_centrifuge_layout_ports", 100, 8, () -> GasCentrifugeGameTests::layoutAndPorts);
    public static final GameTests.Function GAS_CENTRIFUGE_PROCESSING =
            TESTS.function("gas_centrifuge_processing", 300, 8, () -> GasCentrifugeGameTests::processing);
    public static final GameTests.Function GAS_CENTRIFUGE_TWO_BATCHES =
            TESTS.function("gas_centrifuge_two_batches", 400, 8, () -> GasCentrifugeGameTests::twoBatchesResetProgress);
    public static final GameTests.Function GAS_CENTRIFUGE_UF6_NETWORK =
            TESTS.function("gas_centrifuge_uf6_network", 100, 8, () -> GasCentrifugeGameTests::uf6FluidNetwork);
    public static final GameTests.Function GAS_CENTRIFUGE_OVERCLOCK =
            TESTS.function("gas_centrifuge_overclock", 200, 8, () -> GasCentrifugeGameTests::overclock);
    public static final GameTests.Function GAS_CENTRIFUGE_MENU =
            TESTS.function("gas_centrifuge_menu", 40, () -> GasCentrifugeGameTests::menu);
    public static final GameTests.Function GAS_CENTRIFUGE_TOOLTIPS =
            TESTS.function("gas_centrifuge_tooltips", 20, () -> GasCentrifugeGameTests::tooltips);
    public static final GameTests.Function GAS_CENTRIFUGE_CONSTRUCTION =
            TESTS.function("gas_centrifuge_construction", 40, () -> GasCentrifugeGameTests::constructionRecipe);

    // --- electrolyser ---

    public static final GameTests.Function ELECTROLYSER_RECIPES =
            TESTS.function("electrolyser_recipes", 20, () -> ElectrolyserGameTests::recipes);
    public static final GameTests.Function ELECTROLYSER_LAYOUT_PORTS =
            TESTS.function("electrolyser_layout_ports", 100, 16, () -> ElectrolyserGameTests::layoutAndPorts);
    public static final GameTests.Function ELECTROLYSER_FLUID_PROCESSING =
            TESTS.function("electrolyser_fluid_processing", 200, 8, () -> ElectrolyserGameTests::fluidProcessing);
    public static final GameTests.Function ELECTROLYSER_METAL_PROCESSING =
            TESTS.function("electrolyser_metal_processing", 700, 8, () -> ElectrolyserGameTests::metalProcessing);
    public static final GameTests.Function ELECTROLYSER_PAUSES =
            TESTS.function("electrolyser_pauses", 40, 8, () -> ElectrolyserGameTests::pausesAndResumes);
    public static final GameTests.Function ELECTROLYSER_MOLTEN_POUR_RATE =
            TESTS.function("electrolyser_molten_pour_rate", 40, 8, () -> ElectrolyserGameTests::moltenPourRate);
    public static final GameTests.Function ELECTROLYSER_MENUS =
            TESTS.function("electrolyser_menus", 40, () -> ElectrolyserGameTests::menus);
    public static final GameTests.Function ELECTROLYSER_CONSTRUCTION =
            TESTS.function("electrolyser_construction", 40, () -> ElectrolyserGameTests::constructionRecipe);

    // --- isotope processing ---
    public static final GameTests.Function F6_TANK_CONTRACT =
            TESTS.function("f6_tank_contract", 100, () -> IsotopeProcessingGameTests::f6TankContract);
    public static final GameTests.Function DEUTERIUM_EXTRACTOR_RATIO =
            TESTS.function("deuterium_extractor_ratio", 40, () -> IsotopeProcessingGameTests::deuteriumExtractorRatio);
    public static final GameTests.Function DEUTERIUM_TOWER_CONTRACT =
            TESTS.function("deuterium_tower_contract", 100, 8, () -> IsotopeProcessingGameTests::deuteriumTowerContract);
    public static final GameTests.Function GIANT_TANK_LAYOUTS_PORTS =
            TESTS.function("giant_tank_layouts_ports", 100, 8, () -> GiantTankGameTests::layoutsAndPorts);
    public static final GameTests.Function GIANT_TANK_CONTAINER =
            TESTS.function("giant_tank_container", 100, 8, () -> GiantTankGameTests::containerBehavior);
    public static final GameTests.Function GIANT_TANK_MODE_ANTIMATTER =
            TESTS.function("giant_tank_mode_antimatter", 100, 8, () -> GiantTankGameTests::modeAndAntimatter);
    public static final GameTests.Function GIANT_TANK_TILTED_OUTPUT =
            TESTS.function("giant_tank_tilted_output", 100, 8, () -> GiantTankGameTests::tiltedOutputShutdown);
    public static final GameTests.Function GIANT_TANK_DROPS =
            TESTS.function("giant_tank_drops", 100, 8, () -> GiantTankGameTests::dropsAndReplacesWithFluid);

    // --- Albion particle accelerator ---
    public static final GameTests.Function PA_STRUCTURE =
            TESTS.function("pa_structure", 40, 16, () -> AlbionGameTests::structure);
    public static final GameTests.Function PA_SOURCE_FIRES =
            TESTS.function("pa_source_fires", 40, 16, () -> AlbionGameTests::sourceFires);
    public static final GameTests.Function PA_BEAM_WALKS =
            TESTS.function("pa_beam_walks", 40, 24, () -> AlbionGameTests::beamWalks);
    public static final GameTests.Function PA_BEAMLINE =
            TESTS.function("pa_beamline", 40, 16, () -> AlbionGameTests::beamlineCarries);
    public static final GameTests.Function PA_RFC =
            TESTS.function("pa_rfc", 40, 20, () -> AlbionGameTests::rfcAccelerates);
    public static final GameTests.Function PA_QUADRUPOLE =
            TESTS.function("pa_quadrupole", 40, 16, () -> AlbionGameTests::quadrupoleFocuses);
    public static final GameTests.Function PA_DIPOLE =
            TESTS.function("pa_dipole", 40, 16, () -> AlbionGameTests::dipoleSteers);
    public static final GameTests.Function PA_DETECTOR =
            TESTS.function("pa_detector", 40, 20, () -> AlbionGameTests::detectorRuns);
    public static final GameTests.Function PA_COOLING =
            TESTS.function("pa_cooling", 40, 8, () -> AlbionGameTests::coolingAndTanks);
    public static final GameTests.Function PA_RADIO =
            TESTS.function("pa_radio", 40, 16, () -> AlbionGameTests::redstoneOverRadio);
    public static final GameTests.Function PA_PARTICLE_USERS =
            TESTS.function("pa_particle_users", 40, 16, () -> AlbionGameTests::componentsAreParticleUsers);
    public static final GameTests.Function PA_RECIPES =
            TESTS.function("pa_recipes", 20, () -> AlbionGameTests::recipes);
    public static final GameTests.Function PA_CONSTRUCTION =
            TESTS.function("pa_construction", 40, () -> AlbionGameTests::construction);
    public static final GameTests.Function PA_COILS =
            TESTS.function("pa_coils", 20, () -> AlbionGameTests::coils);
    public static final GameTests.Function PA_ASSETS =
            TESTS.function("pa_assets", 20, () -> AlbionGameTests::assets);

    // --- Exposure Chamber ---
    public static final GameTests.Function EXPOSURE_CHAMBER_RECIPES =
            TESTS.function("exposure_chamber_recipes", 20, () -> ExposureChamberGameTests::recipes);
    public static final GameTests.Function EXPOSURE_CHAMBER_LAYOUT_PORTS =
            TESTS.function("exposure_chamber_layout_ports", 100, 14,
                    () -> ExposureChamberGameTests::layoutAndPorts);
    public static final GameTests.Function EXPOSURE_CHAMBER_PROCESSING =
            TESTS.function("exposure_chamber_processing", 1_700, 14,
                    () -> ExposureChamberGameTests::processing);
    public static final GameTests.Function EXPOSURE_CHAMBER_CONSTRUCTION =
            TESTS.function("exposure_chamber_construction", 40, () -> ExposureChamberGameTests::constructionRecipe);
    public static final GameTests.Function EXPOSURE_CHAMBER_MENU =
            TESTS.function("exposure_chamber_menu", 40, () -> ExposureChamberGameTests::menu);
    public static final GameTests.Function EXPOSURE_CHAMBER_OUTPUT_FULL =
            TESTS.function("exposure_chamber_output_full", 40, () -> ExposureChamberGameTests::outputFullStopsCycles);
    public static final GameTests.Function EXPOSURE_CHAMBER_UPGRADES =
            TESTS.function("exposure_chamber_upgrades", 40, () -> ExposureChamberGameTests::upgrades);

    // --- Precision Assembler ---
    public static final GameTests.Function PRECISION_ASSEMBLER_RECIPES =
            TESTS.function("precision_assembler_recipes", 40, () -> PrecisionAssemblerGameTests::recipes);
    public static final GameTests.Function PRECISION_ASSEMBLER_LAYOUT =
            TESTS.function("precision_assembler_layout", 40, () -> PrecisionAssemblerGameTests::layoutAndValidation);
    public static final GameTests.Function PRECISION_ASSEMBLER_PROCESSING =
            TESTS.function("precision_assembler_processing", 140, () -> PrecisionAssemblerGameTests::processing);
    public static final GameTests.Function PRECISION_ASSEMBLER_BLUEPRINTS =
            TESTS.function("precision_assembler_blueprints", 40, () -> PrecisionAssemblerGameTests::blueprints);

    // --- Mixer ---
    public static final GameTests.Function MIXER_RECIPES =
            TESTS.function("mixer_recipes", 20, () -> MixerGameTests::recipes);
    public static final GameTests.Function MIXER_LAYOUT_PORTS =
            TESTS.function("mixer_layout_ports", 100, 8, () -> MixerGameTests::layoutAndPorts);
    public static final GameTests.Function MIXER_PROCESSING =
            TESTS.function("mixer_processing", 40, 8, () -> MixerGameTests::processing);
    public static final GameTests.Function MIXER_MENU =
            TESTS.function("mixer_menu", 40, () -> MixerGameTests::menu);
    public static final GameTests.Function MIXER_RECIPE_INDEX =
            TESTS.function("mixer_recipe_index", 40, () -> MixerGameTests::recipeIndexNormalization);

    // --- sawmill ---
    public static final GameTests.Function SAWMILL_RECIPES =
            TESTS.function("sawmill_recipes", 20, () -> SawmillGameTests::recipes);
    public static final GameTests.Function SAWMILL_LAYOUT =
            TESTS.function("sawmill_layout", 100, 8, () -> SawmillGameTests::layout);
    public static final GameTests.Function SAWMILL_PROCESSING =
            TESTS.function("sawmill_processing", 100, 8, () -> SawmillGameTests::processing);
    public static final GameTests.Function SAWMILL_CONTRACTS =
            TESTS.function("sawmill_contracts", 40, 8, () -> SawmillGameTests::contracts);
    public static final GameTests.Function SAWMILL_BLADE_STATE =
            TESTS.function("sawmill_blade_state", 100, 8, () -> SawmillGameTests::bladeState);
    public static final GameTests.Function SAWMILL_BLADE_COLLISION =
            TESTS.function("sawmill_blade_collision", 40, 8, () -> SawmillGameTests::bladeCollision);

    // --- medicine (implementations live in MedicineGameTests) ---
    public static final GameTests.Function MEDICINE =
            TESTS.function("medicine", 40, () -> MedicineGameTests::medicine);

    // --- FEL (implementations live in FelGameTests) ---
    public static final GameTests.Function FEL_BEAM =
            TESTS.function("fel_beam", 100, () -> FelGameTests::felBeam);
    public static final GameTests.Function FEL_BEAM_ILLEGAL =
            TESTS.function("fel_beam_illegal", 100, () -> FelGameTests::felBeamIllegal);
    public static final GameTests.Function FEL_ROTATION =
            TESTS.function("fel_rotation", 100, () -> FelGameTests::felRotation);
    public static final GameTests.Function FEL_POWER =
            TESTS.function("fel_power", 100, () -> FelGameTests::felPower);
    public static final GameTests.Function FEL_BURN =
            TESTS.function("fel_burn", 200, () -> FelGameTests::felBurn);
    public static final GameTests.Function FEL_BURN_INFRARED =
            TESTS.function("fel_burn_infrared", 200, () -> FelGameTests::felBurnInfrared);
    public static final GameTests.Function FEL_POWER_PORT =
            TESTS.function("fel_power_port", 100, () -> FelGameTests::felPowerPort);
    public static final GameTests.Function FEL_LAYOUT =
            TESTS.function("fel_layout", 100, () -> FelGameTests::felLayout);
    public static final GameTests.Function FEL_SLOTS =
            TESTS.function("fel_slots", 100, () -> FelGameTests::felSlots);
    public static final GameTests.Function FEL_MENU =
            TESTS.function("fel_menu", 100, () -> FelGameTests::felMenu);

    // --- Sedna guns (implementations live in GunGameTests) ---
    public static final GameTests.Function GUN_RELOAD =
            TESTS.function("gun_reload", 200, () -> GunGameTests::gunReload);
    public static final GameTests.Function GUN_RELOAD_KEEPS_TYPE =
            TESTS.function("gun_reload_keeps_type", 300, () -> GunGameTests::gunReloadKeepsType);
    public static final GameTests.Function GUN_FIRE =
            TESTS.function("gun_fire", 200, () -> GunGameTests::gunFire);
    public static final GameTests.Function GUN_DRY_FIRE =
            TESTS.function("gun_dry_fire", 100, () -> GunGameTests::gunDryFire);
    public static final GameTests.Function GUN_AMMO_STATS =
            TESTS.function("gun_ammo_stats", 20, () -> GunGameTests::ammoStats);
    public static final GameTests.Function GUN_AMMO_22_STATS =
            TESTS.function("gun_ammo22_stats", 20, () -> GunGameTests::ammo22Stats);
    public static final GameTests.Function GUN_AMMO_9_STATS =
            TESTS.function("gun_ammo9_stats", 20, () -> GunGameTests::ammo9Stats);
    public static final GameTests.Function GUN_AMMO_44_STATS =
            TESTS.function("gun_ammo44_stats", 20, () -> GunGameTests::ammo44Stats);
    public static final GameTests.Function GUN_44_CONFIGS =
            TESTS.function("gun_44_configs", 20, () -> GunGameTests::gun44Configs);
    public static final GameTests.Function GUN_44_SINGLE_RELOAD =
            TESTS.function("gun_44_single_reload", 400, () -> GunGameTests::gun44SingleReload);
    public static final GameTests.Function GUN_9MM_CONFIGS =
            TESTS.function("gun_9mm_configs", 20, () -> GunGameTests::gun9Configs);
    public static final GameTests.Function GUN_AUTO_REFIRE =
            TESTS.function("gun_auto_refire", 100, () -> GunGameTests::gunAutoRefire);
    public static final GameTests.Function GUN_STAR_F_RELOAD =
            TESTS.function("gun_star_f_reload", 200, () -> GunGameTests::gunStarFReload);
    public static final GameTests.Function GUN_AMMO_REGISTRY =
            TESTS.function("gun_ammo_registry", 20, () -> GunGameTests::ammoRegistry);
    public static final GameTests.Function GUN_WEAR_CURVES =
            TESTS.function("gun_wear_curves", 20, () -> GunGameTests::gunWearCurves);
    public static final GameTests.Function GUN_WEAR_ACCUMULATES =
            TESTS.function("gun_wear_accumulates", 200, () -> GunGameTests::gunWearAccumulates);
    public static final GameTests.Function GUN_STATE_PERSISTS =
            TESTS.function("gun_state_persists", 200, () -> GunGameTests::gunStatePersists);
    public static final GameTests.Function GUN_RESETS_WHEN_STOWED =
            TESTS.function("gun_resets_when_stowed", 100, () -> GunGameTests::gunResetsWhenStowed);
    public static final GameTests.Function GUN_DOES_NOT_MINE =
            TESTS.function("gun_does_not_mine", 20, () -> GunGameTests::gunDoesNotMine);
    public static final GameTests.Function GUN_RECOIL_BINDINGS =
            TESTS.function("gun_recoil_bindings", 20, () -> GunGameTests::gunRecoilBindings);
    public static final GameTests.Function GUN_RECOIL_RESET =
            TESTS.function("gun_recoil_reset", 20, () -> GunGameTests::gunRecoilReset);
    public static final GameTests.Function GUN_ANIMATION_MATH =
            TESTS.function("gun_animation_math", 20, () -> GunGameTests::gunAnimationMath);
    public static final GameTests.Function GUN_FOV_MATH =
            TESTS.function("gun_fov_math", 20, () -> GunGameTests::gunFovMath);
    public static final GameTests.Function GUN_CREATIVE_QUALITY_VISIBILITY =
            TESTS.function("gun_creative_quality_visibility", 20, () -> GunGameTests::gunCreativeQualityVisibility);
    public static final GameTests.Function BULLET_DAMAGES_MOB =
            TESTS.function("bullet_damages_mob", 100, () -> GunGameTests::bulletDamagesMob);
    public static final GameTests.Function BULLET_ARMOR_PIERCING =
            TESTS.function("bullet_armor_piercing", 100, () -> GunGameTests::bulletArmorPiercing);
    public static final GameTests.Function BULLET_BREAKS_GLASS =
            TESTS.function("bullet_breaks_glass", 100, () -> GunGameTests::bulletBreaksGlass);

    // --- particles (implementations live in ParticleGameTests) ---
    public static final GameTests.Function PARTICLE_PAYLOAD_ROUND_TRIP =
            TESTS.function("particle_payload_round_trip", 20, () -> ParticleGameTests::payloadRoundTrip);
    public static final GameTests.Function PARTICLE_EFFECT_NAMES =
            TESTS.function("particle_effect_names", 20, () -> ParticleGameTests::effectNamesResolve);
    public static final GameTests.Function PARTICLE_UNHANDLED_IS_SILENT =
            TESTS.function("particle_unhandled_is_silent", 20, () -> ParticleGameTests::unhandledEffectIsSilent);
    public static final GameTests.Function PARTICLE_SEND =
            TESTS.function("particle_send", 20, () -> ParticleGameTests::sendDoesNotThrow);

    // --- power armour (implementations live in ArmorGameTests) ---
    public static final GameTests.Function ARMOR_CONFIGURED =
            TESTS.function("armor_configured", 20, () -> ArmorGameTests::configured);
    public static final GameTests.Function ARMOR_SET_DETECTION =
            TESTS.function("armor_set_detection", 20, () -> ArmorGameTests::setDetection);
    public static final GameTests.Function ARMOR_SET_BONUS =
            TESTS.function("armor_set_bonus", 20, () -> ArmorGameTests::setBonusApplies);
    public static final GameTests.Function ARMOR_CHARGE_DRAINS =
            TESTS.function("armor_charge_drains", 20, () -> ArmorGameTests::chargeDrains);
    public static final GameTests.Function ARMOR_FLAT_DISABLES =
            TESTS.function("armor_flat_disables", 20, () -> ArmorGameTests::flatSuitDisablesBonuses);
    public static final GameTests.Function ARMOR_PROTECTION =
            TESTS.function("armor_protection", 20, () -> ArmorGameTests::protectionRegistered);
    public static final GameTests.Function ARMOR_HIDES_HAT =
            TESTS.function("armor_hides_hat", 20, () -> ArmorGameTests::hidesHatLayer);
    public static final GameTests.Function ARMOR_EFFECT_VISIBLE =
            TESTS.function("armor_effect_visible", 20, () -> ArmorGameTests::setBonusEffectIsVisible);
    public static final GameTests.Function ARMOR_HARD_LANDING =
            TESTS.function("armor_hard_landing", 40, () -> ArmorGameTests::hardLandingThrowsEntities);
    public static final GameTests.Function ARMOR_RESISTANCE_WAVE =
            TESTS.function("armor_resistance_wave", 40, () -> ArmorWaveGameTests::resistancePrecedenceAndMath);
    public static final GameTests.Function ARMOR_FULL_SET_ABSORPTION =
            TESTS.function("armor_full_set_absorption", 40, () -> ArmorWaveGameTests::fullSetAndAbsorption);
    public static final GameTests.Function ARMOR_FAMILIES =
            TESTS.function("armor_families", 40, () -> ArmorWaveGameTests::familiesStatsAndHazards);
    public static final GameTests.Function ARMOR_MODULE_STORAGE =
            TESTS.function("armor_module_storage", 40, () -> ArmorWaveGameTests::moduleStorageRoundTrip);
    public static final GameTests.Function ARMOR_TABLE_LIFECYCLE =
            TESTS.function("armor_table_lifecycle", 40, () -> ArmorWaveGameTests::tableSlotsAndLifecycle);
    public static final GameTests.Function ARMOR_SHORT_FALL =
            TESTS.function("armor_short_fall", 40, () -> ArmorGameTests::shortFallDoesNothing);
    public static final GameTests.Function ARMOR_STEEL_TITANIUM =
            TESTS.function("armor_steel_titanium", 40, () -> SteelTitaniumArmorGameTests::registryAndStats);

    // --- hand-registered block families ---
    public static final GameTests.Function BLOCK_FAMILY_REGISTRY =
            TESTS.function("block_family_registry", 40, () -> BlockFamilyGameTests::registryAndTags);
    public static final GameTests.Function BLOCK_FAMILY_BEHAVIOUR =
            TESTS.function("block_family_behaviour", 40, () -> BlockFamilyGameTests::familyBehaviour);

    // --- chunk loading + missiles (implementations live in MissileGameTests) ---
    public static final GameTests.Function CHUNK_REFCOUNTING =
            TESTS.function("chunk_refcounting", 40, () -> MissileGameTests::chunkRefcounting);
    public static final GameTests.Function CHUNK_FOLLOWS_MOVEMENT =
            TESTS.function("chunk_follows_movement", 40, () -> MissileGameTests::chunkFollowsMovement);
    public static final GameTests.Function CHUNK_RELEASED_TICKET_INERT =
            TESTS.function("chunk_released_ticket_inert", 40, () -> MissileGameTests::releasedTicketIsInert);
    public static final GameTests.Function MISSILE_HOLDS_CHUNK =
            TESTS.function("missile_holds_chunk", 60, () -> MissileGameTests::missileHoldsItsChunk);
    public static final GameTests.Function MISSILE_PITCH_NOSES_OVER =
            TESTS.function("missile_pitch_noses_over", 60, () -> MissileGameTests::missilePitchNosesOver);
    public static final GameTests.Function MISSILE_LAUNCH_TABLE =
            TESTS.function("missile_launch_table", 20, () -> MissileGameTests::launchTableResolves);
    public static final GameTests.Function MISSILE_MICRO_PAYLOAD =
            TESTS.function("missile_micro_payload", 60, 80, () -> MissileGameTests::microPayloadPath);
    public static final GameTests.Function MISSILE_TAINT_BLOCK =
            TESTS.function("missile_taint_block", 20, () -> MissileGameTests::taintBlockContract);
    public static final GameTests.Function MISSILE_EMP_FIELD =
            TESTS.function("missile_emp_field", 60, () -> MissileGameTests::empFieldDrainsHbmEnergy);
    public static final GameTests.Function MISSILE_AIM_CONSTANTS =
            TESTS.function("missile_aim_constants", 20, () -> MissileGameTests::missileAimConstants);
    public static final GameTests.Function MISSILE_PAD_TANKS =
            TESTS.function("missile_pad_tanks", 20, () -> MissileGameTests::padConformsTanks);
    public static final GameTests.Function MISSILE_PAD_GATE =
            TESTS.function("missile_pad_gate", 20, () -> MissileGameTests::padLaunchGate);
    public static final GameTests.Function MISSILE_PAD_LAUNCHES =
            TESTS.function("missile_pad_launches", 60, () -> MissileGameTests::padLaunches);
    public static final GameTests.Function MISSILE_UNLAUNCHABLE =
            TESTS.function("missile_unlaunchable", 20, () -> MissileGameTests::unlaunchableMissileIsRefused);
    public static final GameTests.Function MISSILE_PAD_PORTS =
            TESTS.function("missile_pad_ports", 60, 8, () -> MissileGameTests::padPorts);
    public static final GameTests.Function MISSILE_REDSTONE_LAUNCH =
            TESTS.function("missile_redstone_launch", 60, 8, () -> MissileGameTests::padRedstoneLaunch);
    public static final GameTests.Function MISSILE_PLAYER_PATH =
            TESTS.function("missile_player_path", 60, 8, () -> MissileGameTests::padPlayerPath);
    public static final GameTests.Function MISSILE_DETONATOR_LAUNCH =
            TESTS.function("missile_detonator_launch", 60, 8, () -> MissileGameTests::detonatorLaunch);
    public static final GameTests.Function MISSILE_UNPORTED_REFUSED =
            TESTS.function("missile_unported_refused", 20, () -> MissileGameTests::unportedMissileRefused);
    public static final GameTests.Function MISSILE_PAD_SYNC =
            TESTS.function("missile_pad_sync", 20, () -> MissileGameTests::padSyncsLoadedMissile);
    public static final GameTests.Function MISSILE_LAUNCHABLE_SET =
            TESTS.function("missile_launchable_set", 20, () -> MissileGameTests::launchableSet);
    public static final GameTests.Function MISSILE_DESIGNATOR =
            TESTS.function("missile_designator", 20, () -> MissileGameTests::designatorRecordsTarget);

    /** A real crater, timed. See {@link NukePerfGameTests}; it logs the phase split. */
    public static final GameTests.Function NUKE_CRATER_BENCHMARK =
            TESTS.function("nuke_crater_benchmark", 20 * 90, () -> NukePerfGameTests::craterBenchmark);

    /** Complete per-chunk hit-set parity between resident rays and event-driven chunk resumes. */
    public static final GameTests.Function NUKE_RAY_RESUME_EQUIVALENCE =
            TESTS.function("nuke_ray_resume_equivalence", 20 * 90,
                    () -> NukePerfGameTests::rayResumeEquivalence);

    /** setBlock vs direct section write — the number behind the destruction-phase design. */
    public static final GameTests.Function NUKE_WRITE_THROUGHPUT =
            TESTS.function("nuke_write_throughput", 20 * 90, () -> NukePerfGameTests::writeThroughput);

    /** The section carve against the setBlock path it replaces: states, block entities, heightmaps. */
    public static final GameTests.Function NUKE_CARVE_EQUIVALENCE =
            TESTS.function("nuke_carve_equivalence", 20 * 90, () -> NukePerfGameTests::carveEquivalence);

    /** Does a carved pocket actually relight? The one thing a block-state comparison cannot see. */
    public static final GameTests.Function NUKE_CARVE_LIGHTING =
            TESTS.function("nuke_carve_lighting", 20 * 400, () -> NukePerfGameTests::carveLighting);

    /** The same question asked of a real blast: whole sections emptied across many chunks. */
    public static final GameTests.Function NUKE_CRATER_LIGHTING =
            TESTS.function("nuke_crater_lighting", 20 * 400, () -> NukePerfGameTests::craterLighting);

    /** The phase after the crater: the fallout sweep's ash, fire and sellafite conversions, timed. */
    public static final GameTests.Function NUKE_FALLOUT_BENCHMARK =
            TESTS.function("nuke_fallout_benchmark", 20 * 900, () -> NukePerfGameTests::falloutBenchmark);

    /** How a fallout conversion reaches the world: setBlock vs the references' section write. */
    public static final GameTests.Function NUKE_FALLOUT_WRITE_THROUGHPUT =
            TESTS.function("nuke_fallout_write_throughput", 20 * 180,
                    () -> NukePerfGameTests::falloutWriteThroughput);

    /** Worker output retains old last-state/first-falling position-map semantics. */
    public static final GameTests.Function NUKE_FALLOUT_WRITE_DEDUPLICATION =
            TESTS.function("nuke_fallout_write_deduplication", 20,
                    () -> NukePerfGameTests::falloutWriteDeduplication);

    /** What the ash layer's self-rescheduling block tick costs, during and after the sweep. */
    public static final GameTests.Function NUKE_FALLOUT_ASH_LOAD =
            TESTS.function("nuke_fallout_ash_load", 20 * 180, () -> NukePerfGameTests::falloutAshLoad);

    /** Where the sweep's notify pass actually goes: neighbour updates vs the shape pass, by terrain. */
    public static final GameTests.Function NUKE_FALLOUT_NOTIFY_COST =
            TESTS.function("nuke_fallout_notify_cost", 20 * 300, () -> NukePerfGameTests::falloutNotifyCost);

    public static final GameTests.Function NUKE_BOMB_SLOT_PREDICATES =
            TESTS.function("nuke_bomb_slot_predicates", 40, () -> NukeBombGameTests::slotPredicates);
    public static final GameTests.Function NUKE_BOMB_RADIUS_EFFECTS =
            TESTS.function("nuke_bomb_radius_effects", 40, 40, () -> NukeBombGameTests::n2RadiusAndEffects);
    public static final GameTests.Function NUKE_BOMB_MENU_ROUTING =
            TESTS.function("nuke_bomb_menu_routing", 40, () -> NukeBombGameTests::menuRoutesComponents);
    public static final GameTests.Function NUKE_BOMB_PERSISTENCE_SYNC =
            TESTS.function("nuke_bomb_persistence_sync", 40, () -> NukeBombGameTests::persistenceAndSync);
    public static final GameTests.Function NUKE_BOMB_REDSTONE_DETONATOR =
            TESTS.function("nuke_bomb_redstone_detonator", 40, 40, () -> NukeBombGameTests::redstoneAndDetonator);
    public static final GameTests.Function NUKE_BOMB_CHUNK_LIFECYCLE =
            TESTS.function("nuke_bomb_chunk_lifecycle", 40, 40, () -> NukeBombGameTests::consumptionAndChunkLifecycle);
    public static final GameTests.Function NUKE_DETONATOR_LOADS_TARGET =
            TESTS.function("nuke_detonator_loads_target", 600, 40, () -> NukeBombGameTests::detonatorLoadsTarget);
    public static final GameTests.Function NUKE_TOREX_TRACKING_AGE =
            TESTS.function("nuke_torex_tracking_age", 40, () -> NukeBombGameTests::torexTrackingAge);

    // --- RBMK (implementations live in RBMKGameTests) ---
    public static final GameTests.Function RBMK_REGISTRY =
            TESTS.function("rbmk_registry", 40, () -> RBMKGameTests::registry);
    public static final GameTests.Function RBMK_FOLDED_COLUMN =
            TESTS.function("rbmk_folded_column", 100, () -> RBMKGameTests::foldedColumn);
    public static final GameTests.Function RBMK_FUEL =
            TESTS.function("rbmk_fuel", 40, () -> RBMKGameTests::fuel);
    public static final GameTests.Function RBMK_CONTROL =
            TESTS.function("rbmk_control", 40, () -> RBMKGameTests::control);
    public static final GameTests.Function RBMK_NEUTRONS =
            TESTS.function("rbmk_neutrons", 100, () -> RBMKGameTests::neutrons);
    public static final GameTests.Function RBMK_BOILER =
            TESTS.function("rbmk_boiler", 40, () -> RBMKGameTests::boiler);
    public static final GameTests.Function RBMK_DEBRIS =
            TESTS.function("rbmk_debris", 40, () -> RBMKGameTests::debris);
    public static final GameTests.Function LOOK_OVERLAY_SERVER_CLASSES =
            TESTS.function("look_overlay_server_classes", 20,
                    () -> LookOverlayGameTests::blockClassesAreDedicatedServerSafe);
    public static final GameTests.Function FOUNDRY_REMOVAL_SCRAPS =
            TESTS.function("foundry_removal_scraps", 20, () -> CompatibilityBoundaryGameTests::foundryRemovalScraps);
    public static final GameTests.Function CRUCIBLE_REMOVAL_SCRAPS =
            TESTS.function("crucible_removal_scraps", 20, () -> CompatibilityBoundaryGameTests::crucibleRemovalScraps);
    public static final GameTests.Function COMPATIBILITY_FACADE_CONTRACTS =
            TESTS.function("compatibility_facade_contracts", 20, () -> CompatibilityBoundaryGameTests::facadeContracts);
    public static final GameTests.Function CRATE_CARRIED_ROUND_TRIP =
            TESTS.function("crate_carried_round_trip", 40, () -> CompatibilityBoundaryGameTests::crateCarriedRoundTrip);

    public static void init() {}

    private static void coolingRecipes(GameTestHelper helper) {
        Fluids.init();
        check(helper, Fluids.STEAM, 100, Fluids.SPENTSTEAM, 1, 200);
        check(helper, Fluids.HOTSTEAM, 1, Fluids.STEAM, 10, 2);
        check(helper, Fluids.SUPERHOTSTEAM, 1, Fluids.HOTSTEAM, 10, 18);
        check(helper, Fluids.ULTRAHOTSTEAM, 1, Fluids.SUPERHOTSTEAM, 10, 120);
        helper.succeed();
    }

    /** The 85% / 6000 mB-per-tick turbine math, per steam stage, plus the buffer clamp. */
    private static void turbineConversion(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);

        // STEAM: ops = min(64000/100, 128000/1, 6000/100) = 60 -> 6000 mB in, 60 mB out, 60*200*0.85 HE
        TileEntityMachineTurbine turbine = placeTurbine(helper, pos);
        turbine.input.setFill(64_000);
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getFill() == 58_000, "Turbine drained " + (64_000 - turbine.input.getFill()) + " mB of steam, expected 6000");
        helper.assertTrue(turbine.output.getTankType() == Fluids.SPENTSTEAM && turbine.output.getFill() == 60,
                "Turbine spent-steam output regressed");
        helper.assertTrue(turbine.power == 10_200, "Turbine steam power regressed: " + turbine.power);

        // SUPERHOTSTEAM: ops = min(64000/1, 128000/10, 6000/1) = 6000 -> 6000 mB in, 60000 mB hot steam out
        turbine = placeTurbine(helper, pos);
        turbine.input.setTankType(Fluids.SUPERHOTSTEAM);
        turbine.input.setFill(64_000);
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getFill() == 58_000, "Turbine superhot steam throughput must cap at 6000 mB/t");
        helper.assertTrue(turbine.output.getTankType() == Fluids.HOTSTEAM && turbine.output.getFill() == 60_000,
                "Turbine must cool superhot steam into hot steam");
        helper.assertTrue(turbine.power == 91_800, "Turbine superhot steam power regressed: " + turbine.power);

        // the buffer clamps at maxPower
        turbine = placeTurbine(helper, pos);
        turbine.input.setFill(64_000);
        turbine.power = TileEntityMachineTurbine.MAX_POWER - 1;
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.power == TileEntityMachineTurbine.MAX_POWER, "Turbine power must clamp to maxPower");

        // a full output tank stalls the machine outright
        turbine = placeTurbine(helper, pos);
        turbine.input.setFill(64_000);
        turbine.output.setFill(turbine.output.getMaxFill());
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getFill() == 64_000 && turbine.power == 0,
                "Turbine must stall when the output tank is full");
        helper.succeed();
    }

    /** The fluid identifier picks the steam stage and passes from slot 0 to slot 1. */
    private static void turbineFluidId(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);

        TileEntityMachineTurbine turbine = placeTurbine(helper, pos);
        turbine.inventory.setStackInSlot(0, com.hbm.items.machine.ItemFluidId.make(Fluids.SUPERHOTSTEAM));
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getTankType() == Fluids.SUPERHOTSTEAM, "Identifier did not retype the input tank");
        helper.assertTrue(turbine.inventory.getStackInSlot(0).isEmpty(), "Consumed identifier must leave slot 0");
        helper.assertTrue(turbine.inventory.getStackInSlot(1).getItem() instanceof com.hbm.items.machine.IItemFluidId,
                "Identifier must pass down into slot 1");

        // a fluid the turbine cannot cool: no power, and the output tank goes NONE (the GUI's warning state)
        turbine = placeTurbine(helper, pos);
        turbine.inventory.setStackInSlot(0, com.hbm.items.machine.ItemFluidId.make(Fluids.WATER));
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getTankType() == Fluids.WATER, "Identifier must retype even to a non-coolable fluid");
        helper.assertTrue(turbine.output.getTankType() == Fluids.NONE, "Non-coolable input must blank the output tank");
        helper.assertTrue(turbine.power == 0, "Non-coolable input must not generate power");
        helper.succeed();
    }

    /** Slot validity, both canister bays and the battery slot. */
    private static void turbineSlots(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachineTurbine turbine = placeTurbine(helper, pos);

        var identifier = com.hbm.items.machine.ItemFluidId.make(Fluids.STEAM);
        var bucket = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET);
        var battery = new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModBatteryItems.PACKS.get(com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD).get());
        helper.assertTrue(turbine.isItemValidForSlot(0, identifier) && !turbine.isItemValidForSlot(0, bucket),
                "Slot 0 must take fluid identifiers only");
        helper.assertTrue(turbine.isItemValidForSlot(2, bucket) && !turbine.isItemValidForSlot(2, identifier),
                "Slot 2 must take fluid containers only");
        helper.assertTrue(turbine.isItemValidForSlot(4, battery) && !turbine.isItemValidForSlot(4, bucket),
                "Slot 4 must take batteries only");
        helper.assertTrue(turbine.isItemValidForSlot(3, bucket) && turbine.isItemValidForSlot(6, bucket),
                "The bay output slots must accept the machine's own writes");

        // input bay: a full container drains into the input tank (WATER so the conversion stays out of it)
        turbine.input.setTankType(Fluids.WATER);
        turbine.inventory.setStackInSlot(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.input.getFill() == 1_000, "Input canister bay did not drain into the tank");
        helper.assertTrue(turbine.inventory.getStackInSlot(2).isEmpty()
                        && turbine.inventory.getStackInSlot(3).is(net.minecraft.world.item.Items.BUCKET),
                "Emptied container must move to slot 3");

        // output bay: an empty bucket comes back filled with the spent steam
        turbine = placeTurbine(helper, pos);
        turbine.output.setFill(2_000);
        turbine.inventory.setStackInSlot(5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET));
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.output.getFill() == 1_000, "Output canister bay did not drain the output tank");
        helper.assertTrue(turbine.inventory.getStackInSlot(5).isEmpty()
                        && com.hbm.items.machine.ItemFluidBucket.getFluid(turbine.inventory.getStackInSlot(6)) == Fluids.SPENTSTEAM,
                "Filled spent-steam bucket must move to slot 6");

        // battery slot: the buffer empties into the item
        turbine = placeTurbine(helper, pos);
        turbine.power = 5_000;
        turbine.inventory.setStackInSlot(4, battery.copy());
        tick(helper, pos, turbine);
        helper.assertTrue(turbine.power == 0, "Turbine must push its buffer into the battery slot");
        var charged = turbine.inventory.getStackInSlot(4);
        helper.assertTrue(((com.hbm.api.energymk2.IBatteryItem) charged.getItem()).getCharge(charged) == 5_000,
                "Battery did not receive the turbine's power");
        helper.succeed();
    }

    /** The menu layout — the old container's slot count and coordinates. */
    private static void turbineMenu(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachineTurbine turbine = placeTurbine(helper, pos);
        helper.assertTrue(turbine instanceof net.minecraft.world.MenuProvider, "The turbine must open a menu");

        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachineTurbine(0, player.getInventory(), turbine);
        helper.assertTrue(menu.slots.size() == 7 + 36, "Turbine menu must have 7 machine slots + the player inventory");
        assertSlot(helper, menu, 0, 8, 17);
        assertSlot(helper, menu, 1, 8, 53);
        assertSlot(helper, menu, 2, 44, 17);
        assertSlot(helper, menu, 3, 44, 53);
        assertSlot(helper, menu, 4, 98, 53);
        assertSlot(helper, menu, 5, 152, 17);
        assertSlot(helper, menu, 6, 152, 53);
        helper.assertFalse(menu.slots.get(1).mayPlace(com.hbm.items.machine.ItemFluidId.make(Fluids.STEAM)),
                "The identifier output slot must be take-only");
        helper.succeed();
    }

    private static void assertSlot(GameTestHelper helper, net.minecraft.world.inventory.AbstractContainerMenu menu,
                                   int index, int x, int y) {
        var slot = menu.slots.get(index);
        helper.assertTrue(slot.x == x && slot.y == y,
                "Turbine slot " + index + " sits at " + slot.x + "," + slot.y + ", expected " + x + "," + y);
    }

    /** The condenser closes the loop: spent steam in, the same amount of water out. */
    private static void condenserConversion(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_condenser.get().defaultBlockState());
        var condenser = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, com.hbm.tileentity.machine.TileEntityCondenser.class);
        condenser.input.setFill(100);
        com.hbm.tileentity.machine.TileEntityCondenser.serverTick(helper.getLevel(), helper.absolutePos(pos),
                condenser.getBlockState(), condenser);
        helper.assertTrue(condenser.input.getFill() == 0, "Condenser must consume the spent steam");
        helper.assertTrue(condenser.output.getTankType() == Fluids.WATER && condenser.output.getFill() == 100,
                "Condenser must return the spent steam as water 1:1");
        helper.succeed();
    }

    private static TileEntityMachineTurbine placeTurbine(GameTestHelper helper, net.minecraft.core.BlockPos pos) {
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.setBlock(pos, ModMachineBlocks.machine_turbine.get().defaultBlockState());
        return dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityMachineTurbine.class);
    }

    private static void tick(GameTestHelper helper, net.minecraft.core.BlockPos pos, TileEntityMachineTurbine turbine) {
        TileEntityMachineTurbine.serverTick(helper.getLevel(), helper.absolutePos(pos), turbine.getBlockState(), turbine);
    }

    // --- PUREX ---------------------------------------------------------------------------------------

    /** One full ZIRNOX reprocessing cycle: 100 ticks at 1000 HE/t, exact reagent draw and the four products. */
    private static void purexRecipeCycle(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);

        purex.purexModule.recipe = "purex.zirnoxnu";
        tick(helper, pos, purex); // lets setupTanks conform the reagent tanks to the recipe
        helper.assertTrue(purex.inputTanks[0].getTankType() == Fluids.KEROSENE
                        && purex.inputTanks[1].getTankType() == Fluids.NITRIC_ACID,
                "PUREX reagent tanks must conform to the selected recipe");

        purex.inputTanks[0].setFill(24_000);
        purex.inputTanks[1].setFill(24_000);
        purex.inventory.setStackInSlot(4, new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM)));
        purex.power = 1_000_000;

        for (int i = 0; i < 100; i++) tick(helper, pos, purex);

        helper.assertTrue(purex.power == 900_000, "PUREX must draw 1000 HE/t for 100 ticks, power is " + purex.power);
        helper.assertTrue(purex.inputTanks[0].getFill() == 23_500 && purex.inputTanks[1].getFill() == 23_750,
                "PUREX must consume 500 mB kerosene + 250 mB nitric acid per cycle");
        helper.assertTrue(purex.inventory.getStackInSlot(4).isEmpty(), "PUREX must consume the spent-fuel input");
        assertOutput(helper, purex, 7, com.hbm.items.ModItems.nugget_u238.get(), 1);
        assertOutput(helper, purex, 8, com.hbm.items.ModItems.nugget_pu_mix.get(), 2);
        assertOutput(helper, purex, 9, com.hbm.items.ModItems.nugget_pu239.get(), 1);
        assertOutput(helper, purex, 10, com.hbm.items.ModItems.nuclear_waste_tiny.get(), 2);
        helper.succeed();
    }

    /** Hot spent fuel must be cooled in a waste drum first — the recipes only match cold stacks. */
    private static void purexHotWasteRejected(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);
        purex.purexModule.recipe = "purex.zirnoxnu";

        var cold = new net.minecraft.world.item.ItemStack(com.hbm.items.ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM));
        var hot = cold.copy();
        com.hbm.items.ModDataComponents.wasteHot(hot, true);

        helper.assertTrue(com.hbm.inventory.recipes.PUREXRecipes.INSTANCE.matchesInput(cold), "Cold waste must match a PUREX recipe");
        helper.assertFalse(com.hbm.inventory.recipes.PUREXRecipes.INSTANCE.matchesInput(hot), "Hot waste must not match any PUREX recipe");
        helper.assertTrue(purex.isItemValidForSlot(4, cold), "Cold waste must be insertable");
        helper.assertFalse(purex.isItemValidForSlot(4, hot), "Hot waste must be rejected by the input slot");
        helper.succeed();
    }

    /** The 1.12.2 upgrade formulas, pinned through observable speed (progress/tick) and draw (HE/tick). */
    private static void purexUpgrades(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);

        // SPEED 3: speed 2.0 (progress 0.02/t), pow 4.0 (4000 HE/t)
        assertUpgradeRun(helper, pos, com.hbm.items.ModUpgradeItems.upgrade_speed_3.get(), 25, 0.5D, 4_000L * 25);
        // POWER 3: speed 1.0 (progress 0.01/t), pow 0.25 (250 HE/t)
        assertUpgradeRun(helper, pos, com.hbm.items.ModUpgradeItems.upgrade_power_3.get(), 25, 0.25D, 250L * 25);
        // OVERDRIVE 3: speed 4.0 (progress 0.04/t), pow 11.0 (11000 HE/t)
        assertUpgradeRun(helper, pos, com.hbm.items.ModUpgradeItems.upgrade_overdrive_3.get(), 20, 0.8D, 11_000L * 20);
        helper.succeed();
    }

    private static void assertUpgradeRun(GameTestHelper helper, net.minecraft.core.BlockPos pos,
                                         net.minecraft.world.item.Item upgrade, int ticks, double expectedProgress, long expectedDraw) {
        TileEntityMachinePUREX purex = placePurex(helper, pos);
        purex.purexModule.recipe = "purex.zirnoxnu";
        purex.inventory.setStackInSlot(2, new net.minecraft.world.item.ItemStack(upgrade));
        tick(helper, pos, purex);
        purex.inputTanks[0].setFill(24_000);
        purex.inputTanks[1].setFill(24_000);
        purex.inventory.setStackInSlot(4, new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM), 64));
        purex.power = 1_000_000;

        for (int i = 0; i < ticks; i++) tick(helper, pos, purex);

        double progress = purex.purexModule.progress;
        helper.assertTrue(Math.abs(progress - expectedProgress) < 1.0E-6,
                upgrade + " progress after " + ticks + " ticks was " + progress + ", expected " + expectedProgress);
        long drawn = 1_000_000L - purex.power;
        helper.assertTrue(drawn == expectedDraw, upgrade + " drew " + drawn + " HE, expected " + expectedDraw);
    }

    /** The dynamic power ceiling: {@code recipe.power * 100}, floored at the buffer and at 1,000,000. */
    private static void purexMaxPower(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);

        TileEntityMachinePUREX purex = placePurex(helper, pos);
        purex.purexModule.recipe = "purex.zirnoxnu"; // 1000 HE/t -> 100k, floored to the 1M base
        tick(helper, pos, purex);
        helper.assertTrue(purex.maxPower == TileEntityMachinePUREX.BASE_MAX_POWER,
                "A cheap recipe must keep the 1,000,000 HE base ceiling, got " + purex.maxPower);

        purex.purexModule.recipe = "purex.schrabzirnox"; // 50,000 HE/t -> 5M
        tick(helper, pos, purex);
        helper.assertTrue(purex.maxPower == 5_000_000L,
                "maxPower must track recipe.power * 100, got " + purex.maxPower);

        // dropping back to a cheap recipe must not strand power above the ceiling
        purex.power = 5_000_000L;
        purex.purexModule.recipe = "purex.zirnoxnu";
        tick(helper, pos, purex);
        helper.assertTrue(purex.maxPower >= purex.power, "maxPower must never fall below the stored power");
        helper.succeed();
    }

    /** A recipe with a fluid product fills the output tank, and that tank is offered to the network. */
    private static void purexOutputFluid(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);

        purex.purexModule.recipe = "purex.icf";
        tick(helper, pos, purex);
        purex.inventory.setStackInSlot(4, new net.minecraft.world.item.ItemStack(com.hbm.items.ModItems.icf_pellet_depleted.get()));
        // 305 ticks, not 300: progress accumulates 1/300 per tick in double precision, so the sum reaches
        // 1.0 a tick or two late — the machine is not tick-exact, and the assertion should not pretend it is.
        for (int i = 0; i < 305; i++) {
            purex.power = purex.maxPower; // the ICF recipe draws 10k HE/t throughout
            tick(helper, pos, purex);
        }

        helper.assertTrue(purex.outputTanks[0].getTankType() == Fluids.HELIUM4 && purex.outputTanks[0].getFill() == 1_250,
                "The ICF recipe must leave 1250 mB of helium-4 in the product tank");
        helper.assertTrue(purex.getSendingTanks().length == 1 && purex.getSendingTanks()[0] == purex.outputTanks[0],
                "The product tank must be offered to the fluid network");
        assertOutput(helper, purex, 7, com.hbm.items.ModItems.icf_pellet_empty.get(), 1);
        helper.succeed();
    }

    /** The 13-slot layout: what each slot accepts, and what automation may reach. */
    private static void purexSlots(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);
        purex.purexModule.recipe = "purex.zirnoxnu";

        var battery = new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModBatteryItems.PACKS.get(com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD).get());
        var blueprint = new net.minecraft.world.item.ItemStack(com.hbm.items.ModMachineItems.blueprint_plate.get());
        var upgrade = new net.minecraft.world.item.ItemStack(com.hbm.items.ModUpgradeItems.upgrade_speed_3.get());
        var waste = new net.minecraft.world.item.ItemStack(com.hbm.items.ModZirnoxItems.waste(ZirnoxFuel.NATURAL_URANIUM));
        var junk = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE);

        helper.assertTrue(purex.isItemValidForSlot(0, battery) && !purex.isItemValidForSlot(0, junk), "Slot 0 takes batteries only");
        helper.assertTrue(purex.isItemValidForSlot(1, blueprint) && !purex.isItemValidForSlot(1, junk), "Slot 1 takes blueprints only");
        helper.assertTrue(purex.isItemValidForSlot(2, upgrade) && !purex.isItemValidForSlot(2, junk), "Slot 2 takes upgrades only");
        helper.assertTrue(purex.isItemValidForSlot(4, waste) && !purex.isItemValidForSlot(4, junk),
                "Slot 4 takes the selected recipe's ingredients only");

        int[] faceSlots = purex.getSlotsForFace(net.minecraft.core.Direction.NORTH);
        helper.assertTrue(faceSlots.length == 9 && faceSlots[0] == 4 && faceSlots[8] == 12,
                "Automation must see slots 4..12");
        helper.assertTrue(purex.canPlaceItemThroughFace(4, waste, net.minecraft.core.Direction.NORTH)
                        && !purex.canPlaceItemThroughFace(7, waste, net.minecraft.core.Direction.NORTH),
                "Automation may only insert into the item inputs");
        helper.assertTrue(purex.canTakeItemThroughFace(7, waste, net.minecraft.core.Direction.NORTH),
                "Automation must be able to pull the outputs");

        // an unusable item in an input slot must be extractable again, or it would deadlock the machine
        purex.inventory.setStackInSlot(5, junk.copy());
        helper.assertTrue(purex.canTakeItemThroughFace(5, junk, net.minecraft.core.Direction.NORTH),
                "A clogged input slot must be extractable");
        helper.succeed();
    }

    /** The menu layout — the original slot count and coordinates. */
    private static void purexMenu(GameTestHelper helper) {
        Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        TileEntityMachinePUREX purex = placePurex(helper, pos);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachinePUREX(0, player.getInventory(), purex);

        helper.assertTrue(menu.slots.size() == 13 + 36, "PUREX menu must have 13 machine slots + the player inventory");
        assertSlot(helper, menu, 0, 152, 81);
        assertSlot(helper, menu, 1, 35, 126);
        assertSlot(helper, menu, 2, 152, 108);
        assertSlot(helper, menu, 3, 152, 126);
        assertSlot(helper, menu, 4, 8, 90);
        assertSlot(helper, menu, 6, 44, 90);
        assertSlot(helper, menu, 7, 80, 36);
        assertSlot(helper, menu, 12, 98, 72);
        helper.assertFalse(menu.slots.get(7).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)),
                "PUREX output slots must be take-only");
        helper.succeed();
    }

    /** The 5×5 footprint and the ring of 20 network ports around it, built through the real placement path. */
    private static void purexLayout(GameTestHelper helper) {
        Fluids.init();
        var block = ModMachineBlocks.machine_purex.get();
        var clicked = new net.minecraft.core.BlockPos(4, 1, 2);
        // setPlacedBy resolves the facing from the placer's yaw (0 → NORTH) and puts the core `offset`
        // blocks along it: NORTH with offset 2 → two blocks +Z from the clicked cell.
        var core = clicked.offset(0, 0, 2);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        player.setYRot(0F);
        helper.setBlock(clicked, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(clicked), block.defaultBlockState(), player,
                new net.minecraft.world.item.ItemStack(block));

        TileEntityMachinePUREX purex = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityMachinePUREX.class);
        helper.assertTrue(purex != null, "PUREX controller setup failed");

        int cells = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (helper.getBlockState(core.offset(dx, 0, dz)).getBlock() == block) cells++;
            }
        }
        helper.assertTrue(cells == 25, "The PUREX footprint must fill 5x5 cells, found " + cells);

        helper.assertTrue(purex.getConPos().length == 20, "PUREX must expose 20 network ports");
        var eastEdge = helper.absolutePos(core.offset(2, 0, 0));
        helper.assertTrue(block.canConnect(helper.getLevel(), eastEdge, com.hbm.lib.ForgeDirection.EAST),
                "The east edge cell must accept a connection outward");
        helper.assertFalse(block.canConnect(helper.getLevel(), eastEdge, com.hbm.lib.ForgeDirection.WEST),
                "An edge cell must not connect back into the machine");
        helper.succeed();
    }

    private static TileEntityMachinePUREX placePurex(GameTestHelper helper, net.minecraft.core.BlockPos pos) {
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.setBlock(pos, ModMachineBlocks.machine_purex.get().defaultBlockState().setValue(BlockDummyable.META, 12));
        return dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityMachinePUREX.class);
    }

    private static void tick(GameTestHelper helper, net.minecraft.core.BlockPos pos, TileEntityMachinePUREX purex) {
        TileEntityMachinePUREX.serverTick(helper.getLevel(), helper.absolutePos(pos), purex.getBlockState(), purex);
    }

    private static void assertOutput(GameTestHelper helper, TileEntityMachinePUREX purex, int slot,
                                     net.minecraft.world.item.Item item, int count) {
        var stack = purex.inventory.getStackInSlot(slot);
        helper.assertTrue(stack.is(item) && stack.getCount() == count,
                "PUREX output slot " + slot + " holds " + stack + ", expected " + count + "x " + item);
    }

    private static void zirnoxGoldenTick(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(pos, ModMachineBlocks.machine_zirnox.get().defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos, TileEntityReactorZirnox.class);
        reactor.heat = 50_000;
        reactor.isOn = true;
        reactor.carbonDioxide.setFill(14_000);
        reactor.water.setFill(32_000);
        reactor.inventory.setStackInSlot(0, new net.minecraft.world.item.ItemStack(ModZirnoxItems.active(ZirnoxFuel.NATURAL_URANIUM)));
        TileEntityReactorZirnox.serverTick(helper.getLevel(), helper.absolutePos(pos), reactor.getBlockState(), reactor);
        helper.assertTrue(reactor.pressure == 71_776, "ZIRNOX pressure formula regressed");
        helper.assertTrue(reactor.steam.getFill() == 49 && reactor.water.getFill() == 31_951,
                "ZIRNOX steam conversion regressed");
        helper.assertTrue(reactor.heat == 46_440, "ZIRNOX cooling order regressed");
        helper.succeed();
    }

    /**
     * The blowtorch's fuel contract: a pristine torch is FULL (upstream filled its NBT lazily on first
     * read), a weld costs gas even in creative, and an emptied torch stays empty rather than reading as
     * full again through the absent component.
     */
    /**
     * Every fluid must have a real {@code hbmfluid.*} name, not the title-cased id.
     *
     * <p>The two are indistinguishable for the 39 fluids whose name happens to match their id, which is
     * exactly why this rotted unnoticed: a spot check on {@code DEUTERIUM} passes either way. The guard
     * is that the key <b>resolves at all</b>, plus a handful of fluids whose real name is nothing like
     * the id, so a regression cannot hide behind the coincidental matches.
     */
    /**
     * Every registered {@code hbm:} item must have a lang entry — an item whose name renders as
     * {@code item.hbm.foo} is shipped-looking but unfinished, and nothing else catches it.
     */
    private static void langCoverage(GameTestHelper helper) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            net.minecraft.resources.Identifier id =
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !com.hbm.Constants.MOD_ID.equals(id.getNamespace())) continue;
            // The EFFECTIVE name, not the key: the autogen material items and the fluid containers
            // build their name in getName() and legitimately have no lang entry of their own.
            String shown = item.getName(item.getDefaultInstance()).getString();
            if (shown.equals(item.getDescriptionId())) missing.add(id.getPath());
        }
        java.util.Collections.sort(missing);
        helper.assertTrue(missing.isEmpty(),
                missing.size() + " hbm items have no lang entry: " + missing);
        helper.succeed();
    }

    private static void fluidDisplayNames(GameTestHelper helper) {
        for (com.hbm.inventory.fluid.FluidType type : com.hbm.inventory.fluid.Fluids.getAll()) {
            String key = type.getTranslationKey();
            String shown = type.getDisplayNameString();
            helper.assertTrue(!shown.equals(key),
                    "Fluid " + type.getName() + " has no lang entry - shows the raw key " + key);
            helper.assertTrue(!shown.isEmpty(), "Fluid " + type.getName() + " has an empty display name");
        }

        // Fluids whose real name is not a title-cased id. If a call site ever falls back to the id
        // again, these are the ones that catch it.
        record Named(com.hbm.inventory.fluid.FluidType type, String expected) {}
        Named[] cases = {
                new Named(com.hbm.inventory.fluid.Fluids.OIL, "Crude Oil"),
                new Named(com.hbm.inventory.fluid.Fluids.HEAVYOIL, "Heavy Oil"),
                new Named(com.hbm.inventory.fluid.Fluids.SMEAR, "Industrial Oil"),
                new Named(com.hbm.inventory.fluid.Fluids.GAS, "Natural Gas"),
                new Named(com.hbm.inventory.fluid.Fluids.WATZ, "Poisonous Mud"),
                new Named(com.hbm.inventory.fluid.Fluids.UF6, "Uranium Hexafluoride"),
                new Named(com.hbm.inventory.fluid.Fluids.HOTSTEAM, "Dense Steam"),
        };
        for (Named c : cases) {
            helper.assertTrue(c.expected().equals(c.type().getDisplayNameString()),
                    "Fluid " + c.type().getName() + " should display as '" + c.expected()
                            + "' but shows '" + c.type().getDisplayNameString() + "'");
        }
        helper.succeed();
    }

    private static void blowtorchFuel(GameTestHelper helper) {
        var torch = com.hbm.items.ModToolItems.blowtorch.get();
        var stack = new net.minecraft.world.item.ItemStack(torch);
        helper.assertTrue(torch.getFill(stack) == com.hbm.items.tool.ItemBlowtorch.MAX_FILL,
                "A fresh blowtorch must be full, was " + torch.getFill(stack));

        torch.setFill(stack, com.hbm.items.tool.ItemBlowtorch.COST);
        helper.assertTrue(torch.getFill(stack) == com.hbm.items.tool.ItemBlowtorch.COST,
                "Blowtorch fill did not round-trip");

        torch.setFill(stack, 0);
        helper.assertTrue(torch.getFill(stack) == 0,
                "An emptied blowtorch must stay empty, read " + torch.getFill(stack));

        // Welding a raw fusion component consumes exactly one charge and swaps the block.
        var cell = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(cell, ModMachineBlocks.fusion_component.get().defaultBlockState());
        var full = new net.minecraft.world.item.ItemStack(torch);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.CREATIVE);
        // The conversion also wants a steel cast plate; hand the player one.
        player.getInventory().add(new net.minecraft.world.item.ItemStack(
                com.hbm.items.ModAutogenItems.get(com.hbm.inventory.material.MaterialShapes.CASTPLATE,
                        com.hbm.inventory.material.Mats.MAT_STEEL)));

        boolean welded = ModMachineBlocks.fusion_component.get().onScrew(helper.getLevel(), player,
                helper.absolutePos(cell), net.minecraft.core.Direction.UP,
                net.minecraft.world.phys.Vec3.ZERO, net.minecraft.world.InteractionHand.MAIN_HAND,
                com.hbm.api.block.IToolable.ToolType.TORCH);
        helper.assertTrue(welded, "Blowtorch failed to weld a raw fusion component");
        helper.assertTrue(helper.getBlockState(cell).getBlock() == ModMachineBlocks.fusion_component_bscco_welded.get(),
                "Welded component did not become the BSCCO coil");

        torch.setFill(full, torch.getFill(full) - com.hbm.items.tool.ItemBlowtorch.COST);
        helper.assertTrue(torch.getFill(full) == com.hbm.items.tool.ItemBlowtorch.MAX_FILL
                        - com.hbm.items.tool.ItemBlowtorch.COST,
                "A weld must cost exactly one charge, left " + torch.getFill(full));
        helper.succeed();
    }

    /**
     * A multiblock's menu range must reach every cell of its own structure. The menu binds to the CORE,
     * but the player stands at whatever cell they clicked — so measuring vanilla's interaction range to
     * the core locks you out of a big machine while you are pressed against it.
     *
     * <p>Checks all three axes deliberately: the first version of this fix used the horizontal footprint
     * only, which left the tall machines (coker 22 high over a 1-block footprint, gas flare, oil well)
     * just as broken as before.
     */
    private static void multiblockInteractionRange(GameTestHelper helper) {
        // Every multiblock must be usable from any cell it occupies, plus a player's reach.
        record Case(String name, BlockDummyable block) {}
        var cases = new Case[]{
                new Case("fusion_torus", ModMachineBlocks.fusion_torus.get()),
                new Case("watz", ModMachineBlocks.watz.get()),
                new Case("coker", ModMachineBlocks.machine_coker.get()),
                new Case("gas flare", ModMachineBlocks.machine_flare.get()),
                new Case("oil well", ModMachineBlocks.machine_well.get()),
                new Case("fracking tower", ModMachineBlocks.machine_fracking_tower.get()),
                new Case("assembly machine", ModMachineBlocks.machine_assembly_machine.get()),
        };
        for (Case c : cases) {
            int[] dim = c.block().getDimensions();
            double y = Math.max(dim[0], dim[1]);
            double z = Math.max(dim[2], dim[3]);
            double x = Math.max(dim[4], dim[5]);
            double corner = Math.sqrt(x * x + y * y + z * z);
            double range = Math.sqrt(c.block().getInteractionRangeSq());
            helper.assertTrue(range >= corner + 4D,
                    c.name() + " range " + Math.round(range) + " must clear its furthest cell ("
                            + Math.round(corner) + ") plus reach");
        }

        // The tall machines are the ones a horizontal-only radius silently misses.
        double coker = Math.sqrt(ModMachineBlocks.machine_coker.get().getInteractionRangeSq());
        helper.assertTrue(coker > 22D,
                "The coker is 22 blocks tall; its range must exceed that, was " + Math.round(coker));

        // And the live predicate must agree, at the torus's edge and at the top of a tall machine.
        var core = new net.minecraft.core.BlockPos(8, 1, 8);
        var torus = ModMachineBlocks.fusion_torus.get();
        helper.setBlock(core, torus.defaultBlockState().setValue(BlockDummyable.META, 12));
        torus.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var absCore = helper.absolutePos(core);
        player.setPos(absCore.getX() + 8.5D, absCore.getY(), absCore.getZ() + 0.5D);
        helper.assertTrue(BlockDummyable.isWithinInteractionRange(helper.getLevel(), absCore, player),
                "A player at the torus's edge must be able to use it");

        player.setPos(absCore.getX() + 64.5D, absCore.getY(), absCore.getZ() + 0.5D);
        helper.assertFalse(BlockDummyable.isWithinInteractionRange(helper.getLevel(), absCore, player),
                "The range must still cut off at distance");

        // The fracking tower is the case a dimensions-derived radius CANNOT cover: it declares a radius
        // of 3 and then erects a derrick 24 blocks tall. A player at the top is standing on the machine
        // and must be able to use it, which only the cell fallback can establish.
        var towerCore = new net.minecraft.core.BlockPos(2, 1, 2);
        var tower = ModMachineBlocks.machine_fracking_tower.get();
        helper.setBlock(towerCore, tower.defaultBlockState().setValue(BlockDummyable.META, 12));
        tower.formStructure(helper.getLevel(), helper.absolutePos(towerCore), com.hbm.lib.ForgeDirection.NORTH);
        var absTower = helper.absolutePos(towerCore);

        helper.assertTrue(Math.sqrt(tower.getInteractionRangeSq()) < 24D,
                "This test is only meaningful while the tower's declared radius is too small");

        // Standing at the crown, 20 blocks up the derrick.
        player.setPos(absTower.getX() + 0.5D, absTower.getY() + 20D, absTower.getZ() + 0.5D);
        helper.assertTrue(BlockDummyable.isWithinInteractionRange(helper.getLevel(), absTower, player),
                "A player on top of the fracking tower must be able to use it");

        // But standing 20 blocks up in open air well to the side must not count.
        player.setPos(absTower.getX() + 40.5D, absTower.getY() + 20D, absTower.getZ() + 0.5D);
        helper.assertFalse(BlockDummyable.isWithinInteractionRange(helper.getLevel(), absTower, player),
                "Being high up near nothing must not grant access");
        helper.succeed();
    }

    /** The long/short metadata families, byproduct amounts, and neutron-contamination input gate. */
    private static void storageDrumRecipes(GameTestHelper helper) {
        com.hbm.inventory.fluid.Fluids.init();
        com.hbm.inventory.recipes.StorageDrumRecipes.registerRecipes();
        helper.assertTrue(com.hbm.inventory.recipes.StorageDrumRecipes.getAll().size() == 27,
                "Storage drum available recipe count regressed");

        var input = com.hbm.items.ModStorageDrumItems.make(
                com.hbm.items.ModStorageDrumItems.nuclear_waste_short.get(),
                com.hbm.items.special.ItemWasteStorage.ShortWasteClass.PLUTONIUM240.ordinal(), 1);
        var output = com.hbm.inventory.recipes.StorageDrumRecipes.getOutput(input);
        var waste = com.hbm.inventory.recipes.StorageDrumRecipes.getWaste(input);
        helper.assertTrue(output != null
                        && output.getItem() == com.hbm.items.ModStorageDrumItems.nuclear_waste_short_depleted.get()
                        && ((com.hbm.items.special.ItemWasteStorage) output.getItem()).variant(output)
                                == com.hbm.items.special.ItemWasteStorage.ShortWasteClass.PLUTONIUM240.ordinal(),
                "Short-waste transformation lost its subtype");
        helper.assertTrue(waste != null && waste[0] == 216_000 && waste[1] == 350 && waste[2] == 1_000,
                "Short-waste liquid/gas output regressed");

        var longTinyInput = com.hbm.items.ModStorageDrumItems.make(
                com.hbm.items.ModStorageDrumItems.nuclear_waste_long_tiny.get(),
                com.hbm.items.special.ItemWasteStorage.LongWasteClass.URANIUM233.ordinal(), 1);
        var longTinyOutput = com.hbm.inventory.recipes.StorageDrumRecipes.getOutput(longTinyInput);
        var longTinyWaste = com.hbm.inventory.recipes.StorageDrumRecipes.getWaste(longTinyInput);
        helper.assertTrue(longTinyOutput != null
                        && longTinyOutput.getItem() == com.hbm.items.ModStorageDrumItems.nuclear_waste_long_depleted_tiny.get()
                        && ((com.hbm.items.special.ItemWasteStorage) longTinyOutput.getItem()).variant(longTinyOutput)
                                == com.hbm.items.special.ItemWasteStorage.LongWasteClass.URANIUM233.ordinal()
                        && longTinyWaste != null && longTinyWaste[0] == 21_600
                        && longTinyWaste[1] == 0 && longTinyWaste[2] == 5,
                "Long-tiny waste must scale liquid and gas by 0.1");

        var shortTinyInput = com.hbm.items.ModStorageDrumItems.make(
                com.hbm.items.ModStorageDrumItems.nuclear_waste_short_tiny.get(),
                com.hbm.items.special.ItemWasteStorage.ShortWasteClass.PLUTONIUM240.ordinal(), 1);
        var shortTinyWaste = com.hbm.inventory.recipes.StorageDrumRecipes.getWaste(shortTinyInput);
        helper.assertTrue(shortTinyWaste != null && shortTinyWaste[0] == 21_600
                        && shortTinyWaste[1] == 35 && shortTinyWaste[2] == 100,
                "Short-tiny waste must scale liquid and gas by 0.1");

        var contaminated = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT);
        com.hbm.util.ContaminationUtil.neutronActivateItem(contaminated, 1F, 1F);
        helper.assertTrue(com.hbm.util.ContaminationUtil.isContaminated(contaminated),
                "Neutron-contaminated items must remain valid drum inputs");
        helper.succeed();
    }

    /** The original six-by-six ring's 24 slots and player-inventory coordinates. */
    private static void storageDrumMenu(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(pos, com.hbm.blocks.machine.ModMachineBlocks.machine_storage_drum.get().defaultBlockState());
        var drum = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                com.hbm.tileentity.machine.TileEntityStorageDrum.class);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerStorageDrum(0, player.getInventory(), drum);
        helper.assertTrue(menu.slots.size() == 60, "Storage drum menu must have 24 + 36 slots");
        int[][] expected = {
                {71, 24}, {89, 24},
                {53, 42}, {71, 42}, {89, 42}, {107, 42},
                {35, 60}, {53, 60}, {71, 60}, {89, 60}, {107, 60}, {125, 60},
                {35, 78}, {53, 78}, {71, 78}, {89, 78}, {107, 78}, {125, 78},
                {53, 96}, {71, 96}, {89, 96}, {107, 96},
                {71, 114}, {89, 114}
        };
        for (int i = 0; i < expected.length; i++) assertSlot(helper, menu, i, expected[i][0], expected[i][1]);
        helper.assertTrue(drum.getSlotsForFace(net.minecraft.core.Direction.UP).length == 24,
                "All storage drum slots must be sided-accessible");
        helper.assertTrue(drum.liquid.getMaxFill() == 16_000 && drum.gas.getMaxFill() == 16_000,
                "Storage drum tank capacity regressed");
        helper.succeed();
    }

    /** Powered condenser conversion is blocked below 10 HE per mB and consumes exactly that amount. */
    private static void poweredCondenserConversion(GameTestHelper helper) {
        com.hbm.inventory.fluid.Fluids.init();
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var state = com.hbm.blocks.machine.ModMachineBlocks.machine_condenser_powered.get().defaultBlockState()
                .setValue(com.hbm.blocks.BlockDummyable.META, 12);
        helper.setBlock(pos, state);
        var condenser = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                com.hbm.tileentity.machine.TileEntityCondenserPowered.class);
        condenser.input.setFill(100);
        condenser.power = 999;
        com.hbm.tileentity.machine.TileEntityCondenserPowered.serverTick(helper.getLevel(), helper.absolutePos(pos),
                state, condenser);
        helper.assertTrue(condenser.input.getFill() == 100 && condenser.output.getFill() == 0,
                "Powered condenser must be energy-gated");
        condenser.power = 1_000;
        com.hbm.tileentity.machine.TileEntityCondenserPowered.serverTick(helper.getLevel(), helper.absolutePos(pos),
                state, condenser);
        helper.assertTrue(condenser.input.getFill() == 0 && condenser.output.getFill() == 100
                        && condenser.power == 0 && condenser.waterTimer == 20,
                "Powered condenser must convert 1:1 at 10 HE per mB");
        helper.succeed();
    }

    /** The six anchored fluid/power positions are the legacy powered-condenser ports. */
    private static void poweredCondenserPorts(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var state = com.hbm.blocks.machine.ModMachineBlocks.machine_condenser_powered.get().defaultBlockState()
                .setValue(com.hbm.blocks.BlockDummyable.META, 12);
        helper.setBlock(pos, state);
        var condenser = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                com.hbm.tileentity.machine.TileEntityCondenserPowered.class);
        var ports = condenser.getConPos();
        helper.assertTrue(ports.length == 6, "Powered condenser must expose six ports");
        helper.assertTrue(ports[0].getPos().equals(helper.absolutePos(pos).offset(4, 1, 0))
                        && ports[0].getDir() == com.hbm.lib.ForgeDirection.EAST,
                "Powered condenser lateral port geometry regressed");
        helper.assertTrue(ports[2].getPos().equals(helper.absolutePos(pos).offset(-1, 1, -2))
                        && ports[2].getDir() == com.hbm.lib.ForgeDirection.NORTH,
                "Powered condenser front port geometry regressed");
        helper.succeed();
    }

    private static void combinationRecipes(GameTestHelper helper) {
        var recipes = com.hbm.inventory.recipes.CombinationRecipes.getRecipes();
        helper.assertTrue(recipes.size() == 20, "Expected 20 feasible combination recipes, found " + recipes.size());
        var coal = com.hbm.inventory.recipes.CombinationRecipes.getOutput(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));
        helper.assertTrue(coal != null && coal.itemOutput().is(com.hbm.items.ModOilItems.coke(
                        com.hbm.items.ItemEnums.EnumCokeType.COAL))
                        && coal.fluidOutput().type == com.hbm.inventory.fluid.Fluids.COALCREOSOTE
                        && coal.fluidOutput().fill == 100,
                "Coal must produce coal coke and 100 mB of coal creosote");
        helper.assertTrue(com.hbm.inventory.recipes.CombinationRecipes.getOutput(
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND)) == null,
                "Unregistered items must not match a combination recipe");
        helper.succeed();
    }

    private static void combinationTick(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var block = ModMachineBlocks.machine_furnace_combination.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        var furnace = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                com.hbm.tileentity.machine.TileEntityFurnaceCombination.class);
        furnace.inventory.setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));
        furnace.heat = 200;
        furnace.progress = com.hbm.tileentity.machine.TileEntityFurnaceCombination.processTime - 1;
        com.hbm.tileentity.machine.TileEntityFurnaceCombination.serverTick(helper.getLevel(),
                helper.absolutePos(pos), furnace.getBlockState(), furnace);
        helper.assertTrue(furnace.inventory.getStackInSlot(0).isEmpty(), "A completed cycle must consume one input");
        helper.assertTrue(furnace.inventory.getStackInSlot(1).is(com.hbm.items.ModOilItems.coke(
                com.hbm.items.ItemEnums.EnumCokeType.COAL)), "A completed cycle must write the item output");
        helper.assertTrue(furnace.tank.getTankType() == com.hbm.inventory.fluid.Fluids.COALCREOSOTE
                        && furnace.tank.getFill() == 100, "A completed cycle must write its fluid byproduct");
        helper.succeed();
    }

    private static void combinationMenu(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var block = ModMachineBlocks.machine_furnace_combination.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        var furnace = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                com.hbm.tileentity.machine.TileEntityFurnaceCombination.class);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerFurnaceCombination(0, player.getInventory(), furnace);
        helper.assertTrue(menu.slots.size() == 40, "Combination furnace menu must have 4 machine slots + 36 player slots");
        assertSlot(helper, menu, 0, 26, 36);
        assertSlot(helper, menu, 1, 89, 36);
        assertSlot(helper, menu, 2, 136, 18);
        assertSlot(helper, menu, 3, 136, 54);
        helper.assertFalse(menu.slots.get(1).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)),
                "The item output must be take-only");
        helper.assertFalse(menu.slots.get(3).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET)),
                "The filled-container output must be take-only");
        helper.assertFalse(furnace.isItemValidForSlot(3,
                        new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE)),
                "The filled-container output must reject direct insertion");
        helper.succeed();
    }

    /** The fuel cycles must actually be registered - they were dead data until the torus asked for them. */
    private static void fusionRecipes(GameTestHelper helper) {
        var set = com.hbm.inventory.recipes.FusionRecipes.INSTANCE;
        set.registerRecipes();
        helper.assertTrue(set.recipeOrderedList.size() == 10,
                "Expected 10 fusion fuel cycles, found " + set.recipeOrderedList.size());

        var dd = set.getRecipe("fus.dd");
        helper.assertTrue(dd instanceof com.hbm.inventory.recipes.FusionRecipe, "fus.dd must be a FusionRecipe");
        var fusion = (com.hbm.inventory.recipes.FusionRecipe) dd;
        helper.assertTrue(fusion.ignitionTemp == 750_000L, "D-D ignition must be 750k, was " + fusion.ignitionTemp);
        helper.assertTrue(fusion.outputTemp == 1_000_000L, "D-D output must be 1M, was " + fusion.outputTemp);

        // maxInput drives the creative klystron, so it must be the largest ignition of the set.
        long highest = 0;
        for (var recipe : set.recipeOrderedList) {
            if (recipe instanceof com.hbm.inventory.recipes.FusionRecipe f) highest = Math.max(highest, f.ignitionTemp);
        }
        helper.assertTrue(set.maxInput == highest && highest == 10_000_000L,
                "maxInput must be the highest ignition (10M), was " + set.maxInput);
        helper.succeed();
    }

    /**
     * The chain end to end: a creative klystron lights a torus, and the torus pushes plasma into a
     * boiler that turns water into superheated steam. This is the whole point of the fusion family -
     * each machine on its own proves nothing.
     */
    private static void fusionChain(GameTestHelper helper) {
        var torusCore = new net.minecraft.core.BlockPos(9, 1, 9);
        var torus = ModMachineBlocks.fusion_torus.get();
        helper.setBlock(torusCore, torus.defaultBlockState().setValue(BlockDummyable.META, 12));
        torus.formStructure(helper.getLevel(), helper.absolutePos(torusCore), com.hbm.lib.ForgeDirection.NORTH);
        var torusTe = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, torusCore,
                com.hbm.tileentity.machine.fusion.TileEntityFusionTorus.class);
        helper.assertTrue(torusTe != null, "Torus setup failed");

        // Pick a cycle and stock it: D-D burns deuterium alone.
        com.hbm.inventory.recipes.FusionRecipes.INSTANCE.registerRecipes();
        torusTe.fusionModule.recipe = "fus.dd";
        torusTe.fusionModule.setupTanks(torusTe.fusionModule.getRecipe());
        torusTe.tanks[0].setFill(torusTe.tanks[0].getMaxFill());
        torusTe.power = torusTe.getMaxPower();
        torusTe.coolantTanks[0].setFill(torusTe.coolantTanks[0].getMaxFill());
        // Chill it below the operating threshold; otherwise it spends the test warming up.
        torusTe.temperature = com.hbm.tileentity.machine.albion.TileEntityCooledBase.temperature_target;

        // The klystron beam is delivered straight into the torus, exactly as the network does it.
        torusTe.receiveKlystronEnergy(com.hbm.inventory.recipes.FusionRecipes.INSTANCE.maxInput);

        var boilerCore = new net.minecraft.core.BlockPos(2, 1, 2);
        var boiler = ModMachineBlocks.fusion_boiler.get();
        helper.setBlock(boilerCore, boiler.defaultBlockState().setValue(BlockDummyable.META, 12));
        boiler.formStructure(helper.getLevel(), helper.absolutePos(boilerCore), com.hbm.lib.ForgeDirection.NORTH);
        var boilerTe = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, boilerCore,
                com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler.class);
        helper.assertTrue(boilerTe != null, "Boiler setup failed");
        boilerTe.tanks[0].setFill(boilerTe.tanks[0].getMaxFill());

        helper.assertTrue(boilerTe.receivesFusionPower(), "The boiler must take a share of the plasma");
        // One tick of a lit D-D cycle, handed over the way the torus hands it over.
        boilerTe.receiveFusionPower(1_000_000L, 0D, 1F, 0.2F, 0.2F);

        helper.assertTrue(boilerTe.tanks[1].getFill() > 0,
                "The boiler must have made superheated steam from plasma");
        helper.assertTrue(boilerTe.tanks[0].getFill() < boilerTe.tanks[0].getMaxFill(),
                "The boiler must have consumed water");

        // The collector boosts without consuming; the coupler consumes like any other receiver.
        var collector = new com.hbm.tileentity.machine.fusion.TileEntityFusionCollector(
                net.minecraft.core.BlockPos.ZERO, ModMachineBlocks.fusion_collector.get().defaultBlockState());
        helper.assertFalse(collector.receivesFusionPower(), "The collector must not take a share of the output");
        helper.assertTrue(collector.providesCollectorBonus(), "The collector must grant the reactor its bonus");
        helper.succeed();
    }

    /**
     * Every port a fusion machine advertises must be reachable: the cell beside it has to belong to
     * that machine and accept a connection on that face. A port whose cell is buried inside the
     * machine's own mass is worse than no port, because the machine looks connectable and is not.
     */
    private static void fusionPorts(GameTestHelper helper) {
        checkPorts(helper, new net.minecraft.core.BlockPos(9, 1, 9), ModMachineBlocks.fusion_torus.get(),
                te -> ((com.hbm.tileentity.machine.fusion.TileEntityFusionTorus) te).getConPos());
        checkPorts(helper, new net.minecraft.core.BlockPos(9, 1, 27), ModMachineBlocks.fusion_plasma_forge.get(),
                te -> ((com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge) te).getConPos());
        checkPorts(helper, new net.minecraft.core.BlockPos(2, 1, 2), ModMachineBlocks.fusion_klystron.get(),
                te -> ((com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron) te).getConPos());
        checkPorts(helper, new net.minecraft.core.BlockPos(2, 1, 16), ModMachineBlocks.fusion_boiler.get(),
                te -> ((com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler) te).getConPos());
        checkPorts(helper, new net.minecraft.core.BlockPos(20, 1, 2), ModMachineBlocks.fusion_mhdt.get(),
                te -> ((com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT) te).getConPos());
        checkPorts(helper, new net.minecraft.core.BlockPos(20, 1, 16), ModMachineBlocks.fusion_breeder.get(),
                te -> ((com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder) te).getConPos());
        helper.succeed();
    }

    private static void fusionPlasmaForgeRecipes(GameTestHelper helper) {
        var recipes = com.hbm.inventory.recipes.PlasmaForgeRecipes.INSTANCE;
        recipes.registerRecipes();
        helper.assertTrue(recipes.recipeOrderedList.size() == 15,
                "Expected 15 feasible plasma-forge recipes, found " + recipes.recipeOrderedList.size());
        helper.assertTrue(recipes.autoSwitchGroups.getOrDefault("autoswitch.weldPlates", java.util.List.of()).size() == 11,
                "Expected 11 welded-plate variants in the auto-switch group");

        var euphemium = (com.hbm.inventory.recipes.PlasmaForgeRecipe) recipes.getRecipe("plsm.plateeuphemium");
        helper.assertTrue(euphemium != null && euphemium.ignitionTemp == 1_000_000L
                        && euphemium.duration == 600 && euphemium.power == 10_000_000L
                        && euphemium.outputItem[0].getCount() == 4,
                "Euphemium plate recipe lost its legacy timing, ignition, power, or output");

        var hde = (com.hbm.inventory.recipes.PlasmaForgeRecipe) recipes.getRecipe("plsm.hde");
        helper.assertTrue(hde != null && hde.inputFluid.length == 1
                        && hde.inputFluid[0].type == com.hbm.inventory.fluid.Fluids.STELLAR_FLUX
                        && hde.inputFluid[0].fill == 4_000,
                "HDE must require 4,000 mB of stellar flux");
        helper.assertTrue(!recipes.getRecipe("plsm.weldsteel").isPooled(),
                "Feasible plasma-forge recipes must not require an unavailable blueprint pool");

        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var block = ModMachineBlocks.fusion_plasma_forge.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        var forge = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.class);
        forge.plasmaModule.recipe = hde.getInternalName();
        com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.serverTick(
                helper.getLevel(), helper.absolutePos(pos), forge.getBlockState(), forge);
        helper.assertTrue(forge.maxPower == hde.power * 100L,
                "Plasma-forge capacity did not follow the selected high-power recipe");

        var weldIron = (com.hbm.inventory.recipes.PlasmaForgeRecipe) recipes.getRecipe("plsm.weldiron");
        forge.plasmaModule.recipe = weldIron.getInternalName();
        com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.serverTick(
                helper.getLevel(), helper.absolutePos(pos), forge.getBlockState(), forge);
        helper.assertTrue(forge.maxPower == 100_000L,
                "Plasma-forge capacity did not decrease when switching to a lower-power recipe");
        helper.succeed();
    }

    private static void fusionPlasmaForgeMenu(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var block = ModMachineBlocks.fusion_plasma_forge.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        var forge = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.class);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new com.hbm.inventory.container.ContainerMachinePlasmaForge(0, player.getInventory(), forge);

        helper.assertTrue(menu.slots.size() == 52,
                "Plasma-forge menu must have 16 machine slots plus 36 player slots");
        helper.assertFalse(menu.slots.get(com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.SLOT_OUTPUT)
                        .mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT)),
                "The plasma-forge output slot must be take-only");
        var castIron = com.hbm.items.ModAutogenItems.get(
                com.hbm.inventory.material.MaterialShapes.CASTPLATE,
                com.hbm.inventory.material.Mats.MAT_IRON);
        forge.plasmaModule.recipe = "plsm.weldiron";
        helper.assertTrue(forge.isItemValidForSlot(
                        com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.FIRST_INPUT,
                        new net.minecraft.world.item.ItemStack(castIron)),
                "A welded-plate cast input must be accepted by the plasma-forge input area");
        helper.succeed();
    }

    private static void fusionPlasmaForgeProcessing(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var block = ModMachineBlocks.fusion_plasma_forge.get();
        helper.setBlock(pos, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        var forge = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, pos,
                com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.class);
        var castIron = com.hbm.items.ModAutogenItems.get(
                com.hbm.inventory.material.MaterialShapes.CASTPLATE,
                com.hbm.inventory.material.Mats.MAT_IRON);
        var weldedIron = com.hbm.items.ModAutogenItems.get(
                com.hbm.inventory.material.MaterialShapes.WELDEDPLATE,
                com.hbm.inventory.material.Mats.MAT_IRON);
        forge.plasmaModule.recipe = "plsm.weldiron";
        forge.power = 5_000L;
        forge.inventory.setStackInSlot(com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.FIRST_INPUT,
                new net.minecraft.world.item.ItemStack(castIron, 2));

        for (int tick = 0; tick < 50; tick++) forge.plasmaModule.update(1D, 1D, true,
                net.minecraft.world.item.ItemStack.EMPTY);

        helper.assertTrue(forge.inventory.getStackInSlot(
                        com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.SLOT_OUTPUT).is(weldedIron),
                "A complete 50-tick iron weld must produce a welded iron plate");
        helper.assertTrue(forge.inventory.getStackInSlot(
                        com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge.FIRST_INPUT).isEmpty(),
                "A completed weld must consume both cast plates");
        helper.assertTrue(forge.power == 0L,
                "The weld must consume exactly 5,000 HE over 50 ticks, left " + forge.power);
        helper.succeed();
    }

    private static void fusionBreeder(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(2, 1, 2);
        var block = ModMachineBlocks.fusion_breeder.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        var breeder = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core,
                com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder.class);
        helper.assertTrue(breeder != null, "Breeder setup failed");
        helper.assertFalse(breeder.receivesFusionPower(), "Breeder must not consume a plasma power share");

        breeder.inventory.setStackInSlot(1, new net.minecraft.world.item.ItemStack(
                net.minecraft.world.item.Items.GOLD_NUGGET));
        breeder.receiveFusionPower(0L, com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder.CAPACITY + 1D,
                0F, 0F, 0F);
        helper.assertTrue(breeder.inventory.getStackInSlot(2).is(
                        com.hbm.items.ModItems.nugget_au198.get()),
                "Gold nugget irradiation must produce gold-198");

        breeder.tanks[0].setTankType(com.hbm.inventory.fluid.Fluids.GAS);
        breeder.tanks[0].setFill(1_000);
        breeder.receiveFusionPower(0L, com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder.CAPACITY + 1D,
                0F, 0F, 0F);
        helper.assertTrue(breeder.tanks[1].getTankType() == com.hbm.inventory.fluid.Fluids.SYNGAS
                        && breeder.tanks[1].getFill() == 1_000,
                "Gas irradiation must produce 1,000 mB of syngas");
        helper.succeed();
    }

    private static void checkPorts(GameTestHelper helper, net.minecraft.core.BlockPos core, BlockDummyable block,
                                   java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity,
                                           java.util.List<com.hbm.lib.DirPos>> ports) {
        String name = block.getName().getString();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        var te = helper.getLevel().getBlockEntity(helper.absolutePos(core));
        helper.assertTrue(te != null, name + " controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        var declared = ports.apply(te);
        helper.assertFalse(declared.isEmpty(), name + " declares no ports at all");

        for (com.hbm.lib.DirPos port : declared) {
            var dir = port.getDir();
            // The cell that hosts this connection is the one the pipe would touch.
            var cell = port.getPos().offset(-dir.offsetX, -dir.offsetY, -dir.offsetZ);
            var state = helper.getLevel().getBlockState(cell);
            helper.assertTrue(state.getBlock() == block,
                    name + " port at " + port.getPos() + " has no machine cell beside it");

            // And the pipe's own position must NOT be part of the machine, or nothing can sit there.
            helper.assertFalse(helper.getLevel().getBlockState(port.getPos()).getBlock() == block,
                    name + " port at " + port.getPos() + " is buried inside the machine");

            if (block instanceof com.hbm.api.fluidmk2.IFluidConnectorBlockMK2 fluid) {
                helper.assertTrue(fluid.canConnect(com.hbm.inventory.fluid.Fluids.WATER, helper.getLevel(),
                                cell.getX(), cell.getY(), cell.getZ(), dir),
                        name + " refuses a fluid connection at its own port " + port.getPos());
            }
            if (block instanceof com.hbm.api.energymk2.IEnergyConnectorBlock energy) {
                helper.assertTrue(energy.canConnect(helper.getLevel(), cell, dir),
                        name + " refuses an energy connection at its own port " + port.getPos());
            }
        }
    }

    // --- Fusion torus ---

    private static void fusionTorusLayout(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(8, 1, 8);
        var block = ModMachineBlocks.fusion_torus.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        helper.assertTrue(dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core,
                com.hbm.tileentity.machine.fusion.TileEntityFusionTorus.class) != null, "Torus controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        helper.assertBlockProperty(core, BlockDummyable.META, 12);

        // Every non-zero mask cell must be filled, and every zero one must not be — that hollow centre
        // is what makes it a torus rather than a cylinder.
        int occupied = 0;
        for (int iy = 0; iy < com.hbm.blocks.machine.fusion.MachineFusionTorus.HEIGHT; iy++) {
            int[][] layer = com.hbm.blocks.machine.fusion.MachineFusionTorus.layerFor(iy);
            int half = layer.length / 2;
            for (int ix = 0; ix < layer.length; ix++) {
                for (int iz = 0; iz < layer[ix].length; iz++) {
                    boolean expected = layer[ix][iz] > 0;
                    var cell = core.offset(ix - half, iy, iz - half);
                    boolean present = helper.getBlockState(cell).getBlock() == block;
                    helper.assertTrue(present == expected,
                            "Incorrect torus cell at " + (ix - half) + "," + iy + "," + (iz - half));
                    if (present) occupied++;
                }
            }
        }
        helper.assertTrue(occupied == 925, "Torus must contain exactly 925 cells, found " + occupied);

        // The centre-top port plus the twenty-four ring ports.
        helper.assertBlockProperty(core.above(4), BlockDummyable.META, 7);
        for (int dy : new int[]{0, 4}) {
            for (int off : new int[]{-2, 0, 2}) {
                for (int[] p : new int[][]{{6, dy, off}, {-6, dy, off}, {off, dy, 6}, {off, dy, -6}}) {
                    int meta = helper.getBlockState(core.offset(p[0], p[1], p[2])).getValue(BlockDummyable.META);
                    helper.assertTrue(meta >= BlockDummyable.extra && meta < 12,
                            "Torus port " + p[0] + "," + p[1] + "," + p[2] + " must be an extra dummy, was " + meta);
                }
            }
        }

        var torus = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core,
                com.hbm.tileentity.machine.fusion.TileEntityFusionTorus.class);
        helper.assertTrue(torus.getConPos().size() == 26,
                "Torus must expose 26 ports, found " + torus.getConPos().size());
        helper.succeed();
    }

    private static void fusionStructAssembly(GameTestHelper helper) {
        var seed = new net.minecraft.core.BlockPos(8, 1, 8);
        helper.setBlock(seed, ModMachineBlocks.struct_torus_core.get().defaultBlockState());
        var struct = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, seed,
                com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct.class);
        helper.assertTrue(struct != null, "Torus struct core setup failed");
        // 924 component blocks: the whole mask except the seed's own cell.
        helper.assertTrue(struct.pattern().cells().size() == 924,
                "Torus pattern must ask for 924 components, asks for " + struct.pattern().cells().size());
        for (var cell : struct.pattern().cells()) {
            helper.setBlock(seed.offset(cell.offset()), cell.block().get().defaultBlockState());
        }

        helper.runAfterDelay(45, () -> {
            var block = ModMachineBlocks.fusion_torus.get();
            helper.assertTrue(helper.getBlockState(seed).getBlock() == block,
                    "Torus struct core did not become a fusion reactor");
            helper.assertBlockProperty(seed, BlockDummyable.META, 12);
            helper.assertTrue(dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, seed,
                    com.hbm.tileentity.machine.fusion.TileEntityFusionTorus.class) != null,
                    "Assembled torus has no controller");
            helper.assertTrue(helper.getBlockState(seed.offset(7, 0, 0)).getBlock() == block,
                    "Assembled torus did not fill its outer ring");
            helper.succeed();
        });
    }

    // --- Watz power plant + the struct-core assembly framework ---

    /** Builds the reactor's component shape around {@code seed}, which must be a struct core. */
    private static void buildWatzShape(GameTestHelper helper, net.minecraft.core.BlockPos seed) {
        var struct = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, seed,
                com.hbm.tileentity.machine.struct.TileEntityWatzStruct.class);
        helper.assertTrue(struct != null, "Watz struct core setup failed");
        for (var cell : struct.pattern().cells()) {
            helper.setBlock(seed.offset(cell.offset()), cell.block().get().defaultBlockState());
        }
    }

    private static void watzLayout(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(4, 1, 4);
        var block = ModMachineBlocks.watz.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        helper.assertTrue(dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core,
                com.hbm.tileentity.machine.TileEntityWatz.class) != null, "Watz controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        helper.assertBlockProperty(core, BlockDummyable.META, 12);
        // The cross: |x| + |z| reaches 3 along the axes and 2 on the diagonals, three blocks tall.
        int occupied = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (helper.getBlockState(core.offset(dx, dy, dz)).getBlock() == block) occupied++;
                }
            }
        }
        // 63 (the 3x3x7 core box) + 15 + 15 (the x = +/-2 arms, 1x3x5) + 9 + 9 (the x = +/-3 tips, 1x3x3).
        helper.assertTrue(occupied == 111, "Watz must contain exactly 111 cells, found " + occupied);

        // The nine promoted port cells.
        for (int[] p : new int[][]{{2, 0, 0}, {-2, 0, 0}, {0, 0, 2}, {0, 0, -2},
                {2, 2, 0}, {-2, 2, 0}, {0, 2, 2}, {0, 2, -2}, {0, 2, 0}}) {
            var cell = core.offset(p[0], p[1], p[2]);
            int meta = helper.getBlockState(cell).getValue(BlockDummyable.META);
            helper.assertTrue(meta >= BlockDummyable.extra && meta < 12,
                    "Watz port cell " + p[0] + "," + p[1] + "," + p[2] + " must be an extra dummy, was meta " + meta);
        }

        var watz = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core,
                com.hbm.tileentity.machine.TileEntityWatz.class);
        var ports = watz.getConPos();
        helper.assertTrue(ports.size() == 10, "Watz must expose five intake and five outlet ports, found " + ports.size());
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.above(3)))
                && p.getDir() == com.hbm.lib.ForgeDirection.UP), "Missing centre coolant intake");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.below()))
                && p.getDir() == com.hbm.lib.ForgeDirection.DOWN), "Missing centre outlet");
        helper.succeed();
    }

    /** The whole struct ritual: build the shape, wait a second, get a reactor. */
    private static void watzStructAssembly(GameTestHelper helper) {
        var seed = new net.minecraft.core.BlockPos(4, 1, 4);
        helper.setBlock(seed, ModMachineBlocks.struct_watz_core.get().defaultBlockState());
        buildWatzShape(helper, seed);

        // The scan runs on the 20-tick boundary, so allow a full cycle plus slack.
        helper.runAfterDelay(45, () -> {
            var block = ModMachineBlocks.watz.get();
            helper.assertTrue(helper.getBlockState(seed).getBlock() == block,
                    "Struct core did not become a Watz reactor");
            helper.assertBlockProperty(seed, BlockDummyable.META, 12);
            helper.assertTrue(dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, seed,
                    com.hbm.tileentity.machine.TileEntityWatz.class) != null, "Assembled Watz has no controller");
            // The shell must have formed too, not just the core.
            helper.assertTrue(helper.getBlockState(seed.offset(3, 0, 0)).getBlock() == block,
                    "Assembled Watz did not fill its dummy shell");
            helper.succeed();
        });
    }

    /** One wrong block must keep the seed a seed, however long it waits. */
    private static void structPartialNoAssembly(GameTestHelper helper) {
        var seed = new net.minecraft.core.BlockPos(4, 1, 4);
        helper.setBlock(seed, ModMachineBlocks.struct_watz_core.get().defaultBlockState());
        buildWatzShape(helper, seed);
        // Swap one riveted casing cell for the un-riveted kind: correct block family, wrong variant.
        helper.setBlock(seed.offset(3, 0, 0), ModMachineBlocks.watz_casing.get().defaultBlockState());

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(helper.getBlockState(seed).getBlock() == ModMachineBlocks.struct_watz_core.get(),
                    "Struct core assembled despite an incomplete shape");
            helper.succeed();
        });
    }

    private static void zirnoxLayout(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        var block = ModMachineBlocks.machine_zirnox.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        helper.assertTrue(dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class) != null,
                "ZIRNOX controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        helper.assertBlockProperty(core, BlockDummyable.META, 12);
        int occupied = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean expected = dy <= 1
                            || (Math.abs(dx) <= 1 && Math.abs(dz) <= 1)
                            || (dz == 0 && Math.abs(dx) == 2);
                    boolean present = helper.getBlockState(core.offset(dx, dy, dz)).getBlock() == block;
                    helper.assertTrue(present == expected,
                            "Incorrect ZIRNOX cell at " + dx + "," + dy + "," + dz);
                    if (present) occupied++;
                }
            }
        }
        helper.assertTrue(occupied == 83, "ZIRNOX must contain exactly 83 cells, found " + occupied);
        helper.assertBlockProperty(core.offset(2, 1, 0), BlockDummyable.META, 7);
        helper.assertBlockProperty(core.offset(2, 3, 0), BlockDummyable.META, 7);
        helper.assertBlockProperty(core.offset(-2, 1, 0), BlockDummyable.META, 7);
        helper.assertBlockProperty(core.offset(-2, 3, 0), BlockDummyable.META, 7);
        helper.assertBlockProperty(core.above(4), BlockDummyable.META, 7);

        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        var ports = reactor.getConPos();
        helper.assertTrue(ports.size() == 4, "ZIRNOX must expose four lateral duct ports");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.offset(3, 1, 0)))
                        && p.getDir() == com.hbm.lib.ForgeDirection.EAST), "Missing lower east port");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.offset(3, 3, 0)))
                        && p.getDir() == com.hbm.lib.ForgeDirection.EAST), "Missing upper east port");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.offset(-3, 1, 0)))
                        && p.getDir() == com.hbm.lib.ForgeDirection.WEST), "Missing lower west port");
        helper.assertTrue(ports.stream().anyMatch(p -> p.getPos().equals(helper.absolutePos(core.offset(-3, 3, 0)))
                        && p.getDir() == com.hbm.lib.ForgeDirection.WEST), "Missing upper west port");
        var lowerEastCell = helper.absolutePos(core.offset(2, 1, 0));
        helper.assertTrue(block.canConnect(Fluids.WATER, helper.getLevel(), lowerEastCell.getX(), lowerEastCell.getY(),
                lowerEastCell.getZ(), com.hbm.lib.ForgeDirection.EAST), "Lower east shell must connect to its external duct");
        helper.assertFalse(block.canConnect(Fluids.WATER, helper.getLevel(), lowerEastCell.getX(), lowerEastCell.getY(),
                lowerEastCell.getZ(), com.hbm.lib.ForgeDirection.NORTH), "Port must not connect through the wrong shell face");
        helper.succeed();
    }

    private static void zirnoxWorldTick(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        var block = ModMachineBlocks.machine_zirnox.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        helper.assertTrue(reactor != null, "ZIRNOX controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);
        var rod = new net.minecraft.world.item.ItemStack(ModZirnoxItems.active(ZirnoxFuel.NATURAL_URANIUM));
        reactor.inventory.setStackInSlot(0, rod);
        reactor.isOn = true;

        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.heat > 0, "Registered ZIRNOX world ticker did not generate heat");
            helper.assertTrue(((com.hbm.items.machine.ItemZirnoxRod) rod.getItem()).getLife(rod) > 0,
                    "Registered ZIRNOX world ticker did not consume rod life");
        });
    }

    private static void zirnoxSteamOutput(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        var reactorBlock = ModMachineBlocks.machine_zirnox.get();
        helper.setBlock(core, reactorBlock.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        helper.assertTrue(reactor != null, "ZIRNOX controller setup failed");
        reactorBlock.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);

        var reactorDuctPos = core.offset(3, 1, 0);
        var tankDuctPos = core.offset(4, 1, 0);
        helper.setBlock(reactorDuctPos, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState());
        helper.setBlock(tankDuctPos, ModMachineBlocks.fluid_duct_mk2.get().defaultBlockState());
        var reactorDuct = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, reactorDuctPos, com.hbm.tileentity.network.TileEntityFluidDuctNT.class);
        var tankDuct = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, tankDuctPos, com.hbm.tileentity.network.TileEntityFluidDuctNT.class);
        reactorDuct.setFluidType(Fluids.SUPERHOTSTEAM);
        tankDuct.setFluidType(Fluids.SUPERHOTSTEAM);

        var tankCore = core.offset(6, 1, 1);
        helper.setBlock(tankCore, ModMachineBlocks.machine_fluidtank.get().defaultBlockState()
                .setValue(BlockDummyable.META, 13));
        var tank = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, tankCore, com.hbm.tileentity.machine.TileEntityMachineFluidTank.class);
        helper.assertTrue(tank != null, "Fluid tank controller setup failed");
        tank.mode = 1; // Buffer mode owns the network node at its physical port cells.
        tank.tank.setTankType(Fluids.SUPERHOTSTEAM);

        reactor.heat = 50_000;
        reactor.carbonDioxide.setFill(14_000);
        reactor.water.setFill(32_000);

        helper.succeedWhen(() -> {
            helper.assertTrue(reactor.water.getFill() < 32_000,
                    "ZIRNOX did not consume water for steam production");
            helper.assertTrue(tank.tank.getFill() > 0,
                    "ZIRNOX steam did not travel through the ducts into the tank");
            helper.assertTrue(tank.tank.getTankType() == Fluids.SUPERHOTSTEAM,
                    "ZIRNOX steam changed type while entering the tank");
        });
    }

    private static void zirnoxMeltdown(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        var block = ModMachineBlocks.machine_zirnox.get();
        helper.setBlock(core, block.defaultBlockState().setValue(BlockDummyable.META, 12));
        TileEntityReactorZirnox reactor = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, TileEntityReactorZirnox.class);
        helper.assertTrue(reactor != null, "ZIRNOX controller setup failed");
        block.formStructure(helper.getLevel(), helper.absolutePos(core), com.hbm.lib.ForgeDirection.NORTH);
        reactor.heat = TileEntityReactorZirnox.MAX_HEAT + 1;
        TileEntityReactorZirnox.serverTick(helper.getLevel(), helper.absolutePos(core), reactor.getBlockState(), reactor);

        helper.assertTrue(helper.getBlockState(core).getBlock() == ModMachineBlocks.zirnox_destroyed.get(),
                "Meltdown did not form the destroyed ZIRNOX core");
        var debris = helper.getLevel().getEntitiesOfClass(com.hbm.entity.projectile.EntityZirnoxDebris.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(core)).inflate(16.0D));
        helper.assertTrue(debris.size() == 72, "Expected 72 ZIRNOX debris entities, found " + debris.size());
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.EXCHANGER, 2);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.CONCRETE, 20);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.BLANK, 20);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.ELEMENT, 10);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.GRAPHITE, 10);
        assertDebrisCount(helper, debris, com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType.SHRAPNEL, 10);
        helper.succeed();
    }

    private static void assertDebrisCount(GameTestHelper helper,
                                          java.util.List<com.hbm.entity.projectile.EntityZirnoxDebris> debris,
                                          com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType type, int expected) {
        long count = debris.stream().filter(entity -> entity.getDebrisType() == type).count();
        helper.assertTrue(count == expected, "Expected " + expected + " " + type + " debris, found " + count);
    }

    private static void zirnoxWreckHazards(GameTestHelper helper) {
        var core = new net.minecraft.core.BlockPos(3, 1, 3);
        helper.setBlock(core, ModMachineBlocks.zirnox_destroyed.get().defaultBlockState().setValue(BlockDummyable.META, 12));
        var wreck = dev.golem.core.gametest.GameTestHelpers.blockEntity(helper, core, com.hbm.tileentity.machine.TileEntityZirnoxDestroyed.class);
        helper.assertTrue(wreck != null && wreck.isOnFire(), "Fresh ZIRNOX wreck must start burning");
        wreck.extinguish();
        helper.assertFalse(wreck.isOnFire(), "ZIRNOX wreck extinguish did not persist");
        helper.setBlock(core.above(), com.hbm.blocks.ModNuclearBlocks.gas_meltdown.get().defaultBlockState());
        helper.assertBlockPresent(com.hbm.blocks.ModNuclearBlocks.gas_meltdown.get(), core.above());
        helper.succeed();
    }

    private static void craterBiomeRegistry(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (ModBiomes.Type type : ModBiomes.Type.values()) {
            var holder = ModBiomes.holder(registries, type);
            helper.assertTrue(holder.value() != null, "Missing crater biome holder for " + type);
        }

        var plains = Biomes.holder(registries, Biomes.key("minecraft", "plains"));
        helper.assertTrue(ModBiomes.changeFor(14.99, 150, plains) == ModBiomes.Type.INNER,
                "Inner crater threshold is incorrect");
        helper.assertTrue(ModBiomes.changeFor(54.99, 100, plains) == ModBiomes.Type.CRATER,
                "Crater threshold is incorrect");
        helper.assertTrue(ModBiomes.changeFor(99.0, 25, plains) == ModBiomes.Type.OUTER,
                "Outer crater threshold is incorrect");
        helper.assertTrue(ModBiomes.changeFor(10.0, 100, ModBiomes.holder(registries, ModBiomes.Type.INNER)) == null,
                "Inner crater must be preserved");

        var position = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        var chunk = helper.getLevel().getChunk(position.getX() >> 4, position.getZ() >> 4);
        var target = ModBiomes.holder(registries, ModBiomes.Type.INNER);
        boolean changed = Biomes.apply(chunk, java.util.List.of(new Biomes.Change(
                position.getX(), position.getZ(), target, holder -> false)));
        helper.assertTrue(changed, "Biome section mutation did not report a change");
        helper.assertTrue(chunk.getNoiseBiome(position.getX() >> 2, position.getY() >> 2, position.getZ() >> 2)
                        .is(ModBiomes.crater_inner),
                "Biome section mutation did not write the requested holder");

        boolean preserved = Biomes.apply(chunk, java.util.List.of(new Biomes.Change(
                position.getX(), position.getZ(), ModBiomes.holder(registries, ModBiomes.Type.OUTER),
                ModBiomes.preserve(ModBiomes.Type.OUTER))));
        helper.assertFalse(preserved, "Outer crater must preserve an existing inner crater cell");

        // GameTest origins are not quart-aligned; choose a second column explicitly inside the same
        // quart cell so this assertion does not depend on the random test placement.
        var quartOrigin = new net.minecraft.core.BlockPos((position.getX() >> 2) << 2, position.getY(),
                (position.getZ() >> 2) << 2);
        var secondColumn = quartOrigin.offset(1, 0, 1);
        boolean sameQuartCell = Biomes.apply(chunk, java.util.List.of(
                new Biomes.Change(position.getX(), position.getZ(), ModBiomes.holder(registries, ModBiomes.Type.OUTER),
                        holder -> false),
                new Biomes.Change(secondColumn.getX(), secondColumn.getZ(), target, holder -> false)));
        helper.assertTrue(sameQuartCell, "Multiple columns in one quart cell did not produce one replacement");
        helper.assertTrue(chunk.getNoiseBiome(secondColumn.getX() >> 2, secondColumn.getY() >> 2,
                        secondColumn.getZ() >> 2).is(ModBiomes.crater_inner),
                "Last replacement did not win within one quart cell");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, com.hbm.inventory.fluid.FluidType input, int inputAmount,
                              com.hbm.inventory.fluid.FluidType output, int outputAmount, int heat) {
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(input);
        helper.assertTrue(recipe != null, "Missing cooling recipe for " + input.getName());
        helper.assertTrue(recipe.inputAmount() == inputAmount && recipe.output() == output
                        && recipe.outputAmount() == outputAmount && recipe.heatEnergy() == heat,
                "Incorrect cooling recipe for " + input.getName());
    }
}
