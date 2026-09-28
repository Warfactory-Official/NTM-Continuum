package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.ModSatelliteBlocks;
import com.hbm.tileentity.machine.TileEntityMachineSatDock;
import com.hbm.tileentity.machine.TileEntityMachineSatLinker;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types of the satellite family; 1.7 {@code TileMappings} "tileentity_satlinker"/"tileentity_miner_dock". */
public final class ModSatelliteBlockEntities {

    private ModSatelliteBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityMachineSatLinker>> MACHINE_SATLINKER =
            BLOCK_ENTITIES.register("machine_satlinker", () -> BlockEntities.serverTicking(
                    TileEntityMachineSatLinker::new, TileEntityMachineSatLinker::serverTick,
                    ModSatelliteBlocks.machine_satlinker.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineSatDock>> SAT_DOCK =
            BLOCK_ENTITIES.register("sat_dock", () -> BlockEntities.serverTicking(
                    TileEntityMachineSatDock::new, TileEntityMachineSatDock::serverTick,
                    ModSatelliteBlocks.sat_dock.get()));

    public static void init() {}
}
