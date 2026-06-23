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
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import com.hbm.tileentity.bomb.TileEntityNukeMan;
import com.hbm.tileentity.machine.TileEntityCreativeFluidSource;
import com.hbm.tileentity.machine.TileEntityCreativeGenerator;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
import com.hbm.tileentity.machine.TileEntityMachineEPress;
import com.hbm.tileentity.machine.TileEntityMachinePress;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import com.hbm.tileentity.network.TileEntityCableBaseNT;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityMachineBattery>> MACHINE_BATTERY =
            BLOCK_ENTITIES.register("machine_battery",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineBattery::new, ModMachineBlocks.machine_battery.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCableBaseNT>> RED_CABLE =
            BLOCK_ENTITIES.register("red_cable",
                    () -> Services.PLATFORM.blockEntityType(TileEntityCableBaseNT::new, ModMachineBlocks.red_cable.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeGenerator>> CREATIVE_GENERATOR =
            BLOCK_ENTITIES.register("creative_generator",
                    () -> Services.PLATFORM.blockEntityType(TileEntityCreativeGenerator::new, ModMachineBlocks.creative_generator.get()));

    public static final RegistryObject<BlockEntityType<TileEntityFluidDuctNT>> FLUID_DUCT =
            BLOCK_ENTITIES.register("fluid_duct_mk2",
                    () -> Services.PLATFORM.blockEntityType(TileEntityFluidDuctNT::new, ModMachineBlocks.fluid_duct_mk2.get()));

    public static final RegistryObject<BlockEntityType<TileEntityCreativeFluidSource>> CREATIVE_FLUID_SOURCE =
            BLOCK_ENTITIES.register("creative_fluid_source",
                    () -> Services.PLATFORM.blockEntityType(TileEntityCreativeFluidSource::new, ModMachineBlocks.creative_fluid_source.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineFluidTank>> FLUID_TANK =
            BLOCK_ENTITIES.register("machine_fluidtank",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineFluidTank::new, ModMachineBlocks.machine_fluidtank.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBatterySocket>> BATTERY_SOCKET =
            BLOCK_ENTITIES.register("machine_battery_socket",
                    () -> Services.PLATFORM.blockEntityType(TileEntityBatterySocket::new, ModBatteryBlocks.machine_battery_socket.get()));

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

    public static final RegistryObject<BlockEntityType<TileEntityMachineElectricFurnace>> MACHINE_ELECTRIC_FURNACE =
            BLOCK_ENTITIES.register("machine_electric_furnace",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineElectricFurnace::new, ModMachineBlocks.machine_electric_furnace_off.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineEPress>> MACHINE_EPRESS =
            BLOCK_ENTITIES.register("machine_epress",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineEPress::new, ModMachineBlocks.machine_epress.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachinePress>> MACHINE_PRESS =
            BLOCK_ENTITIES.register("machine_press",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachinePress::new, ModMachineBlocks.machine_press.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineAssemblyMachine>> MACHINE_ASSEMBLY_MACHINE =
            BLOCK_ENTITIES.register("machine_assembly_machine",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineAssemblyMachine::new, ModMachineBlocks.machine_assembly_machine.get()));

    public static final RegistryObject<BlockEntityType<TileEntityMachineChemicalPlant>> MACHINE_CHEMICAL_PLANT =
            BLOCK_ENTITIES.register("machine_chemical_plant",
                    () -> Services.PLATFORM.blockEntityType(TileEntityMachineChemicalPlant::new, ModMachineBlocks.machine_chemical_plant.get()));

    public static final RegistryObject<BlockEntityType<TileEntityLaunchPad>> LAUNCH_PAD =
            BLOCK_ENTITIES.register("launch_pad",
                    () -> Services.PLATFORM.blockEntityType(TileEntityLaunchPad::new, ModMachineBlocks.launch_pad.get()));

    public static final RegistryObject<BlockEntityType<TileEntityNukeMan>> NUKE_MAN =
            BLOCK_ENTITIES.register("nuke_man",
                    () -> Services.PLATFORM.blockEntityType(TileEntityNukeMan::new, ModBombBlocks.nuke_man.get()));

    public static void init() {}
}
