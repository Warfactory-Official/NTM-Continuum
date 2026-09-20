package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.blocks.machine.pile.BlockGraphiteBreedingFuel;
import com.hbm.blocks.machine.pile.BlockGraphiteBreedingProduct;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilled;
import com.hbm.blocks.machine.pile.BlockGraphiteFuel;
import com.hbm.blocks.machine.pile.BlockGraphiteNeutronDetector;
import com.hbm.blocks.machine.pile.BlockGraphiteRod;
import com.hbm.blocks.machine.pile.BlockGraphiteSource;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import com.hbm.blocks.machine.MachineRadiolysis;
import com.hbm.blocks.machine.MachineReactorBreeding;
import com.hbm.blocks.machine.MachineReactorControl;
import com.hbm.blocks.machine.ReactorResearch;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;

/** Feature-local blocks and block items: the RTG radiolysis chamber, the legacy reactors and the legacy pile. */
public final class ModNuclearMiscBlocks {
    private ModNuclearMiscBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7: {@code Material.iron}, hardness 10, resistance 10. */
    public static final RegistryObject<MachineRadiolysis> MACHINE_RADIOLYSIS = BLOCKS.registerBlock(
            "machine_radiolysis", MachineRadiolysis::new,
            p -> p.strength(10.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7 {@code machine_reactor_breeding}, registry id {@code machine_reactor}. */
    public static final RegistryObject<MachineReactorBreeding> MACHINE_REACTOR_BREEDING = BLOCKS.registerBlock(
            "machine_reactor", MachineReactorBreeding::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** 1.7 {@code reactor_research}, registry id {@code machine_reactor_small}. */
    public static final RegistryObject<ReactorResearch> REACTOR_RESEARCH = BLOCKS.registerBlock(
            "machine_reactor_small", ReactorResearch::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final RegistryObject<MachineReactorControl> MACHINE_CONTROLLER = BLOCKS.registerBlock(
            "machine_controller", MachineReactorControl::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    // The legacy pile: 1.7 BlockGraphiteDrilledBase = graphite material, metal step sound, hardness 5, resistance 10.
    public static final RegistryObject<BlockGraphiteDrilled> BLOCK_GRAPHITE_DRILLED = graphite("block_graphite_drilled", BlockGraphiteDrilled::new);
    public static final RegistryObject<BlockGraphiteFuel> BLOCK_GRAPHITE_FUEL = graphite("block_graphite_fuel", BlockGraphiteFuel::new);
    public static final RegistryObject<BlockGraphiteSource> BLOCK_GRAPHITE_PLUTONIUM = graphite("block_graphite_plutonium", BlockGraphiteSource::new);
    public static final RegistryObject<BlockGraphiteRod> BLOCK_GRAPHITE_ROD = graphite("block_graphite_rod", BlockGraphiteRod::new);
    public static final RegistryObject<BlockGraphiteSource> BLOCK_GRAPHITE_SOURCE = graphite("block_graphite_source", BlockGraphiteSource::new);
    public static final RegistryObject<BlockGraphiteBreedingFuel> BLOCK_GRAPHITE_LITHIUM = graphite("block_graphite_lithium", BlockGraphiteBreedingFuel::new);
    public static final RegistryObject<BlockGraphiteBreedingProduct> BLOCK_GRAPHITE_TRITIUM = graphite("block_graphite_tritium", BlockGraphiteBreedingProduct::new);
    public static final RegistryObject<BlockGraphiteNeutronDetector> BLOCK_GRAPHITE_DETECTOR = graphite("block_graphite_detector", BlockGraphiteNeutronDetector::new);

    public static final RegistryObject<BlockItem> MACHINE_RADIOLYSIS_ITEM = ITEMS.registerItem(
            "machine_radiolysis", p -> new BlockItem(MACHINE_RADIOLYSIS.get(), p), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_REACTOR_BREEDING_ITEM = ITEMS.registerItem(
            "machine_reactor", p -> new BlockItem(MACHINE_REACTOR_BREEDING.get(), p), Compat::blockDescription);
    public static final RegistryObject<BlockItem> REACTOR_RESEARCH_ITEM = ITEMS.registerItem(
            "machine_reactor_small", p -> new BlockItem(REACTOR_RESEARCH.get(), p), Compat::blockDescription);
    public static final RegistryObject<BlockItem> MACHINE_CONTROLLER_ITEM = ITEMS.registerItem(
            "machine_controller", p -> new BlockItem(MACHINE_CONTROLLER.get(), p), Compat::blockDescription);

    private static <B extends Block> RegistryObject<B> graphite(String id, Function<BlockBehaviour.Properties, B> factory) {
        RegistryObject<B> block = BLOCKS.registerBlock(id, factory,
                p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
        // 1.7 registers an ItemBlock for every block; these had no creative tab.
        ITEMS.registerBlockItem(id, block::get);
        return block;
    }

    static {
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_RADIOLYSIS_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, MACHINE_REACTOR_BREEDING_ITEM);
        ModTabs.addTo(HbmTab.MACHINES, REACTOR_RESEARCH_ITEM);
        // machine_controller: 1.7 setCreativeTab(null)
    }

    public static void init() {}
}
