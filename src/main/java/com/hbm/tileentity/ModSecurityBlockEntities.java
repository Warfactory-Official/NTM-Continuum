package com.hbm.tileentity;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModSecurityBlocks;
import com.hbm.client.render.SecurityObjRenderer;
import com.hbm.tileentity.machine.TileEntityMachineKeyForge;

/** Feature-local block-entity types: the locksmith table and the renderer-only OBJ block entity. */
public final class ModSecurityBlockEntities {
    private ModSecurityBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityMachineKeyForge>> MACHINE_KEYFORGE =
            BLOCK_ENTITIES.register("machine_keyforge", () -> BlockEntities.serverTicking(
                    TileEntityMachineKeyForge::new, TileEntityMachineKeyForge::serverTick,
                    ModSecurityBlocks.machine_keyforge.get()));

    /** Barbed wire (all six) and spikes: no state, exists so {@code SecurityObjRenderer} runs. */
    public static final RegistryObject<BlockEntityType<TileEntitySecurityObj>> SECURITY_OBJ =
            BLOCK_ENTITIES.register("security_obj", () -> BlockEntities.type(TileEntitySecurityObj::new,
                    java.util.stream.Stream.concat(ModSecurityBlocks.BARBED_WIRES.stream().map(RegistryObject::get),
                            java.util.stream.Stream.of(ModSecurityBlocks.spikes.get())).toArray(Block[]::new)));

    public static void init() {}
}
