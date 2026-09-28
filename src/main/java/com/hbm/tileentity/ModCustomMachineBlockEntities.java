package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.tileentity.machine.TileEntityCMPort;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types of the custom machine system: the controller (1.7 {@code tileentity_custom_machine}) and the port proxy. */
public final class ModCustomMachineBlockEntities {

    private ModCustomMachineBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityCustomMachine>> CUSTOM_MACHINE =
            BLOCK_ENTITIES.register("custom_machine", () -> BlockEntities.serverTicking(TileEntityCustomMachine::new,
                    TileEntityCustomMachine::serverTick, ModCustomMachineBlocks.custom_machine.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCMPort>> CM_PORT =
            BLOCK_ENTITIES.register("cm_port", () -> BlockEntities.type(TileEntityCMPort::new,
                    ModCustomMachineBlocks.cm_port.stream().map(RegistryObject::get).toArray(Block[]::new)));

    public static void init() {}
}
