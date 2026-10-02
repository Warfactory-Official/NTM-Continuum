package com.hbm.blocks;

import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
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

/**
 * Feature-local block and block-item registry for the production machines (group A), with each block's model,
 * blockstate, loot and mining tag stated on its registration (Reg; emitted by Dejson at build time). The
 * OBJ-rendered machines (rotary furnace, thresher, microwave, conveyor press, funnel) get the shared empty
 * blockstate placeholder and a typed {@code hbm:machine_obj} item definition ({@code ObjItemDeclarations});
 * the two plain cubes are fully declared.
 */
public final class ModProductionBlocks {
    private ModProductionBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Drops itself, pickaxe-mined. */
    private static final Preset SELF_PICKAXE = handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** Renderer-drawn folded machine: the blockstate exists only because vanilla demands one. */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true"))).and(SELF_PICKAXE);
    private static final Preset FACING = Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west"))
            .and(SELF_PICKAXE);

    /** 1.7: {@code Material.iron}, hardness 5, resistance 10, machineTab (the autocrafter is 10/20). */
    public static final RegistryObject<MachineRotaryFurnace> MACHINE_ROTARY_FURNACE = REG.block(
            "machine_rotary_furnace", MachineRotaryFurnace::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED);
    public static final RegistryObject<MachineThresher> MACHINE_THRESHER = REG.block(
            "machine_thresher", MachineThresher::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FACING);
    public static final RegistryObject<MachineMicrowave> MACHINE_MICROWAVE = REG.block(
            "machine_microwave", MachineMicrowave::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FACING);
    public static final RegistryObject<MachineAutocrafter> MACHINE_AUTOCRAFTER = REG.block(
            "machine_autocrafter", MachineAutocrafter::new,
            p -> p.strength(10.0F, 20.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            // 1.7 icons: machine_autocrafter_top / _side / _bottom.
            .transform(Presets.cubeBottomTop("hbm:blocks/machine_autocrafter_bottom",
                    "hbm:blocks/machine_autocrafter_side", "hbm:blocks/machine_autocrafter_top").and(SELF_PICKAXE)); // 1.7 ModBlocks:2246 hardness 10 / resistance 20
    public static final RegistryObject<MachineConveyorPress> MACHINE_CONVEYOR_PRESS = REG.block(
            "machine_conveyor_press", MachineConveyorPress::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED);
    /** 1.7: a plain {@code BlockBase(Material.iron)} whose only job is to be recognised by the burner press. */
    public static final RegistryObject<Block> PRESS_PREHEATER = REG.<Block>block(
            "press_preheater", dev.golem.core.world.CompatBlock::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            // 1.7: one animated texture on every face
            .transform(Presets.cubeAll().and(SELF_PICKAXE));
    public static final RegistryObject<MachineFunnel> MACHINE_FUNNEL = REG.block(
            "machine_funnel", MachineFunnel::new,
            p -> p.strength(10.0F, 20.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.self());

    public static final RegistryObject<BlockItem> MACHINE_ROTARY_FURNACE_ITEM = REG.blockItem(
            MACHINE_ROTARY_FURNACE, Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_THRESHER_ITEM = REG.<MachineThresher, BlockItem>blockItem(
            MACHINE_THRESHER, (block, p) -> new ItemBlockProductionDesc(block, p, 7), Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_MICROWAVE_ITEM = REG.blockItem(
            MACHINE_MICROWAVE, Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_AUTOCRAFTER_ITEM = REG.<MachineAutocrafter, BlockItem>blockItem(
            MACHINE_AUTOCRAFTER, BlockItem::new, Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_CONVEYOR_PRESS_ITEM = REG.<MachineConveyorPress, BlockItem>blockItem(
            MACHINE_CONVEYOR_PRESS, (block, p) -> new ItemBlockProductionDesc(block, p, 3), Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> PRESS_PREHEATER_ITEM = REG.<Block, BlockItem>blockItem(
            PRESS_PREHEATER, BlockItem::new, Compat::blockDescription)
            .tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_FUNNEL_ITEM = REG.blockItem(
            MACHINE_FUNNEL, Compat::blockDescription)
            .tab(HbmTab.MACHINES);

    public static void init() {}
}
