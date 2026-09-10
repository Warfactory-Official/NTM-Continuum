package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.tileentity.machine.storage.CrateType;
import com.hbm.tileentity.machine.storage.TileEntityBatterySocket;
import com.hbm.tileentity.machine.storage.TileEntityMachineFENSU;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import com.hbm.tileentity.bomb.TileEntityNukeMan;
import com.hbm.tileentity.bomb.TileEntityNukeN2;
import com.hbm.tileentity.bomb.TileEntityNukeBoy;
import com.hbm.tileentity.bomb.TileEntityNukeGadget;
import com.hbm.tileentity.bomb.TileEntityNukeTsar;
import com.hbm.tileentity.deco.TileEntityDeco;
import com.hbm.tileentity.machine.TileEntityCreativeFluidSource;
import com.hbm.tileentity.machine.TileEntityCreativeGenerator;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.TileEntityMachineMiniRTG;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
import com.hbm.tileentity.machine.TileEntityRadSensor;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import com.hbm.tileentity.machine.albion.TileEntityPABeamline;
import com.hbm.tileentity.machine.albion.TileEntityPADetector;
import com.hbm.tileentity.machine.albion.TileEntityPADipole;
import com.hbm.tileentity.machine.albion.TileEntityPAQuadrupole;
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import com.hbm.tileentity.machine.albion.TileEntityPASource;
import com.hbm.tileentity.machine.TileEntityMachineMixer;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
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
import com.hbm.tileentity.machine.TileEntityMachineAmmoPress;
import com.hbm.tileentity.machine.TileEntitySawmill;
import com.hbm.tileentity.machine.TileEntityStirling;
import com.hbm.tileentity.machine.TileEntityMachinePumpElectric;
import com.hbm.tileentity.machine.TileEntityMachinePumpSteam;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.machine.TileEntityMachineUF6Tank;
import com.hbm.tileentity.machine.TileEntityMachinePuF6Tank;
import com.hbm.tileentity.machine.TileEntityMachineBigAssTank;
import com.hbm.tileentity.machine.TileEntityMachineBAT9000;
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
                    () -> BlockEntities.type(TileEntityMachineBattery::new, ModMachineBlocks.machine_battery.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineRadGen>> MACHINE_RAD_GEN =
            BLOCK_ENTITIES.register("machine_radgen",
                    () -> BlockEntities.type(TileEntityMachineRadGen::new, ModMachineBlocks.machine_radgen.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMiniRTG>> MACHINE_MINI_RTG =
            BLOCK_ENTITIES.register("machine_minirtg",
                    () -> BlockEntities.type(TileEntityMachineMiniRTG::new,
                            ModMachineBlocks.machine_minirtg.get(), ModMachineBlocks.rtg_polonium.get()));

    public static final RegistryObject<BlockEntityType<TileEntityRadSensor>> RAD_SENSOR =
            BLOCK_ENTITIES.register("radsensor",
                    () -> BlockEntities.type(TileEntityRadSensor::new, ModMachineBlocks.radsensor.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCapacitor>>
            CAPACITOR = BLOCK_ENTITIES.register("capacitor",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityCapacitor::new,
                            ModMachineBlocks.machine_capacitor_copper.get(), ModMachineBlocks.machine_capacitor_gold.get(),
                            ModMachineBlocks.machine_capacitor_niobium.get(), ModMachineBlocks.machine_capacitor_tantalium.get(),
                            ModMachineBlocks.machine_capacitor_schrabidate.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCargoElevator>>
            CARGO_ELEVATOR = BLOCK_ENTITIES.register("cargo_elevator",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityCargoElevator::new,
                            ModMachineBlocks.cargo_elevator.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityBarrel>> BARREL =
            BLOCK_ENTITIES.register("barrel",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityBarrel::new,
                            ModMachineBlocks.barrel_plastic.get(), ModMachineBlocks.barrel_corroded.get(), ModMachineBlocks.barrel_iron.get(), ModMachineBlocks.barrel_steel.get(), ModMachineBlocks.barrel_tcalloy.get(), ModMachineBlocks.barrel_antimatter.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityGeiger>> GEIGER =
            BLOCK_ENTITIES.register("geiger",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityGeiger::new,
                            ModMachineBlocks.geiger.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.network.TileEntityPipeGauge>>
            PIPE_GAUGE = BLOCK_ENTITIES.register("fluid_duct_gauge",
                    () -> BlockEntities.type(com.hbm.tileentity.network.TileEntityPipeGauge::new,
                            ModMachineBlocks.fluid_duct_gauge.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.network.TileEntityCableGauge>>
            CABLE_GAUGE = BLOCK_ENTITIES.register("red_cable_gauge",
                    () -> BlockEntities.type(com.hbm.tileentity.network.TileEntityCableGauge::new,
                            ModMachineBlocks.red_cable_gauge.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCableBaseNT>> RED_CABLE =
            BLOCK_ENTITIES.register("red_cable",
                    () -> BlockEntities.type(TileEntityCableBaseNT::new, ModMachineBlocks.red_cable.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeGenerator>> CREATIVE_GENERATOR =
            BLOCK_ENTITIES.register("creative_generator",
                    () -> BlockEntities.type(TileEntityCreativeGenerator::new, ModMachineBlocks.creative_generator.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbine>> MACHINE_TURBINE =
            BLOCK_ENTITIES.register("machine_turbine",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineTurbine::new, ModMachineBlocks.machine_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineLargeTurbine>> MACHINE_LARGE_TURBINE =
            BLOCK_ENTITIES.register("machine_large_turbine",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineLargeTurbine::new,
                            ModMachineBlocks.machine_large_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbineGas>> MACHINE_TURBINE_GAS =
            BLOCK_ENTITIES.register("machine_turbine_gas",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineTurbineGas::new,
                            ModMachineBlocks.machine_turbine_gas.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbofan>> MACHINE_TURBOFAN =
            BLOCK_ENTITIES.register("machine_turbofan",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineTurbofan::new,
                            ModMachineBlocks.machine_turbofan.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineSteamEngine>> MACHINE_STEAM_ENGINE =
            BLOCK_ENTITIES.register("machine_steam_engine",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineSteamEngine::new,
                            ModMachineBlocks.machine_steam_engine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpElectric>> MACHINE_PUMP_ELECTRIC =
            BLOCK_ENTITIES.register("machine_pump_electric",
                    () -> BlockEntities.type(TileEntityMachinePumpElectric::new,
                            ModMachineBlocks.machine_pump_electric.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpSteam>> MACHINE_PUMP_STEAM =
            BLOCK_ENTITIES.register("machine_pump_steam",
                    () -> BlockEntities.type(TileEntityMachinePumpSteam::new,
                            ModMachineBlocks.machine_pump_steam.get()));

    public static final RegistryObject<BlockEntityType<TileEntityStirling>> MACHINE_STIRLING =
            BLOCK_ENTITIES.register("machine_stirling",
                    () -> BlockEntities.type(TileEntityStirling::new,
                            ModMachineBlocks.machine_stirling.get(),
                            ModMachineBlocks.machine_stirling_steel.get(),
                            ModMachineBlocks.machine_stirling_creative.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine>> MACHINE_INDUSTRIAL_TURBINE =
            BLOCK_ENTITIES.register("machine_industrial_turbine",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine::new,
                            ModMachineBlocks.machine_industrial_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2>> PILE_BLOCK =
            BLOCK_ENTITIES.register("pile_block",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2::new,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_block.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileCore>> PILE_CORE =
            BLOCK_ENTITIES.register("pile_core",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.pile.TileEntityPileCore::new,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_block.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileLoader>> PILE_LOADER =
            BLOCK_ENTITIES.register("pile_loader",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.pile.TileEntityPileLoader::new,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_device.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileVent>> PILE_VENT =
            BLOCK_ENTITIES.register("pile_vent",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.pile.TileEntityPileVent::new,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_device.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileControl>> PILE_CONTROL =
            BLOCK_ENTITIES.register("pile_control",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.pile.TileEntityPileControl::new,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_device.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityChungus>> MACHINE_CHUNGUS =
            BLOCK_ENTITIES.register("machine_chungus",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityChungus::new,
                            ModMachineBlocks.machine_chungus.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCondenser>> MACHINE_CONDENSER =
            BLOCK_ENTITIES.register("machine_condenser",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityCondenser::new, ModMachineBlocks.machine_condenser.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCondenserPowered>> MACHINE_CONDENSER_POWERED =
            BLOCK_ENTITIES.register("machine_condenser_powered",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityCondenserPowered::new,
                            ModMachineBlocks.machine_condenser_powered.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionCoupler>> FUSION_COUPLER =
            BLOCK_ENTITIES.register("fusion_coupler",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionCoupler::new, ModMachineBlocks.fusion_coupler.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionCollector>> FUSION_COLLECTOR =
            BLOCK_ENTITIES.register("fusion_collector",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionCollector::new, ModMachineBlocks.fusion_collector.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron>> FUSION_KLYSTRON =
            BLOCK_ENTITIES.register("fusion_klystron",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron::new, ModMachineBlocks.fusion_klystron.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative>> FUSION_KLYSTRON_CREATIVE =
            BLOCK_ENTITIES.register("fusion_klystron_creative",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative::new, ModMachineBlocks.fusion_klystron_creative.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler>> FUSION_BOILER =
            BLOCK_ENTITIES.register("fusion_boiler",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler::new, ModMachineBlocks.fusion_boiler.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder>> FUSION_BREEDER =
            BLOCK_ENTITIES.register("fusion_breeder",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder::new, ModMachineBlocks.fusion_breeder.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT>> FUSION_MHDT =
            BLOCK_ENTITIES.register("fusion_mhdt",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT::new, ModMachineBlocks.fusion_mhdt.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionTorus>> FUSION_TORUS =
            BLOCK_ENTITIES.register("fusion_torus",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionTorus::new, ModMachineBlocks.fusion_torus.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge>> FUSION_PLASMA_FORGE =
            BLOCK_ENTITIES.register("fusion_plasma_forge",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge::new,
                            ModMachineBlocks.fusion_plasma_forge.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct>> STRUCT_TORUS_CORE =
            BLOCK_ENTITIES.register("struct_torus_core",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct::new, ModMachineBlocks.struct_torus_core.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWatz>> WATZ =
            BLOCK_ENTITIES.register("watz",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityWatz::new, ModMachineBlocks.watz.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWatzPump>> WATZ_PUMP =
            BLOCK_ENTITIES.register("watz_pump",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityWatzPump::new, ModMachineBlocks.watz_pump.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.struct.TileEntityWatzStruct>> STRUCT_WATZ_CORE =
            BLOCK_ENTITIES.register("struct_watz_core",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.struct.TileEntityWatzStruct::new, ModMachineBlocks.struct_watz_core.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityReactorZirnox>> MACHINE_ZIRNOX =
            BLOCK_ENTITIES.register("machine_zirnox",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityReactorZirnox::new, ModMachineBlocks.machine_zirnox.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityZirnoxDestroyed>> ZIRNOX_DESTROYED =
            BLOCK_ENTITIES.register("zirnox_destroyed",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityZirnoxDestroyed::new, ModMachineBlocks.zirnox_destroyed.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWasteDrum>> MACHINE_WASTE_DRUM =
            BLOCK_ENTITIES.register("machine_waste_drum",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityWasteDrum::new, ModMachineBlocks.machine_waste_drum.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityStorageDrum>> MACHINE_STORAGE_DRUM =
            BLOCK_ENTITIES.register("machine_storage_drum",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityStorageDrum::new,
                            ModMachineBlocks.machine_storage_drum.get()));

    // --- Phase C: fluid network demo ---

    public static final RegistryObject<BlockEntityType<TileEntityFluidDuctNT>> FLUID_DUCT =
            BLOCK_ENTITIES.register("fluid_duct_mk2",
                    () -> BlockEntities.type(TileEntityFluidDuctNT::new, ModMachineBlocks.fluid_duct_mk2.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeFluidSource>> CREATIVE_FLUID_SOURCE =
            BLOCK_ENTITIES.register("creative_fluid_source",
                    () -> BlockEntities.type(TileEntityCreativeFluidSource::new, ModMachineBlocks.creative_fluid_source.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineFluidTank>> FLUID_TANK =
            BLOCK_ENTITIES.register("machine_fluidtank",
                    () -> BlockEntities.type(TileEntityMachineFluidTank::new, ModMachineBlocks.machine_fluidtank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineUF6Tank>> MACHINE_UF6_TANK =
            BLOCK_ENTITIES.register("machine_uf6_tank",
                    () -> BlockEntities.type(TileEntityMachineUF6Tank::new, ModMachineBlocks.machine_uf6_tank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePuF6Tank>> MACHINE_PUF6_TANK =
            BLOCK_ENTITIES.register("machine_puf6_tank",
                    () -> BlockEntities.type(TileEntityMachinePuF6Tank::new, ModMachineBlocks.machine_puf6_tank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineBigAssTank>> MACHINE_BIG_ASS_TANK =
            BLOCK_ENTITIES.register("machine_bigasstank",
                    () -> BlockEntities.type(TileEntityMachineBigAssTank::new,
                            ModMachineBlocks.machine_bigasstank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineBAT9000>> MACHINE_BAT9000 =
            BLOCK_ENTITIES.register("machine_bat9000",
                    () -> BlockEntities.type(TileEntityMachineBAT9000::new,
                            ModMachineBlocks.machine_bat9000.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDeuteriumExtractor>> MACHINE_DEUTERIUM_EXTRACTOR =
            BLOCK_ENTITIES.register("machine_deuterium_extractor",
                    () -> BlockEntities.type(TileEntityDeuteriumExtractor::new,
                            ModMachineBlocks.machine_deuterium_extractor.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDeuteriumTower>> MACHINE_DEUTERIUM_TOWER =
            BLOCK_ENTITIES.register("machine_deuterium_tower",
                    () -> BlockEntities.type(TileEntityDeuteriumTower::new,
                            ModMachineBlocks.machine_deuterium_tower.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFluidValve>> FLUID_VALVE =
            BLOCK_ENTITIES.register("fluid_valve",
                    () -> BlockEntities.type(TileEntityFluidValve::new, ModMachineBlocks.fluid_valve.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.network.TileEntityFluidCounterValve>>
            FLUID_COUNTER_VALVE = BLOCK_ENTITIES.register("fluid_counter_valve",
                    () -> BlockEntities.type(com.hbm.tileentity.network.TileEntityFluidCounterValve::new,
                            ModMachineBlocks.fluid_counter_valve.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineDrain>> MACHINE_DRAIN =
            BLOCK_ENTITIES.register("machine_drain",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineDrain::new,
                            ModMachineBlocks.machine_drain.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretChekhov>> TURRET_CHEKHOV =
            BLOCK_ENTITIES.register("turret_chekhov",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretChekhov::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_chekhov.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretFriendly>> TURRET_FRIENDLY =
            BLOCK_ENTITIES.register("turret_friendly",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretFriendly::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_friendly.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretSentry>> TURRET_SENTRY =
            BLOCK_ENTITIES.register("turret_sentry",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretSentry::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_sentry.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretSentryDamaged>> TURRET_SENTRY_DAMAGED =
            BLOCK_ENTITIES.register("turret_sentry_damaged",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretSentryDamaged::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_sentry_damaged.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretHoward>> TURRET_HOWARD =
            BLOCK_ENTITIES.register("turret_howard",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretHoward::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_howard.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretHowardDamaged>> TURRET_HOWARD_DAMAGED =
            BLOCK_ENTITIES.register("turret_howard_damaged",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretHowardDamaged::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_howard_damaged.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretTauon>> TURRET_TAUON =
            BLOCK_ENTITIES.register("turret_tauon",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretTauon::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_tauon.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretMaxwell>> TURRET_MAXWELL =
            BLOCK_ENTITIES.register("turret_maxwell",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretMaxwell::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_maxwell.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretJeremy>> TURRET_JEREMY =
            BLOCK_ENTITIES.register("turret_jeremy",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretJeremy::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_jeremy.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretRichard>> TURRET_RICHARD =
            BLOCK_ENTITIES.register("turret_richard",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretRichard::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_richard.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretFritz>> TURRET_FRITZ =
            BLOCK_ENTITIES.register("turret_fritz",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretFritz::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_fritz.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretHIMARS>> TURRET_HIMARS =
            BLOCK_ENTITIES.register("turret_himars",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretHIMARS::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_himars.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretArty>> TURRET_ARTY =
            BLOCK_ENTITIES.register("turret_arty",
                    () -> BlockEntities.type(com.hbm.tileentity.turret.TileEntityTurretArty::new,
                            com.hbm.blocks.turret.ModTurretBlocks.turret_arty.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBatterySocket>> BATTERY_SOCKET =
            BLOCK_ENTITIES.register("machine_battery_socket",
                    () -> BlockEntities.type(TileEntityBatterySocket::new, ModBatteryBlocks.machine_battery_socket.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineFENSU>> FENSU =
            BLOCK_ENTITIES.register("machine_fensu",
                    () -> BlockEntities.type(TileEntityMachineFENSU::new, ModBatteryBlocks.machine_fensu.get()));
    public static final RegistryObject<BlockEntityType<TileEntityBatteryREDD>> BATTERY_REDD =
            BLOCK_ENTITIES.register("machine_battery_redd",
                    () -> BlockEntities.type(TileEntityBatteryREDD::new,
                            ModBatteryBlocks.machine_battery_redd.get()));

    // Storage crates — one TileEntityCrate class, three types (each carries its CrateType layout). The
    // BE resolves its own type via CrateType.blockEntityType(), wired below (avoids a self-reference).
    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_IRON =
            BLOCK_ENTITIES.register("crate_iron", () -> BlockEntities.type(
                    (pos, state) -> new TileEntityCrate(CrateType.IRON, pos, state), ModStorageBlocks.crate_iron.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_STEEL =
            BLOCK_ENTITIES.register("crate_steel", () -> BlockEntities.type(
                    (pos, state) -> new TileEntityCrate(CrateType.STEEL, pos, state), ModStorageBlocks.crate_steel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_DESH =
            BLOCK_ENTITIES.register("crate_desh", () -> BlockEntities.type(
                    (pos, state) -> new TileEntityCrate(CrateType.DESH, pos, state), ModStorageBlocks.crate_desh.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.storage.TileEntityMassStorage>> MASS_STORAGE_WOOD =
            massStorage(com.hbm.tileentity.machine.storage.MassStorageType.WOOD, ModStorageBlocks.mass_storage_wood);
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.storage.TileEntityMassStorage>> MASS_STORAGE_IRON =
            massStorage(com.hbm.tileentity.machine.storage.MassStorageType.IRON, ModStorageBlocks.mass_storage_iron);
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.storage.TileEntityMassStorage>> MASS_STORAGE_DESH =
            massStorage(com.hbm.tileentity.machine.storage.MassStorageType.DESH, ModStorageBlocks.mass_storage_desh);
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.storage.TileEntityMassStorage>> MASS_STORAGE =
            massStorage(com.hbm.tileentity.machine.storage.MassStorageType.MASS, ModStorageBlocks.mass_storage);
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.storage.TileEntityCrateTemplate>> CRATE_TEMPLATE =
            BLOCK_ENTITIES.register("crate_template", () -> BlockEntities.type(
                    com.hbm.tileentity.machine.storage.TileEntityCrateTemplate::new, ModStorageBlocks.crate_template.get()));

    static {
        CrateType.IRON.setTypeSupplier(CRATE_IRON::get);
        CrateType.STEEL.setTypeSupplier(CRATE_STEEL::get);
        CrateType.DESH.setTypeSupplier(CRATE_DESH::get);
        com.hbm.tileentity.machine.storage.MassStorageType.WOOD.setBlockEntityTypeSupplier(MASS_STORAGE_WOOD::get);
        com.hbm.tileentity.machine.storage.MassStorageType.IRON.setBlockEntityTypeSupplier(MASS_STORAGE_IRON::get);
        com.hbm.tileentity.machine.storage.MassStorageType.DESH.setBlockEntityTypeSupplier(MASS_STORAGE_DESH::get);
        com.hbm.tileentity.machine.storage.MassStorageType.MASS.setBlockEntityTypeSupplier(MASS_STORAGE::get);
        com.hbm.tileentity.machine.storage.TemplateCrateLayout.setBlockEntityTypeSupplier(CRATE_TEMPLATE::get);
    }

    // --- scaffold machines (placeable + persistent; logic deferred, see IMPLEMENTATION.md §18) ---

    public static final RegistryObject<BlockEntityType<TileEntityMachineElectricFurnace>> MACHINE_ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("machine_electric_furnace",
                    () -> BlockEntities.type(TileEntityMachineElectricFurnace::new, ModMachineBlocks.machine_electric_furnace_off.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineArcWelder>> MACHINE_ARC_WELDER =
            BLOCK_ENTITIES.register("machine_arc_welder", () -> BlockEntities.type(
                    com.hbm.tileentity.machine.TileEntityMachineArcWelder::new, ModMachineBlocks.machine_arc_welder.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineIntake>> MACHINE_INTAKE =
            BLOCK_ENTITIES.register("machine_intake", () -> BlockEntities.type(
                    ModBlockEntities::newIntake, ModMachineBlocks.machine_intake.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.blocks.machine.MachineFan.TileEntityFan>> MACHINE_FAN =
            BLOCK_ENTITIES.register("fan", () -> BlockEntities.type(
                    ModBlockEntities::newFan, ModMachineBlocks.fan.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFurnaceBrick>> FURNACE_BRICK =
            BLOCK_ENTITIES.register("machine_furnace_brick_off",
                    () -> BlockEntities.type(TileEntityFurnaceBrick::new, ModMachineBlocks.machine_furnace_brick_off.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFurnaceIron>> FURNACE_IRON =
            BLOCK_ENTITIES.register("furnace_iron",
                    () -> BlockEntities.type(TileEntityFurnaceIron::new, ModMachineBlocks.furnace_iron.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFurnaceSteel>> FURNACE_STEEL =
            BLOCK_ENTITIES.register("furnace_steel",
                    () -> BlockEntities.type(TileEntityFurnaceSteel::new, ModMachineBlocks.furnace_steel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineShredder>> MACHINE_SHREDDER =
            BLOCK_ENTITIES.register("machine_shredder",
                    () -> BlockEntities.type(TileEntityMachineShredder::new, ModMachineBlocks.machine_shredder.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineEPress>> MACHINE_EPRESS =
            BLOCK_ENTITIES.register("machine_epress",
                    () -> BlockEntities.type(TileEntityMachineEPress::new, ModMachineBlocks.machine_epress.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePress>> MACHINE_PRESS =
            BLOCK_ENTITIES.register("machine_press",
                    () -> BlockEntities.type(TileEntityMachinePress::new, ModMachineBlocks.machine_press.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineAmmoPress>> MACHINE_AMMO_PRESS =
            BLOCK_ENTITIES.register("machine_ammo_press",
                    () -> BlockEntities.type(TileEntityMachineAmmoPress::new,
                            ModMachineBlocks.machine_ammo_press.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySawmill>> MACHINE_SAWMILL =
            BLOCK_ENTITIES.register("machine_sawmill",
                    () -> BlockEntities.type(TileEntitySawmill::new, ModMachineBlocks.machine_sawmill.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineAssemblyMachine>> MACHINE_ASSEMBLY_MACHINE =
            BLOCK_ENTITIES.register("machine_assembly_machine",
                    () -> BlockEntities.type(TileEntityMachineAssemblyMachine::new, ModMachineBlocks.machine_assembly_machine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePrecAss>> MACHINE_PREC_ASS =
            BLOCK_ENTITIES.register("machine_precass",
                    () -> BlockEntities.type(TileEntityMachinePrecAss::new, ModMachineBlocks.machine_precass.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPASource>> PA_SOURCE =
            BLOCK_ENTITIES.register("pa_source",
                    () -> BlockEntities.type(TileEntityPASource::new, ModMachineBlocks.pa_source.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPABeamline>> PA_BEAMLINE =
            BLOCK_ENTITIES.register("pa_beamline",
                    () -> BlockEntities.type(TileEntityPABeamline::new, ModMachineBlocks.pa_beamline.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPARFC>> PA_RFC =
            BLOCK_ENTITIES.register("pa_rfc",
                    () -> BlockEntities.type(TileEntityPARFC::new, ModMachineBlocks.pa_rfc.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPAQuadrupole>> PA_QUADRUPOLE =
            BLOCK_ENTITIES.register("pa_quadrupole",
                    () -> BlockEntities.type(TileEntityPAQuadrupole::new, ModMachineBlocks.pa_quadrupole.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPADipole>> PA_DIPOLE =
            BLOCK_ENTITIES.register("pa_dipole",
                    () -> BlockEntities.type(TileEntityPADipole::new, ModMachineBlocks.pa_dipole.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPADetector>> PA_DETECTOR =
            BLOCK_ENTITIES.register("pa_detector",
                    () -> BlockEntities.type(TileEntityPADetector::new, ModMachineBlocks.pa_detector.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineChemicalPlant>> MACHINE_CHEMICAL_PLANT =
            BLOCK_ENTITIES.register("machine_chemical_plant",
                    () -> BlockEntities.type(TileEntityMachineChemicalPlant::new, ModMachineBlocks.machine_chemical_plant.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMixer>> MACHINE_MIXER =
            BLOCK_ENTITIES.register("machine_mixer",
                    () -> BlockEntities.type(TileEntityMachineMixer::new, ModMachineBlocks.machine_mixer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineExposureChamber>> MACHINE_EXPOSURE_CHAMBER =
            BLOCK_ENTITIES.register("machine_exposure_chamber",
                    () -> BlockEntities.type(TileEntityMachineExposureChamber::new,
                            ModMachineBlocks.machine_exposure_chamber.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineBlastFurnace>> MACHINE_BLAST_FURNACE =
            BLOCK_ENTITIES.register("machine_blast_furnace",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineBlastFurnace::new,
                            ModMachineBlocks.machine_blast_furnace.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachinePUREX>> MACHINE_PUREX =
            BLOCK_ENTITIES.register("machine_purex",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachinePUREX::new, ModMachineBlocks.machine_purex.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineSolderingStation>> MACHINE_SOLDERING_STATION =
            BLOCK_ENTITIES.register("machine_soldering_station",
                    () -> BlockEntities.type(TileEntityMachineSolderingStation::new, ModMachineBlocks.machine_soldering_station.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCrystallizer>> MACHINE_CRYSTALLIZER =
            BLOCK_ENTITIES.register("machine_crystallizer",
                    () -> BlockEntities.type(TileEntityMachineCrystallizer::new, ModMachineBlocks.machine_crystallizer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCentrifuge>> MACHINE_CENTRIFUGE =
            BLOCK_ENTITIES.register("machine_centrifuge",
                    () -> BlockEntities.type(TileEntityMachineCentrifuge::new, ModMachineBlocks.machine_centrifuge.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCyclotron>> MACHINE_CYCLOTRON =
            BLOCK_ENTITIES.register("machine_cyclotron",
                    () -> BlockEntities.type(TileEntityMachineCyclotron::new, ModMachineBlocks.machine_cyclotron.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineGasCent>> MACHINE_GAS_CENT =
            BLOCK_ENTITIES.register("machine_gascent",
                    () -> BlockEntities.type(TileEntityMachineGasCent::new, ModMachineBlocks.machine_gascent.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineElectrolyser>> MACHINE_ELECTROLYSER =
            BLOCK_ENTITIES.register("machine_electrolyser",
                    () -> BlockEntities.type(TileEntityMachineElectrolyser::new, ModMachineBlocks.machine_electrolyser.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntitySILEX>> MACHINE_SILEX =
            BLOCK_ENTITIES.register("machine_silex",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntitySILEX::new, ModMachineBlocks.machine_silex.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFEL>> MACHINE_FEL =
            BLOCK_ENTITIES.register("machine_fel",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityFEL::new, ModMachineBlocks.machine_fel.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineOilWell>> MACHINE_OIL_WELL =
            BLOCK_ENTITIES.register("machine_well",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineOilWell::new, ModMachineBlocks.machine_well.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack>> MACHINE_PUMPJACK =
            BLOCK_ENTITIES.register("machine_pumpjack",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack::new, ModMachineBlocks.machine_pumpjack.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineDiesel>> MACHINE_DIESEL =
            BLOCK_ENTITIES.register("machine_diesel",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineDiesel::new, ModMachineBlocks.machine_diesel.get()));

    // --- advanced oil wave ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower>> MACHINE_FRACKING_TOWER =
            BLOCK_ENTITIES.register("machine_fracking_tower",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower::new, ModMachineBlocks.machine_fracking_tower.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill>> MACHINE_VACUUM_DISTILL =
            BLOCK_ENTITIES.register("machine_vacuum_distill",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill::new, ModMachineBlocks.machine_vacuum_distill.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer>> MACHINE_CATALYTIC_REFORMER =
            BLOCK_ENTITIES.register("machine_catalytic_reformer",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer::new, ModMachineBlocks.machine_catalytic_reformer.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker>> MACHINE_CATALYTIC_CRACKER =
            BLOCK_ENTITIES.register("machine_catalytic_cracker",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker::new, ModMachineBlocks.machine_catalytic_cracker.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater>> MACHINE_HYDROTREATER =
            BLOCK_ENTITIES.register("machine_hydrotreater",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater::new, ModMachineBlocks.machine_hydrotreater.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCoker>> MACHINE_COKER =
            BLOCK_ENTITIES.register("machine_coker",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineCoker::new, ModMachineBlocks.machine_coker.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFurnaceCombination>> FURNACE_COMBINATION =
            BLOCK_ENTITIES.register("machine_furnace_combination",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityFurnaceCombination::new,
                            ModMachineBlocks.machine_furnace_combination.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven>> MACHINE_PYROOVEN =
            BLOCK_ENTITIES.register("machine_pyrooven",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven::new, ModMachineBlocks.machine_pyrooven.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineCombustionEngine>> MACHINE_COMBUSTION_ENGINE =
            BLOCK_ENTITIES.register("machine_combustion_engine",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineCombustionEngine::new, ModMachineBlocks.machine_combustion_engine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityRefueler>> REFUELER =
            BLOCK_ENTITIES.register("refueler",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityRefueler::new, ModMachineBlocks.refueler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineCompressor>> MACHINE_COMPRESSOR =
            BLOCK_ENTITIES.register("machine_compressor",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineCompressor::new, ModMachineBlocks.machine_compressor.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare>> MACHINE_GAS_FLARE =
            BLOCK_ENTITIES.register("machine_flare",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare::new, ModMachineBlocks.machine_flare.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower>> MACHINE_FRACTION_TOWER =
            BLOCK_ENTITIES.register("machine_fraction_tower",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower::new, ModMachineBlocks.machine_fraction_tower.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntitySpacer>> FRACTION_SPACER =
            BLOCK_ENTITIES.register("fraction_spacer",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntitySpacer::new, ModMachineBlocks.fraction_spacer.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor>> MACHINE_LIQUEFACTOR =
            BLOCK_ENTITIES.register("machine_liquefactor",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor::new, ModMachineBlocks.machine_liquefactor.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier>> MACHINE_SOLIDIFIER =
            BLOCK_ENTITIES.register("machine_solidifier",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier::new, ModMachineBlocks.machine_solidifier.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineRefinery>> MACHINE_REFINERY =
            BLOCK_ENTITIES.register("machine_refinery",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntityMachineRefinery::new, ModMachineBlocks.machine_refinery.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeatBoiler>> HEAT_BOILER =
            BLOCK_ENTITIES.register("heat_boiler",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityHeatBoiler::new, ModMachineBlocks.machine_boiler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial>> MACHINE_INDUSTRIAL_BOILER =
            BLOCK_ENTITIES.register("machine_industrial_boiler",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial::new, ModMachineBlocks.machine_industrial_boiler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterOilburner>> HEATER_OILBURNER =
            BLOCK_ENTITIES.register("heater_oilburner",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityHeaterOilburner::new, ModMachineBlocks.heater_oilburner.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineWoodBurner>> MACHINE_WOOD_BURNER =
            BLOCK_ENTITIES.register("machine_wood_burner",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineWoodBurner::new, ModMachineBlocks.machine_wood_burner.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineMiningLaser>> MACHINE_MINING_LASER =
            BLOCK_ENTITIES.register("machine_mining_laser",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityMachineMiningLaser::new, ModMachineBlocks.machine_mining_laser.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCrucible>> MACHINE_CRUCIBLE =
            BLOCK_ENTITIES.register("machine_crucible",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityCrucible::new, ModMachineBlocks.machine_crucible.get()));

    // --- foundry transport / casting ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryChannel>> FOUNDRY_CHANNEL =
            BLOCK_ENTITIES.register("foundry_channel",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityFoundryChannel::new, ModMachineBlocks.foundry_channel.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryMold>> FOUNDRY_MOLD =
            BLOCK_ENTITIES.register("foundry_mold",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityFoundryMold::new, ModMachineBlocks.foundry_mold.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryBasin>> FOUNDRY_BASIN =
            BLOCK_ENTITIES.register("foundry_basin",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityFoundryBasin::new, ModMachineBlocks.foundry_basin.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryOutlet>> FOUNDRY_OUTLET =
            BLOCK_ENTITIES.register("foundry_outlet",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityFoundryOutlet::new, ModMachineBlocks.foundry_outlet.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundrySlagtap>> FOUNDRY_SLAGTAP =
            BLOCK_ENTITIES.register("foundry_slagtap",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityFoundrySlagtap::new, ModMachineBlocks.foundry_slagtap.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag>> SLAG =
            BLOCK_ENTITIES.register("slag",
                    () -> BlockEntities.type(com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag::new, ModMachineBlocks.slag.get()));

    // --- foundry heat sources ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterFirebox>> HEATER_FIREBOX =
            BLOCK_ENTITIES.register("heater_firebox",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityHeaterFirebox::new, ModMachineBlocks.heater_firebox.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterOven>> HEATER_OVEN =
            BLOCK_ENTITIES.register("heater_oven",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityHeaterOven::new, ModMachineBlocks.heater_oven.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterElectric>> HEATER_ELECTRIC =
            BLOCK_ENTITIES.register("heater_electric",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityHeaterElectric::new, ModMachineBlocks.heater_electric.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterHeatex>> HEATER_HEATEX =
            BLOCK_ENTITIES.register("heater_heatex",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityHeaterHeatex::new, ModMachineBlocks.heater_heatex.get()));

    public static final RegistryObject<BlockEntityType<TileEntityLaunchPad>> LAUNCH_PAD =
            BLOCK_ENTITIES.register("launch_pad",
                    () -> BlockEntities.type(TileEntityLaunchPad::new, ModMachineBlocks.launch_pad.get()));

    // --- bombs ---

    public static final RegistryObject<BlockEntityType<TileEntityNukeMan>> NUKE_MAN =
            BLOCK_ENTITIES.register("nuke_man",
                    () -> BlockEntities.type(TileEntityNukeMan::new, ModBombBlocks.nuke_man.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeN2>> NUKE_N2 =
            BLOCK_ENTITIES.register("nuke_n2",
                    () -> BlockEntities.type(TileEntityNukeN2::new, ModBombBlocks.nuke_n2.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeBoy>> NUKE_BOY =
            BLOCK_ENTITIES.register("nuke_boy",
                    () -> BlockEntities.type(TileEntityNukeBoy::new, ModBombBlocks.nuke_boy.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeGadget>> NUKE_GADGET =
            BLOCK_ENTITIES.register("nuke_gadget",
                    () -> BlockEntities.type(TileEntityNukeGadget::new, ModBombBlocks.nuke_gadget.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeTsar>> NUKE_TSAR =
            BLOCK_ENTITIES.register("nuke_tsar",
                    () -> BlockEntities.type(TileEntityNukeTsar::new, ModBombBlocks.nuke_tsar.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.bomb.TileEntityNukeMike>> NUKE_MIKE =
            BLOCK_ENTITIES.register("nuke_mike",
                    () -> BlockEntities.type(com.hbm.tileentity.bomb.TileEntityNukeMike::new,
                            ModBombBlocks.nuke_mike.get()));

    // --- the 12 tiered anvils (OBJ-rendered; the BE exists only for the renderer) ---

    public static final RegistryObject<BlockEntityType<TileEntityAnvil>> ANVIL =
            BLOCK_ENTITIES.register("anvil",
                    () -> BlockEntities.type(TileEntityAnvil::new, com.hbm.blocks.ModAnvilBlocks.ANVILS.stream()
                            .map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)));

    // --- decorative OBJ-rendered blocks (boxcar / boat / satellite dishes) ---

    public static final RegistryObject<BlockEntityType<TileEntityDeco>> DECO =
            BLOCK_ENTITIES.register("deco",
                    () -> BlockEntities.type(TileEntityDeco::new, java.util.stream.Stream.concat(
                            com.hbm.blocks.ModDecoBlocks.OBJ_BLOCKS.stream(), com.hbm.blocks.ModSteelBlocks.OBJ_BLOCKS.stream())
                            .map(RegistryObject::get).toArray(net.minecraft.world.level.block.Block[]::new)));

    private static RegistryObject<BlockEntityType<com.hbm.tileentity.machine.storage.TileEntityMassStorage>> massStorage(
            com.hbm.tileentity.machine.storage.MassStorageType type,
            RegistryObject<? extends net.minecraft.world.level.block.Block> block) {
        return BLOCK_ENTITIES.register(type.id, () -> BlockEntities.type(
                (pos, state) -> new com.hbm.tileentity.machine.storage.TileEntityMassStorage(type, pos, state), block.get()));
    }

    private static com.hbm.tileentity.machine.TileEntityMachineIntake newIntake(
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new com.hbm.tileentity.machine.TileEntityMachineIntake(MACHINE_INTAKE.get(), pos, state);
    }

    private static com.hbm.blocks.machine.MachineFan.TileEntityFan newFan(
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new com.hbm.blocks.machine.MachineFan.TileEntityFan(MACHINE_FAN.get(), pos, state);
    }

    public static void init() {}
}
