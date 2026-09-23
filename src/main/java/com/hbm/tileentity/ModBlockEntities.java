package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.machine.TileEntityFoundryCastingBase;
import com.hbm.tileentity.machine.oil.TileEntityOilDrillBase;
import com.hbm.tileentity.machine.TileEntityMachineCompressorBase;
import com.hbm.tileentity.machine.TileEntityFoundryOutlet;
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
                    () -> BlockEntities.serverTicking(TileEntityMachineBattery::new, TileEntityMachineBattery::serverTick, ModMachineBlocks.machine_battery.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineRadGen>> MACHINE_RAD_GEN =
            BLOCK_ENTITIES.register("machine_radgen",
                    () -> BlockEntities.serverTicking(TileEntityMachineRadGen::new, TileEntityMachineRadGen::serverTick, ModMachineBlocks.machine_radgen.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMiniRTG>> MACHINE_MINI_RTG =
            BLOCK_ENTITIES.register("machine_minirtg",
                    () -> BlockEntities.serverTicking(TileEntityMachineMiniRTG::new, TileEntityMachineMiniRTG::serverTick, ModMachineBlocks.machine_minirtg.get(), ModMachineBlocks.rtg_polonium.get()));

    public static final RegistryObject<BlockEntityType<TileEntityRadSensor>> RAD_SENSOR =
            BLOCK_ENTITIES.register("radsensor",
                    () -> BlockEntities.serverTicking(TileEntityRadSensor::new, TileEntityRadSensor::serverTick, ModMachineBlocks.radsensor.get()));


    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCargoElevator>>
            CARGO_ELEVATOR = BLOCK_ENTITIES.register("cargo_elevator",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityCargoElevator::new,
                            com.hbm.tileentity.machine.TileEntityCargoElevator::clientTick,
                            com.hbm.tileentity.machine.TileEntityCargoElevator::serverTick,
                            ModMachineBlocks.cargo_elevator.get()));


    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityGeiger>> GEIGER =
            BLOCK_ENTITIES.register("geiger",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityGeiger::new,
                            com.hbm.tileentity.machine.TileEntityGeiger::serverTick,
                            ModMachineBlocks.geiger.get()));



    public static final RegistryObject<BlockEntityType<TileEntityCableBaseNT>> RED_CABLE =
            BLOCK_ENTITIES.register("red_cable",
                    () -> BlockEntities.type(TileEntityCableBaseNT::new, ModMachineBlocks.red_cable.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeGenerator>> CREATIVE_GENERATOR =
            BLOCK_ENTITIES.register("creative_generator",
                    () -> BlockEntities.serverTicking(TileEntityCreativeGenerator::new, TileEntityCreativeGenerator::serverTick, ModMachineBlocks.creative_generator.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbine>> MACHINE_TURBINE =
            BLOCK_ENTITIES.register("machine_turbine",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityMachineTurbine::new, com.hbm.tileentity.machine.TileEntityMachineTurbine::serverTick, ModMachineBlocks.machine_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineLargeTurbine>> MACHINE_LARGE_TURBINE =
            BLOCK_ENTITIES.register("machine_large_turbine",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineLargeTurbine::new, com.hbm.tileentity.machine.TileEntityMachineLargeTurbine::clientTick, com.hbm.tileentity.machine.TileEntityMachineLargeTurbine::serverTick, ModMachineBlocks.machine_large_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbineGas>> MACHINE_TURBINE_GAS =
            BLOCK_ENTITIES.register("machine_turbine_gas",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineTurbineGas::new, com.hbm.tileentity.machine.TileEntityMachineTurbineGas::clientTick, com.hbm.tileentity.machine.TileEntityMachineTurbineGas::serverTick, ModMachineBlocks.machine_turbine_gas.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineTurbofan>> MACHINE_TURBOFAN =
            BLOCK_ENTITIES.register("machine_turbofan",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineTurbofan::new, com.hbm.tileentity.machine.TileEntityMachineTurbofan::clientTick, com.hbm.tileentity.machine.TileEntityMachineTurbofan::serverTick, ModMachineBlocks.machine_turbofan.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineSteamEngine>> MACHINE_STEAM_ENGINE =
            BLOCK_ENTITIES.register("machine_steam_engine",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineSteamEngine::new, com.hbm.tileentity.machine.TileEntityMachineSteamEngine::clientTick, com.hbm.tileentity.machine.TileEntityMachineSteamEngine::serverTick, ModMachineBlocks.machine_steam_engine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpElectric>> MACHINE_PUMP_ELECTRIC =
            BLOCK_ENTITIES.register("machine_pump_electric",
                    () -> BlockEntities.ticking(TileEntityMachinePumpElectric::new, TileEntityMachinePumpElectric::clientTick, TileEntityMachinePumpElectric::serverTick, ModMachineBlocks.machine_pump_electric.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpSteam>> MACHINE_PUMP_STEAM =
            BLOCK_ENTITIES.register("machine_pump_steam",
                    () -> BlockEntities.ticking(TileEntityMachinePumpSteam::new, TileEntityMachinePumpSteam::clientTick, TileEntityMachinePumpSteam::serverTick, ModMachineBlocks.machine_pump_steam.get()));

    public static final RegistryObject<BlockEntityType<TileEntityStirling>> MACHINE_STIRLING =
            BLOCK_ENTITIES.register("machine_stirling",
                    () -> BlockEntities.ticking(TileEntityStirling::new, TileEntityStirling::clientTick, TileEntityStirling::serverTick, ModMachineBlocks.machine_stirling.get(), ModMachineBlocks.machine_stirling_steel.get(), ModMachineBlocks.machine_stirling_creative.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine>> MACHINE_INDUSTRIAL_TURBINE =
            BLOCK_ENTITIES.register("machine_industrial_turbine",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine::new, com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine::clientTick, com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine::serverTick, ModMachineBlocks.machine_industrial_turbine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2>> PILE_BLOCK =
            BLOCK_ENTITIES.register("pile_block",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2::new,
                            com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2::serverTick,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_block.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileCore>> PILE_CORE =
            BLOCK_ENTITIES.register("pile_core",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.pile.TileEntityPileCore::new,
                            com.hbm.tileentity.machine.pile.TileEntityPileCore::serverTick,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_block.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileLoader>> PILE_LOADER =
            BLOCK_ENTITIES.register("pile_loader",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.pile.TileEntityPileLoader::new,
                            com.hbm.tileentity.machine.pile.TileEntityPileLoader::serverTick,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_device.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileVent>> PILE_VENT =
            BLOCK_ENTITIES.register("pile_vent",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.pile.TileEntityPileVent::new,
                            com.hbm.tileentity.machine.pile.TileEntityPileVent::serverTick,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_device.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.pile.TileEntityPileControl>> PILE_CONTROL =
            BLOCK_ENTITIES.register("pile_control",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.pile.TileEntityPileControl::new,
                            com.hbm.tileentity.machine.pile.TileEntityPileControl::serverTick,
                            com.hbm.blocks.machine.pile.ModPileBlocks.pile_device.get()));


    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCondenser>> MACHINE_CONDENSER =
            BLOCK_ENTITIES.register("machine_condenser",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityCondenser::new, com.hbm.tileentity.machine.TileEntityCondenser::serverTick, ModMachineBlocks.machine_condenser.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCondenserPowered>> MACHINE_CONDENSER_POWERED =
            BLOCK_ENTITIES.register("machine_condenser_powered",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityCondenserPowered::new, com.hbm.tileentity.machine.TileEntityCondenserPowered::clientTick, com.hbm.tileentity.machine.TileEntityCondenserPowered::serverTick, ModMachineBlocks.machine_condenser_powered.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionCoupler>> FUSION_COUPLER =
            BLOCK_ENTITIES.register("fusion_coupler",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.fusion.TileEntityFusionCoupler::new, com.hbm.tileentity.machine.fusion.TileEntityFusionCoupler::serverTick, ModMachineBlocks.fusion_coupler.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionCollector>> FUSION_COLLECTOR =
            BLOCK_ENTITIES.register("fusion_collector",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.fusion.TileEntityFusionCollector::new, com.hbm.tileentity.machine.fusion.TileEntityFusionCollector::serverTick, ModMachineBlocks.fusion_collector.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron>> FUSION_KLYSTRON =
            BLOCK_ENTITIES.register("fusion_klystron",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron::new, com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron::clientTick, com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron::serverTick, ModMachineBlocks.fusion_klystron.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative>> FUSION_KLYSTRON_CREATIVE =
            BLOCK_ENTITIES.register("fusion_klystron_creative",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative::new, com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative::clientTick, com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative::serverTick, ModMachineBlocks.fusion_klystron_creative.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler>> FUSION_BOILER =
            BLOCK_ENTITIES.register("fusion_boiler",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler::new, com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler::serverTick, ModMachineBlocks.fusion_boiler.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder>> FUSION_BREEDER =
            BLOCK_ENTITIES.register("fusion_breeder",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder::new, com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder::serverTick, ModMachineBlocks.fusion_breeder.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT>> FUSION_MHDT =
            BLOCK_ENTITIES.register("fusion_mhdt",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT::new, com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT::clientTick, com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT::serverTick, ModMachineBlocks.fusion_mhdt.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionTorus>> FUSION_TORUS =
            BLOCK_ENTITIES.register("fusion_torus",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.fusion.TileEntityFusionTorus::new, com.hbm.tileentity.machine.fusion.TileEntityFusionTorus::clientTick, com.hbm.tileentity.machine.fusion.TileEntityFusionTorus::serverTick, ModMachineBlocks.fusion_torus.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge>> FUSION_PLASMA_FORGE =
            BLOCK_ENTITIES.register("fusion_plasma_forge",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge::new, com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge::clientTick, com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge::serverTick, ModMachineBlocks.fusion_plasma_forge.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct>> STRUCT_TORUS_CORE =
            BLOCK_ENTITIES.register("struct_torus_core",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct::new, com.hbm.tileentity.machine.struct.TileEntityStructBase::serverTick, ModMachineBlocks.struct_torus_core.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWatz>> WATZ =
            BLOCK_ENTITIES.register("watz",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityWatz::new, com.hbm.tileentity.machine.TileEntityWatz::serverTick, ModMachineBlocks.watz.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWatzPump>> WATZ_PUMP =
            BLOCK_ENTITIES.register("watz_pump",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.TileEntityWatzPump::new, ModMachineBlocks.watz_pump.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.struct.TileEntityWatzStruct>> STRUCT_WATZ_CORE =
            BLOCK_ENTITIES.register("struct_watz_core",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.struct.TileEntityWatzStruct::new, com.hbm.tileentity.machine.struct.TileEntityStructBase::serverTick, ModMachineBlocks.struct_watz_core.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.dfc.TileEntityCore>> DFC_CORE =
            BLOCK_ENTITIES.register("dfc_core",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.dfc.TileEntityCore::new,
                            com.hbm.tileentity.machine.dfc.TileEntityCore::serverTick,
                            com.hbm.blocks.machine.dfc.ModDFCBlocks.dfc_core.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter>> DFC_EMITTER =
            BLOCK_ENTITIES.register("dfc_emitter",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter::new,
                            com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter::serverTick,
                            com.hbm.blocks.machine.dfc.ModDFCBlocks.dfc_emitter.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver>> DFC_RECEIVER =
            BLOCK_ENTITIES.register("dfc_receiver",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver::new,
                            com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver::serverTick,
                            com.hbm.blocks.machine.dfc.ModDFCBlocks.dfc_receiver.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.dfc.TileEntityCoreInjector>> DFC_INJECTOR =
            BLOCK_ENTITIES.register("dfc_injector",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.dfc.TileEntityCoreInjector::new,
                            com.hbm.tileentity.machine.dfc.TileEntityCoreInjector::serverTick,
                            com.hbm.blocks.machine.dfc.ModDFCBlocks.dfc_injector.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer>> DFC_STABILIZER =
            BLOCK_ENTITIES.register("dfc_stabilizer",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer::new,
                            com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer::serverTick,
                            com.hbm.blocks.machine.dfc.ModDFCBlocks.dfc_stabilizer.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher>> SOYUZ_LAUNCHER =
            BLOCK_ENTITIES.register("soyuz_launcher",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher::new,
                            com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher::serverTick,
                            com.hbm.blocks.machine.soyuz.ModSoyuzBlocks.soyuz_launcher.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.soyuz.TileEntitySoyuzStruct>> STRUCT_SOYUZ_CORE =
            BLOCK_ENTITIES.register("struct_soyuz_core",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.soyuz.TileEntitySoyuzStruct::new,
                            com.hbm.tileentity.machine.soyuz.TileEntitySoyuzStruct::serverTick,
                            com.hbm.blocks.machine.soyuz.ModSoyuzBlocks.struct_soyuz_core.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineSatLink>> MACHINE_SATLINK =
            BLOCK_ENTITIES.register("machine_satlink",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityMachineSatLink::new,
                            com.hbm.tileentity.machine.TileEntityMachineSatLink::serverTick,
                            ModMachineBlocks.machine_satlink.get()));


    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityReactorZirnox>> MACHINE_ZIRNOX =
            BLOCK_ENTITIES.register("machine_zirnox",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityReactorZirnox::new, com.hbm.tileentity.machine.TileEntityReactorZirnox::serverTick, ModMachineBlocks.machine_zirnox.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityZirnoxDestroyed>> ZIRNOX_DESTROYED =
            BLOCK_ENTITIES.register("zirnox_destroyed",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityZirnoxDestroyed::new, com.hbm.tileentity.machine.TileEntityZirnoxDestroyed::clientTick, com.hbm.tileentity.machine.TileEntityZirnoxDestroyed::serverTick, ModMachineBlocks.zirnox_destroyed.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityWasteDrum>> MACHINE_WASTE_DRUM =
            BLOCK_ENTITIES.register("machine_waste_drum",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityWasteDrum::new, com.hbm.tileentity.machine.TileEntityWasteDrum::serverTick, ModMachineBlocks.machine_waste_drum.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityStorageDrum>> MACHINE_STORAGE_DRUM =
            BLOCK_ENTITIES.register("machine_storage_drum",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityStorageDrum::new, com.hbm.tileentity.machine.TileEntityStorageDrum::serverTick, ModMachineBlocks.machine_storage_drum.get()));

    // --- Phase C: fluid network demo ---

    public static final RegistryObject<BlockEntityType<TileEntityFluidDuctNT>> FLUID_DUCT =
            BLOCK_ENTITIES.register("fluid_duct_mk2",
                    () -> BlockEntities.type(TileEntityFluidDuctNT::new, ModMachineBlocks.fluid_duct_mk2.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeFluidSource>> CREATIVE_FLUID_SOURCE =
            BLOCK_ENTITIES.register("creative_fluid_source",
                    () -> BlockEntities.serverTicking(TileEntityCreativeFluidSource::new, TileEntityCreativeFluidSource::serverTick, ModMachineBlocks.creative_fluid_source.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineFluidTank>> FLUID_TANK =
            BLOCK_ENTITIES.register("machine_fluidtank",
                    () -> BlockEntities.ticking(TileEntityMachineFluidTank::new, TileEntityMachineFluidTank::clientTick, TileEntityMachineFluidTank::serverTick, ModMachineBlocks.machine_fluidtank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineUF6Tank>> MACHINE_UF6_TANK =
            BLOCK_ENTITIES.register("machine_uf6_tank",
                    () -> BlockEntities.serverTicking(TileEntityMachineUF6Tank::new, TileEntityMachineUF6Tank::serverTick, ModMachineBlocks.machine_uf6_tank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePuF6Tank>> MACHINE_PUF6_TANK =
            BLOCK_ENTITIES.register("machine_puf6_tank",
                    () -> BlockEntities.serverTicking(TileEntityMachinePuF6Tank::new, TileEntityMachinePuF6Tank::serverTick, ModMachineBlocks.machine_puf6_tank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineBigAssTank>> MACHINE_BIG_ASS_TANK =
            BLOCK_ENTITIES.register("machine_bigasstank",
                    () -> BlockEntities.serverTicking(TileEntityMachineBigAssTank::new, TileEntityMachineBigAssTank::serverTick, ModMachineBlocks.machine_bigasstank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineBAT9000>> MACHINE_BAT9000 =
            BLOCK_ENTITIES.register("machine_bat9000",
                    () -> BlockEntities.serverTicking(TileEntityMachineBAT9000::new, TileEntityMachineBAT9000::serverTick, ModMachineBlocks.machine_bat9000.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDeuteriumExtractor>> MACHINE_DEUTERIUM_EXTRACTOR =
            BLOCK_ENTITIES.register("machine_deuterium_extractor",
                    () -> BlockEntities.serverTicking(TileEntityDeuteriumExtractor::new, TileEntityDeuteriumExtractor::serverTick, ModMachineBlocks.machine_deuterium_extractor.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDeuteriumTower>> MACHINE_DEUTERIUM_TOWER =
            BLOCK_ENTITIES.register("machine_deuterium_tower",
                    () -> BlockEntities.serverTicking(TileEntityDeuteriumTower::new, TileEntityDeuteriumTower::serverTick, ModMachineBlocks.machine_deuterium_tower.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFluidValve>> FLUID_VALVE =
            BLOCK_ENTITIES.register("fluid_valve",
                    () -> BlockEntities.type(TileEntityFluidValve::new, ModMachineBlocks.fluid_valve.get()));


    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineDrain>> MACHINE_DRAIN =
            BLOCK_ENTITIES.register("machine_drain",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityMachineDrain::new, com.hbm.tileentity.machine.TileEntityMachineDrain::serverTick, ModMachineBlocks.machine_drain.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretChekhov>> TURRET_CHEKHOV =
            BLOCK_ENTITIES.register("turret_chekhov",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretChekhov::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_chekhov.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretFriendly>> TURRET_FRIENDLY =
            BLOCK_ENTITIES.register("turret_friendly",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretFriendly::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_friendly.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretSentry>> TURRET_SENTRY =
            BLOCK_ENTITIES.register("turret_sentry",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretSentry::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_sentry.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretSentryDamaged>> TURRET_SENTRY_DAMAGED =
            BLOCK_ENTITIES.register("turret_sentry_damaged",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretSentryDamaged::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_sentry_damaged.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretHoward>> TURRET_HOWARD =
            BLOCK_ENTITIES.register("turret_howard",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretHoward::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_howard.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretHowardDamaged>> TURRET_HOWARD_DAMAGED =
            BLOCK_ENTITIES.register("turret_howard_damaged",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretHowardDamaged::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_howard_damaged.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretTauon>> TURRET_TAUON =
            BLOCK_ENTITIES.register("turret_tauon",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretTauon::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_tauon.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretMaxwell>> TURRET_MAXWELL =
            BLOCK_ENTITIES.register("turret_maxwell",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretMaxwell::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_maxwell.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretJeremy>> TURRET_JEREMY =
            BLOCK_ENTITIES.register("turret_jeremy",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretJeremy::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_jeremy.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretRichard>> TURRET_RICHARD =
            BLOCK_ENTITIES.register("turret_richard",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretRichard::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_richard.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretFritz>> TURRET_FRITZ =
            BLOCK_ENTITIES.register("turret_fritz",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretFritz::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_fritz.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretHIMARS>> TURRET_HIMARS =
            BLOCK_ENTITIES.register("turret_himars",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretHIMARS::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_himars.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.turret.TileEntityTurretArty>> TURRET_ARTY =
            BLOCK_ENTITIES.register("turret_arty",
                    () -> BlockEntities.ticking(com.hbm.tileentity.turret.TileEntityTurretArty::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, com.hbm.blocks.turret.ModTurretBlocks.turret_arty.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBatterySocket>> BATTERY_SOCKET =
            BLOCK_ENTITIES.register("machine_battery_socket",
                    () -> BlockEntities.serverTicking(TileEntityBatterySocket::new, TileEntityBatterySocket::serverTick, ModBatteryBlocks.machine_battery_socket.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineFENSU>> FENSU =
            BLOCK_ENTITIES.register("machine_fensu",
                    () -> BlockEntities.ticking(TileEntityMachineFENSU::new, TileEntityMachineFENSU::clientTick, TileEntityMachineFENSU::serverTick, ModBatteryBlocks.machine_fensu.get()));
    public static final RegistryObject<BlockEntityType<TileEntityBatteryREDD>> BATTERY_REDD =
            BLOCK_ENTITIES.register("machine_battery_redd",
                    () -> BlockEntities.ticking(TileEntityBatteryREDD::new, TileEntityBatteryREDD::clientTick, TileEntityBatteryREDD::serverTick, ModBatteryBlocks.machine_battery_redd.get()));

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

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.storage.TileEntityCrateSupply>> CRATE_SUPPLY =
            BLOCK_ENTITIES.register("crate_supply", () -> BlockEntities.type(
                    com.hbm.tileentity.machine.storage.TileEntityCrateSupply::new, ModStorageBlocks.crate_supply.get()));

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
                    () -> BlockEntities.serverTicking(TileEntityMachineElectricFurnace::new, TileEntityMachineElectricFurnace::serverTick, ModMachineBlocks.machine_electric_furnace_off.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineArcWelder>> MACHINE_ARC_WELDER =
            BLOCK_ENTITIES.register("machine_arc_welder", () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityMachineArcWelder::new, com.hbm.tileentity.machine.TileEntityMachineArcWelder::serverTick, ModMachineBlocks.machine_arc_welder.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineIntake>> MACHINE_INTAKE =
            BLOCK_ENTITIES.register("machine_intake", () -> BlockEntities.ticking(
                    ModBlockEntities::newIntake,
                    com.hbm.tileentity.machine.TileEntityMachineIntake::clientTick,
                    com.hbm.tileentity.machine.TileEntityMachineIntake::serverTick,
                    ModMachineBlocks.machine_intake.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.blocks.machine.MachineFan.TileEntityFan>> MACHINE_FAN =
            BLOCK_ENTITIES.register("fan", () -> BlockEntities.ticking(
                    ModBlockEntities::newFan,
                    com.hbm.blocks.machine.MachineFan.TileEntityFan::tick,
                    com.hbm.blocks.machine.MachineFan.TileEntityFan::tick,
                    ModMachineBlocks.fan.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFurnaceBrick>> FURNACE_BRICK =
            BLOCK_ENTITIES.register("machine_furnace_brick_off",
                    () -> BlockEntities.serverTicking(TileEntityFurnaceBrick::new, TileEntityFurnaceBrick::serverTick, ModMachineBlocks.machine_furnace_brick_off.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFurnaceIron>> FURNACE_IRON =
            BLOCK_ENTITIES.register("furnace_iron",
                    () -> BlockEntities.ticking(TileEntityFurnaceIron::new, TileEntityFurnaceIron::clientTick, TileEntityFurnaceIron::serverTick, ModMachineBlocks.furnace_iron.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFurnaceSteel>> FURNACE_STEEL =
            BLOCK_ENTITIES.register("furnace_steel",
                    () -> BlockEntities.ticking(TileEntityFurnaceSteel::new, TileEntityFurnaceSteel::clientTick, TileEntityFurnaceSteel::serverTick, ModMachineBlocks.furnace_steel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineShredder>> MACHINE_SHREDDER =
            BLOCK_ENTITIES.register("machine_shredder",
                    () -> BlockEntities.serverTicking(TileEntityMachineShredder::new, TileEntityMachineShredder::serverTick, ModMachineBlocks.machine_shredder.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineEPress>> MACHINE_EPRESS =
            BLOCK_ENTITIES.register("machine_epress",
                    () -> BlockEntities.ticking(TileEntityMachineEPress::new, TileEntityMachineEPress::clientTick, TileEntityMachineEPress::serverTick, ModMachineBlocks.machine_epress.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePress>> MACHINE_PRESS =
            BLOCK_ENTITIES.register("machine_press",
                    () -> BlockEntities.ticking(TileEntityMachinePress::new, TileEntityMachinePress::clientTick, TileEntityMachinePress::serverTick, ModMachineBlocks.machine_press.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineAmmoPress>> MACHINE_AMMO_PRESS =
            BLOCK_ENTITIES.register("machine_ammo_press",
                    () -> BlockEntities.ticking(TileEntityMachineAmmoPress::new, TileEntityMachineAmmoPress::clientTick, TileEntityMachineAmmoPress::serverTick, ModMachineBlocks.machine_ammo_press.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySawmill>> MACHINE_SAWMILL =
            BLOCK_ENTITIES.register("machine_sawmill",
                    () -> BlockEntities.ticking(TileEntitySawmill::new, TileEntitySawmill::clientTick, TileEntitySawmill::serverTick, ModMachineBlocks.machine_sawmill.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineAssemblyMachine>> MACHINE_ASSEMBLY_MACHINE =
            BLOCK_ENTITIES.register("machine_assembly_machine",
                    () -> BlockEntities.ticking(TileEntityMachineAssemblyMachine::new, TileEntityMachineAssemblyMachine::clientTick, TileEntityMachineAssemblyMachine::serverTick, ModMachineBlocks.machine_assembly_machine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePrecAss>> MACHINE_PREC_ASS =
            BLOCK_ENTITIES.register("machine_precass",
                    () -> BlockEntities.ticking(TileEntityMachinePrecAss::new, TileEntityMachinePrecAss::clientTick, TileEntityMachinePrecAss::serverTick, ModMachineBlocks.machine_precass.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPASource>> PA_SOURCE =
            BLOCK_ENTITIES.register("pa_source",
                    () -> BlockEntities.serverTicking(TileEntityPASource::new, TileEntityPASource::serverTick, ModMachineBlocks.pa_source.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPABeamline>> PA_BEAMLINE =
            BLOCK_ENTITIES.register("pa_beamline",
                    () -> BlockEntities.type(TileEntityPABeamline::new, ModMachineBlocks.pa_beamline.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPARFC>> PA_RFC =
            BLOCK_ENTITIES.register("pa_rfc",
                    () -> BlockEntities.serverTicking(TileEntityPARFC::new, TileEntityPARFC::serverTick, ModMachineBlocks.pa_rfc.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPAQuadrupole>> PA_QUADRUPOLE =
            BLOCK_ENTITIES.register("pa_quadrupole",
                    () -> BlockEntities.serverTicking(TileEntityPAQuadrupole::new, TileEntityPAQuadrupole::serverTick, ModMachineBlocks.pa_quadrupole.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPADipole>> PA_DIPOLE =
            BLOCK_ENTITIES.register("pa_dipole",
                    () -> BlockEntities.serverTicking(TileEntityPADipole::new, TileEntityPADipole::serverTick, ModMachineBlocks.pa_dipole.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPADetector>> PA_DETECTOR =
            BLOCK_ENTITIES.register("pa_detector",
                    () -> BlockEntities.serverTicking(TileEntityPADetector::new, TileEntityPADetector::serverTick, ModMachineBlocks.pa_detector.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineChemicalPlant>> MACHINE_CHEMICAL_PLANT =
            BLOCK_ENTITIES.register("machine_chemical_plant",
                    () -> BlockEntities.ticking(TileEntityMachineChemicalPlant::new, TileEntityMachineChemicalPlant::clientTick, TileEntityMachineChemicalPlant::serverTick, ModMachineBlocks.machine_chemical_plant.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMixer>> MACHINE_MIXER =
            BLOCK_ENTITIES.register("machine_mixer",
                    () -> BlockEntities.ticking(TileEntityMachineMixer::new, TileEntityMachineMixer::clientTick, TileEntityMachineMixer::serverTick, ModMachineBlocks.machine_mixer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineExposureChamber>> MACHINE_EXPOSURE_CHAMBER =
            BLOCK_ENTITIES.register("machine_exposure_chamber",
                    () -> BlockEntities.ticking(TileEntityMachineExposureChamber::new, TileEntityMachineExposureChamber::clientTick, TileEntityMachineExposureChamber::serverTick, ModMachineBlocks.machine_exposure_chamber.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineBlastFurnace>> MACHINE_BLAST_FURNACE =
            BLOCK_ENTITIES.register("machine_blast_furnace",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineBlastFurnace::new, com.hbm.tileentity.machine.TileEntityMachineBlastFurnace::clientTick, com.hbm.tileentity.machine.TileEntityMachineBlastFurnace::serverTick, ModMachineBlocks.machine_blast_furnace.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachinePUREX>> MACHINE_PUREX =
            BLOCK_ENTITIES.register("machine_purex",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachinePUREX::new, com.hbm.tileentity.machine.TileEntityMachinePUREX::clientTick, com.hbm.tileentity.machine.TileEntityMachinePUREX::serverTick, ModMachineBlocks.machine_purex.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineSolderingStation>> MACHINE_SOLDERING_STATION =
            BLOCK_ENTITIES.register("machine_soldering_station",
                    () -> BlockEntities.serverTicking(TileEntityMachineSolderingStation::new, TileEntityMachineSolderingStation::serverTick, ModMachineBlocks.machine_soldering_station.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCrystallizer>> MACHINE_CRYSTALLIZER =
            BLOCK_ENTITIES.register("machine_crystallizer",
                    () -> BlockEntities.ticking(TileEntityMachineCrystallizer::new, TileEntityMachineCrystallizer::clientTick, TileEntityMachineCrystallizer::serverTick, ModMachineBlocks.machine_crystallizer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCentrifuge>> MACHINE_CENTRIFUGE =
            BLOCK_ENTITIES.register("machine_centrifuge",
                    () -> BlockEntities.ticking(TileEntityMachineCentrifuge::new, TileEntityMachineCentrifuge::clientTick, TileEntityMachineCentrifuge::serverTick, ModMachineBlocks.machine_centrifuge.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCyclotron>> MACHINE_CYCLOTRON =
            BLOCK_ENTITIES.register("machine_cyclotron",
                    () -> BlockEntities.ticking(TileEntityMachineCyclotron::new, TileEntityMachineCyclotron::clientTick, TileEntityMachineCyclotron::serverTick, ModMachineBlocks.machine_cyclotron.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineGasCent>> MACHINE_GAS_CENT =
            BLOCK_ENTITIES.register("machine_gascent",
                    () -> BlockEntities.ticking(TileEntityMachineGasCent::new, TileEntityMachineGasCent::clientTick, TileEntityMachineGasCent::serverTick, ModMachineBlocks.machine_gascent.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineElectrolyser>> MACHINE_ELECTROLYSER =
            BLOCK_ENTITIES.register("machine_electrolyser",
                    () -> BlockEntities.ticking(TileEntityMachineElectrolyser::new, TileEntityMachineElectrolyser::clientTick, TileEntityMachineElectrolyser::serverTick, ModMachineBlocks.machine_electrolyser.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntitySILEX>> MACHINE_SILEX =
            BLOCK_ENTITIES.register("machine_silex",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntitySILEX::new, com.hbm.tileentity.machine.TileEntitySILEX::serverTick, ModMachineBlocks.machine_silex.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFEL>> MACHINE_FEL =
            BLOCK_ENTITIES.register("machine_fel",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityFEL::new, com.hbm.tileentity.machine.TileEntityFEL::clientTick, com.hbm.tileentity.machine.TileEntityFEL::serverTick, ModMachineBlocks.machine_fel.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineOilWell>> MACHINE_OIL_WELL =
            BLOCK_ENTITIES.register("machine_well",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.oil.TileEntityMachineOilWell::new, com.hbm.tileentity.machine.oil.TileEntityMachineOilWell::serverTick, ModMachineBlocks.machine_well.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack>> MACHINE_PUMPJACK =
            BLOCK_ENTITIES.register("machine_pumpjack",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack::new, com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack::clientTick, TileEntityOilDrillBase::serverTick, ModMachineBlocks.machine_pumpjack.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineDiesel>> MACHINE_DIESEL =
            BLOCK_ENTITIES.register("machine_diesel",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineDiesel::new, com.hbm.tileentity.machine.TileEntityMachineDiesel::clientTick, com.hbm.tileentity.machine.TileEntityMachineDiesel::serverTick, ModMachineBlocks.machine_diesel.get()));

    // --- advanced oil wave ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower>> MACHINE_FRACKING_TOWER =
            BLOCK_ENTITIES.register("machine_fracking_tower",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower::new, TileEntityOilDrillBase::serverTick, ModMachineBlocks.machine_fracking_tower.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill>> MACHINE_VACUUM_DISTILL =
            BLOCK_ENTITIES.register("machine_vacuum_distill",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill::new, com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill::clientTick, com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill::serverTick, ModMachineBlocks.machine_vacuum_distill.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer>> MACHINE_CATALYTIC_REFORMER =
            BLOCK_ENTITIES.register("machine_catalytic_reformer",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer::new, com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer::serverTick, ModMachineBlocks.machine_catalytic_reformer.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker>> MACHINE_CATALYTIC_CRACKER =
            BLOCK_ENTITIES.register("machine_catalytic_cracker",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker::new, com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker::serverTick, ModMachineBlocks.machine_catalytic_cracker.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater>> MACHINE_HYDROTREATER =
            BLOCK_ENTITIES.register("machine_hydrotreater",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater::new, com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater::serverTick, ModMachineBlocks.machine_hydrotreater.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineCoker>> MACHINE_COKER =
            BLOCK_ENTITIES.register("machine_coker",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.oil.TileEntityMachineCoker::new, com.hbm.tileentity.machine.oil.TileEntityMachineCoker::clientTick, com.hbm.tileentity.machine.oil.TileEntityMachineCoker::serverTick, ModMachineBlocks.machine_coker.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFurnaceCombination>> FURNACE_COMBINATION =
            BLOCK_ENTITIES.register("machine_furnace_combination",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityFurnaceCombination::new, com.hbm.tileentity.machine.TileEntityFurnaceCombination::clientTick, com.hbm.tileentity.machine.TileEntityFurnaceCombination::serverTick, ModMachineBlocks.machine_furnace_combination.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven>> MACHINE_PYROOVEN =
            BLOCK_ENTITIES.register("machine_pyrooven",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven::new, com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven::clientTick, com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven::serverTick, ModMachineBlocks.machine_pyrooven.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineCombustionEngine>> MACHINE_COMBUSTION_ENGINE =
            BLOCK_ENTITIES.register("machine_combustion_engine",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineCombustionEngine::new, com.hbm.tileentity.machine.TileEntityMachineCombustionEngine::clientTick, com.hbm.tileentity.machine.TileEntityMachineCombustionEngine::serverTick, ModMachineBlocks.machine_combustion_engine.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityRefueler>> REFUELER =
            BLOCK_ENTITIES.register("refueler",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityRefueler::new, com.hbm.tileentity.machine.TileEntityRefueler::clientTick, com.hbm.tileentity.machine.TileEntityRefueler::serverTick, ModMachineBlocks.refueler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineCompressor>> MACHINE_COMPRESSOR =
            BLOCK_ENTITIES.register("machine_compressor",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineCompressor::new, com.hbm.tileentity.machine.TileEntityMachineCompressor::clientTick, TileEntityMachineCompressorBase::serverTick, ModMachineBlocks.machine_compressor.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineCompressorCompact>> MACHINE_COMPRESSOR_COMPACT =
            BLOCK_ENTITIES.register("machine_compressor_compact",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineCompressorCompact::new,
                            com.hbm.tileentity.machine.TileEntityMachineCompressorCompact::clientTick,
                            TileEntityMachineCompressorBase::serverTick,
                            ModMachineBlocks.machine_compressor_compact.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.BlockEntityDecon>> DECON =
            BLOCK_ENTITIES.register("decon", () -> BlockEntities.ticking(
                    com.hbm.tileentity.machine.BlockEntityDecon::new,
                    com.hbm.tileentity.machine.BlockEntityDecon::clientTick,
                    com.hbm.tileentity.machine.BlockEntityDecon::serverTick,
                    ModMachineBlocks.decon.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityTowerSmall>> MACHINE_TOWER_SMALL =
            BLOCK_ENTITIES.register("cooling_tower_small", () -> BlockEntities.ticking(
                    com.hbm.tileentity.machine.TileEntityTowerSmall::new,
                    com.hbm.tileentity.machine.TileEntityTowerSmall::clientTick,
                    com.hbm.tileentity.machine.TileEntityTowerSmall::serverTick,
                    ModMachineBlocks.machine_tower_small.get()));
    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityTowerLarge>> MACHINE_TOWER_LARGE =
            BLOCK_ENTITIES.register("cooling_tower_large", () -> BlockEntities.ticking(
                    com.hbm.tileentity.machine.TileEntityTowerLarge::new,
                    com.hbm.tileentity.machine.TileEntityTowerLarge::clientTick,
                    com.hbm.tileentity.machine.TileEntityTowerLarge::serverTick,
                    ModMachineBlocks.machine_tower_large.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare>> MACHINE_GAS_FLARE =
            BLOCK_ENTITIES.register("machine_flare",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare::new, com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare::clientTick, com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare::serverTick, ModMachineBlocks.machine_flare.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower>> MACHINE_FRACTION_TOWER =
            BLOCK_ENTITIES.register("machine_fraction_tower",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower::new, com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower::serverTick, ModMachineBlocks.machine_fraction_tower.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntitySpacer>> FRACTION_SPACER =
            BLOCK_ENTITIES.register("fraction_spacer",
                    () -> BlockEntities.type(com.hbm.tileentity.machine.oil.TileEntitySpacer::new, ModMachineBlocks.fraction_spacer.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor>> MACHINE_LIQUEFACTOR =
            BLOCK_ENTITIES.register("machine_liquefactor",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor::new, com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor::serverTick, ModMachineBlocks.machine_liquefactor.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier>> MACHINE_SOLIDIFIER =
            BLOCK_ENTITIES.register("machine_solidifier",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier::new, com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier::serverTick, ModMachineBlocks.machine_solidifier.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.oil.TileEntityMachineRefinery>> MACHINE_REFINERY =
            BLOCK_ENTITIES.register("machine_refinery",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.oil.TileEntityMachineRefinery::new, com.hbm.tileentity.machine.oil.TileEntityMachineRefinery::clientTick, com.hbm.tileentity.machine.oil.TileEntityMachineRefinery::serverTick, ModMachineBlocks.machine_refinery.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeatBoiler>> HEAT_BOILER =
            BLOCK_ENTITIES.register("heat_boiler",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityHeatBoiler::new, com.hbm.tileentity.machine.TileEntityHeatBoiler::clientTick, com.hbm.tileentity.machine.TileEntityHeatBoiler::serverTick, ModMachineBlocks.machine_boiler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial>> MACHINE_INDUSTRIAL_BOILER =
            BLOCK_ENTITIES.register("machine_industrial_boiler",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial::new, com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial::clientTick, com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial::serverTick, ModMachineBlocks.machine_industrial_boiler.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterOilburner>> HEATER_OILBURNER =
            BLOCK_ENTITIES.register("heater_oilburner",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityHeaterOilburner::new, com.hbm.tileentity.machine.TileEntityHeaterOilburner::serverTick, ModMachineBlocks.heater_oilburner.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineWoodBurner>> MACHINE_WOOD_BURNER =
            BLOCK_ENTITIES.register("machine_wood_burner",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityMachineWoodBurner::new, com.hbm.tileentity.machine.TileEntityMachineWoodBurner::clientTick, com.hbm.tileentity.machine.TileEntityMachineWoodBurner::serverTick, ModMachineBlocks.machine_wood_burner.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityMachineMiningLaser>> MACHINE_MINING_LASER =
            BLOCK_ENTITIES.register("machine_mining_laser",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityMachineMiningLaser::new, com.hbm.tileentity.machine.TileEntityMachineMiningLaser::serverTick, ModMachineBlocks.machine_mining_laser.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCrucible>> MACHINE_CRUCIBLE =
            BLOCK_ENTITIES.register("machine_crucible",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityCrucible::new, com.hbm.tileentity.machine.TileEntityCrucible::serverTick, ModMachineBlocks.machine_crucible.get()));

    // --- foundry transport / casting ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryChannel>> FOUNDRY_CHANNEL =
            BLOCK_ENTITIES.register("foundry_channel",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityFoundryChannel::new, com.hbm.tileentity.machine.TileEntityFoundryChannel::serverTick, ModMachineBlocks.foundry_channel.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryMold>> FOUNDRY_MOLD =
            BLOCK_ENTITIES.register("foundry_mold",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityFoundryMold::new, TileEntityFoundryCastingBase::serverTick, ModMachineBlocks.foundry_mold.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryBasin>> FOUNDRY_BASIN =
            BLOCK_ENTITIES.register("foundry_basin",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityFoundryBasin::new, TileEntityFoundryCastingBase::serverTick, ModMachineBlocks.foundry_basin.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundryOutlet>> FOUNDRY_OUTLET =
            BLOCK_ENTITIES.register("foundry_outlet",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityFoundryOutlet::new, com.hbm.tileentity.machine.TileEntityFoundryOutlet::serverTick, ModMachineBlocks.foundry_outlet.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityFoundrySlagtap>> FOUNDRY_SLAGTAP =
            BLOCK_ENTITIES.register("foundry_slagtap",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityFoundrySlagtap::new, TileEntityFoundryOutlet::serverTick, ModMachineBlocks.foundry_slagtap.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag>> SLAG =
            BLOCK_ENTITIES.register("slag",
                    () -> BlockEntities.type(com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag::new, ModMachineBlocks.slag.get()));

    // --- foundry heat sources ---

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterFirebox>> HEATER_FIREBOX =
            BLOCK_ENTITIES.register("heater_firebox",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityHeaterFirebox::new, com.hbm.tileentity.machine.TileEntityHeaterFirebox::clientTick, com.hbm.tileentity.machine.TileEntityHeaterFirebox::serverTick, ModMachineBlocks.heater_firebox.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterOven>> HEATER_OVEN =
            BLOCK_ENTITIES.register("heater_oven",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityHeaterOven::new, com.hbm.tileentity.machine.TileEntityHeaterOven::clientTick, com.hbm.tileentity.machine.TileEntityHeaterOven::serverTick, ModMachineBlocks.heater_oven.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterElectric>> HEATER_ELECTRIC =
            BLOCK_ENTITIES.register("heater_electric",
                    () -> BlockEntities.ticking(com.hbm.tileentity.machine.TileEntityHeaterElectric::new, com.hbm.tileentity.machine.TileEntityHeaterElectric::clientTick, com.hbm.tileentity.machine.TileEntityHeaterElectric::serverTick, ModMachineBlocks.heater_electric.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityHeaterHeatex>> HEATER_HEATEX =
            BLOCK_ENTITIES.register("heater_heatex",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityHeaterHeatex::new, com.hbm.tileentity.machine.TileEntityHeaterHeatex::serverTick, ModMachineBlocks.heater_heatex.get()));

    public static final RegistryObject<BlockEntityType<TileEntityLaunchPad>> LAUNCH_PAD =
            BLOCK_ENTITIES.register("launch_pad",
                    () -> BlockEntities.serverTicking(TileEntityLaunchPad::new, TileEntityLaunchPad::serverTick, ModMachineBlocks.launch_pad.get()));

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

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.bomb.TileEntityFireworks>> FIREWORKS =
            BLOCK_ENTITIES.register("fireworks",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.bomb.TileEntityFireworks::new,
                            (level, pos, state, be) -> be.serverTick(),
                            com.hbm.blocks.bomb.ModBombBlocks.fireworks.get()));

    public static final RegistryObject<BlockEntityType<com.hbm.tileentity.machine.TileEntityCyberCrab>> METEOR_SPAWNER =
            BLOCK_ENTITIES.register("meteor_spawner",
                    () -> BlockEntities.serverTicking(com.hbm.tileentity.machine.TileEntityCyberCrab::new,
                            (level, pos, state, be) -> be.serverTick(),
                            com.hbm.blocks.ModBlocks.meteor_spawner.get()));

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
