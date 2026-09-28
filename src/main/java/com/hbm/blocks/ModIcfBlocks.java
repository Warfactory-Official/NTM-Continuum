package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.BlockToolConvertible;
import com.hbm.blocks.machine.BlockICF;
import com.hbm.blocks.machine.BlockICFStruct;
import com.hbm.blocks.machine.MachineICF;
import com.hbm.blocks.machine.MachineICFController;
import com.hbm.blocks.machine.MachineICFPress;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.DescBlockItem;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.world.item.BlockItem;
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

    private static UnaryOperator<BlockBehaviour.Properties> iron(float hardness, float resistance) {
        return p -> p.strength(hardness, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /** Renderer-drawn: the block model is empty, so nothing may cull or suffocate against it. */
    private static UnaryOperator<BlockBehaviour.Properties> rendered(float hardness, float resistance) {
        return p -> iron(hardness, resistance).apply(p).noOcclusion()
                .isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false);
    }

    // 1.7: struct_icf_core, setLightLevel(1F), 5.0/10.0, isOpaqueCube false
    public static final RegistryObject<BlockICFStruct> struct_icf_core = BLOCKS.registerBlock("struct_icf_core",
            BlockICFStruct::new, p -> iron(5.0F, 10.0F).apply(p).lightLevel(s -> 15).noOcclusion());
    // 1.7: machine_icf_press, icf, icf_component, icf_controller, icf_laser_component all 5.0/60.0
    public static final RegistryObject<MachineICFPress> machine_icf_press =
            BLOCKS.registerBlock("machine_icf_press", MachineICFPress::new, iron(5.0F, 60.0F));
    public static final RegistryObject<MachineICF> icf =
            BLOCKS.registerBlock("icf", MachineICF::new, rendered(5.0F, 60.0F));
    public static final RegistryObject<MachineICFController> icf_controller =
            BLOCKS.registerBlock("icf_controller", MachineICFController::new, iron(5.0F, 60.0F));
    /** 1.7: 5.0/10.0, no creative tab, drops nothing; only a controller's assembly places it. */
    public static final RegistryObject<BlockICF> icf_block =
            BLOCKS.registerBlock("icf_block", BlockICF::new, iron(5.0F, 10.0F));

    public static final RegistryObject<BlockToolConvertible> icf_component = component("icf_component");
    public static final RegistryObject<BlockToolConvertible> icf_component_vessel = component("icf_component_vessel");
    public static final RegistryObject<BlockToolConvertible> icf_component_vessel_welded = component("icf_component_vessel_welded");
    public static final RegistryObject<BlockToolConvertible> icf_component_structure = component("icf_component_structure");
    public static final RegistryObject<BlockToolConvertible> icf_component_structure_bolted = component("icf_component_structure_bolted");

    public static final RegistryObject<CompatBlock> icf_laser_component_casing = laser("icf_laser_component_casing");
    public static final RegistryObject<CompatBlock> icf_laser_component_port = laser("icf_laser_component_port");
    public static final RegistryObject<CompatBlock> icf_laser_component_cell = laser("icf_laser_component_cell");
    public static final RegistryObject<CompatBlock> icf_laser_component_emitter = laser("icf_laser_component_emitter");
    public static final RegistryObject<CompatBlock> icf_laser_component_capacitor = laser("icf_laser_component_capacitor");
    public static final RegistryObject<CompatBlock> icf_laser_component_turbo = laser("icf_laser_component_turbo");

    static {
        item("struct_icf_core", struct_icf_core);
        // 1.7 ITooltipProvider: the three "$"-separated lines of tile.machine_icf_press.desc
        RegistryObject<DescBlockItem> press = ITEMS.registerItem("machine_icf_press",
                p -> new DescBlockItem(machine_icf_press.get(), p, "machine_icf_press", 3), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, press);
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

    private static RegistryObject<BlockToolConvertible> component(String name) {
        return BLOCKS.registerBlock(name, BlockToolConvertible::new, iron(5.0F, 60.0F));
    }

    private static RegistryObject<CompatBlock> laser(String name) {
        return BLOCKS.registerBlock(name, CompatBlock::new, iron(5.0F, 60.0F));
    }

    private static void item(String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block, Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
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
