package com.hbm.tileentity;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.tileentity.machine.TileEntityBlastDoor;
import com.hbm.tileentity.machine.TileEntityBlastDoorDummy;

/** Block-entity types of the door family. */
public final class ModDoorBlockEntities {
    private ModDoorBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    /** One type over every generic door block, as the original had one tile class for all of them. */
    public static final RegistryObject<BlockEntityType<TileEntityDoorGeneric>> DOOR_GENERIC =
            BLOCK_ENTITIES.register("door_generic", () -> BlockEntities.serverTicking(
                    TileEntityDoorGeneric::new,
                    (level, pos, state, be) -> be.serverTick(),
                    new Block[] {
                            ModDoorBlocks.VAULT_DOOR.get(), ModDoorBlocks.SLIDING_BLAST_DOOR.get(),
                            ModDoorBlocks.FIRE_DOOR.get(), ModDoorBlocks.TRANSITION_SEAL.get(),
                            ModDoorBlocks.SILO_HATCH.get(), ModDoorBlocks.SILO_HATCH_LARGE.get(),
                            ModDoorBlocks.SECURE_ACCESS_DOOR.get(), ModDoorBlocks.LARGE_VEHICLE_DOOR.get(),
                            ModDoorBlocks.QE_CONTAINMENT.get(), ModDoorBlocks.QE_SLIDING_DOOR.get(),
                            ModDoorBlocks.ROUND_AIRLOCK_DOOR.get(), ModDoorBlocks.SLIDING_SEAL_DOOR.get(),
                            ModDoorBlocks.WATER_DOOR.get(), ModDoorBlocks.CARGO_DOOR.get() }));

    public static final RegistryObject<BlockEntityType<TileEntityBlastDoor>> BLAST_DOOR =
            BLOCK_ENTITIES.register("blast_door", () -> BlockEntities.serverTicking(
                    TileEntityBlastDoor::new,
                    (level, pos, state, be) -> be.serverTick(), ModDoorBlocks.BLAST_DOOR.get()));

    public static final RegistryObject<BlockEntityType<TileEntityBlastDoorDummy>> BLAST_DOOR_DUMMY =
            BLOCK_ENTITIES.register("blast_door_dummy", () -> BlockEntities.serverTicking(
                    TileEntityBlastDoorDummy::new,
                    (level, pos, state, be) -> be.serverTick(), ModDoorBlocks.DUMMY_BLOCK_BLAST.get()));

    public static void init() {}
}
