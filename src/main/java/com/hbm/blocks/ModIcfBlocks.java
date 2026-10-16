package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockToolConvertible;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.blocks.machine.BlockICFStruct;
import com.hbm.blocks.machine.MachineICF;
import com.hbm.blocks.machine.MachineICFController;
import com.hbm.blocks.machine.MachineICFPress;
import com.hbm.creativetabs.HbmTab;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

/**
 * The inertial confinement fusion reactor's blocks, as 1.7 {@code ModBlocks} declared them. Every one is
 * {@code Material.iron} on the machine tab.
 *
 * <p>The two 1.7 metadata blocks are flattened the port's way, {@code <base>_<variant>}:
 * {@code icf_component} (0 stability element, 1 vessel, 2 welded vessel, 3 structure, 4 riveted structure)
 * and {@code icf_laser_component} ({@code EnumICFPart}: casing, port, cell, emitter, capacitor, turbo).
 * Meta 0 of {@code icf_component} keeps the bare id, as {@code BlockToolConversion}'s unsuffixed name did.
 */
public final class ModIcfBlocks {

    private ModIcfBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Every ICF block is pickaxe-mined; all but the assembled shell drop themselves. */
    private static final Preset SELF = handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE);

    private static UnaryOperator<BlockBehaviour.Properties> iron(float hardness, float resistance) {
        return p -> p.strength(hardness, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /** Renderer-drawn: the block model is empty, so nothing may cull or suffocate against it. */
    private static UnaryOperator<BlockBehaviour.Properties> rendered(float hardness, float resistance) {
        return p -> iron(hardness, resistance).apply(p).noOcclusion()
                .isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false);
    }

    // 1.7: struct_icf_core, setLightLevel(1F), 5.0/10.0, isOpaqueCube false
    public static final RegistryObject<BlockICFStruct> struct_icf_core = REG.block("struct_icf_core",
            BlockICFStruct::new, p -> iron(5.0F, 10.0F).apply(p).lightLevel(s -> 15).noOcclusion())
            .transform(Presets.cubeAll().states(Variants.uniform("facing", "north", "east", "south", "west")).and(SELF));
    // 1.7: machine_icf_press, icf, icf_component, icf_controller, icf_laser_component all 5.0/60.0
    // 1.7: the pellet maker's machine_icf_press_top on both ends
    public static final RegistryObject<MachineICFPress> machine_icf_press =
            REG.block("machine_icf_press", MachineICFPress::new, iron(5.0F, 60.0F))
                    .transform(Presets.column("hbm:blocks/machine_icf_press_top", "hbm:blocks/machine_icf_press_side").and(SELF));
    // The reactor itself is drawn by its renderer; its item is 1.7's ItemRenderBase.
    public static final RegistryObject<MachineICF> icf =
            REG.block("icf", MachineICF::new, rendered(5.0F, 60.0F))
                    .transform(Presets.rendered(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).and(SELF))
                    .transform(ModIcfBlocks::icfItem);
    // 1.7: icf_controller on its front over icf_casing
    public static final RegistryObject<MachineICFController> icf_controller =
            REG.block("icf_controller", MachineICFController::new, iron(5.0F, 60.0F))
                    .transform(Presets.orientable("hbm:blocks/icf_controller", "hbm:blocks/icf_casing", "hbm:blocks/icf_casing")
                            .states(Variants.facing()).and(SELF));
    /** 1.7: 5.0/10.0, no creative tab, drops nothing; only a controller's assembly places it. */
    // The assembled shell draws icf_block or icf_block_port (1.7's connected-texture variants are not ported).
    public static final RegistryObject<BlockICF> icf_block =
            REG.block("icf_block", BlockICF::new, iron(5.0F, 10.0F))
                    .transform(Presets.shape(Models.group()
                                    .and("", Models.cubeAll("hbm:blocks/icf_block"))
                                    .and("_port", Models.cubeAll("hbm:blocks/icf_block_port")))
                            .states(Variants.flag("port", "_port")).noItem())
                    .loot(Loot.nothing())
                    .tag(BlockTag.PICKAXE);

    // 1.7 icons: icf_component and its .vessel, .vessel_welded, .structure and .structure_bolted variants
    public static final RegistryObject<BlockToolConvertible> icf_component = component("icf_component", "icf_component");
    public static final RegistryObject<BlockToolConvertible> icf_component_vessel = component("icf_component_vessel", "icf_component.vessel");
    public static final RegistryObject<BlockToolConvertible> icf_component_vessel_welded = component("icf_component_vessel_welded", "icf_component.vessel_welded");
    public static final RegistryObject<BlockToolConvertible> icf_component_structure = component("icf_component_structure", "icf_component.structure");
    public static final RegistryObject<BlockToolConvertible> icf_component_structure_bolted = component("icf_component_structure_bolted", "icf_component.structure_bolted");

    // 1.7 icons: icf_casing, icf_port, icf_cell, icf_emitter; capacitor and turbocharger their own side over icf_capacitor_top
    public static final RegistryObject<CompatBlock> icf_laser_component_casing = laser("icf_laser_component_casing", Presets.cubeAll("hbm:blocks/icf_casing"));
    public static final RegistryObject<CompatBlock> icf_laser_component_port = laser("icf_laser_component_port", Presets.cubeAll("hbm:blocks/icf_port"));
    public static final RegistryObject<CompatBlock> icf_laser_component_cell = laser("icf_laser_component_cell", Presets.cubeAll("hbm:blocks/icf_cell"));
    public static final RegistryObject<CompatBlock> icf_laser_component_emitter = laser("icf_laser_component_emitter", Presets.cubeAll("hbm:blocks/icf_emitter"));
    public static final RegistryObject<CompatBlock> icf_laser_component_capacitor = laser("icf_laser_component_capacitor", Presets.column("hbm:blocks/icf_capacitor_top", "hbm:blocks/icf_capacitor_side"));
    public static final RegistryObject<CompatBlock> icf_laser_component_turbo = laser("icf_laser_component_turbo", Presets.column("hbm:blocks/icf_capacitor_top", "hbm:blocks/icf_turbocharger"));

    static {
        item("struct_icf_core", struct_icf_core);
        REG.blockItem(machine_icf_press, Compat::blockDescription).tab(HbmTab.MACHINES);
        item("icf", icf);
        item("icf_component", icf_component);
        item("icf_component_vessel", icf_component_vessel);
        item("icf_component_vessel_welded", icf_component_vessel_welded);
        item("icf_component_structure", icf_component_structure);
        item("icf_component_structure_bolted", icf_component_structure_bolted);
        item("icf_controller", icf_controller);
        item("icf_laser_component_casing", icf_laser_component_casing);
        item("icf_laser_component_port", icf_laser_component_port);
        item("icf_laser_component_cell", icf_laser_component_cell);
        item("icf_laser_component_emitter", icf_laser_component_emitter);
        item("icf_laser_component_capacitor", icf_laser_component_capacitor);
        item("icf_laser_component_turbo", icf_laser_component_turbo);
    }

    private static RegistryObject<BlockToolConvertible> component(String name, String texture) {
        return REG.block(name, BlockToolConvertible::new, iron(5.0F, 60.0F))
                .transform(Presets.cubeAll("hbm:blocks/" + texture).and(SELF));
    }

    private static RegistryObject<CompatBlock> laser(String name, Presets.BlockModel model) {
        return REG.block(name, CompatBlock::new, iron(5.0F, 60.0F)).transform(model.and(SELF));
    }

    private static void item(String name, RegistryObject<? extends Block> block) {
        REG.blockItem(block, Compat::blockDescription).tab(HbmTab.MACHINES);
    }

    /** 1.7 {@code ItemRenderBase}: inventory {@code T(0,-1.5,0) S(2.125)}, common {@code S(0.5) Ry(90)}. */
    private static void icfItem(Reg.Handle<?> handle) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", "hbm:models/reactors/icf.obj");
        model.addProperty("texture", "hbm:textures/models/machines/icf.png");
        model.addProperty("scale", 0.5);
        model.addProperty("yaw", 90.0);
        model.addProperty("rawLegacyTransform", true);
        JsonArray guiTranslate = new JsonArray();
        guiTranslate.add(0.0);
        guiTranslate.add(-1.5);
        guiTranslate.add(0.0);
        model.add("guiTranslate", guiTranslate);
        model.addProperty("guiScale", 2.125);
        handle.icon(d -> d.shares("hbm:item/machine_obj_base"));
        Dejson.itemDefinition("hbm:item/" + handle.path()).add("model", model);
        Dejson.itemDefinition("hbm:item/" + handle.path()).addProperty("oversized_in_gui", true);
    }

    /** The laser parts the controller's flood fill recognises, by 1.7 {@code EnumICFPart} ordinal. */
    public static Block laserPart(int ordinal) {
        return switch (ordinal) {
            case 0 -> icf_laser_component_casing.get();
            case 1 -> icf_laser_component_port.get();
            case 2 -> icf_laser_component_cell.get();
            case 3 -> icf_laser_component_emitter.get();
            case 4 -> icf_laser_component_capacitor.get();
            default -> icf_laser_component_turbo.get();
        };
    }

    public static void init() {}
}
