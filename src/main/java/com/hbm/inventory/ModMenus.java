package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerBatterySocket;
import com.hbm.inventory.container.ContainerMachineFENSU;
import com.hbm.inventory.container.ContainerBatteryREDD;
import com.hbm.inventory.container.ContainerCrate;
import com.hbm.inventory.container.ContainerMachineAssemblyMachine;
import com.hbm.inventory.container.ContainerMachinePrecAss;
import com.hbm.inventory.container.ContainerMachineBattery;
import com.hbm.inventory.container.ContainerCrystallizer;
import com.hbm.inventory.container.ContainerCentrifuge;
import com.hbm.inventory.container.ContainerMachineCyclotron;
import com.hbm.inventory.container.ContainerMachineGasCent;
import com.hbm.inventory.container.ContainerMachineElectrolyser;
import com.hbm.inventory.container.ContainerMachineChemicalPlant;
import com.hbm.inventory.container.ContainerMixer;
import com.hbm.inventory.container.ContainerMachineExposureChamber;
import com.hbm.inventory.container.ContainerBlastFurnace;
import com.hbm.inventory.container.ContainerMachinePUREX;
import com.hbm.inventory.container.ContainerMachineElectricFurnace;
import com.hbm.inventory.container.ContainerFurnaceBrick;
import com.hbm.inventory.container.ContainerFurnaceIron;
import com.hbm.inventory.container.ContainerFurnaceSteel;
import com.hbm.inventory.container.ContainerMachineEPress;
import com.hbm.inventory.container.ContainerMachinePress;
import com.hbm.inventory.container.ContainerMachineAmmoPress;
import com.hbm.inventory.container.ContainerMachineShredder;
import com.hbm.inventory.container.ContainerMachineSolderingStation;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.container.ContainerMachineF6Tank;
import com.hbm.inventory.container.ContainerMachineGiantTank;
import com.hbm.inventory.container.ContainerNukeMan;
import com.hbm.inventory.container.ContainerNukeN2;
import com.hbm.inventory.container.ContainerNukeBoy;
import com.hbm.inventory.container.ContainerNukeGadget;
import com.hbm.inventory.container.ContainerNukeTsar;
import com.hbm.inventory.container.ContainerNukeMike;
import com.hbm.inventory.container.ContainerWasteDrum;
import com.hbm.inventory.container.ContainerArmorTable;
import com.hbm.inventory.container.ContainerWeaponTable;
import com.hbm.inventory.container.ContainerStorageDrum;
import com.hbm.inventory.container.ContainerMachineRadGen;
import com.hbm.inventory.container.ContainerReactorZirnox;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.hbm.registration.Reg;

/** Registry for HBM menu types. Menu types are created via the platform service so each loader can
 * carry the {@link net.minecraft.core.BlockPos} open-data in its own way. */
public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);
    private static final Reg REG = Reg.of(MENUS);

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerPASource>> PA_SOURCE =
            REG.menu("pa_source", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerPASource::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerPARFC>> PA_RFC =
            REG.menu("pa_rfc", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerPARFC::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerPAQuadrupole>> PA_QUADRUPOLE =
            REG.menu("pa_quadrupole", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerPAQuadrupole::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerPADipole>> PA_DIPOLE =
            REG.menu("pa_dipole", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerPADipole::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerPADetector>> PA_DETECTOR =
            REG.menu("pa_detector", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerPADetector::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCore>> DFC_CORE =
            REG.menu("dfc_core", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerCore::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCoreEmitter>> DFC_EMITTER =
            REG.menu("dfc_emitter", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerCoreEmitter::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCoreReceiver>> DFC_RECEIVER =
            REG.menu("dfc_receiver", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerCoreReceiver::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCoreInjector>> DFC_INJECTOR =
            REG.menu("dfc_injector", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerCoreInjector::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCoreStabilizer>> DFC_STABILIZER =
            REG.menu("dfc_stabilizer", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerCoreStabilizer::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerSoyuzLauncher>> SOYUZ_LAUNCHER =
            REG.menu("soyuz_launcher", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerSoyuzLauncher::new));


    public static final RegistryObject<MenuType<ContainerMachineBattery>> MACHINE_BATTERY =
            REG.menu("machine_battery", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineBattery::new));

    public static final RegistryObject<MenuType<ContainerWasteDrum>> MACHINE_WASTE_DRUM =
            REG.menu("machine_waste_drum", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerWasteDrum::new));
    public static final RegistryObject<MenuType<ContainerStorageDrum>> MACHINE_STORAGE_DRUM =
            REG.menu("machine_storage_drum", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerStorageDrum::new));

    public static final RegistryObject<MenuType<ContainerMachineRadGen>> MACHINE_RAD_GEN =
            REG.menu("machine_radgen", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineRadGen::new));

    public static final RegistryObject<MenuType<ContainerMachineAssemblyMachine>> MACHINE_ASSEMBLY_MACHINE =
            REG.menu("machine_assembly_machine", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineAssemblyMachine::new));
    public static final RegistryObject<MenuType<ContainerMachinePrecAss>> MACHINE_PREC_ASS =
            REG.menu("machine_precass", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachinePrecAss::new));

    public static final RegistryObject<MenuType<ContainerMachineChemicalPlant>> MACHINE_CHEMICAL_PLANT =
            REG.menu("machine_chemical_plant", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineChemicalPlant::new));

    public static final RegistryObject<MenuType<ContainerMixer>> MACHINE_MIXER =
            REG.menu("machine_mixer", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMixer::new));

    public static final RegistryObject<MenuType<ContainerMachineExposureChamber>> MACHINE_EXPOSURE_CHAMBER =
            REG.menu("machine_exposure_chamber",
                    () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineExposureChamber::new));

    public static final RegistryObject<MenuType<ContainerBlastFurnace>> MACHINE_BLAST_FURNACE =
            REG.menu("machine_blast_furnace", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerBlastFurnace::new));

    public static final RegistryObject<MenuType<ContainerMachinePUREX>> MACHINE_PUREX =
            REG.menu("machine_purex", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachinePUREX::new));

    public static final RegistryObject<MenuType<ContainerMachineSolderingStation>> MACHINE_SOLDERING_STATION =
            REG.menu("machine_soldering_station", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineSolderingStation::new));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineArcWelder>> MACHINE_ARC_WELDER =
            REG.menu("machine_arc_welder", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineArcWelder::new));

    public static final RegistryObject<MenuType<ContainerCrystallizer>> MACHINE_CRYSTALLIZER =
            REG.menu("machine_crystallizer", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerCrystallizer::new));

    public static final RegistryObject<MenuType<ContainerCentrifuge>> MACHINE_CENTRIFUGE =
            REG.menu("machine_centrifuge", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerCentrifuge::new));

    public static final RegistryObject<MenuType<ContainerMachineCyclotron>> MACHINE_CYCLOTRON =
            REG.menu("machine_cyclotron", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineCyclotron::new));

    public static final RegistryObject<MenuType<ContainerMachineGasCent>> MACHINE_GAS_CENT =
            REG.menu("machine_gascent", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineGasCent::new));

    public static final RegistryObject<MenuType<ContainerMachineElectrolyser>> MACHINE_ELECTROLYSER_FLUID =
            REG.menu("machine_electrolyser_fluid", () -> dev.golem.core.gui.Menus.typeWithPos(
                    (id, inventory, pos) -> new ContainerMachineElectrolyser(id, inventory, pos, false)));
    public static final RegistryObject<MenuType<ContainerMachineElectrolyser>> MACHINE_ELECTROLYSER_METAL =
            REG.menu("machine_electrolyser_metal", () -> dev.golem.core.gui.Menus.typeWithPos(
                    (id, inventory, pos) -> new ContainerMachineElectrolyser(id, inventory, pos, true)));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerSILEX>> MACHINE_SILEX =
            REG.menu("machine_silex", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerSILEX::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerFEL>> MACHINE_FEL =
            REG.menu("machine_fel", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerFEL::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineTurbine>> MACHINE_TURBINE =
            REG.menu("machine_turbine", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineTurbine::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineLargeTurbine>> MACHINE_LARGE_TURBINE =
            REG.menu("machine_large_turbine", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineLargeTurbine::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineTurbineGas>> MACHINE_TURBINE_GAS =
            REG.menu("machine_turbine_gas", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineTurbineGas::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineTurbofan>> MACHINE_TURBOFAN =
            REG.menu("machine_turbofan", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineTurbofan::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineDiesel>> MACHINE_DIESEL =
            REG.menu("machine_diesel", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineDiesel::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineGasFlare>> MACHINE_GAS_FLARE =
            REG.menu("machine_flare", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineGasFlare::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerLiquefactor>> MACHINE_LIQUEFACTOR =
            REG.menu("machine_liquefactor", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerLiquefactor::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerSolidifier>> MACHINE_SOLIDIFIER =
            REG.menu("machine_solidifier", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerSolidifier::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineRefinery>> MACHINE_REFINERY =
            REG.menu("machine_refinery", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineRefinery::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerOilburner>> HEATER_OILBURNER =
            REG.menu("heater_oilburner", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerOilburner::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineWoodBurner>> MACHINE_WOOD_BURNER =
            REG.menu("machine_wood_burner", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineWoodBurner::new));

    /** Shared by the oil derrick and the pumpjack (both bind {@code TileEntityOilDrillBase}). */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineOilWell>> MACHINE_OIL_WELL =
            REG.menu("machine_oil_well", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineOilWell::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineMiningLaser>> MACHINE_MINING_LASER =
            REG.menu("machine_mining_laser", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineMiningLaser::new));

    // --- advanced oil wave ---

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineVacuumDistill>> MACHINE_VACUUM_DISTILL =
            REG.menu("machine_vacuum_distill", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineVacuumDistill::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineCatalyticReformer>> MACHINE_CATALYTIC_REFORMER =
            REG.menu("machine_catalytic_reformer", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineCatalyticReformer::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineHydrotreater>> MACHINE_HYDROTREATER =
            REG.menu("machine_hydrotreater", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineHydrotreater::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineCoker>> MACHINE_COKER =
            REG.menu("machine_coker", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachineCoker::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerFurnaceCombination>> FURNACE_COMBINATION =
            REG.menu("machine_furnace_combination", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerFurnaceCombination::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerPyroOven>> MACHINE_PYROOVEN =
            REG.menu("machine_pyrooven", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerPyroOven::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCombustionEngine>> MACHINE_COMBUSTION_ENGINE =
            REG.menu("machine_combustion_engine", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerCombustionEngine::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCompressor>> MACHINE_COMPRESSOR =
            REG.menu("machine_compressor", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerCompressor::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCrucible>> MACHINE_CRUCIBLE =
            REG.menu("machine_crucible", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerCrucible::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerFirebox>> HEATER_FIREBOX =
            REG.menu("heater_firebox", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerFirebox::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerHeaterHeatex>> HEATER_HEATEX =
            REG.menu("heater_heatex", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerHeaterHeatex::new));

    public static final RegistryObject<MenuType<ContainerNukeMan>> NUKE_MAN =
            REG.menu("nuke_man", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerNukeMan::new));

    public static final RegistryObject<MenuType<ContainerNukeN2>> NUKE_N2 =
            REG.menu("nuke_n2", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerNukeN2::new));
    public static final RegistryObject<MenuType<ContainerNukeBoy>> NUKE_BOY =
            REG.menu("nuke_boy", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerNukeBoy::new));
    public static final RegistryObject<MenuType<ContainerNukeGadget>> NUKE_GADGET =
            REG.menu("nuke_gadget", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerNukeGadget::new));
    public static final RegistryObject<MenuType<ContainerNukeTsar>> NUKE_TSAR =
            REG.menu("nuke_tsar", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerNukeTsar::new));
    public static final RegistryObject<MenuType<ContainerNukeMike>> NUKE_MIKE =
            REG.menu("nuke_mike", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerNukeMike::new));

    public static final RegistryObject<MenuType<ContainerReactorZirnox>> REACTOR_ZIRNOX =
            REG.menu("reactor_zirnox", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerReactorZirnox::new));

    public static final RegistryObject<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerWatz>> WATZ =
            REG.menu("watz", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerWatz::new));

    public static final RegistryObject<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerFusionTorus>> FUSION_TORUS =
            REG.menu("fusion_torus", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerFusionTorus::new));


    public static final RegistryObject<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerFusionBreeder>> FUSION_BREEDER =
            REG.menu("fusion_breeder", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerFusionBreeder::new));

    public static final RegistryObject<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerFusionKlystron>> FUSION_KLYSTRON =
            REG.menu("fusion_klystron", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerFusionKlystron::new));
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerMachinePlasmaForge>> FUSION_PLASMA_FORGE =
            REG.menu("fusion_plasma_forge", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerMachinePlasmaForge::new));

    public static final RegistryObject<MenuType<ContainerMachinePress>> MACHINE_PRESS =
            REG.menu("machine_press", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachinePress::new));

    public static final RegistryObject<MenuType<ContainerMachineAmmoPress>> MACHINE_AMMO_PRESS =
            REG.menu("machine_ammo_press", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineAmmoPress::new));

    public static final RegistryObject<MenuType<ContainerMachineEPress>> MACHINE_EPRESS =
            REG.menu("machine_epress", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineEPress::new));

    public static final RegistryObject<MenuType<ContainerMachineElectricFurnace>> MACHINE_ELECTRIC_FURNACE =
            REG.menu("machine_electric_furnace", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineElectricFurnace::new));

    public static final RegistryObject<MenuType<ContainerFurnaceBrick>> FURNACE_BRICK =
            REG.menu("machine_furnace_brick_off", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerFurnaceBrick::new));
    public static final RegistryObject<MenuType<ContainerFurnaceIron>> FURNACE_IRON =
            REG.menu("furnace_iron", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerFurnaceIron::new));
    public static final RegistryObject<MenuType<ContainerFurnaceSteel>> FURNACE_STEEL =
            REG.menu("furnace_steel", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerFurnaceSteel::new));

    public static final RegistryObject<MenuType<ContainerMachineShredder>> MACHINE_SHREDDER =
            REG.menu("machine_shredder", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineShredder::new));


    public static final RegistryObject<MenuType<ContainerMachineFluidTank>> FLUID_TANK =
            REG.menu("machine_fluidtank", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineFluidTank::new));

    public static final RegistryObject<MenuType<ContainerMachineF6Tank>> F6_TANK =
            REG.menu("f6_tank", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineF6Tank::new));

    public static final RegistryObject<MenuType<ContainerMachineGiantTank>> GIANT_TANK =
            REG.menu("giant_tank", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineGiantTank::new));

    public static final RegistryObject<MenuType<ContainerBatterySocket>> BATTERY_SOCKET =
            REG.menu("machine_battery_socket", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerBatterySocket::new));
    public static final RegistryObject<MenuType<ContainerMachineFENSU>> FENSU =
            REG.menu("machine_fensu", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerMachineFENSU::new));
    public static final RegistryObject<MenuType<ContainerBatteryREDD>> BATTERY_REDD =
            REG.menu("machine_battery_redd", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerBatteryREDD::new));

    /** Shared by all 12 anvils; the container re-derives the tier from the block state at the pos. */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerAnvil>> ANVIL =
            REG.menu("anvil", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerAnvil::new));

    /** Shared by all three storage crates; the container reads the BlockEntity's {@code CrateType} for layout. */
    public static final RegistryObject<MenuType<ContainerCrate>> CRATE =
            REG.menu("crate", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerCrate::new));

    // --- the ntm-next entity port: vehicle menus opened through vanilla's data-less MenuProvider path ---
    /** The crate minecart (1.7 ContainerCrate on the cart's own inventory). */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.MenuCrate>> CART_CRATE =
            REG.menu("cart_crate", () -> new MenuType<>(com.hbm.inventory.container.MenuCrate::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    /** The destroyer minecart's two ghost filter grids (1.7 ContainerCartDestroyer). */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.MenuCartDestroyer>> CART_DESTROYER =
            REG.menu("cart_destroyer", () -> new MenuType<>(com.hbm.inventory.container.MenuCartDestroyer::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    /** The cargo tram engine: 28 cargo slots, a charge slot and the power gauge. */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.MenuTrainCargoTram>> TRAIN_CARGO_TRAM =
            REG.menu("train_cargo_tram", () -> new MenuType<>(com.hbm.inventory.container.MenuTrainCargoTram::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    /** The cargo tram trailer: cargo slots only. */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.MenuTrainCargoTramTrailer>> TRAIN_CARGO_TRAM_TRAILER =
            REG.menu("train_cargo_tram_trailer", () -> new MenuType<>(
                    com.hbm.inventory.container.MenuTrainCargoTramTrailer::new,
                    net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<ContainerArmorTable>> ARMOR_TABLE =
            REG.menu("armor_table", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerArmorTable::new));
    public static final RegistryObject<MenuType<ContainerWeaponTable>> WEAPON_TABLE =
            REG.menu("weapon_table", () -> dev.golem.core.gui.Menus.typeWithPos(ContainerWeaponTable::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMassStorage>> MASS_STORAGE =
            REG.menu("mass_storage", () -> dev.golem.core.gui.Menus.typeWithPos((id, inventory, pos) ->
                    new com.hbm.inventory.container.ContainerMassStorage(id, inventory, pos, ModMenus::massStorageMenu)));
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCrateTemplate>> CRATE_TEMPLATE =
            REG.menu("crate_template", () -> dev.golem.core.gui.Menus.typeWithPos((id, inventory, pos) ->
                    new com.hbm.inventory.container.ContainerCrateTemplate(id, inventory, pos, ModMenus::templateCrateMenu)));

    static {
        for (com.hbm.tileentity.machine.storage.MassStorageType type : com.hbm.tileentity.machine.storage.MassStorageType.values()) {
            type.setMenuTypeSupplier(MASS_STORAGE::get);
        }
        com.hbm.tileentity.machine.storage.TemplateCrateLayout.setMenuTypeSupplier(CRATE_TEMPLATE::get);
    }

    /** Shared by every launch pad type (only the small silo pad exists so far). */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerLaunchPad>> LAUNCH_PAD =
            REG.menu("launch_pad", () -> dev.golem.core.gui.Menus.typeWithPos(com.hbm.inventory.container.ContainerLaunchPad::new));

    /** Shared by every turret: the layout is identical, only the background texture differs. */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerTurretBase>> TURRET_BASE =
            REG.menu("turret_base", () -> dev.golem.core.gui.Menus.typeWithPos(
                    com.hbm.inventory.container.ContainerTurretBase::new));

    private static MenuType<com.hbm.inventory.container.ContainerMassStorage> massStorageMenu() {
        return MASS_STORAGE.get();
    }

    private static MenuType<com.hbm.inventory.container.ContainerCrateTemplate> templateCrateMenu() {
        return CRATE_TEMPLATE.get();
    }

    public static void init() {}
}
