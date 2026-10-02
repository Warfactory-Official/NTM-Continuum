package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.machine.BlockCM;
import com.hbm.blocks.machine.BlockCMAnchor;
import com.hbm.blocks.machine.BlockCMFlux;
import com.hbm.blocks.machine.BlockCMGlass;
import com.hbm.blocks.machine.BlockCMHeat;
import com.hbm.blocks.machine.BlockCMPort;
import com.hbm.blocks.machine.BlockCustomMachine;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemCustomMachine;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.special;

/**
 * The custom machine system's blocks as 1.7 {@code ModBlocks} declared them: {@code Material.iron}, hardness 5,
 * resistance 10, all on the machine tab, in 1.7's registration order. The {@code BlockEnumMulti} families are
 * flattened to one block per variant ({@code cm_block_steel} ... {@code cm_circuit_schrabidium}); the controller
 * keeps 1.7's {@code setLightLevel(1F)}.
 *
 * <p>Models follow 1.7's icons: {@code BlockCM} drew {@code <texture>_<variant>} on every face; the receivers are
 * {@code BlockPillar}s ({@code _top} on both ends); the controller and the anchor draw their terminal front on the
 * face turned to the placer. Each is stated on its registration (Reg); the crafting recipes, which are not
 * per-registration boilerplate, stay in {@link #declare()}.
 */
public final class ModCustomMachineBlocks {

    private ModCustomMachineBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** 1.7 {@code EnumCMMaterials}, {@code EnumCMEngines}, {@code EnumCMCircuit}, lower-cased as the icons were. */
    public static final String[] MATERIALS = {"steel", "alloy", "desh", "tcalloy"};
    public static final String[] ENGINES = {"standard", "desh", "bismuth"};
    public static final String[] CIRCUITS = {"aluminium", "copper", "red_copper", "gold", "schrabidium"};

    /** Every id this family registers, in registration order. */
    public static final List<String> IDS = new ArrayList<>();

    /** Every part drops itself and is pickaxe-mined. */
    private static final Preset PART = handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** {@code BlockCM}: {@code <texture>_<variant>} on every face. */
    private static final Preset CUBE = Presets.cubeAll().and(PART);
    /** 1.7 BlockCMGlass: see-through, with the faces between neighbouring tanks skipped. */
    private static final Preset GLASS = ((Preset) handle -> handle.model(d -> d
            .models(Models.renderType("cutout", Models.cubeAll("hbm:blocks/" + d.blockId().substring(4))))
            .states(Variants.simple()).blockItemDirect())).and(PART);

    private static UnaryOperator<BlockBehaviour.Properties> iron() {
        return p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /** BlockCustomMachine.getDrops hands back the configured controller; the table is not consulted. */
    public static final RegistryObject<BlockCustomMachine> custom_machine = REG.<BlockCustomMachine>block("custom_machine",
                    BlockCustomMachine::new, p -> iron().apply(p).lightLevel(state -> 15))
            .model(d -> d.models(Models.orientable("hbm:blocks/cm_terminal_front", "hbm:blocks/cm_terminal_side",
                            "hbm:blocks/cm_terminal_side"))
                    .states(Variants.facing()).blockItemDirect())
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<ItemCustomMachine> custom_machine_item = REG.<BlockCustomMachine, ItemCustomMachine>blockItem(
            custom_machine, ItemCustomMachine::new, Compat::blockDescription);

    public static final List<RegistryObject<BlockCM>> cm_block = family("cm_block", MATERIALS, BlockCM::new, CUBE);
    public static final List<RegistryObject<BlockCM>> cm_sheet = family("cm_sheet", MATERIALS, BlockCM::new, CUBE);
    public static final List<RegistryObject<BlockCM>> cm_engine = family("cm_engine", ENGINES, BlockCM::new, CUBE);
    public static final List<RegistryObject<BlockCMGlass>> cm_tank = family("cm_tank", MATERIALS,
            BlockCMGlass::new, p -> p.noOcclusion().isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false),
            GLASS);
    public static final List<RegistryObject<BlockCM>> cm_circuit = family("cm_circuit", CIRCUITS, BlockCM::new, CUBE);
    public static final List<RegistryObject<BlockCMPort>> cm_port = family("cm_port", MATERIALS, BlockCMPort::new, CUBE);
    public static final RegistryObject<BlockCMFlux> cm_flux = single("cm_flux", BlockCMFlux::new,
            Presets.column("hbm:blocks/cm_flux_top", "hbm:blocks/cm_flux_side").and(PART));
    public static final RegistryObject<BlockCMHeat> cm_heat = single("cm_heat", BlockCMHeat::new,
            Presets.column("hbm:blocks/cm_heat_top", "hbm:blocks/cm_heat_side").and(PART));
    /** 1.7 {@code setBlockName("custom_machine_anchor")}. */
    public static final RegistryObject<BlockCMAnchor> cm_anchor = single("custom_machine_anchor", BlockCMAnchor::new,
            Presets.orientable("hbm:blocks/cmt_terminal_front", "hbm:blocks/cmt_terminal_side",
                    "hbm:blocks/cmt_terminal_side").states(Variants.facing()).and(PART));

    static {
        IDS.add(0, "custom_machine");
        ModTabs.addStacksTo(HbmTab.MACHINES, () -> custom_machine_item.get().subItems());
    }

    private static <B extends Block> List<RegistryObject<B>> family(String base, String[] variants,
                                                                   Function<BlockBehaviour.Properties, B> factory,
                                                                   Preset shape) {
        return family(base, variants, factory, UnaryOperator.identity(), shape);
    }

    private static <B extends Block> List<RegistryObject<B>> family(String base, String[] variants,
                                                                   Function<BlockBehaviour.Properties, B> factory,
                                                                   UnaryOperator<BlockBehaviour.Properties> extra,
                                                                   Preset shape) {
        List<RegistryObject<B>> list = new ArrayList<>();
        for (String variant : variants) {
            String id = base + "_" + variant;
            Reg.Handle<B> block = REG.<B>block(id, factory, p -> extra.apply(iron().apply(p))).transform(shape);
            item(block);
            list.add(block);
        }
        return Collections.unmodifiableList(list);
    }

    private static <B extends Block> RegistryObject<B> single(String id, Function<BlockBehaviour.Properties, B> factory,
                                                              Preset shape) {
        Reg.Handle<B> block = REG.<B>block(id, factory, iron()).transform(shape);
        item(block);
        return block;
    }

    private static void item(Reg.Handle<? extends Block> block) {
        REG.blockItem(block, Compat::blockDescription).tab(HbmTab.MACHINES);
        IDS.add(block.path());
    }

    private static boolean declared;

    /**
     * The custom machine recipes (1.7 {@code CraftingManager}; {@code KEY_ANYGLASS} ("blockGlass") is the
     * {@code c:glass_blocks} tag and the two {@code ANY_*} alloy groups are their {@code c:} aggregate tags).
     * Called by {@code dejsonGenerate} and by {@link ModCustomMachineContent#init()}.
     */
    public static void declare() {
        if (declared) return;
        declared = true;

        // 1.7 CraftingManager: the four casings, metas 0, 2, 3, 1 in that order
        shaped(Constants.MOD_ID, "cm_block_steel", "hbm:cm_block_steel", 4, rows(" I ", "IPI", " I "),
                "I", "#c:ingots/steel", "P", "#c:plates_cast/steel");
        shaped(Constants.MOD_ID, "cm_block_desh", "hbm:cm_block_desh", 4, rows(" I ", "IPI", " I "),
                "I", "#c:ingots/desh", "P", "#c:plates_cast/desh");
        shaped(Constants.MOD_ID, "cm_block_tcalloy", "hbm:cm_block_tcalloy", 4, rows(" I ", "IPI", " I "),
                "I", "#c:ingots/any_resistant_alloy", "P", "#c:plates_cast/any_resistant_alloy");
        shaped(Constants.MOD_ID, "cm_block_alloy", "hbm:cm_block_alloy", 4, rows(" I ", "IPI", " I "),
                "I", "#c:ingots/any_bismoid_bronze", "P", "#c:plates_cast/any_bismoid_bronze");

        for (String m : MATERIALS) {
            String casing = "hbm:cm_block_" + m;
            shaped(Constants.MOD_ID, "cm_sheet_" + m, "hbm:cm_sheet_" + m, 16, rows("BB", "BB"), "B", casing);
            shaped(Constants.MOD_ID, "cm_tank_" + m, "hbm:cm_tank_" + m, 4, rows(" B ", "BGB", " B "),
                    "B", casing, "G", "#c:glass_blocks");
            shaped(Constants.MOD_ID, "cm_port_" + m, "hbm:cm_port_" + m, rows("P", "B", "P"), "B", casing,
                    "P", "#c:plates/iron");
        }

        shaped(Constants.MOD_ID, "cm_engine_standard", "hbm:cm_engine_standard", rows(" I ", "IMI", " I "),
                "I", "#c:ingots/steel", "M", "hbm:motor");
        shaped(Constants.MOD_ID, "cm_engine_desh", "hbm:cm_engine_desh", rows(" I ", "IMI", " I "),
                "I", "#c:ingots/steel", "M", "hbm:motor_desh");
        shaped(Constants.MOD_ID, "cm_engine_bismuth", "hbm:cm_engine_bismuth", rows(" I ", "IMI", " I "),
                "I", "#c:ingots/steel", "M", "hbm:motor_bismuth");
        String[] circuitItems = {"hbm:circuit_vacuum_tube", "hbm:circuit_analog", "hbm:circuit_basic",
                "hbm:circuit_advanced", "hbm:circuit_bismoid"};
        for (int i = 0; i < CIRCUITS.length; i++) {
            shaped(Constants.MOD_ID, "cm_circuit_" + CIRCUITS[i], "hbm:cm_circuit_" + CIRCUITS[i],
                    rows(" I ", "IMI", " I "), "I", "#c:ingots/steel", "M", circuitItems[i]);
        }
        shaped(Constants.MOD_ID, "cm_flux", "hbm:cm_flux", rows("NNN", "ZCZ", "NNN"),
                "Z", "#c:plates_cast/zirconium", "N", "hbm:neutron_reflector", "C", "hbm:reactor_core");
        shaped(Constants.MOD_ID, "cm_heat", "hbm:cm_heat", rows("PCP", "PCP", "PCP"),
                "P", "hbm:plate_polymer", "C", "#c:ingots/copper");

        // 1.7 CustomMachineConfigJSON.readConfig: one recipe per configured machine, matched at crafting time
        special(Constants.MOD_ID, "custom_machine", "hbm:custom_machine");
    }

    public static void init() {}
}
