package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerBatterySocket;
import com.hbm.inventory.container.ContainerCrate;
import com.hbm.inventory.container.ContainerMachineAssemblyMachine;
import com.hbm.inventory.container.ContainerMachineBattery;
import com.hbm.inventory.container.ContainerCrystallizer;
import com.hbm.inventory.container.ContainerMachineChemicalPlant;
import com.hbm.inventory.container.ContainerMachineElectricFurnace;
import com.hbm.inventory.container.ContainerMachineEPress;
import com.hbm.inventory.container.ContainerMachinePress;
import com.hbm.inventory.container.ContainerMachineShredder;
import com.hbm.inventory.container.ContainerMachineSolderingStation;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.container.ContainerNukeMan;
import com.hbm.platform.Services;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Registry for HBM menu types. Menu types are created via the platform service so each loader can
 * carry the {@link net.minecraft.core.BlockPos} open-data in its own way. */
public final class ModMenus {

    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.MOD_ID);

    public static final RegistryObject<MenuType<ContainerMachineBattery>> MACHINE_BATTERY =
            MENUS.register("machine_battery", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineBattery::new));

    public static final RegistryObject<MenuType<ContainerMachineAssemblyMachine>> MACHINE_ASSEMBLY_MACHINE =
            MENUS.register("machine_assembly_machine", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineAssemblyMachine::new));

    public static final RegistryObject<MenuType<ContainerMachineChemicalPlant>> MACHINE_CHEMICAL_PLANT =
            MENUS.register("machine_chemical_plant", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineChemicalPlant::new));

    public static final RegistryObject<MenuType<ContainerMachineSolderingStation>> MACHINE_SOLDERING_STATION =
            MENUS.register("machine_soldering_station", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineSolderingStation::new));

    public static final RegistryObject<MenuType<ContainerCrystallizer>> MACHINE_CRYSTALLIZER =
            MENUS.register("machine_crystallizer", () -> Services.PLATFORM.menuTypeWithPos(ContainerCrystallizer::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineDiesel>> MACHINE_DIESEL =
            MENUS.register("machine_diesel", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineDiesel::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineGasFlare>> MACHINE_GAS_FLARE =
            MENUS.register("machine_flare", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineGasFlare::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerLiquefactor>> MACHINE_LIQUEFACTOR =
            MENUS.register("machine_liquefactor", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerLiquefactor::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerSolidifier>> MACHINE_SOLIDIFIER =
            MENUS.register("machine_solidifier", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerSolidifier::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineRefinery>> MACHINE_REFINERY =
            MENUS.register("machine_refinery", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineRefinery::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerOilburner>> HEATER_OILBURNER =
            MENUS.register("heater_oilburner", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerOilburner::new));

    /** Shared by the oil derrick and the pumpjack (both bind {@code TileEntityOilDrillBase}). */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineOilWell>> MACHINE_OIL_WELL =
            MENUS.register("machine_oil_well", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineOilWell::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineMiningLaser>> MACHINE_MINING_LASER =
            MENUS.register("machine_mining_laser", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineMiningLaser::new));

    // --- advanced oil wave ---

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineVacuumDistill>> MACHINE_VACUUM_DISTILL =
            MENUS.register("machine_vacuum_distill", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineVacuumDistill::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineCatalyticReformer>> MACHINE_CATALYTIC_REFORMER =
            MENUS.register("machine_catalytic_reformer", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineCatalyticReformer::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineHydrotreater>> MACHINE_HYDROTREATER =
            MENUS.register("machine_hydrotreater", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineHydrotreater::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerMachineCoker>> MACHINE_COKER =
            MENUS.register("machine_coker", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerMachineCoker::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerPyroOven>> MACHINE_PYROOVEN =
            MENUS.register("machine_pyrooven", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerPyroOven::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCombustionEngine>> MACHINE_COMBUSTION_ENGINE =
            MENUS.register("machine_combustion_engine", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerCombustionEngine::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCompressor>> MACHINE_COMPRESSOR =
            MENUS.register("machine_compressor", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerCompressor::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerCrucible>> MACHINE_CRUCIBLE =
            MENUS.register("machine_crucible", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerCrucible::new));

    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerFirebox>> HEATER_FIREBOX =
            MENUS.register("heater_firebox", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerFirebox::new));

    public static final RegistryObject<MenuType<ContainerNukeMan>> NUKE_MAN =
            MENUS.register("nuke_man", () -> Services.PLATFORM.menuTypeWithPos(ContainerNukeMan::new));

    public static final RegistryObject<MenuType<ContainerMachinePress>> MACHINE_PRESS =
            MENUS.register("machine_press", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachinePress::new));

    public static final RegistryObject<MenuType<ContainerMachineEPress>> MACHINE_EPRESS =
            MENUS.register("machine_epress", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineEPress::new));

    public static final RegistryObject<MenuType<ContainerMachineElectricFurnace>> MACHINE_ELECTRIC_FURNACE =
            MENUS.register("machine_electric_furnace", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineElectricFurnace::new));

    public static final RegistryObject<MenuType<ContainerMachineShredder>> MACHINE_SHREDDER =
            MENUS.register("machine_shredder", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineShredder::new));

    public static final RegistryObject<MenuType<ContainerMachineFluidTank>> FLUID_TANK =
            MENUS.register("machine_fluidtank", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineFluidTank::new));

    public static final RegistryObject<MenuType<ContainerBatterySocket>> BATTERY_SOCKET =
            MENUS.register("machine_battery_socket", () -> Services.PLATFORM.menuTypeWithPos(ContainerBatterySocket::new));

    /** Shared by all 12 anvils; the container re-derives the tier from the block state at the pos. */
    public static final RegistryObject<MenuType<com.hbm.inventory.container.ContainerAnvil>> ANVIL =
            MENUS.register("anvil", () -> Services.PLATFORM.menuTypeWithPos(com.hbm.inventory.container.ContainerAnvil::new));

    /** Shared by all three storage crates; the container reads the BlockEntity's {@code CrateType} for layout. */
    public static final RegistryObject<MenuType<ContainerCrate>> CRATE =
            MENUS.register("crate", () -> Services.PLATFORM.menuTypeWithPos(ContainerCrate::new));

    public static void init() {}
}
