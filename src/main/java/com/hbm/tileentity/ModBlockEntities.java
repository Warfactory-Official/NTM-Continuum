package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.Constants;
import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockDynamicSlag;
import com.hbm.blocks.machine.MachineFan;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.tileentity.bomb.TileEntityFireworks;
import com.hbm.tileentity.bomb.TileEntityNukeMike;
import com.hbm.tileentity.deco.TileEntityReeds;
import com.hbm.tileentity.machine.BlockEntityDecon;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import com.hbm.tileentity.machine.TileEntityCondenser;
import com.hbm.tileentity.machine.TileEntityCondenserPowered;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.hbm.tileentity.machine.TileEntityCyberCrab;
import com.hbm.tileentity.machine.TileEntityFEL;
import com.hbm.tileentity.machine.TileEntityFoundryBasin;
import com.hbm.tileentity.machine.TileEntityFoundryChannel;
import com.hbm.tileentity.machine.TileEntityFoundryMold;
import com.hbm.tileentity.machine.TileEntityFoundrySlagtap;
import com.hbm.tileentity.machine.TileEntityFurnaceCombination;
import com.hbm.tileentity.machine.TileEntityGeiger;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
import com.hbm.tileentity.machine.TileEntityHeaterElectric;
import com.hbm.tileentity.machine.TileEntityHeaterFirebox;
import com.hbm.tileentity.machine.TileEntityHeaterHeatex;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import com.hbm.tileentity.machine.TileEntityHeaterOven;
import com.hbm.tileentity.machine.TileEntityLaunchpadLambda;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import com.hbm.tileentity.machine.TileEntityMachineCompressor;
import com.hbm.tileentity.machine.TileEntityMachineCompressorCompact;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import com.hbm.tileentity.machine.TileEntityMachineDrain;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import com.hbm.tileentity.machine.TileEntityMachineIntake;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import com.hbm.tileentity.machine.TileEntityMachineSuperComputer;
import com.hbm.tileentity.machine.TileEntityMachineTapeDrive;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import com.hbm.tileentity.machine.TileEntityMachineSteamEngine;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import com.hbm.tileentity.machine.TileEntityMachineWoodBurner;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import com.hbm.tileentity.machine.TileEntityRefueler;
import com.hbm.tileentity.machine.TileEntitySILEX;
import com.hbm.tileentity.machine.TileEntityStorageDrum;
import com.hbm.tileentity.machine.TileEntityTowerLarge;
import com.hbm.tileentity.machine.TileEntityTowerSmall;
import com.hbm.tileentity.machine.TileEntityWasteDrum;
import com.hbm.tileentity.machine.TileEntityWatz;
import com.hbm.tileentity.machine.TileEntityWatzPump;
import com.hbm.tileentity.machine.TileEntityZirnoxDestroyed;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import com.hbm.tileentity.machine.fusion.TileEntityFusionCollector;
import com.hbm.tileentity.machine.fusion.TileEntityFusionCoupler;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystronCreative;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticReformer;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
import com.hbm.tileentity.machine.oil.TileEntityMachineFrackingTower;
import com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import com.hbm.tileentity.machine.oil.TileEntityMachineHydrotreater;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;
import com.hbm.tileentity.machine.oil.TileEntityMachineOilWell;
import com.hbm.tileentity.machine.oil.TileEntityMachinePumpjack;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import com.hbm.tileentity.machine.oil.TileEntityMachineSolidifier;
import com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill;
import com.hbm.tileentity.machine.oil.TileEntitySpacer;
import com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import com.hbm.tileentity.machine.pile.TileEntityPileVent;
import com.hbm.tileentity.machine.TileEntitySoyuzLauncher;
import com.hbm.tileentity.machine.TileEntitySoyuzStruct;
import com.hbm.tileentity.machine.storage.MassStorageType;
import com.hbm.tileentity.machine.storage.TemplateCrateLayout;
import com.hbm.tileentity.machine.storage.TileEntityCrateSupply;
import com.hbm.tileentity.machine.storage.TileEntityCrateTemplate;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import com.hbm.tileentity.machine.struct.TileEntityFusionTorusStruct;
import com.hbm.tileentity.machine.struct.TileEntityStructBase;
import com.hbm.tileentity.machine.struct.TileEntityWatzStruct;
import com.hbm.tileentity.turret.TileEntityTurretArty;
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
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import com.hbm.tileentity.turret.TileEntityTurretChekhov;
import com.hbm.tileentity.turret.TileEntityTurretFriendly;
import com.hbm.tileentity.turret.TileEntityTurretFritz;
import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import com.hbm.tileentity.turret.TileEntityTurretHoward;
import com.hbm.tileentity.turret.TileEntityTurretHowardDamaged;
import com.hbm.tileentity.turret.TileEntityTurretJeremy;
import com.hbm.tileentity.turret.TileEntityTurretMaxwell;
import com.hbm.tileentity.turret.TileEntityTurretRichard;
import com.hbm.tileentity.turret.TileEntityTurretSentry;
import com.hbm.tileentity.turret.TileEntityTurretSentryDamaged;
import com.hbm.tileentity.turret.TileEntityTurretTauon;
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
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.registration.Reg;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
import com.hbm.tileentity.machine.TileEntityChimneyBase;
import com.hbm.tileentity.machine.TileEntityChimneyBrick;
import com.hbm.tileentity.machine.TileEntityChimneyIndustrial;
import com.hbm.tileentity.machine.TileEntityAshpit;
import com.hbm.tileentity.machine.TileEntityMachineAutosaw;
import com.hbm.tileentity.machine.TileEntitySolarBoiler;
import com.hbm.tileentity.machine.TileEntitySolarMirror;
import com.hbm.tileentity.machine.TileEntityDiFurnace;
import com.hbm.tileentity.machine.TileEntityDiFurnaceExtension;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import com.hbm.tileentity.network.TileEntityRadioTelex;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import com.hbm.tileentity.network.TileEntityRadioTorchCounter;
import com.hbm.tileentity.network.TileEntityRadioTorchReceiver;
import com.hbm.tileentity.network.TileEntityRadioTorchSender;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import com.hbm.tileentity.network.TileEntityRadioTorchLogic;
import net.minecraft.world.level.block.state.BlockState;

/** Registry for HBM block entity types. */
public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityMachineBattery>> MACHINE_BATTERY =
            REG.blockEntity("machine_battery",
                    () -> BlockEntities.serverTicking(TileEntityMachineBattery::new, TileEntityMachineBattery::serverTick, ModMachineBlocks.machine_battery.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineRadGen>> MACHINE_RAD_GEN =
            REG.blockEntity("machine_radgen",
                    () -> BlockEntities.serverTicking(TileEntityMachineRadGen::new, TileEntityMachineRadGen::serverTick, ModMachineBlocks.machine_radgen.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMiniRTG>> MACHINE_MINI_RTG =
            REG.blockEntity("machine_minirtg",
                    () -> BlockEntities.serverTicking(TileEntityMachineMiniRTG::new, TileEntityMachineMiniRTG::serverTick, ModMachineBlocks.machine_minirtg.get(), ModMachineBlocks.rtg_polonium.get()));

    public static final RegistryObject<BlockEntityType<TileEntityRadSensor>> RAD_SENSOR =
            REG.blockEntity("radsensor",
                    () -> BlockEntities.serverTicking(TileEntityRadSensor::new, TileEntityRadSensor::serverTick, ModMachineBlocks.radsensor.get()));


    public static final RegistryObject<BlockEntityType<TileEntityCargoElevator>>
            CARGO_ELEVATOR = REG.blockEntity("cargo_elevator",
                    () -> BlockEntities.ticking(TileEntityCargoElevator::new,
                            TileEntityCargoElevator::clientTick,
                            TileEntityCargoElevator::serverTick,
                            ModMachineBlocks.cargo_elevator.get()));


    public static final RegistryObject<BlockEntityType<TileEntityGeiger>> GEIGER =
            REG.blockEntity("geiger",
                    () -> BlockEntities.serverTicking(TileEntityGeiger::new,
                            TileEntityGeiger::serverTick,
                            ModMachineBlocks.geiger.get()));



    public static final RegistryObject<BlockEntityType<TileEntityCableBaseNT>> RED_CABLE =
            REG.blockEntity("red_cable",
                    () -> BlockEntities.type(TileEntityCableBaseNT::new, ModMachineBlocks.red_cable.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeGenerator>> CREATIVE_GENERATOR =
            REG.blockEntity("creative_generator",
                    () -> BlockEntities.serverTicking(TileEntityCreativeGenerator::new, TileEntityCreativeGenerator::serverTick, ModMachineBlocks.creative_generator.get()));


    public static final RegistryObject<BlockEntityType<TileEntityMachineLargeTurbine>> MACHINE_LARGE_TURBINE =
            REG.blockEntity("machine_large_turbine",
                    () -> BlockEntities.ticking(TileEntityMachineLargeTurbine::new, TileEntityMachineLargeTurbine::clientTick, TileEntityMachineLargeTurbine::serverTick, ModMachineBlocks.machine_large_turbine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineTurbineGas>> MACHINE_TURBINE_GAS =
            REG.blockEntity("machine_turbine_gas",
                    () -> BlockEntities.ticking(TileEntityMachineTurbineGas::new, TileEntityMachineTurbineGas::clientTick, TileEntityMachineTurbineGas::serverTick, ModMachineBlocks.machine_turbine_gas.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineTurbofan>> MACHINE_TURBOFAN =
            REG.blockEntity("machine_turbofan",
                    () -> BlockEntities.ticking(TileEntityMachineTurbofan::new, TileEntityMachineTurbofan::clientTick, TileEntityMachineTurbofan::serverTick, ModMachineBlocks.machine_turbofan.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineSteamEngine>> MACHINE_STEAM_ENGINE =
            REG.blockEntity("machine_steam_engine",
                    () -> BlockEntities.ticking(TileEntityMachineSteamEngine::new, TileEntityMachineSteamEngine::clientTick, TileEntityMachineSteamEngine::serverTick, ModMachineBlocks.machine_steam_engine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpElectric>> MACHINE_PUMP_ELECTRIC =
            REG.blockEntity("machine_pump_electric",
                    () -> BlockEntities.ticking(TileEntityMachinePumpElectric::new, TileEntityMachinePumpElectric::clientTick, TileEntityMachinePumpElectric::serverTick, ModMachineBlocks.machine_pump_electric.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpSteam>> MACHINE_PUMP_STEAM =
            REG.blockEntity("machine_pump_steam",
                    () -> BlockEntities.ticking(TileEntityMachinePumpSteam::new, TileEntityMachinePumpSteam::clientTick, TileEntityMachinePumpSteam::serverTick, ModMachineBlocks.machine_pump_steam.get()));

    public static final RegistryObject<BlockEntityType<TileEntityStirling>> MACHINE_STIRLING =
            REG.blockEntity("machine_stirling",
                    () -> BlockEntities.ticking(TileEntityStirling::new, TileEntityStirling::clientTick, TileEntityStirling::serverTick, ModMachineBlocks.machine_stirling.get(), ModMachineBlocks.machine_stirling_steel.get(), ModMachineBlocks.machine_stirling_creative.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineIndustrialTurbine>> MACHINE_INDUSTRIAL_TURBINE =
            REG.blockEntity("machine_industrial_turbine",
                    () -> BlockEntities.ticking(TileEntityMachineIndustrialTurbine::new, TileEntityMachineIndustrialTurbine::clientTick, TileEntityMachineIndustrialTurbine::serverTick, ModMachineBlocks.machine_industrial_turbine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPileBaseMK2>> PILE_BLOCK =
            REG.blockEntity("pile_block",
                    () -> BlockEntities.serverTicking(TileEntityPileBaseMK2::new,
                            TileEntityPileBaseMK2::serverTick,
                            ModPileBlocks.pile_block.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPileCore>> PILE_CORE =
            REG.blockEntity("pile_core",
                    () -> BlockEntities.serverTicking(TileEntityPileCore::new,
                            TileEntityPileCore::serverTick,
                            ModPileBlocks.pile_block.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPileLoader>> PILE_LOADER =
            REG.blockEntity("pile_loader",
                    () -> BlockEntities.serverTicking(TileEntityPileLoader::new,
                            TileEntityPileLoader::serverTick,
                            ModPileBlocks.pile_device.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPileVent>> PILE_VENT =
            REG.blockEntity("pile_vent",
                    () -> BlockEntities.serverTicking(TileEntityPileVent::new,
                            TileEntityPileVent::serverTick,
                            ModPileBlocks.pile_device.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPileControl>> PILE_CONTROL =
            REG.blockEntity("pile_control",
                    () -> BlockEntities.serverTicking(TileEntityPileControl::new,
                            TileEntityPileControl::serverTick,
                            ModPileBlocks.pile_device.get()));


    public static final RegistryObject<BlockEntityType<TileEntityCondenser>> MACHINE_CONDENSER =
            REG.blockEntity("machine_condenser",
                    () -> BlockEntities.serverTicking(TileEntityCondenser::new, TileEntityCondenser::serverTick, ModMachineBlocks.machine_condenser.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCondenserPowered>> MACHINE_CONDENSER_POWERED =
            REG.blockEntity("machine_condenser_powered",
                    () -> BlockEntities.ticking(TileEntityCondenserPowered::new, TileEntityCondenserPowered::clientTick, TileEntityCondenserPowered::serverTick, ModMachineBlocks.machine_condenser_powered.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFusionCoupler>> FUSION_COUPLER =
            REG.blockEntity("fusion_coupler",
                    () -> BlockEntities.serverTicking(TileEntityFusionCoupler::new, TileEntityFusionCoupler::serverTick, ModMachineBlocks.fusion_coupler.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFusionCollector>> FUSION_COLLECTOR =
            REG.blockEntity("fusion_collector",
                    () -> BlockEntities.serverTicking(TileEntityFusionCollector::new, TileEntityFusionCollector::serverTick, ModMachineBlocks.fusion_collector.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFusionKlystron>> FUSION_KLYSTRON =
            REG.blockEntity("fusion_klystron",
                    () -> BlockEntities.ticking(TileEntityFusionKlystron::new, TileEntityFusionKlystron::clientTick, TileEntityFusionKlystron::serverTick, ModMachineBlocks.fusion_klystron.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFusionKlystronCreative>> FUSION_KLYSTRON_CREATIVE =
            REG.blockEntity("fusion_klystron_creative",
                    () -> BlockEntities.ticking(TileEntityFusionKlystronCreative::new, TileEntityFusionKlystronCreative::clientTick, TileEntityFusionKlystronCreative::serverTick, ModMachineBlocks.fusion_klystron_creative.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFusionBoiler>> FUSION_BOILER =
            REG.blockEntity("fusion_boiler",
                    () -> BlockEntities.serverTicking(TileEntityFusionBoiler::new, TileEntityFusionBoiler::serverTick, ModMachineBlocks.fusion_boiler.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFusionBreeder>> FUSION_BREEDER =
            REG.blockEntity("fusion_breeder",
                    () -> BlockEntities.serverTicking(TileEntityFusionBreeder::new, TileEntityFusionBreeder::serverTick, ModMachineBlocks.fusion_breeder.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFusionMHDT>> FUSION_MHDT =
            REG.blockEntity("fusion_mhdt",
                    () -> BlockEntities.ticking(TileEntityFusionMHDT::new, TileEntityFusionMHDT::clientTick, TileEntityFusionMHDT::serverTick, ModMachineBlocks.fusion_mhdt.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFusionTorus>> FUSION_TORUS =
            REG.blockEntity("fusion_torus",
                    () -> BlockEntities.ticking(TileEntityFusionTorus::new, TileEntityFusionTorus::clientTick, TileEntityFusionTorus::serverTick, ModMachineBlocks.fusion_torus.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFusionPlasmaForge>> FUSION_PLASMA_FORGE =
            REG.blockEntity("fusion_plasma_forge",
                    () -> BlockEntities.ticking(TileEntityFusionPlasmaForge::new, TileEntityFusionPlasmaForge::clientTick, TileEntityFusionPlasmaForge::serverTick, ModMachineBlocks.fusion_plasma_forge.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFusionTorusStruct>> STRUCT_TORUS_CORE =
            REG.blockEntity("struct_torus_core",
                    () -> BlockEntities.serverTicking(TileEntityFusionTorusStruct::new, TileEntityStructBase::serverTick, ModMachineBlocks.struct_torus_core.get()));

    public static final RegistryObject<BlockEntityType<TileEntityWatz>> WATZ =
            REG.blockEntity("watz",
                    () -> BlockEntities.serverTicking(TileEntityWatz::new, TileEntityWatz::serverTick, ModMachineBlocks.watz.get()));
    /** The cells of every folded multiblock, any mod's, on loaders that find capabilities only on block entities. */
    public static final RegistryObject<BlockEntityType<TileEntityProxyCell>> PROXY_CELL =
            REG.blockEntity("proxy_cell",
                    () -> BlockEntities.type(TileEntityProxyCell::new, BuiltInRegistries.BLOCK.stream()
                            .filter(block -> block instanceof BlockMultiblockMachine machine && machine.proxiesCells()).toArray(Block[]::new)));
    public static final RegistryObject<BlockEntityType<TileEntityWatzPump>> WATZ_PUMP =
            REG.blockEntity("watz_pump",
                    () -> BlockEntities.type(TileEntityWatzPump::new, ModMachineBlocks.watz_pump.get()));
    public static final RegistryObject<BlockEntityType<TileEntityWatzStruct>> STRUCT_WATZ_CORE =
            REG.blockEntity("struct_watz_core",
                    () -> BlockEntities.serverTicking(TileEntityWatzStruct::new, TileEntityStructBase::serverTick, ModMachineBlocks.struct_watz_core.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCore>> DFC_CORE =
            REG.blockEntity("dfc_core",
                    () -> BlockEntities.serverTicking(TileEntityCore::new,
                            TileEntityCore::serverTick,
                            ModDFCBlocks.dfc_core.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCoreEmitter>> DFC_EMITTER =
            REG.blockEntity("dfc_emitter",
                    () -> BlockEntities.serverTicking(TileEntityCoreEmitter::new,
                            TileEntityCoreEmitter::serverTick,
                            ModDFCBlocks.dfc_emitter.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCoreReceiver>> DFC_RECEIVER =
            REG.blockEntity("dfc_receiver",
                    () -> BlockEntities.serverTicking(TileEntityCoreReceiver::new,
                            TileEntityCoreReceiver::serverTick,
                            ModDFCBlocks.dfc_receiver.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCoreInjector>> DFC_INJECTOR =
            REG.blockEntity("dfc_injector",
                    () -> BlockEntities.serverTicking(TileEntityCoreInjector::new,
                            TileEntityCoreInjector::serverTick,
                            ModDFCBlocks.dfc_injector.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCoreStabilizer>> DFC_STABILIZER =
            REG.blockEntity("dfc_stabilizer",
                    () -> BlockEntities.serverTicking(TileEntityCoreStabilizer::new,
                            TileEntityCoreStabilizer::serverTick,
                            ModDFCBlocks.dfc_stabilizer.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySoyuzLauncher>> SOYUZ_LAUNCHER =
            REG.blockEntity("soyuz_launcher",
                    () -> BlockEntities.ticking(TileEntitySoyuzLauncher::new,
                            TileEntitySoyuzLauncher::clientTick,
                            TileEntitySoyuzLauncher::serverTick,
                            ModMachineBlocks.soyuz_launcher.get()));
    public static final RegistryObject<BlockEntityType<TileEntitySoyuzStruct>> STRUCT_SOYUZ_CORE =
            REG.blockEntity("struct_soyuz_core",
                    () -> BlockEntities.serverTicking(TileEntitySoyuzStruct::new,
                            TileEntitySoyuzStruct::serverTick,
                            ModMachineBlocks.struct_soyuz_core.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineSatLink>> MACHINE_SATLINK =
            REG.blockEntity("machine_satlink",
                    () -> BlockEntities.serverTicking(TileEntityMachineSatLink::new,
                            TileEntityMachineSatLink::serverTick,
                            ModMachineBlocks.machine_satlink.get()));


    public static final RegistryObject<BlockEntityType<TileEntityReactorZirnox>> MACHINE_ZIRNOX =
            REG.blockEntity("machine_zirnox",
                    () -> BlockEntities.serverTicking(TileEntityReactorZirnox::new, TileEntityReactorZirnox::serverTick, ModMachineBlocks.machine_zirnox.get()));

    public static final RegistryObject<BlockEntityType<TileEntityZirnoxDestroyed>> ZIRNOX_DESTROYED =
            REG.blockEntity("zirnox_destroyed",
                    () -> BlockEntities.ticking(TileEntityZirnoxDestroyed::new, TileEntityZirnoxDestroyed::clientTick, TileEntityZirnoxDestroyed::serverTick, ModMachineBlocks.zirnox_destroyed.get()));

    public static final RegistryObject<BlockEntityType<TileEntityWasteDrum>> MACHINE_WASTE_DRUM =
            REG.blockEntity("machine_waste_drum",
                    () -> BlockEntities.serverTicking(TileEntityWasteDrum::new, TileEntityWasteDrum::serverTick, ModMachineBlocks.machine_waste_drum.get()));
    public static final RegistryObject<BlockEntityType<TileEntityStorageDrum>> MACHINE_STORAGE_DRUM =
            REG.blockEntity("machine_storage_drum",
                    () -> BlockEntities.serverTicking(TileEntityStorageDrum::new, TileEntityStorageDrum::serverTick, ModMachineBlocks.machine_storage_drum.get()));

    // Phase C: fluid network demo

    public static final RegistryObject<BlockEntityType<TileEntityCreativeFluidSource>> CREATIVE_FLUID_SOURCE =
            REG.blockEntity("creative_fluid_source",
                    () -> BlockEntities.serverTicking(TileEntityCreativeFluidSource::new, TileEntityCreativeFluidSource::serverTick, ModMachineBlocks.creative_fluid_source.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineFluidTank>> FLUID_TANK =
            REG.blockEntity("machine_fluidtank",
                    () -> BlockEntities.ticking(TileEntityMachineFluidTank::new, TileEntityMachineFluidTank::clientTick, TileEntityMachineFluidTank::serverTick, ModMachineBlocks.machine_fluidtank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineUF6Tank>> MACHINE_UF6_TANK =
            REG.blockEntity("machine_uf6_tank",
                    () -> BlockEntities.serverTicking(TileEntityMachineUF6Tank::new, TileEntityMachineUF6Tank::serverTick, ModMachineBlocks.machine_uf6_tank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePuF6Tank>> MACHINE_PUF6_TANK =
            REG.blockEntity("machine_puf6_tank",
                    () -> BlockEntities.serverTicking(TileEntityMachinePuF6Tank::new, TileEntityMachinePuF6Tank::serverTick, ModMachineBlocks.machine_puf6_tank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineBigAssTank>> MACHINE_BIG_ASS_TANK =
            REG.blockEntity("machine_bigasstank",
                    () -> BlockEntities.serverTicking(TileEntityMachineBigAssTank::new, TileEntityMachineBigAssTank::serverTick, ModMachineBlocks.machine_bigasstank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineBAT9000>> MACHINE_BAT9000 =
            REG.blockEntity("machine_bat9000",
                    () -> BlockEntities.serverTicking(TileEntityMachineBAT9000::new, TileEntityMachineBAT9000::serverTick, ModMachineBlocks.machine_bat9000.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDeuteriumExtractor>> MACHINE_DEUTERIUM_EXTRACTOR =
            REG.blockEntity("machine_deuterium_extractor",
                    () -> BlockEntities.serverTicking(TileEntityDeuteriumExtractor::new, TileEntityDeuteriumExtractor::serverTick, ModMachineBlocks.machine_deuterium_extractor.get()));

    public static final RegistryObject<BlockEntityType<TileEntityDeuteriumTower>> MACHINE_DEUTERIUM_TOWER =
            REG.blockEntity("machine_deuterium_tower",
                    () -> BlockEntities.serverTicking(TileEntityDeuteriumTower::new, TileEntityDeuteriumTower::serverTick, ModMachineBlocks.machine_deuterium_tower.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFluidValve>> FLUID_VALVE =
            REG.blockEntity("fluid_valve",
                    () -> BlockEntities.type(TileEntityFluidValve::new, ModMachineBlocks.fluid_valve.get()));


    public static final RegistryObject<BlockEntityType<TileEntityMachineDrain>> MACHINE_DRAIN =
            REG.blockEntity("machine_drain",
                    () -> BlockEntities.serverTicking(TileEntityMachineDrain::new, TileEntityMachineDrain::serverTick, ModMachineBlocks.machine_drain.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretChekhov>> TURRET_CHEKHOV =
            REG.blockEntity("turret_chekhov",
                    () -> BlockEntities.ticking(TileEntityTurretChekhov::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_chekhov.get()));
    public static final RegistryObject<BlockEntityType<TileEntityTurretFriendly>> TURRET_FRIENDLY =
            REG.blockEntity("turret_friendly",
                    () -> BlockEntities.ticking(TileEntityTurretFriendly::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_friendly.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretSentry>> TURRET_SENTRY =
            REG.blockEntity("turret_sentry",
                    () -> BlockEntities.ticking(TileEntityTurretSentry::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_sentry.get()));
    public static final RegistryObject<BlockEntityType<TileEntityTurretSentryDamaged>> TURRET_SENTRY_DAMAGED =
            REG.blockEntity("turret_sentry_damaged",
                    () -> BlockEntities.ticking(TileEntityTurretSentryDamaged::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_sentry_damaged.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretHoward>> TURRET_HOWARD =
            REG.blockEntity("turret_howard",
                    () -> BlockEntities.ticking(TileEntityTurretHoward::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_howard.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretHowardDamaged>> TURRET_HOWARD_DAMAGED =
            REG.blockEntity("turret_howard_damaged",
                    () -> BlockEntities.ticking(TileEntityTurretHowardDamaged::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_howard_damaged.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretTauon>> TURRET_TAUON =
            REG.blockEntity("turret_tauon",
                    () -> BlockEntities.ticking(TileEntityTurretTauon::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_tauon.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretMaxwell>> TURRET_MAXWELL =
            REG.blockEntity("turret_maxwell",
                    () -> BlockEntities.ticking(TileEntityTurretMaxwell::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_maxwell.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretJeremy>> TURRET_JEREMY =
            REG.blockEntity("turret_jeremy",
                    () -> BlockEntities.ticking(TileEntityTurretJeremy::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_jeremy.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretRichard>> TURRET_RICHARD =
            REG.blockEntity("turret_richard",
                    () -> BlockEntities.ticking(TileEntityTurretRichard::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_richard.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretFritz>> TURRET_FRITZ =
            REG.blockEntity("turret_fritz",
                    () -> BlockEntities.ticking(TileEntityTurretFritz::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_fritz.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretHIMARS>> TURRET_HIMARS =
            REG.blockEntity("turret_himars",
                    () -> BlockEntities.ticking(TileEntityTurretHIMARS::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_himars.get()));

    public static final RegistryObject<BlockEntityType<TileEntityTurretArty>> TURRET_ARTY =
            REG.blockEntity("turret_arty",
                    () -> BlockEntities.ticking(TileEntityTurretArty::new, TileEntityTurretBaseNT::clientTick, TileEntityTurretBaseNT::serverTick, ModTurretBlocks.turret_arty.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBatterySocket>> BATTERY_SOCKET =
            REG.blockEntity("machine_battery_socket",
                    () -> BlockEntities.serverTicking(TileEntityBatterySocket::new, TileEntityBatterySocket::serverTick, ModBatteryBlocks.machine_battery_socket.get()));
    public static final RegistryObject<BlockEntityType<TileEntityBatteryREDD>> BATTERY_REDD =
            REG.blockEntity("machine_battery_redd",
                    () -> BlockEntities.ticking(TileEntityBatteryREDD::new, TileEntityBatteryREDD::clientTick, TileEntityBatteryREDD::serverTick, ModBatteryBlocks.machine_battery_redd.get()));

    // Storage crates: one TileEntityCrate class, three types (each carries its CrateType layout). The
    // BE resolves its own type via CrateType.blockEntityType(), wired below (avoids a self-reference).
    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_IRON =
            REG.blockEntity("crate_iron", () -> BlockEntities.type(
                    (pos, state) -> new TileEntityCrate(CrateType.IRON, pos, state), ModStorageBlocks.crate_iron.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_STEEL =
            REG.blockEntity("crate_steel", () -> BlockEntities.type(
                    (pos, state) -> new TileEntityCrate(CrateType.STEEL, pos, state), ModStorageBlocks.crate_steel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrate>> CRATE_DESH =
            REG.blockEntity("crate_desh", () -> BlockEntities.type(
                    (pos, state) -> new TileEntityCrate(CrateType.DESH, pos, state), ModStorageBlocks.crate_desh.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrateSupply>> CRATE_SUPPLY =
            REG.blockEntity("crate_supply", () -> BlockEntities.type(
                    TileEntityCrateSupply::new, ModStorageBlocks.crate_supply.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMassStorage>> MASS_STORAGE_WOOD =
            massStorage(MassStorageType.WOOD, ModStorageBlocks.mass_storage_wood);
    public static final RegistryObject<BlockEntityType<TileEntityMassStorage>> MASS_STORAGE_IRON =
            massStorage(MassStorageType.IRON, ModStorageBlocks.mass_storage_iron);
    public static final RegistryObject<BlockEntityType<TileEntityMassStorage>> MASS_STORAGE_DESH =
            massStorage(MassStorageType.DESH, ModStorageBlocks.mass_storage_desh);
    public static final RegistryObject<BlockEntityType<TileEntityMassStorage>> MASS_STORAGE =
            massStorage(MassStorageType.MASS, ModStorageBlocks.mass_storage);
    public static final RegistryObject<BlockEntityType<TileEntityCrateTemplate>> CRATE_TEMPLATE =
            REG.blockEntity("crate_template", () -> BlockEntities.type(
                    TileEntityCrateTemplate::new, ModStorageBlocks.crate_template.get()));

    static {
        CrateType.IRON.setTypeSupplier(CRATE_IRON::get);
        CrateType.STEEL.setTypeSupplier(CRATE_STEEL::get);
        CrateType.DESH.setTypeSupplier(CRATE_DESH::get);
        MassStorageType.WOOD.setBlockEntityTypeSupplier(MASS_STORAGE_WOOD::get);
        MassStorageType.IRON.setBlockEntityTypeSupplier(MASS_STORAGE_IRON::get);
        MassStorageType.DESH.setBlockEntityTypeSupplier(MASS_STORAGE_DESH::get);
        MassStorageType.MASS.setBlockEntityTypeSupplier(MASS_STORAGE::get);
        TemplateCrateLayout.setBlockEntityTypeSupplier(CRATE_TEMPLATE::get);
    }

    // Scaffold machines (placeable + persistent; logic deferred, see IMPLEMENTATION.md §18)

    public static final RegistryObject<BlockEntityType<TileEntityMachineElectricFurnace>> MACHINE_ELECTRIC_FURNACE =
            REG.blockEntity("machine_electric_furnace",
                    () -> BlockEntities.serverTicking(TileEntityMachineElectricFurnace::new, TileEntityMachineElectricFurnace::serverTick, ModMachineBlocks.machine_electric_furnace_off.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineArcWelder>> MACHINE_ARC_WELDER =
            REG.blockEntity("machine_arc_welder", () -> BlockEntities.serverTicking(TileEntityMachineArcWelder::new, TileEntityMachineArcWelder::serverTick, ModMachineBlocks.machine_arc_welder.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineIntake>> MACHINE_INTAKE =
            REG.blockEntity("machine_intake", () -> BlockEntities.ticking(
                    ModBlockEntities::newIntake,
                    TileEntityMachineIntake::clientTick,
                    TileEntityMachineIntake::serverTick,
                    ModMachineBlocks.machine_intake.get()));
    public static final RegistryObject<BlockEntityType<MachineFan.TileEntityFan>> MACHINE_FAN =
            REG.blockEntity("fan", () -> BlockEntities.ticking(
                    ModBlockEntities::newFan,
                    MachineFan.TileEntityFan::tick,
                    MachineFan.TileEntityFan::tick,
                    ModMachineBlocks.fan.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFurnaceBrick>> FURNACE_BRICK =
            REG.blockEntity("machine_furnace_brick_off",
                    () -> BlockEntities.serverTicking(TileEntityFurnaceBrick::new, TileEntityFurnaceBrick::serverTick, ModMachineBlocks.machine_furnace_brick_off.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFurnaceIron>> FURNACE_IRON =
            REG.blockEntity("furnace_iron",
                    () -> BlockEntities.ticking(TileEntityFurnaceIron::new, TileEntityFurnaceIron::clientTick, TileEntityFurnaceIron::serverTick, ModMachineBlocks.furnace_iron.get()));
    public static final RegistryObject<BlockEntityType<TileEntityFurnaceSteel>> FURNACE_STEEL =
            REG.blockEntity("furnace_steel",
                    () -> BlockEntities.ticking(TileEntityFurnaceSteel::new, TileEntityFurnaceSteel::clientTick, TileEntityFurnaceSteel::serverTick, ModMachineBlocks.furnace_steel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineShredder>> MACHINE_SHREDDER =
            REG.blockEntity("machine_shredder",
                    () -> BlockEntities.serverTicking(TileEntityMachineShredder::new, TileEntityMachineShredder::serverTick, ModMachineBlocks.machine_shredder.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineEPress>> MACHINE_EPRESS =
            REG.blockEntity("machine_epress",
                    () -> BlockEntities.ticking(TileEntityMachineEPress::new, TileEntityMachineEPress::clientTick, TileEntityMachineEPress::serverTick, ModMachineBlocks.machine_epress.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePress>> MACHINE_PRESS =
            REG.blockEntity("machine_press",
                    () -> BlockEntities.ticking(TileEntityMachinePress::new, TileEntityMachinePress::clientTick, TileEntityMachinePress::serverTick, ModMachineBlocks.machine_press.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineAmmoPress>> MACHINE_AMMO_PRESS =
            REG.blockEntity("machine_ammo_press",
                    () -> BlockEntities.ticking(TileEntityMachineAmmoPress::new, TileEntityMachineAmmoPress::clientTick, TileEntityMachineAmmoPress::serverTick, ModMachineBlocks.machine_ammo_press.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySawmill>> MACHINE_SAWMILL =
            REG.blockEntity("machine_sawmill",
                    () -> BlockEntities.ticking(TileEntitySawmill::new, TileEntitySawmill::clientTick, TileEntitySawmill::serverTick, ModMachineBlocks.machine_sawmill.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineAssemblyMachine>> MACHINE_ASSEMBLY_MACHINE =
            REG.blockEntity("machine_assembly_machine",
                    () -> BlockEntities.ticking(TileEntityMachineAssemblyMachine::new, TileEntityMachineAssemblyMachine::clientTick, TileEntityMachineAssemblyMachine::serverTick, ModMachineBlocks.machine_assembly_machine.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineAssemblyFactory>> MACHINE_ASSEMBLY_FACTORY =
            REG.blockEntity("machine_assembly_factory",
                    () -> BlockEntities.ticking(TileEntityMachineAssemblyFactory::new, TileEntityMachineAssemblyFactory::clientTick, TileEntityMachineAssemblyFactory::serverTick, ModMachineBlocks.machine_assembly_factory.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePrecAss>> MACHINE_PREC_ASS =
            REG.blockEntity("machine_precass",
                    () -> BlockEntities.ticking(TileEntityMachinePrecAss::new, TileEntityMachinePrecAss::clientTick, TileEntityMachinePrecAss::serverTick, ModMachineBlocks.machine_precass.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPASource>> PA_SOURCE =
            REG.blockEntity("pa_source",
                    () -> BlockEntities.serverTicking(TileEntityPASource::new, TileEntityPASource::serverTick, ModMachineBlocks.pa_source.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPABeamline>> PA_BEAMLINE =
            REG.blockEntity("pa_beamline",
                    () -> BlockEntities.type(TileEntityPABeamline::new, ModMachineBlocks.pa_beamline.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPARFC>> PA_RFC =
            REG.blockEntity("pa_rfc",
                    () -> BlockEntities.serverTicking(TileEntityPARFC::new, TileEntityPARFC::serverTick, ModMachineBlocks.pa_rfc.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPAQuadrupole>> PA_QUADRUPOLE =
            REG.blockEntity("pa_quadrupole",
                    () -> BlockEntities.serverTicking(TileEntityPAQuadrupole::new, TileEntityPAQuadrupole::serverTick, ModMachineBlocks.pa_quadrupole.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPADipole>> PA_DIPOLE =
            REG.blockEntity("pa_dipole",
                    () -> BlockEntities.serverTicking(TileEntityPADipole::new, TileEntityPADipole::serverTick, ModMachineBlocks.pa_dipole.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPADetector>> PA_DETECTOR =
            REG.blockEntity("pa_detector",
                    () -> BlockEntities.serverTicking(TileEntityPADetector::new, TileEntityPADetector::serverTick, ModMachineBlocks.pa_detector.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineChemicalPlant>> MACHINE_CHEMICAL_PLANT =
            REG.blockEntity("machine_chemical_plant",
                    () -> BlockEntities.ticking(TileEntityMachineChemicalPlant::new, TileEntityMachineChemicalPlant::clientTick, TileEntityMachineChemicalPlant::serverTick, ModMachineBlocks.machine_chemical_plant.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineChemicalFactory>> MACHINE_CHEMICAL_FACTORY =
            REG.blockEntity("machine_chemical_factory",
                    () -> BlockEntities.ticking(TileEntityMachineChemicalFactory::new, TileEntityMachineChemicalFactory::clientTick, TileEntityMachineChemicalFactory::serverTick, ModMachineBlocks.machine_chemical_factory.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMixer>> MACHINE_MIXER =
            REG.blockEntity("machine_mixer",
                    () -> BlockEntities.ticking(TileEntityMachineMixer::new, TileEntityMachineMixer::clientTick, TileEntityMachineMixer::serverTick, ModMachineBlocks.machine_mixer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineExposureChamber>> MACHINE_EXPOSURE_CHAMBER =
            REG.blockEntity("machine_exposure_chamber",
                    () -> BlockEntities.ticking(TileEntityMachineExposureChamber::new, TileEntityMachineExposureChamber::clientTick, TileEntityMachineExposureChamber::serverTick, ModMachineBlocks.machine_exposure_chamber.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineBlastFurnace>> MACHINE_BLAST_FURNACE =
            REG.blockEntity("machine_blast_furnace",
                    () -> BlockEntities.ticking(TileEntityMachineBlastFurnace::new, TileEntityMachineBlastFurnace::clientTick, TileEntityMachineBlastFurnace::serverTick, ModMachineBlocks.machine_blast_furnace.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePUREX>> MACHINE_PUREX =
            REG.blockEntity("machine_purex",
                    () -> BlockEntities.ticking(TileEntityMachinePUREX::new, TileEntityMachinePUREX::clientTick, TileEntityMachinePUREX::serverTick, ModMachineBlocks.machine_purex.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineRockMill>> MACHINE_ROCKMILL =
            REG.blockEntity("machine_rockmill",
                    () -> BlockEntities.ticking(TileEntityMachineRockMill::new, TileEntityMachineRockMill::clientTick, TileEntityMachineRockMill::serverTick, ModMachineBlocks.machine_rockmill.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineSuperComputer>> MACHINE_SUPERCOMPUTER =
            REG.blockEntity("machine_supercomputer",
                    () -> BlockEntities.serverTicking(TileEntityMachineSuperComputer::new, TileEntityMachineSuperComputer::serverTick, ModMachineBlocks.machine_supercomputer.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineTapeDrive>> MACHINE_TAPE_DRIVE =
            REG.blockEntity("machine_tape_drive",
                    () -> BlockEntities.serverTicking(TileEntityMachineTapeDrive::new, TileEntityMachineTapeDrive::serverTick, ModMachineBlocks.machine_tape_drive.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineSolderingStation>> MACHINE_SOLDERING_STATION =
            REG.blockEntity("machine_soldering_station",
                    () -> BlockEntities.serverTicking(TileEntityMachineSolderingStation::new, TileEntityMachineSolderingStation::serverTick, ModMachineBlocks.machine_soldering_station.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineArcFurnaceLarge>> MACHINE_ARC_FURNACE =
            REG.blockEntity("machine_arc_furnace",
                    () -> BlockEntities.ticking(TileEntityMachineArcFurnaceLarge::new, TileEntityMachineArcFurnaceLarge::clientTick, TileEntityMachineArcFurnaceLarge::serverTick, ModMachineBlocks.machine_arc_furnace.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCrystallizer>> MACHINE_CRYSTALLIZER =
            REG.blockEntity("machine_crystallizer",
                    () -> BlockEntities.ticking(TileEntityMachineCrystallizer::new, TileEntityMachineCrystallizer::clientTick, TileEntityMachineCrystallizer::serverTick, ModMachineBlocks.machine_crystallizer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCentrifuge>> MACHINE_CENTRIFUGE =
            REG.blockEntity("machine_centrifuge",
                    () -> BlockEntities.ticking(TileEntityMachineCentrifuge::new, TileEntityMachineCentrifuge::clientTick, TileEntityMachineCentrifuge::serverTick, ModMachineBlocks.machine_centrifuge.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCyclotron>> MACHINE_CYCLOTRON =
            REG.blockEntity("machine_cyclotron",
                    () -> BlockEntities.ticking(TileEntityMachineCyclotron::new, TileEntityMachineCyclotron::clientTick, TileEntityMachineCyclotron::serverTick, ModMachineBlocks.machine_cyclotron.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineGasCent>> MACHINE_GAS_CENT =
            REG.blockEntity("machine_gascent",
                    () -> BlockEntities.ticking(TileEntityMachineGasCent::new, TileEntityMachineGasCent::clientTick, TileEntityMachineGasCent::serverTick, ModMachineBlocks.machine_gascent.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineElectrolyser>> MACHINE_ELECTROLYSER =
            REG.blockEntity("machine_electrolyser",
                    () -> BlockEntities.ticking(TileEntityMachineElectrolyser::new, TileEntityMachineElectrolyser::clientTick, TileEntityMachineElectrolyser::serverTick, ModMachineBlocks.machine_electrolyser.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySILEX>> MACHINE_SILEX =
            REG.blockEntity("machine_silex",
                    () -> BlockEntities.serverTicking(TileEntitySILEX::new, TileEntitySILEX::serverTick, ModMachineBlocks.machine_silex.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFEL>> MACHINE_FEL =
            REG.blockEntity("machine_fel",
                    () -> BlockEntities.ticking(TileEntityFEL::new, TileEntityFEL::clientTick, TileEntityFEL::serverTick, ModMachineBlocks.machine_fel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineOilWell>> MACHINE_OIL_WELL =
            REG.blockEntity("machine_well",
                    () -> BlockEntities.serverTicking(TileEntityMachineOilWell::new, TileEntityMachineOilWell::serverTick, ModMachineBlocks.machine_well.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePumpjack>> MACHINE_PUMPJACK =
            REG.blockEntity("machine_pumpjack",
                    () -> BlockEntities.ticking(TileEntityMachinePumpjack::new, TileEntityMachinePumpjack::clientTick, TileEntityOilDrillBase::serverTick, ModMachineBlocks.machine_pumpjack.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineDiesel>> MACHINE_DIESEL =
            REG.blockEntity("machine_diesel",
                    () -> BlockEntities.ticking(TileEntityMachineDiesel::new, TileEntityMachineDiesel::clientTick, TileEntityMachineDiesel::serverTick, ModMachineBlocks.machine_diesel.get()));

    // Advanced oil machines

    public static final RegistryObject<BlockEntityType<TileEntityMachineFrackingTower>> MACHINE_FRACKING_TOWER =
            REG.blockEntity("machine_fracking_tower",
                    () -> BlockEntities.serverTicking(TileEntityMachineFrackingTower::new, TileEntityOilDrillBase::serverTick, ModMachineBlocks.machine_fracking_tower.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineVacuumDistill>> MACHINE_VACUUM_DISTILL =
            REG.blockEntity("machine_vacuum_distill",
                    () -> BlockEntities.ticking(TileEntityMachineVacuumDistill::new, TileEntityMachineVacuumDistill::clientTick, TileEntityMachineVacuumDistill::serverTick, ModMachineBlocks.machine_vacuum_distill.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCatalyticReformer>> MACHINE_CATALYTIC_REFORMER =
            REG.blockEntity("machine_catalytic_reformer",
                    () -> BlockEntities.serverTicking(TileEntityMachineCatalyticReformer::new, TileEntityMachineCatalyticReformer::serverTick, ModMachineBlocks.machine_catalytic_reformer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCatalyticCracker>> MACHINE_CATALYTIC_CRACKER =
            REG.blockEntity("machine_catalytic_cracker",
                    () -> BlockEntities.serverTicking(TileEntityMachineCatalyticCracker::new, TileEntityMachineCatalyticCracker::serverTick, ModMachineBlocks.machine_catalytic_cracker.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineHydrotreater>> MACHINE_HYDROTREATER =
            REG.blockEntity("machine_hydrotreater",
                    () -> BlockEntities.serverTicking(TileEntityMachineHydrotreater::new, TileEntityMachineHydrotreater::serverTick, ModMachineBlocks.machine_hydrotreater.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCoker>> MACHINE_COKER =
            REG.blockEntity("machine_coker",
                    () -> BlockEntities.ticking(TileEntityMachineCoker::new, TileEntityMachineCoker::clientTick, TileEntityMachineCoker::serverTick, ModMachineBlocks.machine_coker.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFurnaceCombination>> FURNACE_COMBINATION =
            REG.blockEntity("machine_furnace_combination",
                    () -> BlockEntities.ticking(TileEntityFurnaceCombination::new, TileEntityFurnaceCombination::clientTick, TileEntityFurnaceCombination::serverTick, ModMachineBlocks.machine_furnace_combination.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePyroOven>> MACHINE_PYROOVEN =
            REG.blockEntity("machine_pyrooven",
                    () -> BlockEntities.ticking(TileEntityMachinePyroOven::new, TileEntityMachinePyroOven::clientTick, TileEntityMachinePyroOven::serverTick, ModMachineBlocks.machine_pyrooven.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineAutosaw>> MACHINE_AUTOSAW =
            REG.blockEntity("machine_autosaw",
                    () -> BlockEntities.ticking(TileEntityMachineAutosaw::new, TileEntityMachineAutosaw::clientTick, TileEntityMachineAutosaw::serverTick, ModMachineBlocks.machine_autosaw.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCombustionEngine>> MACHINE_COMBUSTION_ENGINE =
            REG.blockEntity("machine_combustion_engine",
                    () -> BlockEntities.ticking(TileEntityMachineCombustionEngine::new, TileEntityMachineCombustionEngine::clientTick, TileEntityMachineCombustionEngine::serverTick, ModMachineBlocks.machine_combustion_engine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityRefueler>> REFUELER =
            REG.blockEntity("refueler",
                    () -> BlockEntities.ticking(TileEntityRefueler::new, TileEntityRefueler::clientTick, TileEntityRefueler::serverTick, ModMachineBlocks.refueler.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCompressor>> MACHINE_COMPRESSOR =
            REG.blockEntity("machine_compressor",
                    () -> BlockEntities.ticking(TileEntityMachineCompressor::new, TileEntityMachineCompressor::clientTick, TileEntityMachineCompressorBase::serverTick, ModMachineBlocks.machine_compressor.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineCompressorCompact>> MACHINE_COMPRESSOR_COMPACT =
            REG.blockEntity("machine_compressor_compact",
                    () -> BlockEntities.serverTicking(TileEntityMachineCompressorCompact::new,
                            TileEntityMachineCompressorBase::serverTick,
                            ModMachineBlocks.machine_compressor_compact.get()));

    public static final RegistryObject<BlockEntityType<BlockEntityDecon>> DECON =
            REG.blockEntity("decon", () -> BlockEntities.ticking(
                    BlockEntityDecon::new,
                    BlockEntityDecon::clientTick,
                    BlockEntityDecon::serverTick,
                    ModMachineBlocks.decon.get()));
    public static final RegistryObject<BlockEntityType<TileEntitySolarBoiler>> MACHINE_SOLAR_BOILER =
            REG.blockEntity("machine_solar_boiler",
                    () -> BlockEntities.serverTicking(TileEntitySolarBoiler::new, TileEntitySolarBoiler::serverTick, ModMachineBlocks.machine_solar_boiler.get()));
    public static final RegistryObject<BlockEntityType<TileEntitySolarMirror>> SOLAR_MIRROR =
            REG.blockEntity("solar_mirror",
                    () -> BlockEntities.serverTicking(TileEntitySolarMirror::new, TileEntitySolarMirror::serverTick, ModMachineBlocks.solar_mirror.get()));
    public static final RegistryObject<BlockEntityType<TileEntityTowerSmall>> MACHINE_TOWER_SMALL =
            REG.blockEntity("cooling_tower_small", () -> BlockEntities.ticking(
                    TileEntityTowerSmall::new,
                    TileEntityTowerSmall::clientTick,
                    TileEntityTowerSmall::serverTick,
                    ModMachineBlocks.machine_tower_small.get()));
    public static final RegistryObject<BlockEntityType<TileEntityTowerLarge>> MACHINE_TOWER_LARGE =
            REG.blockEntity("cooling_tower_large", () -> BlockEntities.ticking(
                    TileEntityTowerLarge::new,
                    TileEntityTowerLarge::clientTick,
                    TileEntityTowerLarge::serverTick,
                    ModMachineBlocks.machine_tower_large.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineGasFlare>> MACHINE_GAS_FLARE =
            REG.blockEntity("machine_flare",
                    () -> BlockEntities.ticking(TileEntityMachineGasFlare::new, TileEntityMachineGasFlare::clientTick, TileEntityMachineGasFlare::serverTick, ModMachineBlocks.machine_flare.get()));
    public static final RegistryObject<BlockEntityType<TileEntityChimneyBrick>> CHIMNEY_BRICK =
            REG.blockEntity("chimney_brick",
                    () -> BlockEntities.ticking(TileEntityChimneyBrick::new, TileEntityChimneyBase::clientTick, TileEntityChimneyBase::serverTick, ModMachineBlocks.chimney_brick.get()));
    public static final RegistryObject<BlockEntityType<TileEntityChimneyIndustrial>> CHIMNEY_INDUSTRIAL =
            REG.blockEntity("chimney_industrial",
                    () -> BlockEntities.ticking(TileEntityChimneyIndustrial::new, TileEntityChimneyBase::clientTick, TileEntityChimneyBase::serverTick, ModMachineBlocks.chimney_industrial.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineFractionTower>> MACHINE_FRACTION_TOWER =
            REG.blockEntity("machine_fraction_tower",
                    () -> BlockEntities.serverTicking(TileEntityMachineFractionTower::new, TileEntityMachineFractionTower::serverTick, ModMachineBlocks.machine_fraction_tower.get()));

    public static final RegistryObject<BlockEntityType<TileEntitySpacer>> FRACTION_SPACER =
            REG.blockEntity("fraction_spacer",
                    () -> BlockEntities.type(TileEntitySpacer::new, ModMachineBlocks.fraction_spacer.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineLiquefactor>> MACHINE_LIQUEFACTOR =
            REG.blockEntity("machine_liquefactor",
                    () -> BlockEntities.serverTicking(TileEntityMachineLiquefactor::new, TileEntityMachineLiquefactor::serverTick, ModMachineBlocks.machine_liquefactor.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineSolidifier>> MACHINE_SOLIDIFIER =
            REG.blockEntity("machine_solidifier",
                    () -> BlockEntities.serverTicking(TileEntityMachineSolidifier::new, TileEntityMachineSolidifier::serverTick, ModMachineBlocks.machine_solidifier.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineRefinery>> MACHINE_REFINERY =
            REG.blockEntity("machine_refinery",
                    () -> BlockEntities.ticking(TileEntityMachineRefinery::new, TileEntityMachineRefinery::clientTick, TileEntityMachineRefinery::serverTick, ModMachineBlocks.machine_refinery.get()));

    public static final RegistryObject<BlockEntityType<TileEntityHeatBoiler>> HEAT_BOILER =
            REG.blockEntity("heat_boiler",
                    () -> BlockEntities.ticking(TileEntityHeatBoiler::new, TileEntityHeatBoiler::clientTick, TileEntityHeatBoiler::serverTick, ModMachineBlocks.machine_boiler.get()));

    public static final RegistryObject<BlockEntityType<TileEntityHeatBoilerIndustrial>> MACHINE_INDUSTRIAL_BOILER =
            REG.blockEntity("machine_industrial_boiler",
                    () -> BlockEntities.ticking(TileEntityHeatBoilerIndustrial::new, TileEntityHeatBoilerIndustrial::clientTick, TileEntityHeatBoilerIndustrial::serverTick, ModMachineBlocks.machine_industrial_boiler.get()));

    public static final RegistryObject<BlockEntityType<TileEntityHeaterOilburner>> HEATER_OILBURNER =
            REG.blockEntity("heater_oilburner",
                    () -> BlockEntities.serverTicking(TileEntityHeaterOilburner::new, TileEntityHeaterOilburner::serverTick, ModMachineBlocks.heater_oilburner.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineWoodBurner>> MACHINE_WOOD_BURNER =
            REG.blockEntity("machine_wood_burner",
                    () -> BlockEntities.ticking(TileEntityMachineWoodBurner::new, TileEntityMachineWoodBurner::clientTick, TileEntityMachineWoodBurner::serverTick, ModMachineBlocks.machine_wood_burner.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineMiningLaser>> MACHINE_MINING_LASER =
            REG.blockEntity("machine_mining_laser",
                    () -> BlockEntities.serverTicking(TileEntityMachineMiningLaser::new, TileEntityMachineMiningLaser::serverTick, ModMachineBlocks.machine_mining_laser.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCrucible>> MACHINE_CRUCIBLE =
            REG.blockEntity("machine_crucible",
                    () -> BlockEntities.serverTicking(TileEntityCrucible::new, TileEntityCrucible::serverTick, ModMachineBlocks.machine_crucible.get()));

    // Foundry transport / casting

    public static final RegistryObject<BlockEntityType<TileEntityFoundryChannel>> FOUNDRY_CHANNEL =
            REG.blockEntity("foundry_channel",
                    () -> BlockEntities.serverTicking(TileEntityFoundryChannel::new, TileEntityFoundryChannel::serverTick, ModMachineBlocks.foundry_channel.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFoundryMold>> FOUNDRY_MOLD =
            REG.blockEntity("foundry_mold",
                    () -> BlockEntities.serverTicking(TileEntityFoundryMold::new, TileEntityFoundryCastingBase::serverTick, ModMachineBlocks.foundry_mold.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFoundryBasin>> FOUNDRY_BASIN =
            REG.blockEntity("foundry_basin",
                    () -> BlockEntities.serverTicking(TileEntityFoundryBasin::new, TileEntityFoundryCastingBase::serverTick, ModMachineBlocks.foundry_basin.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFoundryOutlet>> FOUNDRY_OUTLET =
            REG.blockEntity("foundry_outlet",
                    () -> BlockEntities.serverTicking(TileEntityFoundryOutlet::new, TileEntityFoundryOutlet::serverTick, ModMachineBlocks.foundry_outlet.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFoundrySlagtap>> FOUNDRY_SLAGTAP =
            REG.blockEntity("foundry_slagtap",
                    () -> BlockEntities.serverTicking(TileEntityFoundrySlagtap::new, TileEntityFoundryOutlet::serverTick, ModMachineBlocks.foundry_slagtap.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineStrandCaster>> MACHINE_STRAND_CASTER =
            REG.blockEntity("machine_strand_caster",
                    () -> BlockEntities.serverTicking(TileEntityMachineStrandCaster::new, TileEntityMachineStrandCaster::serverTick, ModMachineBlocks.machine_strand_caster.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDiFurnace>> MACHINE_DIFURNACE =
            REG.blockEntity("machine_difurnace_off",
                    () -> BlockEntities.serverTicking(TileEntityDiFurnace::new, TileEntityDiFurnace::serverTick, ModMachineBlocks.machine_difurnace_off.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDiFurnaceExtension>> MACHINE_DIFURNACE_EXT =
            REG.blockEntity("machine_difurnace_ext",
                    () -> BlockEntities.type(TileEntityDiFurnaceExtension::new, ModMachineBlocks.machine_difurnace_ext.get()));

    public static final RegistryObject<BlockEntityType<BlockDynamicSlag.TileEntitySlag>> SLAG =
            REG.blockEntity("slag",
                    () -> BlockEntities.type(BlockDynamicSlag.TileEntitySlag::new, ModMachineBlocks.slag.get()));

    // Foundry heat sources

    public static final RegistryObject<BlockEntityType<TileEntityHeaterFirebox>> HEATER_FIREBOX =
            REG.blockEntity("heater_firebox",
                    () -> BlockEntities.ticking(TileEntityHeaterFirebox::new, TileEntityHeaterFirebox::clientTick, TileEntityHeaterFirebox::serverTick, ModMachineBlocks.heater_firebox.get()));
    public static final RegistryObject<BlockEntityType<TileEntityAshpit>> MACHINE_ASHPIT =
            REG.blockEntity("machine_ashpit",
                    () -> BlockEntities.ticking(TileEntityAshpit::new, TileEntityAshpit::clientTick, TileEntityAshpit::serverTick, ModMachineBlocks.machine_ashpit.get()));

    public static final RegistryObject<BlockEntityType<TileEntityHeaterOven>> HEATER_OVEN =
            REG.blockEntity("heater_oven",
                    () -> BlockEntities.ticking(TileEntityHeaterOven::new, TileEntityHeaterOven::clientTick, TileEntityHeaterOven::serverTick, ModMachineBlocks.heater_oven.get()));

    public static final RegistryObject<BlockEntityType<TileEntityHeaterElectric>> HEATER_ELECTRIC =
            REG.blockEntity("heater_electric",
                    () -> BlockEntities.ticking(TileEntityHeaterElectric::new, TileEntityHeaterElectric::clientTick, TileEntityHeaterElectric::serverTick, ModMachineBlocks.heater_electric.get()));

    public static final RegistryObject<BlockEntityType<TileEntityHeaterHeatex>> HEATER_HEATEX =
            REG.blockEntity("heater_heatex",
                    () -> BlockEntities.serverTicking(TileEntityHeaterHeatex::new, TileEntityHeaterHeatex::serverTick, ModMachineBlocks.heater_heatex.get()));

    public static final RegistryObject<BlockEntityType<TileEntityLaunchPad>> LAUNCH_PAD =
            REG.blockEntity("launch_pad",
                    () -> BlockEntities.serverTicking(TileEntityLaunchPad::new, TileEntityLaunchPad::serverTick, ModMachineBlocks.launch_pad.get()));
    public static final RegistryObject<BlockEntityType<TileEntityLaunchpadSoyuz>> LAUNCHPAD_SOYUZ =
            REG.blockEntity("launchpad_soyuz", () -> BlockEntities.ticking(TileEntityLaunchpadSoyuz::new,
                    TileEntityLaunchpadSoyuz::clientTick, TileEntityLaunchpadSoyuz::serverTick, ModMachineBlocks.launchpad_soyuz.get()));
    public static final RegistryObject<BlockEntityType<TileEntityLaunchpadLambda>> LAUNCHPAD_LAMBDA =
            REG.blockEntity("launchpad_lambda", () -> BlockEntities.ticking(TileEntityLaunchpadLambda::new,
                    TileEntityLaunchpadLambda::clientTick, TileEntityLaunchpadLambda::serverTick, ModMachineBlocks.launchpad_lambda.get()));

    // Bombs

    public static final RegistryObject<BlockEntityType<TileEntityNukeMan>> NUKE_MAN =
            REG.blockEntity("nuke_man",
                    () -> BlockEntities.type(TileEntityNukeMan::new, ModBombBlocks.nuke_man.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeN2>> NUKE_N2 =
            REG.blockEntity("nuke_n2",
                    () -> BlockEntities.type(TileEntityNukeN2::new, ModBombBlocks.nuke_n2.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeBoy>> NUKE_BOY =
            REG.blockEntity("nuke_boy",
                    () -> BlockEntities.type(TileEntityNukeBoy::new, ModBombBlocks.nuke_boy.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeGadget>> NUKE_GADGET =
            REG.blockEntity("nuke_gadget",
                    () -> BlockEntities.type(TileEntityNukeGadget::new, ModBombBlocks.nuke_gadget.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeTsar>> NUKE_TSAR =
            REG.blockEntity("nuke_tsar",
                    () -> BlockEntities.type(TileEntityNukeTsar::new, ModBombBlocks.nuke_tsar.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeMike>> NUKE_MIKE =
            REG.blockEntity("nuke_mike",
                    () -> BlockEntities.type(TileEntityNukeMike::new,
                            ModBombBlocks.nuke_mike.get()));

    // The 12 tiered anvils (OBJ-rendered; the BE exists only for the renderer)

    public static final RegistryObject<BlockEntityType<TileEntityAnvil>> ANVIL =
            REG.blockEntity("anvil",
                    () -> BlockEntities.type(TileEntityAnvil::new, ModAnvilBlocks.ANVILS.stream()
                            .map(RegistryObject::get).toArray(Block[]::new)));

    // Decorative OBJ-rendered blocks (boxcar / boat / satellite dishes)

    public static final RegistryObject<BlockEntityType<TileEntityDeco>> DECO =
            REG.blockEntity("deco",
                    () -> BlockEntities.type(TileEntityDeco::new, Stream.concat(
                            ModDecoBlocks.OBJ_BLOCKS.stream(), ModSteelBlocks.OBJ_BLOCKS.stream())
                            .map(RegistryObject::get).toArray(Block[]::new)));

    /** plant_reeds: exists only so RenderReeds can draw the stem down the whole water column. */
    public static final RegistryObject<BlockEntityType<TileEntityReeds>> REEDS =
            REG.blockEntity("reeds",
                    () -> BlockEntities.type(TileEntityReeds::new,
                            ModTerrainBlocks.plant_reeds.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFireworks>> FIREWORKS =
            REG.blockEntity("fireworks",
                    () -> BlockEntities.serverTicking(TileEntityFireworks::new,
                            (level, pos, state, be) -> be.serverTick(),
                            ModBombBlocks.fireworks.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCyberCrab>> METEOR_SPAWNER =
            REG.blockEntity("meteor_spawner",
                    () -> BlockEntities.serverTicking(TileEntityCyberCrab::new,
                            (level, pos, state, be) -> be.serverTick(),
                            ModBlocks.meteor_spawner.get()));

    // Redstone over Radio
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchSender>> RADIO_TORCH_SENDER =
            REG.blockEntity("radio_torch_sender", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchSender::new,
                    (level, pos, state, be) -> be.serverTick(), ModMachineBlocks.radio_torch_sender.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchReceiver>> RADIO_TORCH_RECEIVER =
            REG.blockEntity("radio_torch_receiver", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchReceiver::new,
                    (level, pos, state, be) -> be.serverTick(), ModMachineBlocks.radio_torch_receiver.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchReader>> RADIO_TORCH_READER =
            REG.blockEntity("radio_torch_reader", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchReader::new,
                    (level, pos, state, be) -> be.serverTick(), ModMachineBlocks.radio_torch_reader.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchLogic>> RADIO_TORCH_LOGIC =
            REG.blockEntity("radio_torch_logic", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchLogic::new,
                    (level, pos, state, be) -> be.serverTick(), ModMachineBlocks.radio_torch_logic.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchCounter>> RADIO_TORCH_COUNTER =
            REG.blockEntity("radio_torch_counter", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchCounter::new,
                    (level, pos, state, be) -> be.serverTick(), ModMachineBlocks.radio_torch_counter.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTorchController>> RADIO_TORCH_CONTROLLER =
            REG.blockEntity("radio_torch_controller", () -> BlockEntities.serverTicking(
                    TileEntityRadioTorchController::new,
                    (level, pos, state, be) -> be.serverTick(), ModMachineBlocks.radio_torch_controller.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioTelex>> RADIO_TELEX =
            REG.blockEntity("radio_telex", () -> BlockEntities.serverTicking(
                    TileEntityRadioTelex::new,
                    (level, pos, state, be) -> be.serverTick(), ModMachineBlocks.radio_telex.get()));
    public static final RegistryObject<BlockEntityType<TileEntityRadioAUTOCAL>> RADIO_AUTOCAL =
            REG.blockEntity("radio_autocal", () -> BlockEntities.serverTicking(
                    TileEntityRadioAUTOCAL::new,
                    (level, pos, state, be) -> be.serverTick(), ModMachineBlocks.radio_autocal.get()));

    private static RegistryObject<BlockEntityType<TileEntityMassStorage>> massStorage(
            MassStorageType type,
            RegistryObject<? extends Block> block) {
        return REG.blockEntity(type.id, () -> BlockEntities.serverTicking(
                (pos, state) -> new TileEntityMassStorage(type, pos, state), TileEntityMassStorage::serverTick, block.get()));
    }

    private static TileEntityMachineIntake newIntake(
            BlockPos pos, BlockState state) {
        return new TileEntityMachineIntake(MACHINE_INTAKE.get(), pos, state);
    }

    private static MachineFan.TileEntityFan newFan(
            BlockPos pos, BlockState state) {
        return new MachineFan.TileEntityFan(MACHINE_FAN.get(), pos, state);
    }

    public static void init() {}
}
