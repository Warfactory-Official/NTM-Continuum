package com.hbm.tileentity;

import com.hbm.Constants;
import com.hbm.tileentity.machine.pile.TileEntityPileBreedingFuel;
import com.hbm.tileentity.machine.pile.TileEntityPileFuel;
import com.hbm.tileentity.machine.pile.TileEntityPileNeutronDetector;
import com.hbm.tileentity.machine.pile.TileEntityPileSource;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.tileentity.machine.TileEntityMachineRadiolysis;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;
import com.hbm.tileentity.machine.TileEntityReactorControl;
import com.hbm.tileentity.machine.TileEntityReactorResearch;

/** Feature-local block-entity types for the miscellaneous nuclear machines and the legacy pile. */
public final class ModNuclearMiscBlockEntities {
    private ModNuclearMiscBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityMachineRadiolysis>> RADIOLYSIS =
            BLOCK_ENTITIES.register("machine_radiolysis", () -> BlockEntities.serverTicking(
                    TileEntityMachineRadiolysis::new, TileEntityMachineRadiolysis::serverTick,
                    ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get()));
    public static final RegistryObject<BlockEntityType<TileEntityMachineReactorBreeding>> REACTOR_BREEDING =
            BLOCK_ENTITIES.register("machine_reactor", () -> BlockEntities.serverTicking(
                    TileEntityMachineReactorBreeding::new, TileEntityMachineReactorBreeding::serverTick,
                    ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get()));
    public static final RegistryObject<BlockEntityType<TileEntityReactorResearch>> REACTOR_RESEARCH =
            BLOCK_ENTITIES.register("machine_reactor_small", () -> BlockEntities.ticking(
                    TileEntityReactorResearch::new, TileEntityReactorResearch::clientTick,
                    TileEntityReactorResearch::serverTick, ModNuclearMiscBlocks.REACTOR_RESEARCH.get()));
    public static final RegistryObject<BlockEntityType<TileEntityReactorControl>> REACTOR_CONTROL =
            BLOCK_ENTITIES.register("machine_controller", () -> BlockEntities.serverTicking(
                    TileEntityReactorControl::new, TileEntityReactorControl::serverTick,
                    ModNuclearMiscBlocks.MACHINE_CONTROLLER.get()));

    public static final RegistryObject<BlockEntityType<TileEntityPileFuel>> PILE_FUEL =
            BLOCK_ENTITIES.register("pile_fuel", () -> BlockEntities.serverTicking(
                    TileEntityPileFuel::new, TileEntityPileFuel::serverTick,
                    ModNuclearMiscBlocks.BLOCK_GRAPHITE_FUEL.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPileBreedingFuel>> PILE_BREEDING_FUEL =
            BLOCK_ENTITIES.register("pile_breeding_fuel", () -> BlockEntities.serverTicking(
                    TileEntityPileBreedingFuel::new, TileEntityPileBreedingFuel::serverTick,
                    ModNuclearMiscBlocks.BLOCK_GRAPHITE_LITHIUM.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPileSource>> PILE_SOURCE =
            BLOCK_ENTITIES.register("pile_source", () -> BlockEntities.serverTicking(
                    TileEntityPileSource::new, TileEntityPileSource::serverTick,
                    ModNuclearMiscBlocks.BLOCK_GRAPHITE_SOURCE.get(), ModNuclearMiscBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPileNeutronDetector>> PILE_NEUTRON_DETECTOR =
            BLOCK_ENTITIES.register("pile_neutron_detector", () -> BlockEntities.serverTicking(
                    TileEntityPileNeutronDetector::new, TileEntityPileNeutronDetector::serverTick,
                    ModNuclearMiscBlocks.BLOCK_GRAPHITE_DETECTOR.get()));

    public static void init() {}
}
