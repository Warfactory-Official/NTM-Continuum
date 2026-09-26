package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

import java.util.List;
import com.hbm.blocks.generic.BarbedWire;
import com.hbm.blocks.generic.BlockBarrier;
import com.hbm.blocks.generic.BlockMetalFence;
import com.hbm.blocks.generic.Spikes;
import com.hbm.blocks.machine.MachineKeyForge;

/**
 * The security / perimeter blocks (1.7.10 {@code ModBlocks}): the six barbed wires and the spikes
 * (blockTab, iron), the sand-bag {@code barricade} (no tab, drops nothing), the chain-link
 * {@code fence_metal} and its always-post sub-block {@code fence_metal_post} (machineTab), the
 * {@code wood_barrier} (blockTab) and the {@code machine_keyforge} locksmith table (consumableTab).
 * Iron-material blocks needed a pickaxe in 1.7.10 ({@code Material.iron} requires a tool), so they
 * carry {@code requiresCorrectToolForDrops} and sit in the pickaxe tag.
 */
public final class ModSecurityBlocks {
    private ModSecurityBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<BarbedWire> barbed_wire = wire(BarbedWire.Kind.PLAIN);
    public static final RegistryObject<BarbedWire> barbed_wire_fire = wire(BarbedWire.Kind.FIRE);
    public static final RegistryObject<BarbedWire> barbed_wire_poison = wire(BarbedWire.Kind.POISON);
    public static final RegistryObject<BarbedWire> barbed_wire_acid = wire(BarbedWire.Kind.ACID);
    public static final RegistryObject<BarbedWire> barbed_wire_wither = wire(BarbedWire.Kind.WITHER);
    public static final RegistryObject<BarbedWire> barbed_wire_ultradeath = wire(BarbedWire.Kind.ULTRADEATH);

    /** Every wire, for the shared block-entity type and the renderer. */
    public static final List<RegistryObject<BarbedWire>> BARBED_WIRES = List.of(
            barbed_wire, barbed_wire_fire, barbed_wire_poison, barbed_wire_acid, barbed_wire_wither, barbed_wire_ultradeath);

    public static final RegistryObject<Spikes> spikes = BLOCKS.registerBlock("spikes", Spikes::new,
            p -> p.strength(2.5F, 5.0F).noOcclusion().requiresCorrectToolForDrops());

    public static final RegistryObject<Block> barricade = BLOCKS.registerBlock("barricade", Block::new,
            p -> p.strength(1.0F, 2.5F).sound(SoundType.SAND));

    public static final RegistryObject<BlockMetalFence> fence_metal = BLOCKS.registerBlock("fence_metal",
            p -> new BlockMetalFence(p, false), p -> p.strength(15.0F, 0.25F).noOcclusion().requiresCorrectToolForDrops());
    public static final RegistryObject<BlockMetalFence> fence_metal_post = BLOCKS.registerBlock("fence_metal_post",
            p -> new BlockMetalFence(p, true), p -> p.strength(15.0F, 0.25F).noOcclusion().requiresCorrectToolForDrops());

    public static final RegistryObject<BlockBarrier> wood_barrier = BLOCKS.registerBlock("wood_barrier", BlockBarrier::new,
            p -> p.strength(5.0F, 15.0F).sound(SoundType.WOOD).noOcclusion());

    public static final RegistryObject<MachineKeyForge> machine_keyforge = BLOCKS.registerBlock("machine_keyforge",
            MachineKeyForge::new, p -> p.strength(5.0F, 10.0F).requiresCorrectToolForDrops());

    // --- block items ---

    public static final RegistryObject<BlockItem> barbed_wire_item = item("barbed_wire", barbed_wire, HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> barbed_wire_fire_item = item("barbed_wire_fire", barbed_wire_fire, HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> barbed_wire_poison_item = item("barbed_wire_poison", barbed_wire_poison, HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> barbed_wire_acid_item = item("barbed_wire_acid", barbed_wire_acid, HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> barbed_wire_wither_item = item("barbed_wire_wither", barbed_wire_wither, HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> barbed_wire_ultradeath_item = item("barbed_wire_ultradeath", barbed_wire_ultradeath, HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> spikes_item = item("spikes", spikes, HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> barricade_item = item("barricade", barricade, null);
    public static final RegistryObject<BlockItem> fence_metal_item = item("fence_metal", fence_metal, HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> fence_metal_post_item = item("fence_metal_post", fence_metal_post, HbmTab.MACHINES);
    public static final RegistryObject<BlockItem> wood_barrier_item = item("wood_barrier", wood_barrier, HbmTab.BLOCKS);
    public static final RegistryObject<BlockItem> machine_keyforge_item = item("machine_keyforge", machine_keyforge, HbmTab.CONSUMABLES);

    private static RegistryObject<BarbedWire> wire(BarbedWire.Kind kind) {
        return BLOCKS.registerBlock(kind.texture, p -> new BarbedWire(p, kind),
                p -> p.strength(5.0F, 10.0F).noOcclusion().requiresCorrectToolForDrops());
    }

    private static RegistryObject<BlockItem> item(String id, RegistryObject<? extends Block> block, HbmTab tab) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(id, block, Compat::blockDescription);
        if (tab != null) ModTabs.addTo(tab, item);
        return item;
    }

    public static void init() {}
}
