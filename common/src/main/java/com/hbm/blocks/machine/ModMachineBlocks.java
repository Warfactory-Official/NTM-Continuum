package com.hbm.blocks.machine;

import com.hbm.Constants;
import com.hbm.blocks.network.BlockCable;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public final class ModMachineBlocks {

    private ModMachineBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BlockMachineBattery> machine_battery = BLOCKS.registerBlock(
            "machine_battery", BlockMachineBattery::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<BlockCable> red_cable = BLOCKS.registerBlock(
            "red_cable", BlockCable::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion());

    public static final RegistryObject<BlockCreativeGenerator> creative_generator = BLOCKS.registerBlock(
            "creative_generator", BlockCreativeGenerator::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL));

    public static final RegistryObject<BlockFluidDuct> fluid_duct_mk2 = BLOCKS.registerBlock(
            "fluid_duct_mk2", BlockFluidDuct::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<BlockCreativeFluidSource> creative_fluid_source = BLOCKS.registerBlock(
            "creative_fluid_source", BlockCreativeFluidSource::new,
            p -> p.strength(2.0F, 4.0F).sound(SoundType.METAL));

    public static final RegistryObject<MachineFluidTank> machine_fluidtank = BLOCKS.registerBlock(
            "machine_fluidtank", MachineFluidTank::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<MachineElectricFurnace> machine_electric_furnace_off = BLOCKS.registerBlock(
            "machine_electric_furnace_off", MachineElectricFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static final RegistryObject<MachineEPress> machine_epress = BLOCKS.registerBlock(
            "machine_epress", MachineEPress::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<MachinePress> machine_press = BLOCKS.registerBlock(
            "machine_press", MachinePress::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<MachineAssemblyMachine> machine_assembly_machine = BLOCKS.registerBlock(
            "machine_assembly_machine", MachineAssemblyMachine::new,
            p -> p.strength(5.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<MachineChemicalPlant> machine_chemical_plant = BLOCKS.registerBlock(
            "machine_chemical_plant", MachineChemicalPlant::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<LaunchPad> launch_pad = BLOCKS.registerBlock(
            "launch_pad", LaunchPad::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    static {
        tabItem("machine_battery", machine_battery);
        tabItem("red_cable", red_cable);
        tabItem("creative_generator", creative_generator);
        tabItem("fluid_duct_mk2", fluid_duct_mk2);
        tabItem("creative_fluid_source", creative_fluid_source);
        tabItem("machine_fluidtank", machine_fluidtank);
        tabItem("machine_electric_furnace_off", machine_electric_furnace_off);
        tabItem("machine_epress", machine_epress);
        tabItem("machine_press", machine_press);
        tabItem("machine_assembly_machine", machine_assembly_machine);
        tabItem("machine_chemical_plant", machine_chemical_plant);
        tabItem(HbmTab.MISSILES, "launch_pad", launch_pad);
    }

    private static void tabItem(String name, RegistryObject<? extends Block> block) {
        tabItem(HbmTab.MACHINES, name, block);
    }

    private static void tabItem(HbmTab tab, String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block);
        ModTabs.addTo(tab, item);
    }

    public static void init() {}
}
