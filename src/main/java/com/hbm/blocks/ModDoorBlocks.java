package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
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

import java.util.function.Function;
import com.hbm.blocks.generic.BlockDoorGeneric;
import com.hbm.blocks.machine.BlastDoor;
import com.hbm.blocks.machine.DummyBlockBlast;
import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.DoorDecl;

/**
 * Blocks and block items of the door family: the fourteen {@link BlockDoorGeneric} doors, the old
 * six-high {@link BlastDoor} with its leaf dummy, the four vanilla-style mod doors and the steel
 * trapdoor. Hardness/resistance pairs are the original {@code ModBlocks} values.
 */
public final class ModDoorBlocks {
    private ModDoorBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /**
     * The mod doors' set: metal step sounds and the original {@code hbm:block.openDoor} on both
     * open and close (the original played the one sound for either direction).
     */
    public static final BlockSetType MOD_DOOR_TYPE = BlockSets.type("hbm_mod_door", SoundType.METAL,
            ModDoorSounds.OPEN_DOOR_EVENT, ModDoorSounds.OPEN_DOOR_EVENT,
            SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.WOODEN_DOOR_CLOSE);

    private static BlockBehaviour.Properties dummyable(BlockBehaviour.Properties p) {
        return p.isSuffocating((state, world, pos) -> false).isViewBlocking((state, world, pos) -> false);
    }

    /**
     * A generic door's outline and collision come from its core (the 1.7 {@code getBoundingBox} looked the
     * core up in the world every call), so its shapes must not be baked into vanilla's per-state cache at
     * registry freeze: {@code dynamicShape()} keeps {@code BlockState.getCollisionShape} asking the block.
     */
    private static RegistryObject<BlockDoorGeneric> door(String name, DoorDecl decl, float hardness, float resistance) {
        return BLOCKS.registerBlock(name, p -> new BlockDoorGeneric(p, decl),
                p -> dummyable(p.strength(hardness, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion().dynamicShape()));
    }

    public static final RegistryObject<BlockDoorGeneric> VAULT_DOOR = door("vault_door", DoorDecl.VAULT_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlastDoor> BLAST_DOOR = BLOCKS.registerBlock("blast_door", BlastDoor::new,
            p -> dummyable(p.strength(10.0F, 1_000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<DummyBlockBlast> DUMMY_BLOCK_BLAST = BLOCKS.registerBlock("dummy_block_blast", DummyBlockBlast::new,
            p -> dummyable(p.strength(10.0F, 10_000.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<BlockDoorGeneric> SLIDING_BLAST_DOOR = door("sliding_blast_door", DoorDecl.SLIDE_DOOR, 10.0F, 750.0F);
    public static final RegistryObject<BlockDoorGeneric> FIRE_DOOR = door("fire_door", DoorDecl.FIRE_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> TRANSITION_SEAL = door("transition_seal", DoorDecl.TRANSITION_SEAL, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> SILO_HATCH = door("silo_hatch", DoorDecl.SILO_HATCH, 10.0F, 100.0F);
    public static final RegistryObject<BlockDoorGeneric> SILO_HATCH_LARGE = door("silo_hatch_large", DoorDecl.SILO_HATCH_LARGE, 10.0F, 100.0F);
    public static final RegistryObject<BlockDoorGeneric> SECURE_ACCESS_DOOR = door("secure_access_door", DoorDecl.SECURE_ACCESS_DOOR, 20.0F, 2_000.0F);
    public static final RegistryObject<BlockDoorGeneric> LARGE_VEHICLE_DOOR = door("large_vehicle_door", DoorDecl.LARGE_VEHICLE_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> QE_CONTAINMENT = door("qe_containment", DoorDecl.QE_CONTAINMENT, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> QE_SLIDING_DOOR = door("qe_sliding_door", DoorDecl.QE_SLIDING, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> ROUND_AIRLOCK_DOOR = door("round_airlock_door", DoorDecl.ROUND_AIRLOCK_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> SLIDING_SEAL_DOOR = door("sliding_seal_door", DoorDecl.SLIDING_SEAL_DOOR, 10.0F, 1_000.0F);
    public static final RegistryObject<BlockDoorGeneric> WATER_DOOR = door("water_door", DoorDecl.WATER_DOOR, 5.0F, 50.0F);
    public static final RegistryObject<BlockDoorGeneric> CARGO_DOOR = door("cargo_door", DoorDecl.CARGO_DOOR, 5.0F, 50.0F);

    private static RegistryObject<DoorBlock> modDoor(String name, float hardness, float resistance) {
        return BLOCKS.registerBlock(name, p -> BlockSets.door(MOD_DOOR_TYPE, p),
                p -> p.strength(hardness, resistance).sound(SoundType.METAL).noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
    }

    public static final RegistryObject<DoorBlock> DOOR_METAL = modDoor("door_metal", 5.0F, 5.0F);
    public static final RegistryObject<DoorBlock> DOOR_OFFICE = modDoor("door_office", 10.0F, 10.0F);
    public static final RegistryObject<DoorBlock> DOOR_BUNKER = modDoor("door_bunker", 10.0F, 100.0F);
    public static final RegistryObject<DoorBlock> DOOR_RED = modDoor("door_red", 10.0F, 100.0F);

    public static final RegistryObject<TrapDoorBlock> TRAPDOOR_STEEL = BLOCKS.registerBlock("trapdoor_steel",
            p -> BlockSets.trapdoor(MOD_DOOR_TYPE, p),
            p -> p.strength(3.0F, 8.0F).sound(SoundType.METAL).noOcclusion());

    // --- items ---

    private static RegistryObject<BlockItem> item(HbmTab tab, String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block);
        ModTabs.addTo(tab, item);
        return item;
    }

    private static RegistryObject<BlockItem> item(HbmTab tab, String name, RegistryObject<? extends Block> block,
                                                 Function<net.minecraft.world.item.Item.Properties, net.minecraft.world.item.Item.Properties> props) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block, props::apply);
        ModTabs.addTo(tab, item);
        return item;
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
    public static final RegistryObject<BlockItem> DOOR_RED_ITEM = ITEMS.registerBlockItem("door_red", DOOR_RED, p -> p.stacksTo(1));
    public static final RegistryObject<BlockItem> TRAPDOOR_STEEL_ITEM = item(HbmTab.BLOCKS, "trapdoor_steel", TRAPDOOR_STEEL);

    public static void init() {}
}
