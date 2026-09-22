package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import com.hbm.blocks.machine.MachineAutocrafter;
import com.hbm.blocks.machine.MachineConveyorPress;
import com.hbm.blocks.machine.MachineMicrowave;
import com.hbm.blocks.machine.MachineRotaryFurnace;
import com.hbm.blocks.machine.MachineThresher;
import com.hbm.blocks.machine.MachineFunnel;
import com.hbm.items.block.ItemBlockProductionDesc;

/** Feature-local block and block-item registry for the production machines (group A). */
public final class ModProductionBlocks {
    private ModProductionBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7: {@code Material.iron}, hardness 5, resistance 10, machineTab (the autocrafter is 10/20). */
    public static final RegistryObject<MachineRotaryFurnace> MACHINE_ROTARY_FURNACE = BLOCKS.registerBlock(
            "machine_rotary_furnace", MachineRotaryFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final RegistryObject<MachineThresher> MACHINE_THRESHER = BLOCKS.registerBlock(
            "machine_thresher", MachineThresher::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final RegistryObject<MachineMicrowave> MACHINE_MICROWAVE = BLOCKS.registerBlock(
            "machine_microwave", MachineMicrowave::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final RegistryObject<MachineAutocrafter> MACHINE_AUTOCRAFTER = BLOCKS.registerBlock(
            "machine_autocrafter", MachineAutocrafter::new,
            p -> p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()); // 1.7 ModBlocks:2246 hardness 10 / resistance 20
    public static final RegistryObject<MachineConveyorPress> MACHINE_CONVEYOR_PRESS = BLOCKS.registerBlock(
            "machine_conveyor_press", MachineConveyorPress::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7: a plain {@code BlockBase(Material.iron)} whose only job is to be recognised by the burner press. */
    public static final RegistryObject<Block> PRESS_PREHEATER = BLOCKS.registerBlock(
            "press_preheater", dev.golem.core.world.CompatBlock::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final RegistryObject<MachineFunnel> MACHINE_FUNNEL = BLOCKS.registerBlock(
            "machine_funnel", MachineFunnel::new,
            p -> p.strength(10.0F, 20.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

    public static final RegistryObject<BlockItem> MACHINE_ROTARY_FURNACE_ITEM = ITEMS.registerBlockItem(
            "machine_rotary_furnace", MACHINE_ROTARY_FURNACE, Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_THRESHER_ITEM = ITEMS.registerItem(
            "machine_thresher", p -> new ItemBlockProductionDesc(MACHINE_THRESHER.get(), p, 7), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_MICROWAVE_ITEM = ITEMS.registerBlockItem(
            "machine_microwave", MACHINE_MICROWAVE, Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_AUTOCRAFTER_ITEM = ITEMS.registerItem(
            "machine_autocrafter", p -> new BlockItem(MACHINE_AUTOCRAFTER.get(), p), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_CONVEYOR_PRESS_ITEM = ITEMS.registerItem(
            "machine_conveyor_press", p -> new ItemBlockProductionDesc(MACHINE_CONVEYOR_PRESS.get(), p, 3), Compat::blockDescription);
    public static final RegistryObject<BlockItem> PRESS_PREHEATER_ITEM = ITEMS.registerItem(
            "press_preheater", p -> new BlockItem(PRESS_PREHEATER.get(), p), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_FUNNEL_ITEM = ITEMS.registerBlockItem(
            "machine_funnel", MACHINE_FUNNEL, Compat::blockDescription);

    static {
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_ROTARY_FURNACE_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_THRESHER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_MICROWAVE_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_AUTOCRAFTER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_CONVEYOR_PRESS_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, PRESS_PREHEATER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_FUNNEL_ITEM);
    }

    public static void init() {}
}
