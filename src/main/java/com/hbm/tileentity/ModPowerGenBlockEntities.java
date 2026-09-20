package com.hbm.tileentity;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModPowerGenBlocks;
import com.hbm.tileentity.machine.TileEntityChungus;
import com.hbm.tileentity.machine.TileEntityMachineHephaestus;
import com.hbm.tileentity.machine.TileEntityMachineIGenerator;
import com.hbm.tileentity.machine.TileEntityMachineLPW2;
import com.hbm.tileentity.machine.TileEntityMachineSiren;

/** Feature-local block-entity types for the large power-generation machines. */
public final class ModPowerGenBlockEntities {
    private ModPowerGenBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityChungus>> CHUNGUS =
            BLOCK_ENTITIES.register("machine_chungus", () -> BlockEntities.ticking(
                    TileEntityChungus::new, TileEntityChungus::clientTick, TileEntityChungus::serverTick,
                    ModPowerGenBlocks.MACHINE_CHUNGUS.get()));
    /** 1.7: a bare {@code TileEntity} whose only job is a render bounding box; it never ticks. */
    public static final RegistryObject<BlockEntityType<TileEntityMachineLPW2>> LPW2 =
            BLOCK_ENTITIES.register("machine_lpw2", () -> BlockEntities.type(
                    TileEntityMachineLPW2::new, ModPowerGenBlocks.MACHINE_LPW2.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineHephaestus>> HEPHAESTUS =
            BLOCK_ENTITIES.register("machine_hephaestus", () -> BlockEntities.ticking(
                    TileEntityMachineHephaestus::new, TileEntityMachineHephaestus::clientTick,
                    TileEntityMachineHephaestus::serverTick, ModPowerGenBlocks.MACHINE_HEPHAESTUS.get()));
    /** 1.7: the whole of {@code updateEntity} is commented out ("o7"); the memorial never ticks. */
    public static final RegistryObject<BlockEntityType<TileEntityMachineIGenerator>> INDUSTRIAL_GENERATOR =
            BLOCK_ENTITIES.register("machine_industrial_generator", () -> BlockEntities.type(
                    TileEntityMachineIGenerator::new, ModPowerGenBlocks.MACHINE_INDUSTRIAL_GENERATOR.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineSiren>> SIREN =
            BLOCK_ENTITIES.register("machine_siren", () -> BlockEntities.serverTicking(
                    TileEntityMachineSiren::new, TileEntityMachineSiren::serverTick,
                    ModPowerGenBlocks.MACHINE_SIREN.get()));

    public static void init() {}
}
