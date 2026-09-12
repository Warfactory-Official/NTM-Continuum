package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.machine.TileEntityMachineRTG;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Feature-local block-entity registration for the grey RTG. */
public final class ModRtgBlockEntities {
    private ModRtgBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityMachineRTG>> MACHINE_RTG =
            BLOCK_ENTITIES.register("machine_rtg_grey", () -> BlockEntities.serverTicking(TileEntityMachineRTG::new, TileEntityMachineRTG::serverTick, ModMachineBlocks.machine_rtg_grey.get()));

    public static void init() {}
}
