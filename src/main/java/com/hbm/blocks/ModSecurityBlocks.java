package com.hbm.blocks;

import dev.golem.core.dejson.When;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.States;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
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
 *
 * <p>Each block's blockstate, model, item model, loot and tags are stated on its registration (Reg). Authored
 * instead (Dejson has no vocabulary for them): the multipart blockstates and {@code elements} models of
 * {@code fence_metal} / {@code fence_metal_post} / {@code wood_barrier}, the {@code wood_barrier_inventory} item
 * model; the {@code hbm:machine_obj} item definitions of the barbed wires and spikes (OBJ-drawn blocks whose
 * blockstate is the shared empty placeholder) are typed data in {@code ObjItemDeclarations}.
 */
public final class ModSecurityBlocks {
    private ModSecurityBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    public static final RegistryObject<BarbedWire> barbed_wire = wire(BarbedWire.Kind.PLAIN);
    public static final RegistryObject<BarbedWire> barbed_wire_fire = wire(BarbedWire.Kind.FIRE);
    public static final RegistryObject<BarbedWire> barbed_wire_poison = wire(BarbedWire.Kind.POISON);
    public static final RegistryObject<BarbedWire> barbed_wire_acid = wire(BarbedWire.Kind.ACID);
    public static final RegistryObject<BarbedWire> barbed_wire_wither = wire(BarbedWire.Kind.WITHER);
    public static final RegistryObject<BarbedWire> barbed_wire_ultradeath = wire(BarbedWire.Kind.ULTRADEATH);

    /** Every wire, for the shared block-entity type and the renderer. */
    public static final List<RegistryObject<BarbedWire>> BARBED_WIRES = List.of(
            barbed_wire, barbed_wire_fire, barbed_wire_poison, barbed_wire_acid, barbed_wire_wither, barbed_wire_ultradeath);

    /** OBJ-drawn: the blockstate is the empty placeholder, the BER draws the mesh. */
    public static final RegistryObject<Spikes> spikes = REG.block("spikes", Spikes::new,
                    p -> p.strength(2.5F, 3.0F).noOcclusion().requiresCorrectToolForDrops())
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    /** BlockNoDrop: getItemDropped returns null. */
    public static final RegistryObject<Block> barricade = REG.block("barricade", Block::new,
                    p -> p.strength(1.0F, 1.5F).sound(SoundType.SAND))
            .transform(Presets.cubeAll())
            .loot(Loot.silkTouchOnly()) // 1.7: drops nothing, but a plain cube without a tile entity, so silk touch keeps it
            .tag(BlockTag.SHOVEL);

    /** 1.7.10 ItemBlockBase for the fence: a flat sprite of the block icon (fence_metal / fence_metal_post). */
    public static final RegistryObject<BlockMetalFence> fence_metal = REG.block("fence_metal",
                    p -> new BlockMetalFence(p, false), p -> p.strength(15.0F, 0.15F).noOcclusion().requiresCorrectToolForDrops())
            .transform(Presets.sprite("hbm:blocks/fence_metal"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .andThen(handle -> fenceMetalStates());
    public static final RegistryObject<BlockMetalFence> fence_metal_post = REG.block("fence_metal_post",
                    p -> new BlockMetalFence(p, true), p -> p.strength(15.0F, 0.15F).noOcclusion().requiresCorrectToolForDrops())
            .transform(Presets.sprite("hbm:blocks/fence_metal_post"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .andThen(handle -> fenceMetalPostStates());

    /** The old renderInventoryBlock drew a post and two rails; that is the authored wood_barrier_inventory model. */
    public static final RegistryObject<BlockBarrier> wood_barrier = REG.block("wood_barrier", BlockBarrier::new,
                    p -> p.strength(5.0F, 9.0F).sound(SoundType.WOOD).noOcclusion())
            .icon(d -> d.models(Models.blockItem("hbm:block/wood_barrier_inventory")))
            .loot(Loot.self())
            .tag(BlockTag.AXE)
            .andThen(handle -> woodBarrierStates());

    public static final RegistryObject<MachineKeyForge> machine_keyforge = REG.block("machine_keyforge",
                    MachineKeyForge::new, p -> p.strength(5.0F, 6.0F).requiresCorrectToolForDrops())
            .transform(Presets.cubeBottomTop("hbm:blocks/machine_keyforge_bottom", "hbm:blocks/machine_keyforge_side",
                    "hbm:blocks/machine_keyforge_top"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

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

    /** OBJ-drawn: the blockstate is the empty placeholder, the BER draws the mesh. */
    private static RegistryObject<BarbedWire> wire(BarbedWire.Kind kind) {
        return REG.block(kind.texture, p -> new BarbedWire(p, kind),
                        p -> p.strength(5.0F, 6.0F).noOcclusion().requiresCorrectToolForDrops())
                .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
                .loot(Loot.self())
                .tag(BlockTag.PICKAXE);
    }

    private static RegistryObject<BlockItem> item(String id, RegistryObject<? extends Block> block, HbmTab tab) {
        Reg.Handle<BlockItem> item = REG.blockItem(block, Compat::blockDescription);
        if (tab != null) item.tab(tab);
        return item;
    }

    /** The fence's multipart blockstate: a post unless it runs straight, a side per connection. */
    private static void fenceMetalStates() {
        States.multipart(Constants.MOD_ID, "fence_metal")
                .part(When.or(When.of("north", "true", "south", "false"), When.of("north", "false", "south", "true"), When.of("west", "true", "east", "false"), When.of("west", "false", "east", "true"), When.of("north", "false", "south", "false", "west", "false", "east", "false"), When.of("north", "true", "south", "true", "west", "true", "east", "true")), new Variant("hbm:block/fence_metal_post"))
                .part(When.of("north", "true"), new Variant("hbm:block/fence_metal_side", 0, 0, true))
                .part(When.of("east", "true"), new Variant("hbm:block/fence_metal_side", 0, 90, true))
                .part(When.of("south", "true"), new Variant("hbm:block/fence_metal_side", 0, 180, true))
                .part(When.of("west", "true"), new Variant("hbm:block/fence_metal_side", 0, 270, true))
                .register();
    }

    /** The always-post fence: the post, and a side per connection. */
    private static void fenceMetalPostStates() {
        States.multipart(Constants.MOD_ID, "fence_metal_post")
                .part(new Variant("hbm:block/fence_metal_post"))
                .part(When.of("north", "true"), new Variant("hbm:block/fence_metal_side", 0, 0, true))
                .part(When.of("east", "true"), new Variant("hbm:block/fence_metal_side", 0, 90, true))
                .part(When.of("south", "true"), new Variant("hbm:block/fence_metal_side", 0, 180, true))
                .part(When.of("west", "true"), new Variant("hbm:block/fence_metal_side", 0, 270, true))
                .register();
    }

    /** The wooden barrier: a post, rail and caps per connected side, and a top. */
    private static void woodBarrierStates() {
        States.multipart(Constants.MOD_ID, "wood_barrier")
                .part(When.of("west", "true"), new Variant("hbm:block/wood_barrier_west_post"))
                .part(When.of("west", "true"), new Variant("hbm:block/wood_barrier_west_rail"))
                .part(When.of("west", "true", "north", "false"), new Variant("hbm:block/wood_barrier_west_cap_north"))
                .part(When.of("west", "true", "south", "false"), new Variant("hbm:block/wood_barrier_west_cap_south"))
                .part(When.of("east", "true"), new Variant("hbm:block/wood_barrier_east_post"))
                .part(When.of("east", "true"), new Variant("hbm:block/wood_barrier_east_rail"))
                .part(When.of("east", "true", "north", "false"), new Variant("hbm:block/wood_barrier_east_cap_north"))
                .part(When.of("east", "true", "south", "false"), new Variant("hbm:block/wood_barrier_east_cap_south"))
                .part(When.of("north", "true"), new Variant("hbm:block/wood_barrier_north_post"))
                .part(When.of("north", "true"), new Variant("hbm:block/wood_barrier_north_rail"))
                .part(When.of("north", "true", "west", "false"), new Variant("hbm:block/wood_barrier_north_cap_west"))
                .part(When.of("north", "true", "east", "false"), new Variant("hbm:block/wood_barrier_north_cap_east"))
                .part(When.of("south", "true"), new Variant("hbm:block/wood_barrier_south_post"))
                .part(When.of("south", "true"), new Variant("hbm:block/wood_barrier_south_rail"))
                .part(When.of("south", "true", "west", "false"), new Variant("hbm:block/wood_barrier_south_cap_west"))
                .part(When.of("south", "true", "east", "false"), new Variant("hbm:block/wood_barrier_south_cap_east"))
                .part(When.of("up", "true"), new Variant("hbm:block/wood_barrier_top"))
                .register();
    }

    public static void init() {}
}
