package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.BlockSets;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.machine.BlastDoor;
import com.hbm.blocks.machine.DummyBlockBlast;
import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.DoorDecl;

/**
 * Blocks and block items of the door family: the fourteen {@link BlockDoorGeneric} doors, the old
 * six-high {@link BlastDoor} with its leaf dummy, the four vanilla-style mod doors and the steel
 * trapdoor. Hardness/resistance pairs are the original {@code ModBlocks} values.
 *
 * <p>Each block's blockstate, models, item model, loot and mining tag are stated on its registration (Reg;
 * emitted by Dejson at build time). The renderer-drawn doors get the shared empty blockstate placeholder (their
 * appearance is the block-entity renderer) and a typed {@code hbm:machine_obj} item definition
 * ({@code ObjItemDeclarations}). The vanilla-style mod doors and the steel trapdoor are the vanilla
 * door/trapdoor templates over the original's {@code _lower}/{@code _upper} sprites, with the vanilla
 * blockstate tables stated here because Dejson has no door vocabulary of its own.
 */
public final class ModDoorBlocks {
    private ModDoorBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /**
     * The mod doors' block set. A holder, not a field of this class: it reads sound events that only exist in a
     * running game, and this class also initialises in DejsonMain's plain JVM to record its declarations.
     */
    public static final class Sets {
        private Sets() {}

        /**
         * The mod doors' set: metal step sounds and the original {@code hbm:block.openDoor} on both
         * open and close (the original played the one sound for either direction).
         */
        public static final BlockSetType MOD_DOOR_TYPE = BlockSets.type("hbm_mod_door", SoundType.METAL,
                ModDoorSounds.OPEN_DOOR_EVENT, ModDoorSounds.OPEN_DOOR_EVENT,
                SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.WOODEN_DOOR_CLOSE);
    }

    private static final String EMPTY = "hbm:block/machine_dummy_empty";
    /** Renderer-drawn: facing x cell x open (the 1.7 "extra" flag); drops itself, pickaxe. */
    private static final Preset GENERIC_DOOR = Presets.rendered(EMPTY, Variants.product(
                    Variants.uniform("facing", "north", "east", "south", "west"),
                    Variants.uniform("cell", "false", "true"),
                    Variants.uniform("open", "false", "true")))
            .and(handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE));

    private static BlockBehaviour.Properties dummyable(BlockBehaviour.Properties p) {
        return p.isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false);
    }

    /**
     * A generic door's outline and collision come from its core (the 1.7 {@code getBoundingBox} looked the
     * core up in the world every call), so its shapes must not be baked into vanilla's per-state cache at
     * registry freeze: {@code dynamicShape()} keeps {@code BlockState.getCollisionShape} asking the block.
     * The declaration is read only when the block is built ({@code DoorDecl} needs a running game).
     */
    private static Reg.Handle<BlockDoorGeneric> door(String name, Supplier<DoorDecl> decl, float hardness, float resistance) {
        return REG.block(name, p -> new BlockDoorGeneric(p, decl.get()),
                        p -> dummyable(p.strength(hardness, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().dynamicShape()))
                .transform(GENERIC_DOOR);
    }

    public static final RegistryObject<BlockDoorGeneric> VAULT_DOOR = door("vault_door", () -> DoorDecl.VAULT_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlastDoor> BLAST_DOOR = REG.block("blast_door", BlastDoor::new,
                    p -> dummyable(p.strength(10.0F, 1_000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()))
            .transform(Presets.rendered(EMPTY, Variants.uniform("axis", "x", "z")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<DummyBlockBlast> DUMMY_BLOCK_BLAST = REG.block("dummy_block_blast", DummyBlockBlast::new,
                    p -> dummyable(p.strength(10.0F, 10_000.0F).sound(SoundType.METAL).noOcclusion()))
            .transform(Presets.rendered(EMPTY, Variants.simple()))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockDoorGeneric> SLIDING_BLAST_DOOR = door("sliding_blast_door", () -> DoorDecl.SLIDE_DOOR, 10.0F, 750.0F);
    public static final RegistryObject<BlockDoorGeneric> FIRE_DOOR = door("fire_door", () -> DoorDecl.FIRE_DOOR, 10.0F, 1_000.0F);
    /** 1.7 setBlockTextureName("hbm:transition_seal"): the seal's inventory icon is that flat block sprite. */
    public static final RegistryObject<BlockDoorGeneric> TRANSITION_SEAL = door("transition_seal", () -> DoorDecl.TRANSITION_SEAL, 10.0F, 1_000.0F)
            .transform(Presets.sprite("hbm:blocks/transition_seal"));
    public static final RegistryObject<BlockDoorGeneric> SILO_HATCH = door("silo_hatch", () -> DoorDecl.SILO_HATCH, 10.0F, 100.0F);
    public static final RegistryObject<BlockDoorGeneric> SILO_HATCH_LARGE = door("silo_hatch_large", () -> DoorDecl.SILO_HATCH_LARGE, 10.0F, 100.0F);
    public static final RegistryObject<BlockDoorGeneric> SECURE_ACCESS_DOOR = door("secure_access_door", () -> DoorDecl.SECURE_ACCESS_DOOR, 20.0F, 2_000.0F);
    public static final RegistryObject<BlockDoorGeneric> LARGE_VEHICLE_DOOR = door("large_vehicle_door", () -> DoorDecl.LARGE_VEHICLE_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> QE_CONTAINMENT = door("qe_containment", () -> DoorDecl.QE_CONTAINMENT, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> QE_SLIDING_DOOR = door("qe_sliding_door", () -> DoorDecl.QE_SLIDING, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> ROUND_AIRLOCK_DOOR = door("round_airlock_door", () -> DoorDecl.ROUND_AIRLOCK_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> SLIDING_SEAL_DOOR = door("sliding_seal_door", () -> DoorDecl.SLIDING_SEAL_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> WATER_DOOR = door("water_door", () -> DoorDecl.WATER_DOOR, 5.0F, 50.0F);
    public static final RegistryObject<BlockDoorGeneric> CARGO_DOOR = door("cargo_door", () -> DoorDecl.CARGO_DOOR, 5.0F, 50.0F);

    /**
     * A vanilla-style mod door over the original's {@code _lower}/{@code _upper} sprites; the lower half alone
     * drops, as vanilla doors do.
     */
    private static Reg.Handle<DoorBlock> modDoor(String name, float hardness, float resistance) {
        String lower = "hbm:blocks/" + name + "_lower";
        String upper = "hbm:blocks/" + name + "_upper";
        Models.Group models = Models.group();
        for (String half : new String[] {"bottom", "top"}) {
            for (String hinge : new String[] {"left", "right"}) {
                for (String open : new String[] {"", "_open"}) {
                    String suffix = "_" + half + "_" + hinge + open;
                    models = models.and(suffix, Models.renderType("minecraft:cutout",
                            Models.parented("minecraft:block/door" + suffix, Slots.of("bottom", lower, "top", upper))));
                }
            }
        }
        Models.Group doorModels = models;
        return REG.<DoorBlock>block(name, p -> BlockSets.door(Sets.MOD_DOOR_TYPE, p),
                        p -> p.strength(hardness, resistance).sound(SoundType.METAL).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
                .model(d -> d.models(doorModels).states(doorStates()).item(Models.generated("hbm:items/" + name)))
                .loot(Loot.selfWhen("half", "lower"))
                .tag(BlockTag.PICKAXE);
    }

    public static final RegistryObject<DoorBlock> DOOR_METAL = modDoor("door_metal", 5.0F, 5.0F);
    public static final RegistryObject<DoorBlock> DOOR_OFFICE = modDoor("door_office", 10.0F, 10.0F);
    public static final RegistryObject<DoorBlock> DOOR_BUNKER = modDoor("door_bunker", 10.0F, 100.0F);
    public static final RegistryObject<DoorBlock> DOOR_RED = modDoor("door_red", 10.0F, 100.0F);

    public static final RegistryObject<TrapDoorBlock> TRAPDOOR_STEEL = REG.<TrapDoorBlock>block("trapdoor_steel",
                    p -> BlockSets.trapdoor(Sets.MOD_DOOR_TYPE, p),
                    p -> p.strength(3.0F, 8.0F).sound(SoundType.METAL).noOcclusion())
            .model(d -> d.models(Models.group()
                            .and("_bottom", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/template_trapdoor_bottom", Slots.of("texture", "hbm:blocks/trapdoor_steel"))))
                            .and("_top", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/template_trapdoor_top", Slots.of("texture", "hbm:blocks/trapdoor_steel"))))
                            .and("_open", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/template_trapdoor_open", Slots.of("texture", "hbm:blocks/trapdoor_steel")))))
                    .states(trapdoorStates())
                    .item(Models.blockItem("hbm:block/trapdoor_steel_bottom")))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    // --- items ---

    private static RegistryObject<BlockItem> item(HbmTab tab, String name, RegistryObject<? extends Block> block) {
        return REG.blockItem(block).tab(tab);
    }

    private static RegistryObject<BlockItem> item(HbmTab tab, String name, RegistryObject<? extends Block> block,
                                                 Function<net.minecraft.world.item.Item.Properties, net.minecraft.world.item.Item.Properties> props) {
        return REG.blockItem(block, props::apply).tab(tab);
    }

    public static final RegistryObject<BlockItem> VAULT_DOOR_ITEM = item(HbmTab.MACHINES, "vault_door", VAULT_DOOR);
    public static final RegistryObject<BlockItem> BLAST_DOOR_ITEM = item(HbmTab.MACHINES, "blast_door", BLAST_DOOR);
    public static final RegistryObject<BlockItem> SLIDING_BLAST_DOOR_ITEM = item(HbmTab.MACHINES, "sliding_blast_door", SLIDING_BLAST_DOOR);
    public static final RegistryObject<BlockItem> FIRE_DOOR_ITEM = item(HbmTab.MACHINES, "fire_door", FIRE_DOOR);
    public static final RegistryObject<BlockItem> TRANSITION_SEAL_ITEM = item(HbmTab.MACHINES, "transition_seal", TRANSITION_SEAL);
    public static final RegistryObject<BlockItem> SILO_HATCH_ITEM = item(HbmTab.MACHINES, "silo_hatch", SILO_HATCH);
    public static final RegistryObject<BlockItem> SILO_HATCH_LARGE_ITEM = item(HbmTab.MACHINES, "silo_hatch_large", SILO_HATCH_LARGE);
    public static final RegistryObject<BlockItem> SECURE_ACCESS_DOOR_ITEM = item(HbmTab.MACHINES, "secure_access_door", SECURE_ACCESS_DOOR);
    public static final RegistryObject<BlockItem> LARGE_VEHICLE_DOOR_ITEM = item(HbmTab.MACHINES, "large_vehicle_door", LARGE_VEHICLE_DOOR);
    public static final RegistryObject<BlockItem> QE_CONTAINMENT_ITEM = item(HbmTab.MACHINES, "qe_containment", QE_CONTAINMENT);
    public static final RegistryObject<BlockItem> QE_SLIDING_DOOR_ITEM = item(HbmTab.MACHINES, "qe_sliding_door", QE_SLIDING_DOOR);
    public static final RegistryObject<BlockItem> ROUND_AIRLOCK_DOOR_ITEM = item(HbmTab.MACHINES, "round_airlock_door", ROUND_AIRLOCK_DOOR);
    public static final RegistryObject<BlockItem> SLIDING_SEAL_DOOR_ITEM = item(HbmTab.MACHINES, "sliding_seal_door", SLIDING_SEAL_DOOR);
    public static final RegistryObject<BlockItem> WATER_DOOR_ITEM = item(HbmTab.MACHINES, "water_door", WATER_DOOR);
    public static final RegistryObject<BlockItem> CARGO_DOOR_ITEM = item(HbmTab.MACHINES, "cargo_door", CARGO_DOOR);
    /** The original {@code ItemModDoor}s: one-per-stack door items, on the redstone tab (CONTROL here). */
    public static final RegistryObject<BlockItem> DOOR_METAL_ITEM = item(HbmTab.BLOCKS, "door_metal", DOOR_METAL, p -> p.stacksTo(1));
    public static final RegistryObject<BlockItem> DOOR_OFFICE_ITEM = item(HbmTab.BLOCKS, "door_office", DOOR_OFFICE, p -> p.stacksTo(1));
    public static final RegistryObject<BlockItem> DOOR_BUNKER_ITEM = item(HbmTab.BLOCKS, "door_bunker", DOOR_BUNKER, p -> p.stacksTo(1));
    /** 1.7 ModItems line 4319: {@code setCreativeTab(null)}. */
    public static final RegistryObject<BlockItem> DOOR_RED_ITEM = REG.blockItem(DOOR_RED, p -> p.stacksTo(1));
    public static final RegistryObject<BlockItem> TRAPDOOR_STEEL_ITEM = item(HbmTab.BLOCKS, "trapdoor_steel", TRAPDOOR_STEEL);

    // --- blockstate tables ---

    /** Vanilla's door blockstate: closed doors turn with the facing, open leaves swing a quarter turn either way. */
    private static Dispatch doorStates() {
        Map<String, List<Placement>> placements = new LinkedHashMap<>();
        String[] facings = {"east", "south", "west", "north"};
        for (int i = 0; i < facings.length; i++) {
            String facing = facings[i];
            int closed = i * 90;
            for (String half : new String[] {"lower", "upper"}) {
                String model = half.equals("lower") ? "_bottom" : "_top";
                placements.put(key(facing, half, "left", "false"), List.of(new Placement(model + "_left", 0, closed, false)));
                placements.put(key(facing, half, "left", "true"), List.of(new Placement(model + "_left_open", 0, (closed + 90) % 360, false)));
                placements.put(key(facing, half, "right", "false"), List.of(new Placement(model + "_right", 0, closed, false)));
                placements.put(key(facing, half, "right", "true"), List.of(new Placement(model + "_right_open", 0, (closed + 270) % 360, false)));
            }
        }
        Set<String> properties = new LinkedHashSet<>(List.of("facing", "half", "hinge", "open"));
        return Variants.of(properties, placements);
    }

    private static String key(String facing, String half, String hinge, String open) {
        return "facing=" + facing + ",half=" + half + ",hinge=" + hinge + ",open=" + open;
    }

    /** Vanilla's trapdoor blockstate. */
    private static Dispatch trapdoorStates() {
        Map<String, List<Placement>> placements = new LinkedHashMap<>();
        String[] facings = {"north", "east", "south", "west"};
        for (int i = 0; i < facings.length; i++) {
            String facing = facings[i];
            int y = i * 90;
            placements.put("facing=" + facing + ",half=bottom,open=false", List.of(new Placement("_bottom", 0, y, false)));
            placements.put("facing=" + facing + ",half=top,open=false", List.of(new Placement("_top", 0, y, false)));
            placements.put("facing=" + facing + ",half=bottom,open=true", List.of(new Placement("_open", 0, y, false)));
            placements.put("facing=" + facing + ",half=top,open=true", List.of(new Placement("_open", 180, (y + 180) % 360, false)));
        }
        Set<String> properties = new LinkedHashSet<>(List.of("facing", "half", "open"));
        return Variants.of(properties, placements);
    }

    public static void init() {}
}
