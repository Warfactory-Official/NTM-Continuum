package com.hbm.inventory;

import com.hbm.Constants;
import com.hbm.inventory.container.ContainerBatterySocket;
import com.hbm.inventory.container.ContainerCrate;
import com.hbm.inventory.container.ContainerMachineAssemblyMachine;
import com.hbm.inventory.container.ContainerMachineBattery;
import com.hbm.inventory.container.ContainerMachineChemicalPlant;
import com.hbm.inventory.container.ContainerMachineElectricFurnace;
import com.hbm.inventory.container.ContainerMachineEPress;
import com.hbm.inventory.container.ContainerMachinePress;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.container.ContainerNukeMan;
import com.hbm.platform.Services;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

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

    public static final RegistryObject<MenuType<ContainerNukeMan>> NUKE_MAN =
            MENUS.register("nuke_man", () -> Services.PLATFORM.menuTypeWithPos(ContainerNukeMan::new));

    public static final RegistryObject<MenuType<ContainerMachinePress>> MACHINE_PRESS =
            MENUS.register("machine_press", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachinePress::new));

    public static final RegistryObject<MenuType<ContainerMachineEPress>> MACHINE_EPRESS =
            MENUS.register("machine_epress", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineEPress::new));

    public static final RegistryObject<MenuType<ContainerMachineElectricFurnace>> MACHINE_ELECTRIC_FURNACE =
            MENUS.register("machine_electric_furnace", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineElectricFurnace::new));

    public static final RegistryObject<MenuType<ContainerMachineFluidTank>> FLUID_TANK =
            MENUS.register("machine_fluidtank", () -> Services.PLATFORM.menuTypeWithPos(ContainerMachineFluidTank::new));

    public static final RegistryObject<MenuType<ContainerBatterySocket>> BATTERY_SOCKET =
            MENUS.register("machine_battery_socket", () -> Services.PLATFORM.menuTypeWithPos(ContainerBatterySocket::new));

    public static final RegistryObject<MenuType<ContainerCrate>> CRATE =
            MENUS.register("crate", () -> Services.PLATFORM.menuTypeWithPos(ContainerCrate::new));

    public static void init() {}
}
