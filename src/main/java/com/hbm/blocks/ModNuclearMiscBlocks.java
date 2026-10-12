package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
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
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Renderer-drawn folded machines: the blockstate exists only because vanilla demands one; items are OBJ. */
    private static final Preset FOLDED_MACHINE = Presets.rendered(Variants.product(
                    Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true")))
            .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));
    private static final String GRAPHITE = "hbm:blocks/block_graphite";

    /** 1.7: {@code Material.iron}, hardness 10, resistance 10. */
    public static final RegistryObject<MachineRadiolysis> MACHINE_RADIOLYSIS = REG.block(
            "machine_radiolysis", MachineRadiolysis::new,
            p -> p.strength(10.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED_MACHINE);
    /** 1.7 {@code machine_reactor_breeding}, registry id {@code machine_reactor}. */
    public static final RegistryObject<MachineReactorBreeding> MACHINE_REACTOR_BREEDING = REG.block(
            "machine_reactor", MachineReactorBreeding::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED_MACHINE);
    /** 1.7 {@code reactor_research}, registry id {@code machine_reactor_small}. */
    public static final RegistryObject<ReactorResearch> REACTOR_RESEARCH = REG.block(
            "machine_reactor_small", ReactorResearch::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops())
            .transform(FOLDED_MACHINE);
    /** 1.7 icons: top for up/down, front on the facing, back opposite, side elsewhere. */
    public static final RegistryObject<MachineReactorControl> MACHINE_CONTROLLER = REG.block(
            "machine_controller", MachineReactorControl::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(Presets.shape(Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/machine_controller_side")
                            .and("up", "hbm:blocks/machine_controller_top").and("down", "hbm:blocks/machine_controller_top")
                            .and("north", "hbm:blocks/machine_controller").and("south", "hbm:blocks/machine_controller_back")
                            .and("east", "hbm:blocks/machine_controller_side").and("west", "hbm:blocks/machine_controller_side")))
                    .states(Variants.facing()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // The legacy pile: 1.7 BlockGraphiteDrilledBase = graphite material, metal step sound, hardness 5, resistance 10.
    // Channel faces carry the block's own icon (or its aluminium-shrouded variant), the other four faces plain
    // graphite; drops are computed in code (ingots + shroud + rod), so the loot table is empty.
    public static final RegistryObject<BlockGraphiteDrilled> BLOCK_GRAPHITE_DRILLED = graphite("block_graphite_drilled", BlockGraphiteDrilled::new, "block_graphite_drilled", null);
    public static final RegistryObject<BlockGraphiteFuel> BLOCK_GRAPHITE_FUEL = graphite("block_graphite_fuel", BlockGraphiteFuel::new, "block_graphite_fuel", null);
    public static final RegistryObject<BlockGraphiteSource> BLOCK_GRAPHITE_PLUTONIUM = graphite("block_graphite_plutonium", BlockGraphiteSource::new, "block_graphite_plutonium", null);
    public static final RegistryObject<BlockGraphiteRod> BLOCK_GRAPHITE_ROD = graphite("block_graphite_rod", BlockGraphiteRod::new, "block_graphite_rod_in", "block_graphite_rod_out");
    public static final RegistryObject<BlockGraphiteSource> BLOCK_GRAPHITE_SOURCE = graphite("block_graphite_source", BlockGraphiteSource::new, "block_graphite_source", null);
    public static final RegistryObject<BlockGraphiteBreedingFuel> BLOCK_GRAPHITE_LITHIUM = graphite("block_graphite_lithium", BlockGraphiteBreedingFuel::new, "block_graphite_lithium", null);
    public static final RegistryObject<BlockGraphiteBreedingProduct> BLOCK_GRAPHITE_TRITIUM = graphite("block_graphite_tritium", BlockGraphiteBreedingProduct::new, "block_graphite_tritium", null);
    public static final RegistryObject<BlockGraphiteNeutronDetector> BLOCK_GRAPHITE_DETECTOR = graphite("block_graphite_detector", BlockGraphiteNeutronDetector::new, "block_graphite_detector", "block_graphite_detector_out");

    public static final RegistryObject<BlockItem> MACHINE_RADIOLYSIS_ITEM = REG.blockItem(
            MACHINE_RADIOLYSIS, Compat::blockDescription).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> MACHINE_REACTOR_BREEDING_ITEM = REG.blockItem(
            MACHINE_REACTOR_BREEDING, Compat::blockDescription).tab(HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> REACTOR_RESEARCH_ITEM = REG.blockItem(
            REACTOR_RESEARCH, Compat::blockDescription).tab(HbmTab.MACHINES);
    // machine_controller: 1.7 setCreativeTab(null)
    public static final RegistryObject<BlockItem> MACHINE_CONTROLLER_ITEM = REG.blockItem(
            MACHINE_CONTROLLER, BlockItem::new, Compat::blockDescription);

    /** {@code icon} on the channel faces; {@code outIcon} (may be null) replaces it while {@code out} is set. */
    private static <B extends Block> RegistryObject<B> graphite(String id, Function<BlockBehaviour.Properties, B> factory,
                                                                String icon, String outIcon) {
        Models.Group group = Models.group()
                .and("", Models.column("hbm:blocks/" + icon, GRAPHITE))
                .and("_aluminum", Models.column("hbm:blocks/" + icon + "_aluminum", GRAPHITE));
        Dispatch out;
        if (outIcon != null) {
            group = group.and("_out", Models.column("hbm:blocks/" + outIcon, GRAPHITE))
                    .and("_out_aluminum", Models.column("hbm:blocks/" + outIcon + "_aluminum", GRAPHITE));
            out = Variants.select("out", "false", "", "true", "_out");
        } else {
            out = Variants.uniform("out", "false", "true");
        }
        Reg.Handle<B> block = REG.block(id, factory,
                        p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
                .transform(Presets.shape(group).states(Variants.product(Variants.axis(), out,
                        Variants.select("aluminum", "false", "", "true", "_aluminum"))))
                .loot(Loot.nothing())
                .tag(BlockTag.PICKAXE);
        // 1.7 registers an ItemBlock for every block; these had no creative tab.
        REG.blockItem(block);
        return block;
    }

    public static void init() {}
}
