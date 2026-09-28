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
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The custom machine system's blocks as 1.7 {@code ModBlocks} declared them: {@code Material.iron}, hardness 5,
 * resistance 10, all on the machine tab, in 1.7's registration order. The {@code BlockEnumMulti} families are
 * flattened to one block per variant ({@code cm_block_steel} ... {@code cm_circuit_schrabidium}); the controller
 * keeps 1.7's {@code setLightLevel(1F)}.
 */
public final class ModCustomMachineBlocks {

    private ModCustomMachineBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7 {@code EnumCMMaterials}, {@code EnumCMEngines}, {@code EnumCMCircuit}, lower-cased as the icons were. */
    public static final String[] MATERIALS = {"steel", "alloy", "desh", "tcalloy"};
    public static final String[] ENGINES = {"standard", "desh", "bismuth"};
    public static final String[] CIRCUITS = {"aluminium", "copper", "red_copper", "gold", "schrabidium"};

    /** Every id this family registers, in registration order. */
    public static final List<String> IDS = new ArrayList<>();

    private static UnaryOperator<BlockBehaviour.Properties> iron() {
        return p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static final RegistryObject<BlockCustomMachine> custom_machine = BLOCKS.registerBlock("custom_machine",
            BlockCustomMachine::new, p -> iron().apply(p).lightLevel(state -> 15));
    public static final RegistryObject<ItemCustomMachine> custom_machine_item = ITEMS.registerItem("custom_machine",
            p -> new ItemCustomMachine(custom_machine.get(), p), Compat::blockDescription);

    public static final List<RegistryObject<BlockCM>> cm_block = family("cm_block", MATERIALS, BlockCM::new);
    public static final List<RegistryObject<BlockCM>> cm_sheet = family("cm_sheet", MATERIALS, BlockCM::new);
    public static final List<RegistryObject<BlockCM>> cm_engine = family("cm_engine", ENGINES, BlockCM::new);
    public static final List<RegistryObject<BlockCMGlass>> cm_tank = family("cm_tank", MATERIALS,
            BlockCMGlass::new, p -> p.noOcclusion().isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
    public static final List<RegistryObject<BlockCM>> cm_circuit = family("cm_circuit", CIRCUITS, BlockCM::new);
    public static final List<RegistryObject<BlockCMPort>> cm_port = family("cm_port", MATERIALS, BlockCMPort::new);
    public static final RegistryObject<BlockCMFlux> cm_flux = single("cm_flux", BlockCMFlux::new);
    public static final RegistryObject<BlockCMHeat> cm_heat = single("cm_heat", BlockCMHeat::new);
    /** 1.7 {@code setBlockName("custom_machine_anchor")}. */
    public static final RegistryObject<BlockCMAnchor> cm_anchor = single("custom_machine_anchor", BlockCMAnchor::new);

    static {
        IDS.add(0, "custom_machine");
        ModTabs.addStacksTo(HbmTab.MACHINES, () -> custom_machine_item.get().subItems());
    }

    private static <B extends Block> List<RegistryObject<B>> family(String base, String[] variants,
                                                                   Function<BlockBehaviour.Properties, B> factory) {
        return family(base, variants, factory, UnaryOperator.identity());
    }

    private static <B extends Block> List<RegistryObject<B>> family(String base, String[] variants,
                                                                   Function<BlockBehaviour.Properties, B> factory,
                                                                   UnaryOperator<BlockBehaviour.Properties> extra) {
        List<RegistryObject<B>> list = new ArrayList<>();
        for (String variant : variants) {
            String id = base + "_" + variant;
            RegistryObject<B> block = BLOCKS.registerBlock(id, factory, p -> extra.apply(iron().apply(p)));
            item(id, block);
            list.add(block);
        }
        return Collections.unmodifiableList(list);
    }

    private static <B extends Block> RegistryObject<B> single(String id, Function<BlockBehaviour.Properties, B> factory) {
        RegistryObject<B> block = BLOCKS.registerBlock(id, factory, iron());
        item(id, block);
        return block;
    }

    private static void item(String id, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
        IDS.add(id);
    }

    public static void init() {}
}
