package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.tileentity.machine.TileEntityForceField;
import com.hbm.tileentity.machine.TileEntityHatch;
import com.hbm.tileentity.machine.TileEntityMachineRadarLarge;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineRadarScreen;
import com.hbm.tileentity.machine.TileEntityMachineTeleporter;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.machine.storage.TileEntityMachineOrbus;
import com.hbm.tileentity.machine.storage.TileEntitySoyuzCapsule;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block-entity types for the radar and utility machines. */
public final class ModRadarUtilityBlockEntities {

    private ModRadarUtilityBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityMachineRadarNT>> RADAR =
            BLOCK_ENTITIES.register("machine_radar", () -> BlockEntities.ticking(TileEntityMachineRadarNT::new,
                    TileEntityMachineRadarNT::clientTick, TileEntityMachineRadarNT::serverTick,
                    ModRadarUtilityBlocks.machine_radar.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineRadarLarge>> RADAR_LARGE =
            BLOCK_ENTITIES.register("machine_radar_large", () -> BlockEntities.ticking(TileEntityMachineRadarLarge::new,
                    TileEntityMachineRadarNT::clientTick, TileEntityMachineRadarNT::serverTick,
                    ModRadarUtilityBlocks.machine_radar_large.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineRadarScreen>> RADAR_SCREEN =
            BLOCK_ENTITIES.register("radar_screen", () -> BlockEntities.serverTicking(TileEntityMachineRadarScreen::new,
                    TileEntityMachineRadarScreen::serverTick, ModRadarUtilityBlocks.radar_screen.get()));
    public static final RegistryObject<BlockEntityType<TileEntityForceField>> FORCEFIELD =
            BLOCK_ENTITIES.register("machine_forcefield", () -> BlockEntities.ticking(TileEntityForceField::new,
                    TileEntityForceField::tick, TileEntityForceField::tick, ModRadarUtilityBlocks.machine_forcefield.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineTeleporter>> TELEPORTER =
            BLOCK_ENTITIES.register("machine_teleporter", () -> BlockEntities.ticking(TileEntityMachineTeleporter::new,
                    TileEntityMachineTeleporter::clientTick, TileEntityMachineTeleporter::serverTick,
                    ModRadarUtilityBlocks.machine_teleporter.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineOrbus>> ORBUS =
            BLOCK_ENTITIES.register("machine_orbus", () -> BlockEntities.serverTicking(TileEntityMachineOrbus::new,
                    TileEntityBarrel::serverTick, ModRadarUtilityBlocks.machine_orbus.get()));
    public static final RegistryObject<BlockEntityType<TileEntityHatch>> HATCH =
            BLOCK_ENTITIES.register("seal_hatch", () -> BlockEntities.serverTicking(TileEntityHatch::new,
                    TileEntityHatch::serverTick, ModRadarUtilityBlocks.seal_hatch.get()));
    public static final RegistryObject<BlockEntityType<TileEntitySoyuzCapsule>> SOYUZ_CAPSULE =
            BLOCK_ENTITIES.register("soyuz_capsule", () -> BlockEntities.type(TileEntitySoyuzCapsule::new,
                    ModRadarUtilityBlocks.soyuz_capsule.get()));

    public static void init() {}
}
