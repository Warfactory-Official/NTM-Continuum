package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
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

/**
 * Feature-local block and block-item registry for the large power-generation machines, with each block's
 * model, blockstate, loot and mining tag stated on its registration (Reg; emitted by Dejson at build time).
 *
 * <p>The four OBJ-rendered machines get the shared empty blockstate placeholder and an authored/typed
 * {@code hbm:machine_obj} item definition ({@code ObjItemDeclarations}); the siren is a plain cube.
 */
public final class ModPowerGenBlocks {
    private ModPowerGenBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /**
     * Renderer-drawn folded machines: the blockstate exists only because vanilla demands one, every state
     * pointing at the shared empty placeholder; they drop themselves and are pickaxe-mined.
     */
    private static final Preset FOLDED_MACHINE = Presets.rendered(Variants.product(
                    Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true")))
            .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));

    /** 1.7: {@code Material.iron}, hardness 5, resistance 10, machineTab, {@code block_steel} icon. */
    public static final RegistryObject<MachineChungus> MACHINE_CHUNGUS = REG.block(
            "machine_chungus", MachineChungus::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED_MACHINE);
    /** 1.7: hardness 5, resistance 100. */
    public static final RegistryObject<MachineLPW2> MACHINE_LPW2 = REG.block(
            "machine_lpw2", MachineLPW2::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED_MACHINE);
    /** 1.7: hardness 10, resistance 10, {@code block_steel_machine} icon. */
    public static final RegistryObject<MachineHephaestus> MACHINE_HEPHAESTUS = REG.block(
            "machine_hephaestus", MachineHephaestus::new,
            p -> p.strength(10.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED_MACHINE);
    /** 1.7: hardness 5, resistance 10, vanilla {@code gold_block} icon (never seen: the block is renderer-drawn). */
    public static final RegistryObject<MachineIGenerator> MACHINE_INDUSTRIAL_GENERATOR = REG.block(
            "machine_industrial_generator", MachineIGenerator::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED_MACHINE);
    /** 1.7: {@code Material.iron}, hardness 5, resistance 10; icons block_steel on top and bottom, machine_siren on the sides. */
    public static final RegistryObject<MachineSiren> MACHINE_SIREN = REG.block(
            "machine_siren", MachineSiren::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.cubeBottomTop("hbm:blocks/block_steel", "hbm:blocks/machine_siren",
                    "hbm:blocks/block_steel"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** 1.7 {@code ITooltipProvider.addStandardInfo}: {@code tile.machine_chungus.desc} = "Efficiency: 85%". */
    // The type witness keeps the field's declared RegistryObject<BlockItem>: a chained call cannot infer the
    // item type from the field, so without it the handle would be typed by the factory (PowerGenBlockItem).
    public static final RegistryObject<BlockItem> MACHINE_CHUNGUS_ITEM = REG.<MachineChungus, BlockItem>blockItem(
            MACHINE_CHUNGUS, (block, p) -> new PowerGenBlockItem(block, p, 1), Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    /** 1.7 ModBlocks: the LPW-2 has no item renderer; its item is the flat block_steel sprite. */
    public static final RegistryObject<BlockItem> MACHINE_LPW2_ITEM = REG.blockItem(
            MACHINE_LPW2, BlockItem::new, Compat::blockDescription)
            .transform(Presets.sprite("hbm:blocks/block_steel"))
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_HEPHAESTUS_ITEM = REG.blockItem(
            MACHINE_HEPHAESTUS, Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_INDUSTRIAL_GENERATOR_ITEM = REG.blockItem(
            MACHINE_INDUSTRIAL_GENERATOR, Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_SIREN_ITEM = REG.blockItem(
            MACHINE_SIREN, BlockItem::new, Compat::blockDescription)
            .tab(HbmTab.MACHINES);

    public static void init() {}
}
