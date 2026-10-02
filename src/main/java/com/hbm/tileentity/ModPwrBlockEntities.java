package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.tileentity.machine.TileEntityBlockPWR;
import com.hbm.tileentity.machine.TileEntityPWRController;

/** Feature-local block-entity types for the PWR: the controller and the assembled-shell cell. */
public final class ModPwrBlockEntities {
    private ModPwrBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityPWRController>> PWR_CONTROLLER =
            REG.blockEntity("tileentity_pwr_controller", () -> BlockEntities.ticking(
                    TileEntityPWRController::new,
                    TileEntityPWRController::clientTick,
                    TileEntityPWRController::serverTick,
                    ModPwrBlocks.PWR_CONTROLLER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityBlockPWR>> PWR_BLOCK =
            REG.blockEntity("tileentity_block_pwr", () -> BlockEntities.serverTicking(
                    TileEntityBlockPWR::new,
                    TileEntityBlockPWR::serverTick,
                    ModPwrBlocks.PWR_BLOCK.get()));

    public static void init() {}
}
