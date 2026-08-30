package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import com.hbm.platform.Services;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import com.hbm.tileentity.bomb.TileEntityNukeMan;
import com.hbm.tileentity.deco.TileEntityDeco;
import com.hbm.tileentity.machine.TileEntityCreativeFluidSource;
import com.hbm.tileentity.machine.TileEntityCreativeGenerator;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.TileEntityMachineMiniRTG;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
import com.hbm.tileentity.machine.TileEntityRadSensor;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import com.hbm.tileentity.machine.TileEntityMachineGasCent;
import com.hbm.tileentity.machine.TileEntityMachineElectrolyser;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
import com.hbm.tileentity.machine.TileEntityFurnaceBrick;
import com.hbm.tileentity.machine.TileEntityFurnaceIron;
import com.hbm.tileentity.machine.TileEntityFurnaceSteel;
import com.hbm.tileentity.machine.TileEntityMachineShredder;
import com.hbm.tileentity.machine.TileEntityMachineEPress;
import com.hbm.tileentity.machine.TileEntityMachinePress;
import com.hbm.tileentity.machine.TileEntitySawmill;
import com.hbm.tileentity.machine.TileEntityStirling;
import com.hbm.tileentity.machine.TileEntityMachinePumpElectric;
import com.hbm.tileentity.machine.TileEntityMachinePumpSteam;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.TileEntityMachineUF6Tank;
import com.hbm.tileentity.machine.TileEntityMachinePuF6Tank;
import com.hbm.tileentity.machine.TileEntityDeuteriumExtractor;
import com.hbm.tileentity.machine.TileEntityDeuteriumTower;
import com.hbm.tileentity.network.TileEntityCableBaseNT;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.tileentity.network.TileEntityFluidValve;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Registry for HBM block entity types. */
public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityMachineBattery>> MACHINE_BATTERY =
            BLOCK_ENTITIES.register("machine_battery",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineBattery::new, ModMachineBlocks.machine_battery.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineRadGen>> MACHINE_RAD_GEN =
            BLOCK_ENTITIES.register("machine_radgen",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineRadGen::new, ModMachineBlocks.machine_radgen.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMiniRTG>> MACHINE_MINI_RTG =
            BLOCK_ENTITIES.register("machine_minirtg",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineMiniRTG::new,
                            ModMachineBlocks.machine_minirtg.get(), ModMachineBlocks.rtg_polonium.get()));

    public static final RegistryObject<BlockEntityType<TileEntityRadSensor>> RAD_SENSOR =
            BLOCK_ENTITIES.register("radsensor",
                    () -> Services.PLATFORM.blockEntityType(TileEntityRadSensor::new, ModMachineBlocks.radsensor.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCableBaseNT>> RED_CABLE =
            BLOCK_ENTITIES.register("red_cable",
                    () -> Services.PLATFORM.blockEntityType(TileEntityCableBaseNT::new, ModMachineBlocks.red_cable.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeGenerator>> CREATIVE_GENERATOR =
            BLOCK_ENTITIES.register("creative_generator",
                    () -> Services.PLATFORM.blockEntityType(TileEntityCreativeGenerator::new, ModMachineBlocks.creative_generator.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbine>> MACHINE_TURBINE =
            BLOCK_ENTITIES.register("machine_turbine",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineTurbine::new, ModMachineBlocks.machine_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineLargeTurbine>> MACHINE_LARGE_TURBINE =
            BLOCK_ENTITIES.register("machine_large_turbine",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineLargeTurbine::new,
                            ModMachineBlocks.machine_large_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbineGas>> MACHINE_TURBINE_GAS =
            BLOCK_ENTITIES.register("machine_turbine_gas",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineTurbineGas::new,
                            ModMachineBlocks.machine_turbine_gas.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbofan>> MACHINE_TURBOFAN =
            BLOCK_ENTITIES.register("machine_turbofan",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineTurbofan::new,
                            ModMachineBlocks.machine_turbofan.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineSteamEngine>> MACHINE_STEAM_ENGINE =
            BLOCK_ENTITIES.register("machine_steam_engine",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineSteamEngine::new,
                            ModMachineBlocks.machine_steam_engine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpElectric>> MACHINE_PUMP_ELECTRIC =
            BLOCK_ENTITIES.register("machine_pump_electric",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachinePumpElectric::new,
                            ModMachineBlocks.machine_pump_electric.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpSteam>> MACHINE_PUMP_STEAM =
            BLOCK_ENTITIES.register("machine_pump_steam",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachinePumpSteam::new,
                            ModMachineBlocks.machine_pump_steam.get()));

    public static final RegistryObject<BlockEntityType<TileEntityStirling>> MACHINE_STIRLING =
            BLOCK_ENTITIES.register("machine_stirling",
                    () -> Services.PLATFORM.blockEntityType(TileEntityStirling::new,
                            ModMachineBlocks.machine_stirling.get(),
                            ModMachineBlocks.machine_stirling_steel.get(),
                            ModMachineBlocks.machine_stirling_creative.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine>> MACHINE_INDUSTRIAL_TURBINE =
            BLOCK_ENTITIES.register("machine_industrial_turbine",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine::new,
                            ModMachineBlocks.machine_industrial_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCondenser>> MACHINE_CONDENSER =
            BLOCK_ENTITIES.register("machine_condenser",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityCondenser::new, ModMachineBlocks.machine_condenser.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionCoupler>> FUSION_COUPLER =
            BLOCK_ENTITIES.register("fusion_coupler",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionCoupler::new, ModMachineBlocks.fusion_coupler.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionCollector>> FUSION_COLLECTOR =
            BLOCK_ENTITIES.register("fusion_collector",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionCollector::new, ModMachineBlocks.fusion_collector.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron>> FUSION_KLYSTRON =
            BLOCK_ENTITIES.register("fusion_klystron",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron::new, ModMachineBlocks.fusion_klystron.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative>> FUSION_KLYSTRON_CREATIVE =
            BLOCK_ENTITIES.register("fusion_klystron_creative",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative::new, ModMachineBlocks.fusion_klystron_creative.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler>> FUSION_BOILER =
            BLOCK_ENTITIES.register("fusion_boiler",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler::new, ModMachineBlocks.fusion_boiler.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder>> FUSION_BREEDER =
            BLOCK_ENTITIES.register("fusion_breeder",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder::new, ModMachineBlocks.fusion_breeder.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT>> FUSION_MHDT =
            BLOCK_ENTITIES.register("fusion_mhdt",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT::new, ModMachineBlocks.fusion_mhdt.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionTorus>> FUSION_TORUS =
            BLOCK_ENTITIES.register("fusion_torus",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionTorus::new, ModMachineBlocks.fusion_torus.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge>> FUSION_PLASMA_FORGE =
            BLOCK_ENTITIES.register("fusion_plasma_forge",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge::new,
                            ModMachineBlocks.fusion_plasma_forge.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct>> STRUCT_TORUS_CORE =
            BLOCK_ENTITIES.register("struct_torus_core",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct::new, ModMachineBlocks.struct_torus_core.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWatz>> WATZ =
            BLOCK_ENTITIES.register("watz",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityWatz::new, ModMachineBlocks.watz.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWatzPump>> WATZ_PUMP =
            BLOCK_ENTITIES.register("watz_pump",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityWatzPump::new, ModMachineBlocks.watz_pump.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.struct.TileEntityWatzStruct>> STRUCT_WATZ_CORE =
            BLOCK_ENTITIES.register("struct_watz_core",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.struct.TileEntityWatzStruct::new, ModMachineBlocks.struct_watz_core.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityReactorZirnox>> MACHINE_ZIRNOX =
            BLOCK_ENTITIES.register("machine_zirnox",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityReactorZirnox::new, ModMachineBlocks.machine_zirnox.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityZirnoxDestroyed>> ZIRNOX_DESTROYED =
            BLOCK_ENTITIES.register("zirnox_destroyed",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityZirnoxDestroyed::new, ModMachineBlocks.zirnox_destroyed.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWasteDrum>> MACHINE_WASTE_DRUM =
            BLOCK_ENTITIES.register("machine_waste_drum",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityWasteDrum::new, ModMachineBlocks.machine_waste_drum.get()));

    // --- Phase C: fluid network demo ---

    public static final RegistryObject<BlockEntityType<TileEntityFluidDuctNT>> FLUID_DUCT =
            BLOCK_ENTITIES.register("fluid_duct_mk2",
                    () -> Services.PLATFORM.blockEntityType(TileEntityFluidDuctNT::new, ModMachineBlocks.fluid_duct_mk2.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeFluidSource>> CREATIVE_FLUID_SOURCE =
            BLOCK_ENTITIES.register("creative_fluid_source",
                    () -> Services.PLATFORM.blockEntityType(TileEntityCreativeFluidSource::new, ModMachineBlocks.creative_fluid_source.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineFluidTank>> FLUID_TANK =
            BLOCK_ENTITIES.register("machine_fluidtank",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineFluidTank::new, ModMachineBlocks.machine_fluidtank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineUF6Tank>> MACHINE_UF6_TANK =
            BLOCK_ENTITIES.register("machine_uf6_tank",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineUF6Tank::new, ModMachineBlocks.machine_uf6_tank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePuF6Tank>> MACHINE_PUF6_TANK =
            BLOCK_ENTITIES.register("machine_puf6_tank",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachinePuF6Tank::new, ModMachineBlocks.machine_puf6_tank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDeuteriumExtractor>> MACHINE_DEUTERIUM_EXTRACTOR =
            BLOCK_ENTITIES.register("machine_deuterium_extractor",
                    () -> Services.PLATFORM.blockEntityType(TileEntityDeuteriumExtractor::new,
                            ModMachineBlocks.machine_deuterium_extractor.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDeuteriumTower>> MACHINE_DEUTERIUM_TOWER =
            BLOCK_ENTITIES.register("machine_deuterium_tower",
                    () -> Services.PLATFORM.blockEntityType(TileEntityDeuteriumTower::new,
                            ModMachineBlocks.machine_deuterium_tower.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFluidValve>> FLUID_VALVE =
            BLOCK_ENTITIES.register("fluid_valve",
                    () -> Services.PLATFORM.blockEntityType(TileEntityFluidValve::new, ModMachineBlocks.fluid_valve.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineDrain>> MACHINE_DRAIN =
            BLOCK_ENTITIES.register("machine_drain",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineDrain::new,
                            ModMachineBlocks.machine_drain.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBatterySocket>> BATTERY_SOCKET =
            BLOCK_ENTITIES.register("machine_battery_socket",
                    () -> Services.PLATFORM.blockEntityType(TileEntityBatterySocket::new, ModBatteryBlocks.machine_battery_socket.get()));

    // Storage crates — one TileEntityCrate class, three types (each carries its CrateType layout). The
    // BE resolves its own type via CrateType.blockEntityType(), wired below (avoids a self-reference).
    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_IRON =
            BLOCK_ENTITIES.register("crate_iron", () -> Services.PLATFORM.blockEntityType(
                    (pos, state) -> new TileEntityCrate(CrateType.IRON, pos, state), ModStorageBlocks.crate_iron.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_STEEL =
            BLOCK_ENTITIES.register("crate_steel", () -> Services.PLATFORM.blockEntityType(
                    (pos, state) -> new TileEntityCrate(CrateType.STEEL, pos, state), ModStorageBlocks.crate_steel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_DESH =
            BLOCK_ENTITIES.register("crate_desh", () -> Services.PLATFORM.blockEntityType(
                    (pos, state) -> new TileEntityCrate(CrateType.DESH, pos, state), ModStorageBlocks.crate_desh.get()));

    static {
        CrateType.IRON.setTypeSupplier(CRATE_IRON::get);
        CrateType.STEEL.setTypeSupplier(CRATE_STEEL::get);
        CrateType.DESH.setTypeSupplier(CRATE_DESH::get);
    }

    // --- scaffold machines (placeable + persistent; logic deferred, see IMPLEMENTATION.md §18) ---

    public static final RegistryObject<BlockEntityType<TileEntityMachineElectricFurnace>> MACHINE_ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("machine_electric_furnace",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineElectricFurnace::new, ModMachineBlocks.machine_electric_furnace_off.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFurnaceBrick>> FURNACE_BRICK =
            BLOCK_ENTITIES.register("machine_furnace_brick_off",
                    () -> Services.PLATFORM.blockEntityType(TileEntityFurnaceBrick::new, ModMachineBlocks.machine_furnace_brick_off.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFurnaceIron>> FURNACE_IRON =
            BLOCK_ENTITIES.register("furnace_iron",
                    () -> Services.PLATFORM.blockEntityType(TileEntityFurnaceIron::new, ModMachineBlocks.furnace_iron.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFurnaceSteel>> FURNACE_STEEL =
            BLOCK_ENTITIES.register("furnace_steel",
                    () -> Services.PLATFORM.blockEntityType(TileEntityFurnaceSteel::new, ModMachineBlocks.furnace_steel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineShredder>> MACHINE_SHREDDER =
            BLOCK_ENTITIES.register("machine_shredder",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineShredder::new, ModMachineBlocks.machine_shredder.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineEPress>> MACHINE_EPRESS =
            BLOCK_ENTITIES.register("machine_epress",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineEPress::new, ModMachineBlocks.machine_epress.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePress>> MACHINE_PRESS =
            BLOCK_ENTITIES.register("machine_press",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachinePress::new, ModMachineBlocks.machine_press.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySawmill>> MACHINE_SAWMILL =
            BLOCK_ENTITIES.register("machine_sawmill",
                    () -> Services.PLATFORM.blockEntityType(TileEntitySawmill::new, ModMachineBlocks.machine_sawmill.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineAssemblyMachine>> MACHINE_ASSEMBLY_MACHINE =
            BLOCK_ENTITIES.register("machine_assembly_machine",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineAssemblyMachine::new, ModMachineBlocks.machine_assembly_machine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineChemicalPlant>> MACHINE_CHEMICAL_PLANT =
            BLOCK_ENTITIES.register("machine_chemical_plant",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineChemicalPlant::new, ModMachineBlocks.machine_chemical_plant.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMixer>> MACHINE_MIXER =
            BLOCK_ENTITIES.register("machine_mixer",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineMixer::new, ModMachineBlocks.machine_mixer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineExposureChamber>> MACHINE_EXPOSURE_CHAMBER =
            BLOCK_ENTITIES.register("machine_exposure_chamber",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineExposureChamber::new,
                            ModMachineBlocks.machine_exposure_chamber.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineBlastFurnace>> MACHINE_BLAST_FURNACE =
            BLOCK_ENTITIES.register("machine_blast_furnace",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineBlastFurnace::new,
                            ModMachineBlocks.machine_blast_furnace.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachinePUREX>> MACHINE_PUREX =
            BLOCK_ENTITIES.register("machine_purex",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachinePUREX::new, ModMachineBlocks.machine_purex.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineSolderingStation>> MACHINE_SOLDERING_STATION =
            BLOCK_ENTITIES.register("machine_soldering_station",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineSolderingStation::new, ModMachineBlocks.machine_soldering_station.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCrystallizer>> MACHINE_CRYSTALLIZER =
            BLOCK_ENTITIES.register("machine_crystallizer",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineCrystallizer::new, ModMachineBlocks.machine_crystallizer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCentrifuge>> MACHINE_CENTRIFUGE =
            BLOCK_ENTITIES.register("machine_centrifuge",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineCentrifuge::new, ModMachineBlocks.machine_centrifuge.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineGasCent>> MACHINE_GAS_CENT =
            BLOCK_ENTITIES.register("machine_gascent",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineGasCent::new, ModMachineBlocks.machine_gascent.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineElectrolyser>> MACHINE_ELECTROLYSER =
            BLOCK_ENTITIES.register("machine_electrolyser",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineElectrolyser::new, ModMachineBlocks.machine_electrolyser.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntitySILEX>> MACHINE_SILEX =
            BLOCK_ENTITIES.register("machine_silex",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntitySILEX::new, ModMachineBlocks.machine_silex.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFEL>> MACHINE_FEL =
            BLOCK_ENTITIES.register("machine_fel",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityFEL::new, ModMachineBlocks.machine_fel.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineOilWell>> MACHINE_OIL_WELL =
            BLOCK_ENTITIES.register("machine_well",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineOilWell::new, ModMachineBlocks.machine_well.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack>> MACHINE_PUMPJACK =
            BLOCK_ENTITIES.register("machine_pumpjack",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack::new, ModMachineBlocks.machine_pumpjack.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineDiesel>> MACHINE_DIESEL =
            BLOCK_ENTITIES.register("machine_diesel",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineDiesel::new, ModMachineBlocks.machine_diesel.get()));

    // --- advanced oil wave ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower>> MACHINE_FRACKING_TOWER =
            BLOCK_ENTITIES.register("machine_fracking_tower",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower::new, ModMachineBlocks.machine_fracking_tower.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill>> MACHINE_VACUUM_DISTILL =
            BLOCK_ENTITIES.register("machine_vacuum_distill",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill::new, ModMachineBlocks.machine_vacuum_distill.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer>> MACHINE_CATALYTIC_REFORMER =
            BLOCK_ENTITIES.register("machine_catalytic_reformer",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer::new, ModMachineBlocks.machine_catalytic_reformer.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker>> MACHINE_CATALYTIC_CRACKER =
            BLOCK_ENTITIES.register("machine_catalytic_cracker",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker::new, ModMachineBlocks.machine_catalytic_cracker.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater>> MACHINE_HYDROTREATER =
            BLOCK_ENTITIES.register("machine_hydrotreater",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater::new, ModMachineBlocks.machine_hydrotreater.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCoker>> MACHINE_COKER =
            BLOCK_ENTITIES.register("machine_coker",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineCoker::new, ModMachineBlocks.machine_coker.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFurnaceCombination>> FURNACE_COMBINATION =
            BLOCK_ENTITIES.register("machine_furnace_combination",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityFurnaceCombination::new,
                            ModMachineBlocks.machine_furnace_combination.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven>> MACHINE_PYROOVEN =
            BLOCK_ENTITIES.register("machine_pyrooven",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven::new, ModMachineBlocks.machine_pyrooven.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineCombustionEngine>> MACHINE_COMBUSTION_ENGINE =
            BLOCK_ENTITIES.register("machine_combustion_engine",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineCombustionEngine::new, ModMachineBlocks.machine_combustion_engine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityRefueler>> REFUELER =
            BLOCK_ENTITIES.register("refueler",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityRefueler::new, ModMachineBlocks.refueler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineCompressor>> MACHINE_COMPRESSOR =
            BLOCK_ENTITIES.register("machine_compressor",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineCompressor::new, ModMachineBlocks.machine_compressor.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare>> MACHINE_GAS_FLARE =
            BLOCK_ENTITIES.register("machine_flare",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare::new, ModMachineBlocks.machine_flare.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower>> MACHINE_FRACTION_TOWER =
            BLOCK_ENTITIES.register("machine_fraction_tower",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower::new, ModMachineBlocks.machine_fraction_tower.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntitySpacer>> FRACTION_SPACER =
            BLOCK_ENTITIES.register("fraction_spacer",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntitySpacer::new, ModMachineBlocks.fraction_spacer.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor>> MACHINE_LIQUEFACTOR =
            BLOCK_ENTITIES.register("machine_liquefactor",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor::new, ModMachineBlocks.machine_liquefactor.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier>> MACHINE_SOLIDIFIER =
            BLOCK_ENTITIES.register("machine_solidifier",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier::new, ModMachineBlocks.machine_solidifier.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineRefinery>> MACHINE_REFINERY =
            BLOCK_ENTITIES.register("machine_refinery",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.oil.TileEntityMachineRefinery::new, ModMachineBlocks.machine_refinery.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeatBoiler>> HEAT_BOILER =
            BLOCK_ENTITIES.register("heat_boiler",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityHeatBoiler::new, ModMachineBlocks.machine_boiler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial>> MACHINE_INDUSTRIAL_BOILER =
            BLOCK_ENTITIES.register("machine_industrial_boiler",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial::new, ModMachineBlocks.machine_industrial_boiler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterOilburner>> HEATER_OILBURNER =
            BLOCK_ENTITIES.register("heater_oilburner",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityHeaterOilburner::new, ModMachineBlocks.heater_oilburner.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineWoodBurner>> MACHINE_WOOD_BURNER =
            BLOCK_ENTITIES.register("machine_wood_burner",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineWoodBurner::new, ModMachineBlocks.machine_wood_burner.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineMiningLaser>> MACHINE_MINING_LASER =
            BLOCK_ENTITIES.register("machine_mining_laser",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityMachineMiningLaser::new, ModMachineBlocks.machine_mining_laser.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCrucible>> MACHINE_CRUCIBLE =
            BLOCK_ENTITIES.register("machine_crucible",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityCrucible::new, ModMachineBlocks.machine_crucible.get()));

    // --- foundry transport / casting ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryChannel>> FOUNDRY_CHANNEL =
            BLOCK_ENTITIES.register("foundry_channel",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityFoundryChannel::new, ModMachineBlocks.foundry_channel.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryMold>> FOUNDRY_MOLD =
            BLOCK_ENTITIES.register("foundry_mold",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityFoundryMold::new, ModMachineBlocks.foundry_mold.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryBasin>> FOUNDRY_BASIN =
            BLOCK_ENTITIES.register("foundry_basin",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityFoundryBasin::new, ModMachineBlocks.foundry_basin.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryOutlet>> FOUNDRY_OUTLET =
            BLOCK_ENTITIES.register("foundry_outlet",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityFoundryOutlet::new, ModMachineBlocks.foundry_outlet.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundrySlagtap>> FOUNDRY_SLAGTAP =
            BLOCK_ENTITIES.register("foundry_slagtap",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityFoundrySlagtap::new, ModMachineBlocks.foundry_slagtap.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag>> SLAG =
            BLOCK_ENTITIES.register("slag",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag::new, ModMachineBlocks.slag.get()));

    // --- foundry heat sources ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterFirebox>> HEATER_FIREBOX =
            BLOCK_ENTITIES.register("heater_firebox",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityHeaterFirebox::new, ModMachineBlocks.heater_firebox.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterOven>> HEATER_OVEN =
            BLOCK_ENTITIES.register("heater_oven",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityHeaterOven::new, ModMachineBlocks.heater_oven.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterElectric>> HEATER_ELECTRIC =
            BLOCK_ENTITIES.register("heater_electric",
                    () -> Services.PLATFORM.blockEntityType(com.hbm.tileentity.machine.TileEntityHeaterElectric::new, ModMachineBlocks.heater_electric.get()));

    public static final RegistryObject<BlockEntityType<TileEntityLaunchPad>> LAUNCH_PAD =
            BLOCK_ENTITIES.register("launch_pad",
                    () -> Services.PLATFORM.blockEntityType(TileEntityLaunchPad::new, ModMachineBlocks.launch_pad.get()));

    // --- bombs ---

    public static final RegistryObject<BlockEntityType<TileEntityNukeMan>> NUKE_MAN =
            BLOCK_ENTITIES.register("nuke_man",
                    () -> Services.PLATFORM.blockEntityType(TileEntityNukeMan::new, ModBombBlocks.nuke_man.get()));

    // --- the 12 tiered anvils (OBJ-rendered; the BE exists only for the renderer) ---

    public static final RegistryObject<BlockEntityType<TileEntityAnvil>> ANVIL =
            BLOCK_ENTITIES.register("anvil",
                    () -> Services.PLATFORM.blockEntityType(TileEntityAnvil::new, com.hbm.blocks.ModAnvilBlocks.ANVILS.stream()
                            .map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)));

    // --- decorative OBJ-rendered blocks (boxcar / boat / satellite dishes) ---

    public static final RegistryObject<BlockEntityType<TileEntityDeco>> DECO =
            BLOCK_ENTITIES.register("deco",
                    () -> Services.PLATFORM.blockEntityType(TileEntityDeco::new, java.util.stream.Stream.concat(
                            com.hbm.blocks.ModDecoBlocks.OBJ_BLOCKS.stream(), com.hbm.blocks.ModSteelBlocks.OBJ_BLOCKS.stream())
                            .map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)));

    public static void init() {}
}
