package com.hbm.tileentity;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.tileentity.network.TileEntityCraneBoxer;
import com.hbm.tileentity.network.TileEntityCraneExtractor;
import com.hbm.tileentity.network.TileEntityCraneGrabber;
import com.hbm.tileentity.network.TileEntityCraneInserter;
import com.hbm.tileentity.network.TileEntityCranePartitioner;
import com.hbm.tileentity.network.TileEntityCraneRouter;
import com.hbm.tileentity.network.TileEntityCraneSplitter;
import com.hbm.tileentity.network.TileEntityCraneUnboxer;
import com.hbm.tileentity.network.TileEntityPistonInserter;

/** Feature-local block-entity types for the crane family. */
public final class ModConveyorBlockEntities {

    private ModConveyorBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCK_ENTITIES);

    public static final RegistryObject<BlockEntityType<TileEntityCraneInserter>> CRANE_INSERTER =
            REG.blockEntity("crane_inserter", () -> BlockEntities.serverTicking(
                    TileEntityCraneInserter::new, (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.CRANE_INSERTER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCraneExtractor>> CRANE_EXTRACTOR =
            REG.blockEntity("crane_extractor", () -> BlockEntities.serverTicking(
                    TileEntityCraneExtractor::new, (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.CRANE_EXTRACTOR.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCraneGrabber>> CRANE_GRABBER =
            REG.blockEntity("crane_grabber", () -> BlockEntities.serverTicking(
                    TileEntityCraneGrabber::new, (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.CRANE_GRABBER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCraneRouter>> CRANE_ROUTER =
            REG.blockEntity("crane_router", () -> BlockEntities.serverTicking(
                    TileEntityCraneRouter::new, (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.CRANE_ROUTER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCraneBoxer>> CRANE_BOXER =
            REG.blockEntity("crane_boxer", () -> BlockEntities.serverTicking(
                    TileEntityCraneBoxer::new, (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.CRANE_BOXER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCraneUnboxer>> CRANE_UNBOXER =
            REG.blockEntity("crane_unboxer", () -> BlockEntities.serverTicking(
                    TileEntityCraneUnboxer::new, (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.CRANE_UNBOXER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCraneSplitter>> CRANE_SPLITTER =
            REG.blockEntity("crane_splitter", () -> BlockEntities.serverTicking(
                    TileEntityCraneSplitter::new, (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.CRANE_SPLITTER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityCranePartitioner>> CRANE_PARTITIONER =
            REG.blockEntity("crane_partitioner", () -> BlockEntities.serverTicking(
                    TileEntityCranePartitioner::new, (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.CRANE_PARTITIONER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityPistonInserter>> PISTON_INSERTER =
            REG.blockEntity("piston_inserter", () -> BlockEntities.ticking(
                    TileEntityPistonInserter::new, (level, pos, state, be) -> be.clientTick(),
                    (level, pos, state, be) -> be.serverTick(), ModConveyorBlocks.PISTON_INSERTER.get()));

    public static void init() {}
}
