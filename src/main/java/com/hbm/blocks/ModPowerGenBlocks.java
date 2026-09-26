package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import com.hbm.blocks.machine.MachineChungus;
import com.hbm.blocks.machine.MachineHephaestus;
import com.hbm.blocks.machine.MachineIGenerator;
import com.hbm.blocks.machine.MachineLPW2;
import com.hbm.blocks.machine.MachineSiren;
import com.hbm.items.block.PowerGenBlockItem;

/** Feature-local block and block-item registry for the large power-generation machines. */
public final class ModPowerGenBlocks {
    private ModPowerGenBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7: {@code Material.iron}, hardness 5, resistance 10, machineTab, {@code block_steel} icon. */
    public static final RegistryObject<MachineChungus> MACHINE_CHUNGUS = BLOCKS.registerBlock(
            "machine_chungus", MachineChungus::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7: hardness 5, resistance 100. */
    public static final RegistryObject<MachineLPW2> MACHINE_LPW2 = BLOCKS.registerBlock(
            "machine_lpw2", MachineLPW2::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7: hardness 10, resistance 10, {@code block_steel_machine} icon. */
    public static final RegistryObject<MachineHephaestus> MACHINE_HEPHAESTUS = BLOCKS.registerBlock(
            "machine_hephaestus", MachineHephaestus::new,
            p -> p.strength(10.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7: hardness 5, resistance 10, vanilla {@code gold_block} icon (never seen: the block is renderer-drawn). */
    public static final RegistryObject<MachineIGenerator> MACHINE_INDUSTRIAL_GENERATOR = BLOCKS.registerBlock(
            "machine_industrial_generator", MachineIGenerator::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7: {@code Material.iron}, hardness 5, resistance 10. */
    public static final RegistryObject<MachineSiren> MACHINE_SIREN = BLOCKS.registerBlock(
            "machine_siren", MachineSiren::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    /** 1.7 {@code ITooltipProvider.addStandardInfo}: {@code tile.machine_chungus.desc} = "Efficiency: 85%". */
    public static final RegistryObject<BlockItem> MACHINE_CHUNGUS_ITEM = ITEMS.registerItem(
            "machine_chungus", p -> new PowerGenBlockItem(MACHINE_CHUNGUS.get(), p, 1), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_LPW2_ITEM = ITEMS.registerItem(
            "machine_lpw2", p -> new BlockItem(MACHINE_LPW2.get(), p), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_HEPHAESTUS_ITEM = ITEMS.registerBlockItem(
            "machine_hephaestus", MACHINE_HEPHAESTUS, Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_INDUSTRIAL_GENERATOR_ITEM = ITEMS.registerBlockItem(
            "machine_industrial_generator", MACHINE_INDUSTRIAL_GENERATOR, Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_SIREN_ITEM = ITEMS.registerItem(
            "machine_siren", p -> new BlockItem(MACHINE_SIREN.get(), p), Compat::blockDescription);

    static {
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_CHUNGUS_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_LPW2_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_HEPHAESTUS_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_INDUSTRIAL_GENERATOR_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_SIREN_ITEM);
    }

    public static void init() {}
}
