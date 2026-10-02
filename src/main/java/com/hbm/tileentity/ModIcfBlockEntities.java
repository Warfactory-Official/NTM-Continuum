package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.registration.Reg;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.tileentity.machine.TileEntityBlockICF;
import com.hbm.tileentity.machine.TileEntityICF;
import com.hbm.tileentity.machine.TileEntityICFController;
import com.hbm.tileentity.machine.TileEntityICFPress;
import com.hbm.tileentity.machine.TileEntityICFStruct;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types of the ICF, under 1.7 {@code TileMappings}' names. */
public final class ModIcfBlockEntities {

    private ModIcfBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityICF>> ICF =
            REG.blockEntity("tileentity_icf", () -> BlockEntities.serverTicking(
                    TileEntityICF::new, TileEntityICF::serverTick, ModIcfBlocks.icf.get()));
    public static final RegistryObject<BlockEntityType<TileEntityICFController>> ICF_CONTROLLER =
            REG.blockEntity("tileentity_icf_controller", () -> BlockEntities.ticking(
                    TileEntityICFController::new, TileEntityICFController::clientTick,
                    TileEntityICFController::serverTick, ModIcfBlocks.icf_controller.get()));
    public static final RegistryObject<BlockEntityType<TileEntityICFPress>> ICF_PRESS =
            REG.blockEntity("tileentity_icf_press", () -> BlockEntities.serverTicking(
                    TileEntityICFPress::new, TileEntityICFPress::serverTick, ModIcfBlocks.machine_icf_press.get()));
    public static final RegistryObject<BlockEntityType<TileEntityICFStruct>> ICF_STRUCT =
            REG.blockEntity("tileentity_icf_struct", () -> BlockEntities.serverTicking(
                    TileEntityICFStruct::new, TileEntityICFStruct::serverTick, ModIcfBlocks.struct_icf_core.get()));
    public static final RegistryObject<BlockEntityType<TileEntityBlockICF>> ICF_BLOCK =
            REG.blockEntity("tileentity_block_icf", () -> BlockEntities.serverTicking(
                    TileEntityBlockICF::new, TileEntityBlockICF::serverTick, ModIcfBlocks.icf_block.get()));

    public static void init() {}
}
