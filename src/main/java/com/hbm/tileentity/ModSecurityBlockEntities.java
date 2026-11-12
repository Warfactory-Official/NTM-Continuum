package com.hbm.tileentity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModSecurityBlocks;
import com.hbm.tileentity.machine.TileEntityMachineKeyForge;

/** Feature-local block-entity types: the locksmith table. */
public final class ModSecurityBlockEntities {
    private ModSecurityBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityMachineKeyForge>> MACHINE_KEYFORGE =
            REG.blockEntity("machine_keyforge", () -> BlockEntities.serverTicking(
                    TileEntityMachineKeyForge::new, TileEntityMachineKeyForge::serverTick,
                    ModSecurityBlocks.machine_keyforge.get()));

    public static void init() {}
}
