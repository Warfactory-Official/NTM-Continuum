package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

import java.util.EnumMap;
import java.util.Map;
import com.hbm.blocks.machine.MachineAnnihilator;
import com.hbm.blocks.machine.MachineBoiler;
import com.hbm.blocks.machine.MachineDiFurnaceRTG;
import com.hbm.blocks.machine.MachineExcavator;
import com.hbm.blocks.machine.MachineOreSlopper;
import com.hbm.blocks.machine.MachineRtgFurnace;

/** Feature-local block and item registry for the production machines (group B). */
public final class ModProduction2Blocks {
    private ModProduction2Blocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7: Material.iron, hardness 5, resistance 100, machineTab. */
    public static final RegistryObject<MachineExcavator> MACHINE_EXCAVATOR = BLOCKS.registerBlock(
            "machine_excavator", MachineExcavator::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7: hardness 5, resistance 10, machineTab. */
    public static final RegistryObject<MachineOreSlopper> MACHINE_ORE_SLOPPER = BLOCKS.registerBlock(
            "machine_ore_slopper", MachineOreSlopper::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7: hardness 5, resistance 10, machineTab. */
    public static final RegistryObject<MachineAnnihilator> MACHINE_ANNIHILATOR = BLOCKS.registerBlock(
            "machine_annihilator", MachineAnnihilator::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7: the dead "Old Boiler" decoration block, no tab, hardness 5, resistance 10. */
    public static final RegistryObject<MachineBoiler> MACHINE_BOILER_OFF = BLOCKS.registerBlock(
            "machine_boiler_off", MachineBoiler::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    /** 1.7: the deprecated RTG furnace; the {@code _on} block becomes {@code LIT} (light level 1.0F = 15 when lit). */
    public static final RegistryObject<MachineRtgFurnace> MACHINE_RTG_FURNACE = BLOCKS.registerBlock(
            "machine_rtg_furnace_off", MachineRtgFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineRtgFurnace.LIT) ? 15 : 0));
    /** 1.7: the deprecated nuclear blast furnace, machineTab; {@code _on} is {@code LIT}. */
    public static final RegistryObject<MachineDiFurnaceRTG> MACHINE_DIFURNACE_RTG = BLOCKS.registerBlock(
            "machine_difurnace_rtg_off", MachineDiFurnaceRTG::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineDiFurnaceRTG.LIT) ? 15 : 0));

    public static final RegistryObject<BlockItem> MACHINE_EXCAVATOR_ITEM = ITEMS.registerBlockItem(
            "machine_excavator", MACHINE_EXCAVATOR, Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_ORE_SLOPPER_ITEM = ITEMS.registerBlockItem(
            "machine_ore_slopper", MACHINE_ORE_SLOPPER, Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_ANNIHILATOR_ITEM = ITEMS.registerBlockItem(
            "machine_annihilator", MACHINE_ANNIHILATOR, Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_BOILER_OFF_ITEM = ITEMS.registerItem(
            "machine_boiler_off", p -> new BlockItem(MACHINE_BOILER_OFF.get(), p), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_RTG_FURNACE_ITEM = ITEMS.registerItem(
            "machine_rtg_furnace_off", p -> new BlockItem(MACHINE_RTG_FURNACE.get(), p), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_DIFURNACE_RTG_ITEM = ITEMS.registerItem(
            "machine_difurnace_rtg_off", p -> new BlockItem(MACHINE_DIFURNACE_RTG.get(), p), Compat::blockDescription);

    /**
     * The BASE grade of the 1.7 {@code bedrock_ore} metadata family, flattened to
     * {@code bedrock_ore_base_<type>}: what the ore slopper produces. The other 25 processing grades
     * belong to the (unported) bedrock refining chain and are not registered here.
     */
    public static final Map<BedrockOreType, RegistryObject<ItemBedrockOreGraded>> BEDROCK_ORE_BASE =
            new EnumMap<>(BedrockOreType.class);

    static {
        for (BedrockOreType type : BedrockOreType.VALUES) {
            BEDROCK_ORE_BASE.put(type, ITEMS.registerItem("bedrock_ore_base_" + type.suffix,
                    p -> new ItemBedrockOreGraded(p, ItemBedrockOreGraded.Grade.BASE, type)));
        }

        ModTabs.addTo(HbmTab.MACHINES, MACHINE_EXCAVATOR_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_ORE_SLOPPER_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_ANNIHILATOR_ITEM);
        // 1.7: machine_boiler_off has setCreativeTab(null) and machine_rtg_furnace_off never sets a tab.
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_DIFURNACE_RTG_ITEM);
        for (BedrockOreType type : BedrockOreType.VALUES) ModTabs.addTo(HbmTab.MATERIALS, BEDROCK_ORE_BASE.get(type)); // 1.7 partsTab
    }

    public static ItemBedrockOreGraded bedrockOreBase(BedrockOreType type) {
        return BEDROCK_ORE_BASE.get(type).get();
    }

    public static void init() {}
}
